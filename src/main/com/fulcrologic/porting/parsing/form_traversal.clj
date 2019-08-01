(ns com.fulcrologic.porting.parsing.form-traversal
  (:require
    [com.fulcrologic.porting.parsing.util :as util :refer [find-maplike-vals clear-raw-syms]]
    [com.fulcrologic.porting.specs :as pspec]
    [ghostwheel.core :refer [>defn =>]]
    [rewrite-clj.zip :as z]
    [clojure.core.specs.alpha :as specs]
    [clojure.spec.alpha :as s]
    [clojure.set :as set]
    [taoensso.timbre :as log])
  (:import (clojure.lang ReaderConditional)))

(>defn current-loc
  "Returns the current loc of the current processing position."
  [env]
  [::pspec/processing-env => ::pspec/zloc]
  (:zloc env))

(>defn current-form
  "Returns the current form at the current processing position."
  [env]
  [::pspec/processing-env => any?]
  (z/sexpr (current-loc env)))

(>defn current-node
  "Returns the current parsed node for the current processing position."
  [env]
  [::pspec/processing-env => any?]
  (z/node (current-loc env)))

(declare process-form)

#_(defn process-let [env l]
    [::pspec/processing-env ::pspec/let-like => ::let-like]
    (let [bindings           (second l)
          hazardous-bindings (into #{} (comp
                                         (map second)
                                         (filter simple-symbol?))
                               (partition 2 bindings))
          aliased-symbols    (util/raw-globals env)
          parsed-bindings    (s/conform ::specs/bindings bindings)
          local-syms         (util/all-syms parsed-bindings)
          env                (clear-raw-syms env local-syms)
          rebound-globals    (set/intersection hazardous-bindings aliased-symbols)]
      (when-let [problems (seq rebound-globals)]
        (util/report-warning! (str "The global aliased symbol(s) " (set problems)
                                " are bound AND used as values in the same let.\n\n"
                                "FIX: Refactor your the code to use only "
                                "qualified symbols from other namespaces in the values of the bindings.") l))
      (apply list (map #(process-form env %) l))))

#_(>defn process-defn [env l]
    [::pspec/processing-env ::pspec/defn-like => ::pspec/defn-like]
    (let [args            (rest l)
          syms            (util/all-syms args)
          aliased-symbols (util/raw-globals env)
          env             (clear-raw-syms env syms)
          rebound-globals (set/intersection syms aliased-symbols)]
      (when (seq rebound-globals)
        (util/report-warning!
          (str "Function creates symbols in args that have been aliased to simple symbol(s) in the namespace: " rebound-globals
            " This can cause incorrect rewrites.\n\n"
            "FIX: Rewrite the function's argument list so that it does not shadow "
            "other simple symbols from namespace aliasing.") l))
      (apply list (map #(process-form env %) l))))

(def known-let-forms #{'let 'if-let 'when-let 'binding 'taoensso.encore/if-let 'taoensso.encore/when-let})

(def known-defn-forms #{'defn 'defn- 'ghostwheel.core/>defn 'ghostwheel.core/>defn-})

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

(>defn process-list [env]
  [::pspec/processing-env => any?]
  (let [feature (:feature-context env)
        l       (current-form env)
        {:keys [let-forms defn-forms]} (-> (get-in env [:config feature])
                                         (update :let-forms set/union known-let-forms)
                                         (update :defn-forms set/union known-defn-forms))
        ;; TASK: resolve symbol!
        f       (first l)]
    (cond
      ;(contains? let-forms f) (process-let env l)
      ;(contains? defn-forms f) (process-defn env l)
      :else (process-sequence env))))

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
                                       updated-env   (process-form env)
                                       next-lang-loc (z/right form-loc)]
                                   (if next-lang-loc
                                     (recur (assoc updated-env :zloc next-lang-loc))
                                     (-> updated-env
                                       (assoc :feature-context original-context)
                                       (update :zloc (fn [l] (-> l z/up z/up)))))))
                               env)]
        ending-env)
      (do
        (log/info "Skipping " reader)
        env))))

(>defn process-form
  "Process (recursively) the form at the current zloc of the env, returning an env with zloc at the same position, but
  possibly containing zipper updates/rewrites for that form."
  [env]
  [::pspec/processing-env => ::pspec/processing-env]
  (let [feature    (:feature-context env)
        form       (current-form env)
        transforms (get-in env [:config feature :transforms])
        p          (partial process-form env)
        node       (current-node env)
        ;old-meta   (meta form)
        #_#_form (reduce
                   (fn [f [predicate xform]]
                     (if (predicate env f)
                       (xform env f)
                       f))
                   form
                   transforms)
        #_#_form (if old-meta
                   (with-meta form old-meta)
                   form)]
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
