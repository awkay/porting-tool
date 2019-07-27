(ns com.fulcrologic.porting.parsing.namespace-parser
  (:require
    [com.fulcrologic.porting.parsing.util :refer [compile-error!]]
    [clojure.pprint :refer [pprint]]
    [clojure.core.specs.alpha :as specs]
    [clojure.spec.alpha :as s]
    [taoensso.timbre :as log]))

(defn extract-details [state ns-name clauses]
  (when (contains? clauses :use)
    (log/warn "Porting tool cannot fully analyze use clauses. Please port them to requires."))
  (let [requires (-> clauses :require :body)
        ]
    (doseq [[_ [_ {:keys [lib options]}]] requires
            :let [{:keys [as refer]} (log/spy :info options)
                  referrals       (log/spy :info (into {} [refer]))
                  syms            (log/spy :info (:syms referrals))
                  add-sym-aliases (fn [state-map syms]
                                    (reduce
                                      (fn [s sym]
                                        (assoc-in s [:raw-sym->fqsym sym] (symbol (name lib) (name sym))))
                                      state-map
                                      syms))]]
      (when (contains? referrals :all)
        (println "Porting tool does not support :refer :all. Please refer your symbols."))
      (swap! state
        (fn [s]
          (cond-> s
            as (assoc-in [:nslias->ns as] lib)
            syms (add-sym-aliases syms)
            )
          )))))

(defn parse-namespace [ns-form state]
  (let [pr (s/conform ::specs/ns-form (rest ns-form))]
    (case pr
      ::s/invalid (compile-error! (s/explain-str ::specs/ns-form ns-form) ns-form)
      (let [ns-name (:ns-name pr)
            clauses (into {} (:ns-clauses pr))]
        (extract-details state ns-name clauses)))))
