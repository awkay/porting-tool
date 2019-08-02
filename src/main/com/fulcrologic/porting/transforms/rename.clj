(ns com.fulcrologic.porting.transforms.rename
  (:require
    [ghostwheel.core :refer [>defn >defn- =>]]
    [com.fulcrologic.porting.specs :as pspecs]
    [com.fulcrologic.porting.parsing.util :as util]
    [com.fulcrologic.porting.parsing.form-traversal :as ft]
    [taoensso.encore :as enc]
    [taoensso.timbre :as log]
    [clojure.walk :as walk]
    [clojure.spec.alpha :as s]))

;;(>defn- resolve-new-name
;;  "Given a processing env and a fully-qualified symbol, return
;;  the most succinct qualified symbol that will work after any possible renames."
;;  [env sym]
;;  [::pspecs/processing-env qualified-symbol? => qualified-symbol?]
;;  (let [feature (:feature-context env)
;;        {:keys [nsalias->ns]} (get-in env [:parsing-envs feature])
;;        {:keys [namespace->alias
;;                fqname-old->new]} (get-in env [:config feature])
;;        ]
;;    (if-let [new-fqname (get fqname-old->new sym)]
;;      (enc/if-let [full-namespace (some-> new-fqname namespace symbol)
;;                   desired-alias  (get namespace->alias full-namespace full-namespace)]
;;        (let [{:keys [state-atom]} env]
;;          (when state-atom
;;            (swap! state-atom update-in [::namespaces-needed feature] (fnil conj #{}) full-namespace))
;;          (when-let [alt-ns (get nsalias->ns desired-alias)]
;;            (when (not= alt-ns full-namespace)
;;              (util/report-error!
;;                (str "Error. The file already has namespace alias `" desired-alias
;;                  "`, but it refers to `" alt-ns "`") sym)))
;;          (symbol (name desired-alias) (name sym)))
;;        (do
;;          (util/report-warning! "No namespace alias defined for " sym)
;;          new-fqname))
;;      sym)))
;;
;;(>defn- rename-symbol
;;  [env sym]
;;  [::pspecs/processing-env symbol? => symbol?]
;;  (let [fqsym   (util/sym->fqsym env sym)
;;        new-sym (resolve-new-name env fqsym)]
;;    (if (= new-sym fqsym)
;;      sym
;;      new-sym)))
;;
;;(def rename-artifacts-transform
;;  "A transformer that renames symbols according to the configuration in the processing-env.
;;
;;  The configuration for renames should include:
;;
;;  * `:fqname-old->new` - A map from old to new fully-qualified thing
;;  * `:namespace->alias` - A map from *new* namespaces to your desired alias for it. If not supplied, the new symbol
;;    will simply be fully-qualified in the output.
;;  * `:transforms` - Must include this transform (e.g. [rename-artifacts-transform]) in the order you want it applied.
;;  "
;;  [(fn [{::keys [in-ns?]} s] (and (not in-ns?) (symbol? s))) rename-symbol])
;;
;;(defn rename-nses-predicate? [{::keys [in-ns?]} f]
;;  (or
;;    (and in-ns? (simple-symbol? f))
;;    (and (list? f) (= 'ns (first f)))))
;;
;;(defn rename-namespaces
;;  [env form]
;;  [::pspecs/processing-env (s/or :ns-form list? :ns simple-symbol?)
;;   => (s/or :ns-form list? :ns simple-symbol?)]
;;  (if (list? form)
;;    (apply list
;;      (map #(ft/process-form (assoc env ::in-ns? true) %) form))
;;    (let [feature (:feature-context env)
;;          {:keys [namespace-old->new]} (get-in env [:config feature])]
;;      (if-let [new-sym (and (simple-symbol? form) (get namespace-old->new form))]
;;        new-sym
;;        form))))
;;
;;(def rename-namespaces-transform
;;  "A transformer that renames all namespaces within a namespace form according to the configuration.
;;
;;  The configuration for renames should include:
;;
;;  * `:namespace-old->new` - A map from old to new fully-qualified thing
;;  * `:transforms` - Must include this transform (e.g. [rename-namespaces-transform]) in the order you want it applied.
;;
;;  NOTE: All other aspects of the namespace (e.g. refer/as) will be unchanged.
;;  "
;;  [rename-nses-predicate? rename-namespaces])
;;
;;(defn add-nses-predicate [env form] (and (list? form) (= 'ns (first form))))
;;
;;(defn add-missing-namespaces [{:keys [state] :as env} form]
;;  (let [{::keys [namespaces-needed]} state
;;        feature  (:feature-context env)
;;        nses     (get namespaces-needed feature)
;;        {:keys [nsalias->ns]} (get-in env [:parsing-envs feature])
;;        {:keys [namespace->alias]} (get-in env [:config feature])
;;        elements (reduce
;;                   (fn [result ns]
;;                     (cond-> [ns]
;;                       (get namespace->alias ns) (conj :as (namespace->alias ns))))
;;                   []
;;                   nses)]
;;    (walk/prewalk
;;      (fn [ele]
;;        (if (and (sequential? ele) (= :require (first ele)))
;;          (apply list :require (into [elements] (rest ele)))
;;          ele))
;;      form)))
;;
;;(def add-missing-namespaces-transform
;;  "Adds missing namespaces with desired alias to the ns form. This transform is really a post-step for the rename
;;  transform, and has no configuration of it's own.
;;
;;  The assumption is that a file will have started out in 'good condition', and the only reason there will be a
;;  need to add a require is if the rename transform places a symbol in the file for a namespace that was
;;  not required by that file.
;;
;;  Therefore the only configuration is that `:transforms` have both the rename and add missing transforms:
;;
;;  ```
;;  :transforms [rename-artifacts-transform add-missing-namespaces-transform]
;;  ```
;;
;;  TODO: should this be mixed into the rename transform?"
;;  [add-nses-predicate add-missing-namespaces])
