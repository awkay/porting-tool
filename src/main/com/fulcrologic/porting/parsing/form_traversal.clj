(ns com.fulcrologic.porting.parsing.form-traversal
  (:require
    [com.fulcrologic.porting.parsing.util :refer [find-map-vals clear-raw-syms]]
    [com.fulcrologic.porting.specs :as pspec]
    [ghostwheel.core :refer [>defn =>]]
    [clojure.core.specs.alpha :as specs]
    [clojure.spec.alpha :as s])
  (:import (clojure.lang ReaderConditional)))

(declare process-form)

(defn process-symbol [{:keys [raw-sym->fqsym nsalias->ns rename]} sym]
)

(s/def ::let-like (s/cat :sym symbol? :bindings ::specs/bindings :body (s/* any?)))

(defn process-let [env l]
  [::pspec/processing-env ::let-like => ::let-like]
  (let [bindings        (second l)
        parsed-bindings (s/conform ::specs/bindings bindings)
        local-syms      (find-map-vals parsed-bindings :local-symbol)
        env             (clear-raw-syms env local-syms)]
    (apply list (first l) (second l)
      (map #(process-form env %) (rest l)))))

(>defn process-defn [env l]
  [::pspec/processing-env list? => list?]
  l)

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
