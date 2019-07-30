(ns com.fulcrologic.porting.parsing.namespace-parser
  (:require
    [ghostwheel.core :refer [>defn >defn- =>]]
    [com.fulcrologic.porting.parsing.util :refer [compile-error! compile-warning! *current-form*]]
    [com.fulcrologic.porting.specs :as pspec]
    [clojure.pprint :refer [pprint]]
    [clojure.core.specs.alpha :as specs]
    [clojure.spec.alpha :as s]
    [taoensso.timbre :as log]
    [clojure.spec.gen.alpha :as gen]))

(s/def ::as (s/with-gen simple-symbol? #(s/gen '#{prim comp log})))
(s/def ::refer (s/tuple (s/with-gen keyword? #(s/gen #{:syms})) (s/every simple-symbol?)))
(s/def ::libspec (s/or
                   :simple-symbol ::pspec/namespace
                   :lib+opts (s/keys :opt-un [::as ::refer])))
(s/def ::prefix ::pspec/namespace)
(s/def ::libspecs (s/every (s/tuple #{:lib :lib+opts} ::libspec)))

(>defn- extract-libspec
  [env libspec]
  [::pspec/parsing-env ::libspec => ::pspec/parsing-env]
  (if (symbol? libspec)
    env
    (let [{:keys [lib options]} libspec
          {:keys [as refer]} options
          referrals       (into {} [refer])
          syms            (:syms referrals)
          add-sym-aliases (fn [e syms]
                            (reduce
                              (fn [s sym] (assoc-in s [:raw-sym->fqsym sym] (symbol (name lib) (name sym))))
                              e
                              syms))]
      (when (contains? referrals :all)
        (compile-warning! "Porting tool does not support :refer :all. Please refer your symbols."))
      (cond-> env
        as (assoc-in [:nsalias->ns as] lib)
        as (assoc-in [:ns->alias lib] as)
        (not as) (assoc-in [:nsalias->ns lib] lib)
        (not as) (assoc-in [:ns->alias lib] lib)
        syms (add-sym-aliases syms)))))

(>defn- extract-prefixes [env {:keys [prefix libspecs]}]
  [::pspec/parsing-env (s/keys :opt-un [::prefix ::libspecs])
   => ::pspec/parsing-env]
  (reduce
    (fn [e [_ {:keys [lib options]}]]
      (let [real-libspec {:lib     (symbol (str (name prefix) "." (name lib)))
                          :options options}]
        (extract-libspec e real-libspec)))
    env
    libspecs))

(s/def ::clojure-ns (s/spec (s/cat :ns-key #{'ns} :ns-form ::specs/ns-form)))

(>defn parse-namespace
  "Parse a namespace form. Returns an updated env that has resolutions for required symbols and namespaces."
  [env ns-form]
  [::pspec/parsing-env ::clojure-ns => ::pspec/parsing-env]
  (let [pr (s/conform ::specs/ns-form (rest ns-form))]
    (case pr
      ::s/invalid (compile-error! (s/explain-str ::specs/ns-form ns-form) ns-form)
      (let [ns-name  (:ns-name pr)
            clauses  (into {} (:ns-clauses pr))
            requires (-> clauses :require :body)
            env      (assoc env :current-ns ns-name :current-form ns-form)]
        (when (contains? clauses :use)
          (compile-warning! "Porting tool cannot fully analyze `use` clauses. Please port them to requires."))
        (binding [*current-form* ns-form]
          (reduce
            (fn [e require]
              (case (first require)
                :libspec (extract-libspec e (second (second require)))
                :prefix-list (extract-prefixes e (second require))
                (do
                  (compile-warning! "Unexpected clause in require")
                  e)))
            env
            requires))))))
