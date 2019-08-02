(ns com.fulcrologic.porting.transforms.rename
  (:require
    [ghostwheel.core :refer [>defn >defn- =>]]
    [com.fulcrologic.porting.specs :as pspecs]
    [com.fulcrologic.porting.parsing.util :as util]
    [com.fulcrologic.porting.parsing.form-traversal :as ft]
    [taoensso.encore :as enc]
    [taoensso.timbre :as log]
    [clojure.walk :as walk]
    [clojure.spec.alpha :as s]
    [com.fulcrologic.porting.rewrite-clj.zip :as z]))

(>defn- resolve-new-name
  "Given a processing env and a fully-qualified symbol, return
  the most succinct qualified symbol that will work after any possible renames."
  [env sym]
  [::pspecs/processing-env qualified-symbol? => qualified-symbol?]
  (let [feature (:feature-context env)
        {:keys [nsalias->ns]} (get-in env [:parsing-envs feature])
        {:keys [namespace->alias
                fqname-old->new]} (get-in env [:config feature])]
    (if-let [new-fqname (get fqname-old->new sym)]
      (enc/if-let [full-namespace (some-> new-fqname namespace symbol)
                   desired-alias  (get namespace->alias full-namespace full-namespace)]
        (let [{:keys [state-atom]} env]
          (when state-atom
            (swap! state-atom update-in [::namespaces-needed feature] (fnil conj #{}) full-namespace))
          (when-let [alt-ns (get nsalias->ns desired-alias)]
            (when (not= alt-ns full-namespace)
              (util/report-error!
                (str "Error. The file already has namespace alias `" desired-alias
                  "`, but it refers to `" alt-ns "`") sym)))
          (symbol (name desired-alias) (name sym)))
        (do
          (util/report-warning! "No namespace alias defined for " sym)
          new-fqname))
      sym)))

(>defn- rename-symbol
  [env sym]
  [::pspecs/processing-env symbol? => symbol?]
  (let [fqsym   (log/spy :info (util/sym->fqsym env (log/spy :info sym)))
        new-sym (resolve-new-name env fqsym)]
    (if (= new-sym fqsym)
      sym
      (do
        (log/info "Renaming" sym "to" new-sym)
        new-sym))))

(defn rename-artifacts-transform
  "A transformer that renames symbols according to the configuration in the processing-env.

  The configuration for renames should include:

  * `:fqname-old->new` - A map from old to new fully-qualified thing
  * `:namespace->alias` - A map from *new* namespaces to your desired alias for it. If not supplied, the new symbol
    will simply be fully-qualified in the output.
  * `:transforms` - Must include this transform (e.g. [rename-artifacts-transform]) in the order you want it applied.
  "
  [env]
  (let [f (ft/current-form env)]
    (if (and (not (ft/within env 'ns)) (symbol? f))
      (ft/replace env (rename-symbol env f))
      env)))

(defn rename-namespaces-transform
  "A transformer that renames all namespaces within a namespace form according to the configuration.

  The configuration for renames should include:

  * `:namespace-old->new` - A map from old to new fully-qualified thing
  * `:transforms` - Must include this transform (e.g. [rename-namespaces-transform]) in the order you want it applied.

  NOTE: All other aspects of the namespace (e.g. refer/as) will be unchanged.
  "
  [env]
  (let [f (ft/current-form env)]
    (if (and (symbol? f) (ft/within env 'ns))
      (let [feature (:feature-context env)
            {:keys [namespace-old->new]} (get-in env [:config feature])]
        (if (contains? namespace-old->new f)
          (do
            (log/info "Renaming" f)
            (ft/replace env (get namespace-old->new f)))
          env))
      env)))

(defn add-missing-namespaces-transform [{:keys [state] :as env}]
  "Adds missing namespaces with desired alias to the ns form. This transform is really a post-step for the rename
  transform, and has no configuration of it's own.

  The assumption is that a file will have started out in 'good condition', and the only reason there will be a
  need to add a require is if the rename transform places a symbol in the file for a namespace that was
  not required by that file.

  Therefore the only configuration is that `:transforms` have both the rename and add missing transforms:

  ```
  :transforms [rename-artifacts-transform add-missing-namespaces-transform]
  ```

  TODO: should this be mixed into the rename transform?"
  (let [f (ft/current-form env)]
    (if (and (ft/within env 'ns) (= :require f))
      (let [{::keys [namespaces-needed]} state
            feature  (:feature-context env)
            nses     (get namespaces-needed feature)
            {:keys [namespace->alias]} (get-in env [:config feature])
            elements (reduce
                       (fn [result ns]
                         (cond-> [ns]
                           (get namespace->alias ns) (conj :as (namespace->alias ns))))
                       []
                       nses)]
        (update env :zloc (fn [loc] (-> loc
                                      (z/insert-right elements)
                                      z/right
                                      z/splice))))
      env)))
