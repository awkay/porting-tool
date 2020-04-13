(ns com.fulcrologic.porting.parsing.form-traversal
  (:require
    [com.fulcrologic.porting.parsing.util :as util :refer [find-maplike-vals clear-raw-syms]]
    [com.fulcrologic.porting.specs :as pspec]
    [ghostwheel.core :refer [>defn =>]]
    [com.fulcrologic.porting.rewrite-clj.zip :as z]
    [com.fulcrologic.porting.rewrite-clj.node :as node]
    [clojure.spec.alpha :as s]
    [clojure.set :as set]
    [taoensso.timbre :as log])
  (:import (clojure.lang ReaderConditional)))

(>defn current-loc
  "Returns the current loc of the current processing position."
  [env]
  [::pspec/processing-env => ::pspec/zloc]
  (:zloc env))

(>defn current-node
  "Returns the current parsed node for the current processing position."
  [env]
  [::pspec/processing-env => any?]
  (z/node (current-loc env)))

(declare current-form)


(declare process-form)

(defn process-sequence
  "Assuming that env is on a sequence of things, walk that sequence calling process-form for each element."
  [env]
  [::pspec/processing-env => ::pspec/processing-env]
  (let [loc      (current-loc env)
        top-form (current-form env)]
    (if (and (or
               (map? top-form)
               (vector? top-form)
               (list? top-form)
               (set? top-form))
          (seq top-form))
      (loop [e env motion z/down]
        (if-let [new-loc (motion (current-loc e))]
          (recur (process-form (assoc e :zloc new-loc)) z/right)
          (update e :zloc z/up)))
      env)))

(defn process-let [env]
  [::pspec/processing-env => ::pspec/processing-env]
  (let [let-like-form   (current-form env)
        bindings        (partition 2 (second let-like-form))
        destructurings  (into #{} (map first) bindings)
        hazardous-uses  (into #{} (comp
                                    (map second)
                                    (filter simple-symbol?)) bindings)
        aliased-symbols (util/raw-globals env)
        local-syms      (util/all-symbol-names destructurings)
        cleared-env     (clear-raw-syms env local-syms)
        rebound-globals (set/intersection local-syms aliased-symbols)]
    (when-let [problems (seq (set/intersection hazardous-uses aliased-symbols))]
      (util/report-warning! (str "The global aliased symbol(s) " (set problems)
                              " are bound AND used as values in a let. This can lead to shadowing issues.\n\n"
                              "FIX: Refactor your the code to use "
                              "qualified symbols (i.e. :as in :require).") let-like-form))
    (when-let [problems (seq rebound-globals)]
      (util/report-warning! (str "The global aliased symbol(s) " (set problems)
                              " are bound in the namespace AND the let.\n\n"
                              "FIX: Refactor your the code to use "
                              "qualified symbols (i.e. :as in :require).") let-like-form))
    (let [updated-loc (:zloc (process-sequence cleared-env))]
      (assoc env :zloc updated-loc))))

(>defn process-defn [env]
  [::pspec/processing-env => ::pspec/processing-env]
  (let [defn-like-form  (current-form env)
        args            (rest defn-like-form)
        arg-symbols     (util/all-symbol-names args)
        aliased-symbols (util/raw-globals env)
        cleared-env     (clear-raw-syms env arg-symbols)
        rebound-globals (set/intersection arg-symbols aliased-symbols)]
    (when (seq rebound-globals)
      (util/report-warning!
        (str "Function creates symbols in its args that have been aliased to simple symbol(s) in the namespace: " rebound-globals
          " This can cause incorrect rewrites.\n\n"
          "FIX: Add aliases for the affected namespaces and use qualified versions of the globally aliased thing.")
        defn-like-form))
    (let [updated-loc (:zloc (process-sequence cleared-env))]
      (assoc env :zloc updated-loc))))

(def known-let-forms #{'let 'if-let 'when-let 'binding 'taoensso.encore/if-let 'taoensso.encore/when-let})

(def known-defn-forms #{'defn 'defn- 'ghostwheel.core/>defn 'ghostwheel.core/>defn-})

(>defn process-list [env]
  [::pspec/processing-env => any?]
  (let [possible-function-name (first (current-form env))
        function-name          (cond->> possible-function-name
                                 (symbol? possible-function-name) (util/sym->fqsym env))
        env                    (update env ::path (fnil conj (list)) function-name)
        feature                (:feature-context env)
        {:keys [let-forms defn-forms]} (-> (get-in env [:config feature])
                                         (update :let-forms set/union known-let-forms)
                                         (update :defn-forms set/union known-defn-forms))]
    (update (cond
              (contains? let-forms function-name) (process-let env)
              (contains? defn-forms function-name) (process-defn env)
              :else (process-sequence env))
      ::path rest)))

(defn current-path
  "Returns a stack of current list contexts, with the most recent first. For example, when processing:

  ```
  (ns a
    (:require [lib]))
  ```

  The current path will start out empty. When entering the first list it will become `(ns)`. When nesting into the
  `(:require ...)` it will become `(:require ns)`, etc.
  "
  [env]
  (::path env))

(defn within
  "Returns true if the given `k` is on the current path (the path is a stack of the first element of lists that
  have been recursively entered)."
  [env k]
  (contains? (set (current-path env)) k))

(s/def ::reader-cond #(instance? ReaderConditional %))

(>defn process-reader-conditional [env]
  [::pspec/processing-env => ::pspec/processing-env]
  (let [loc        (current-loc env)
        reader-loc (z/down loc)
        reader     (z/sexpr reader-loc)]
    (if (= '? reader)
      (let [list-loc         (z/right reader-loc)
            starting-loc     (z/down list-loc)
            starting-env     (assoc env :zloc starting-loc)
            original-context (:feature-context env)
            ending-env       (if starting-loc
                               (loop [e starting-env]
                                 (let [lang-loc      (current-loc e)
                                       lang          (z/sexpr lang-loc)
                                       form-loc      (-> lang-loc (z/right))
                                       env           (assoc e :feature-context lang :zloc form-loc)
                                       {updated-zipper :zloc :as updated-env} (process-form env)
                                       next-lang-loc (z/right updated-zipper)]
                                   (if next-lang-loc
                                     (recur (assoc updated-env :zloc next-lang-loc))
                                     (-> updated-env
                                       (assoc :feature-context original-context)
                                       (update :zloc (fn [l] (-> l z/up z/up)))))))
                               env)]
        ending-env)
      env)))

(>defn process-form
  "Process (recursively) the form at the current zloc of the env, returning an env with zloc at the same position, but
  possibly containing zipper updates/rewrites for that form."
  [env]
  [::pspec/processing-env => ::pspec/processing-env]
  (let [feature    (:feature-context env)
        form       (current-form env)
        transforms (get-in env [:config feature :transforms])
        env        (reduce
                     (fn [e xform]
                       (let [apply-xform (fn [{:keys [zloc] :as env}]
                                           ;; this mess ensures the xform doesn't "move" the loc of the zipper
                                           (let [final-env (atom e)
                                                 fz        (fn [zloc]
                                                             (let [tempenv (assoc env :zloc zloc)
                                                                   new-env (xform tempenv)]
                                                               (reset! final-env new-env)
                                                               (:zloc new-env)))
                                                 zloc2     (z/edit-node zloc fz)]
                                             (assoc @final-env :zloc zloc2)))]
                         (or (apply-xform e) e)))
                     env
                     transforms)]
    (cond
      (= :reader-macro (z/tag (current-loc env)))
      (process-reader-conditional env)

      (list? form)
      (process-list env)

      (or
        (map? form)
        (set? form)
        (vector? form))
      (process-sequence env)

      :else env)))

(defn reader-cond? [loc]
  (try
    (= :reader-macro (z/tag loc))
    (catch Throwable t
      false)))

(defn reader->map [loc]
  (let [reader-loc (z/down loc)
        reader     (z/sexpr reader-loc)]
    (if (= '? reader)
      (let [list-loc     (z/right reader-loc)
            starting-loc (z/down list-loc)]
        (if starting-loc
          (loop [lang-loc starting-loc result {}]
            (let [lang          (z/sexpr lang-loc)
                  form-loc      (-> lang-loc (z/right))
                  form          (if (node/printable-only? (z/node form-loc))
                                  (z/string form-loc)
                                  (z/sexpr form-loc))
                  next-lang-loc (z/right form-loc)
                  new-result    (assoc result lang form)]
              (if next-lang-loc
                (recur next-lang-loc new-result)
                new-result)))
          {}))
      {})))

(defn loc->form*
  "Returns the form for the current position in the `env` with reader conditionals
   processed into their correct language side (or omitted). No transforms are
   applied during this conversion."
  [loc lang]
  (loop [start loc]
    (let [cond-loc (z/find-next start z/next reader-cond?)]
      (if (z/end? cond-loc)
        start
        (let [forms       (try (reader->map cond-loc) (catch Exception e
                                                        (util/report-error! "Cannot convert reader conditional to map!")
                                                        {}))
              replacement (if-let [f (get forms lang)]
                            f
                            nil)
              new-loc     (if replacement
                            (z/replace cond-loc replacement)
                            (z/remove cond-loc))]
          (recur new-loc))))))

(defn loc->form
  "Process any conditional reader macros against the current zipper location, and return the resulting
  form as it would look for `lang`, which can be :clj, :cljs.  Any non-matching lang will remove
  the conditional reader elements completely."
  [loc lang]
  (let [actual (z/edit-node loc #(loc->form* % lang))]
    (z/sexpr actual)))

(defn top [loc]
  (loop [pos loc]
    (if-let [p (z/up pos)]
      (recur p)
      pos)))

(>defn current-form
  "Returns the current form at the current processing position. If `lang` is provided reader conditionals
  will be replaced with that branch (if it exists)."
  ([env lang]
   [::pspec/processing-env keyword? => any?]
   (if (node/printable-only? (current-node env))
     (z/string (current-loc env))
     (loc->form (current-loc env) lang)))
  ([env]
   [::pspec/processing-env => any?]
   (if (node/printable-only? (current-node env))
     (z/string (current-loc env))
     (z/sexpr (current-loc env)))))

(defn replace-current-form [env new-value]
  (update env :zloc z/replace new-value))

(comment


  (let [forms-zipper (z/of-string "(ns a (:require #?(:clj 1 :cljs 2)))\n(defn f [a] #?(:clj 42 :cljs 43))")
        ns-loc       (z/leftmost forms-zipper)
        actual       (loc->form ns-loc :clj)]

    actual))
