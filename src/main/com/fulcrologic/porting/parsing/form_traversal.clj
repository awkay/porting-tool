(ns com.fulcrologic.porting.parsing.form-traversal
  (:require
    [com.fulcrologic.porting.parsing.util :as util :refer [find-maplike-vals clear-raw-syms]]
    [com.fulcrologic.porting.specs :as pspec]
    [ghostwheel.core :refer [>defn =>]]
    [clojure.core.specs.alpha :as specs]
    [clojure.spec.alpha :as s]
    [clojure.set :as set]
    [taoensso.timbre :as log])
  (:import (clojure.lang ReaderConditional)))

(declare process-form)

(defn process-symbol [{:keys [raw-sym->fqsym nsalias->ns rename]} sym]
  )

(defn process-let [env l]
  [::pspec/processing-env ::pspec/let-like => ::let-like]
  (let [bindings           (second l)
        hazardous-bindings (into #{} (comp
                                       (map second)
                                       (filter simple-symbol?))
                             (partition 2 bindings))
        aliased-symbols    (util/raw-globals env)
        parsed-bindings    (s/conform ::specs/bindings bindings)
        local-syms         (find-maplike-vals parsed-bindings :local-symbol)
        env                (clear-raw-syms env local-syms)
        rebound-globals    (set/intersection local-syms aliased-symbols)]
    (when-let [problems (seq (set/intersection aliased-symbols hazardous-bindings))]
      (util/compile-warning! (str "The global aliased symbol(s) " (set problems)
                               " are bound AND used as values in the same let.\n\n"
                               "FIX: Refactor your the code to use only "
                               "qualified symbols from other namespaces in the values of the bindings.") l))
    (apply list (first l) (second l)
      (map #(process-form env %) (rest l)))))


(>defn process-defn [env l]
  [::pspec/processing-env ::pspec/defn-like => ::pspec/defn-like]
  (let [args            (rest l)
        syms            (util/all-syms args)
        aliased-symbols (util/raw-globals env)
        env             (clear-raw-syms env syms)
        rebound-globals (set/intersection syms aliased-symbols)]
    (when (seq rebound-globals)
      (util/compile-warning!
        (str "Function creates symbols in args that have been aliased to simple symbol(s) in the namespace: " rebound-globals
          " This can cause incorrect rewrites.\n\n"
          "FIX: Rewrite the function's argument list so that it does not shadow "
          "other simple symbols from namespace aliasing.") l))
    (apply list (first l) (map #(process-form env %) (rest l)))))

(>defn process-list [{:keys [let-forms defn-forms] :as env} l]
  [::pspec/processing-env list? => list?]
  (let [f (first l)]
    (cond
      (contains? let-forms f) (process-let env l)
      (contains? defn-forms f) (process-defn env l)
      :else (with-meta
              (apply list (map (partial process-form env) l))
              (meta l)))))

(s/def ::reader-cond #(instance? ReaderConditional %))

(>defn process-reader-conditional [env {:keys [form splicing?]}]
  [::pspec/processing-env ::reader-cond => ::reader-cond]
  (let [{:keys [clj cljs] :as features} (into {} (map vec) (partition 2 form))
        clj?      (contains? features :clj)
        cljs?     (contains? features :cljs)
        clj-env   (assoc env :feature-context :clj)
        cljs-env  (assoc env :feature-context :cljs)
        clj-form  (when clj? (process-form clj-env clj))
        cljs-form (when cljs? (process-form cljs-env cljs))]
    (ReaderConditional/create
      (with-meta (cond-> (list)
                   cljs? (->> (cons cljs-form) (cons :cljs))
                   clj? (->> (cons clj-form) (cons :clj))) (meta form))
      splicing?)))

(>defn process-form
  "Process (recursively) the given form. Returns the transformed form."
  [env form]
  [::pspec/processing-env any? => any?]
  (let [p (partial process-form env)]
    (cond
      (list? form) (with-meta (process-list env form) (meta form))
      (vector? form) (with-meta (mapv p form) (meta form))
      (map? form) (with-meta (into {}
                               (map (fn [[k v]]
                                      [(p k) (p v)]))
                               form) (meta form))
      (set? form) (with-meta (into #{} (map p) form) (meta form))
      (symbol? form) (with-meta (process-symbol env form) (meta form))
      (instance? ReaderConditional form) (process-reader-conditional env form)
      :else form)))
