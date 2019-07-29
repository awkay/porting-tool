(ns com.fulcrologic.porting.transforms.rename
  (:require
    [ghostwheel.core :refer [>defn >defn- =>]]
    [com.fulcrologic.porting.specs :as pspecs]
    [com.fulcrologic.porting.parsing.util :as util]
    [taoensso.encore :as enc]
    [taoensso.timbre :as log]))

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
        (do
          (when-let [alt-ns (get nsalias->ns desired-alias)]
            (when (not= alt-ns full-namespace)
              (util/compile-error!
                (str "Error. The file already has namespace alias `" desired-alias
                  "`, but it refers to `" alt-ns "`") sym)))
          (symbol (name desired-alias) (name sym)))
        (do
          (util/compile-warning! "No namespace alias defined for " sym)
          new-fqname))
      sym)))

(>defn- rename-xform
  [env sym]
  [::pspecs/processing-env symbol? => symbol?]
  (->> sym
    (util/sym->fqsym env)
    (resolve-new-name env)))

(def rename-transform
  "A transformer renames symbols according to the configuration in the processing-env."
  [symbol? rename-xform])
