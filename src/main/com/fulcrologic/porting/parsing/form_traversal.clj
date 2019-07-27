(ns com.fulcrologic.porting.parsing.form-traversal
  (:require
    [com.fulcrologic.porting.parsing.util :refer [find-map-vals clear-raw-syms]]
    [clojure.core.specs.alpha :as specs]
    [clojure.spec.alpha :as s]))

(declare process-form)

(defn process-symbol [{:keys [raw-sym->fqsym nsalias->ns rename]} sym]
  (let [potential-alias (namespace sym)
        real-ns         (get nsalias->ns potential-alias)
        fqsym           (if real-ns
                          (symbol (name real-ns) (name sym))
                          (get raw-sym->fqsym sym sym))
        new-sym         (get rename fqsym)]
    ;; TASK: support rewriting new sym with new require
    (if new-sym
      fqsym
      sym)))

(defn process-let [env l]
  (let [bindings        (second l)
        parsed-bindings (s/conform ::specs/bindings bindings)
        local-syms      (find-map-vals parsed-bindings :local-symbol)
        env             (clear-raw-syms env local-syms)]
    (apply list (first l) (second l)
      (map #(process-form env %) (rest l)))))

(defn process-defn [env l] l)

(defn process-list [{:keys [let-forms defn-forms] :as env} l]
  (let [f (first l)]
    (cond
      (contains? let-forms f) (process-let env l)
      (contains? defn-forms f) (process-defn env l)
      :else (with-meta
              (apply list (map (partial process-form env) l))
              (meta l)))))

(defn process-form
  "Process (recursively) the given form. Returns the transformed form."
  [env form]
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
      :else form)))
