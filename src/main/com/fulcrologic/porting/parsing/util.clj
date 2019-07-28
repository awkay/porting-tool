(ns com.fulcrologic.porting.parsing.util
  (:require
    [ghostwheel.core :refer [>defn >defn- | =>]]
    [com.fulcrologic.porting.specs :as pspec]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]))

(def ^:dynamic *current-file* "unknown")
(def ^:dynamic *current-form* nil)

(defn deep-merge [& xs]
  "Merges nested maps without overwriting existing keys."
  (if (every? map? xs)
    (apply merge-with deep-merge xs)
    (last xs)))

(>defn processing-env
  "Create a processing env that conforms to the spec (has defaults for required keys).

  `overrides` is a (possibly incomplete) processing-env that will be deep merged (override) against the defaults. "
  ([overrides]
   [map? => ::pspec/processing-env]
   (deep-merge
     {:parsing-envs    {}
      :config          {}
      :feature-context :agnostic}
     overrides))
  ([]
   [=> ::pspec/processing-env]
   {:parsing-envs    {}
    :config          {}
    :feature-context :agnostic}))

(defn compile-warning!
  ([message] (compile-warning! message *current-form*))
  ([message form]
   (let [{:keys [line column]} (meta form)]
     (log/warn (str *current-file* " " line ":" column " - " message)))))

(defn compile-error!
  ([message] (compile-error! message *current-form*))
  ([message form]
   (let [{:keys [line column]} (meta form)]
     (log/error (str *current-file* " " line ":" column " - " message)))
   (throw (ex-info "Failed" {}))))

(>defn find-map-vals
  "Recursively searches `data` for maps that contain `k`. Returns all such values at those `k`."
  [data k]
  [any? keyword? => set?]
  (let [result (atom #{})]
    (clojure.walk/prewalk
      (fn [ele]
        (when (and (map? ele) (contains? ele k))
          (swap! result conj (get ele k)))
        ele)
      data)
    @result))

(>defn- clear-parsing-syms
  "updates a parsing env with the global resolution of the given syms elided."
  [env syms]
  [::pspec/parsing-env (s/every symbol?) => ::pspec/parsing-env]
  (reduce
    (fn [e s]
      (update e :raw-sym->fqsym dissoc s))
    env
    syms))

(>defn clear-raw-syms
  "updates a processing env with the global resolution of the given syms elided for the set given features,
  where `features` is a #{:clj :cljs :agnostic}."
  [env syms features]
  [::pspec/processing-env (s/every symbol?) (s/every ::pspec/feature :kind set?) => ::pspec/processing-env]
  (reduce
    (fn [e feature] (update-in e [:parsing-envs feature] clear-parsing-syms syms))
    env
    features))

(>defn sym->fqsym
  "Resolve the given symbol against the current processing env. Returns a fully-qualified symbol if the symbol
  has been aliased from some other ns; otherwise returns the original symbol."
  [env sym]
  [::pspec/processing-env symbol? => symbol? | #(or (qualified-symbol? %) (= % sym))]
  (let [feature         (:feature-context env)
        {:keys [nsalias->ns raw-sym->fqsym] :as parsing-env} (get-in env [:parsing-envs feature])
        potential-alias (some-> sym namespace symbol)
        real-ns         (get nsalias->ns potential-alias)
        fqsym           (if real-ns
                          (symbol (name real-ns) (name sym))
                          (get raw-sym->fqsym sym sym))]
    (if fqsym
      fqsym
      sym)))

(>defn fqsym->aliased-sym
  "Given a processing env and a fully-qualified symbol, return either
  the most succinct qualified symbol that will work in the given feature context."
  [env sym]
  [::pspec/processing-env qualified-symbol? => qualified-symbol?]
  (let [feature        (:feature-context env)
        {:keys [namespace-aliases]} (get-in env [:config feature])
        full-namespace (some-> sym namespace symbol)
        desired-alias  (get namespace-aliases full-namespace)]
    (if desired-alias
      (symbol (name desired-alias) (name sym))
      sym)))

(>defn require-for
  "Returns a clj require clause for the given namespace, including the alias, for the current processing
  context."
  [env ns]
  [::pspec/processing-env ::pspec/namespace => (s/or
                                                 :sym ::pspec/namespace
                                                 :lib+opt (s/tuple ::pspec/namespace #{:as} symbol?))]
  (let [feature       (:feature-context env)
        {:keys [namespace-aliases]} (get-in env [:config feature])
        desired-alias (get namespace-aliases ns)]
    (if desired-alias
      [ns :as desired-alias]
      ns)))
