(ns com.fulcrologic.porting.parsing.namespace-parser
  (:require
    [com.fulcrologic.porting.parsing.util :refer [compile-error! compile-warning! *current-form*]]
    [clojure.pprint :refer [pprint]]
    [clojure.core.specs.alpha :as specs]
    [clojure.spec.alpha :as s]))

(defn- extract-libspec [env {:keys [lib options]}]
  (let [{:keys [as refer]} options
        referrals       (into {} [refer])
        syms            (:syms referrals)
        add-sym-aliases (fn [e syms]
                          (reduce
                            (fn [s sym] (assoc-in s [:raw-sym->fqsym sym] (symbol (name lib) (name sym))))
                            e
                            syms))]
    (when (contains? referrals :all)
      (compile-warning! "Porting tool does not support :refer :all. Please refer your symbols.")
      (println))
    (cond-> env
      as (assoc-in [:nsalias->ns as] lib)
      syms (add-sym-aliases syms))))

#_[:prefix-list {:prefix   com.fulcrologic.fulcro,
                 :libspecs [[:lib+opts {:lib component, :options {:as comp}}]
                            [:lib+opts {:lib application, :options {:as app}}]]}]
(defn- extract-prefixes [env {:keys [prefix libspecs]}]
  (reduce
    (fn [e [_ {:keys [lib options]}]]
      (let [real-libspec {:lib     (symbol (str (name prefix) "." (name lib)))
                          :options options}]
        (extract-libspec e real-libspec)))
    env
    libspecs))

(defn- extract-details [env clauses]
  (when (contains? clauses :use)
    (compile-warning! "Porting tool cannot fully analyze use clauses. Please port them to requires."))
  (let [requires (-> clauses :require :body)
        state    (atom {})]
    (reduce
      (fn [e require]
        (case (first require)
          :libspec (extract-libspec e (second (second require)))
          :prefix-list (extract-prefixes e (second require))
          (do
            (println "Unexpected clause in require")
            e)))
      env
      requires)))

(defn parse-namespace
  "Parse a namespace form. Returns an updated env that has resolutions for required symbols and namespaces."
  [env ns-form]
  (let [pr (s/conform ::specs/ns-form (rest ns-form))]
    (case pr
      ::s/invalid (compile-error! (s/explain-str ::specs/ns-form ns-form) ns-form)
      (let [ns-name (:ns-name pr)
            clauses (into {} (:ns-clauses pr))
            env     (assoc env :current-ns ns-name :current-form ns-form)]
        (binding [*current-form* ns-form]
          (extract-details env clauses))))))
