(ns com.fulcrologic.porting.parsing.util
  (:require
    [ghostwheel.core :refer [>defn >defn- | =>]]
    [com.fulcrologic.porting.specs :as pspec]
    [clojure.core.specs.alpha :as specs]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]
    [clojure.set :as set]
    [clojure.walk :as walk]))

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
      :zloc            {}
      :feature-context :agnostic}
     overrides))
  ([]
   [=> ::pspec/processing-env]
   {:parsing-envs    {}
    :zloc            {}
    :config          {}
    :feature-context :agnostic}))

(defn report-warning!
  ([message] (report-warning! message *current-form*))
  ([message form]
   (let [{:keys [line column]} (meta form)]
     (log/warn (str *current-file* " " line ":" column " - " message)))))

(defn report-error!
  ([message] (report-error! message *current-form*))
  ([message form]
   (let [{:keys [line column]} (meta form)]
     (log/error (str *current-file* " " line ":" column " - " message)))))

(>defn find-maplike-vals
  "Recursively searches `data` for maps that contain `k`, or vectors that look like MapEntries.
  Returns all such values at those `k`."
  [data k]
  [any? keyword? => set?]
  (let [result (atom #{})]
    (clojure.walk/prewalk
      (fn [ele]
        (cond
          (and (map? ele) (contains? ele k)) (swap! result conj (get ele k))
          (and (vector? ele) (even? (count ele))) (let [m (into {} (map vec) (partition 2 ele))]
                                                    (when-let [v (get m k)]
                                                      (swap! result conj v))))
        ele)
      data)
    @result))

(>defn- clear-parsing-syms
  "updates a parsing env with the global resolution of the given syms elided."
  [env syms]
  [::pspec/parsing-env (s/every symbol?) => ::pspec/parsing-env]
  (reduce
    (fn [e s]
      (update e :raw-sym->fqsym (fnil dissoc {}) s))
    env
    syms))

(>defn clear-raw-syms
  "updates a processing env with the global resolution of the given syms elided for the set given features,
  where `features` is a #{:clj :cljs :agnostic}."
  [env syms]
  [::pspec/processing-env (s/every symbol?) => ::pspec/processing-env]
  (let [feature (:feature-context env)]
    (update-in env [:parsing-envs feature] clear-parsing-syms syms)))

(>defn sym->fqsym
  "Resolve the given symbol against the current processing env. Returns a fully-qualified symbol if the symbol
  has been aliased from some other ns; otherwise returns the original symbol."
  [env sym]
  [::pspec/processing-env symbol? => symbol? | #(or (qualified-symbol? %) (= % sym))]
  (let [feature         (:feature-context env)
        {:keys [nsalias->ns raw-sym->fqsym]} (get-in env [:parsing-envs feature])
        potential-alias (some-> sym namespace symbol)
        real-ns         (get nsalias->ns potential-alias)
        fqsym           (if real-ns
                          (symbol (name real-ns) (name sym))
                          (get raw-sym->fqsym sym sym))]
    (if fqsym
      fqsym
      sym)))

(>defn require-for
  "Returns a clj require clause for the given namespace, including the alias, for the current processing
  context."
  [env ns]
  [::pspec/processing-env ::pspec/namespace => (s/or
                                                 :sym ::pspec/namespace
                                                 :lib+opt (s/tuple ::pspec/namespace #{:as} symbol?))]
  (let [feature       (:feature-context env)
        {:keys [namespace->alias]} (get-in env [:config feature])
        desired-alias (get namespace->alias ns)]
    (if desired-alias
      [ns :as desired-alias]
      ns)))

(>defn raw-globals
  "Returns the current names (as a set of symbols) from other namespaces that are aliased to simple
  symbols for the current processing env."
  [env]
  [::pspec/processing-env => (s/every simple-symbol? :kind set?)]
  (let [feature (:feature-context env)]
    (-> env
      (get-in [:parsing-envs feature :raw-sym->fqsym])
      keys
      set)))

(>defn all-symbol-names
  "Recursively finds all symbols in form, return a set of them as their simple symbols.

  ```
  (all-symbol-names ['a 'b/c]) => #{a c}
  ```
  "
  [form]
  [any? => (s/every simple-symbol? :kind set?)]
  (let [syms (atom #{})]
    (walk/prewalk
      (fn [e]
        (when (symbol? e)
          (swap! syms conj (symbol (name e))))
        e)
      form)
    @syms))

