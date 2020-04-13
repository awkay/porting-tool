(ns com.fulcrologic.porting.transforms.fulcro
  (:require [com.fulcrologic.porting.parsing.form-traversal :as ft]
            [com.fulcrologic.porting.rewrite-clj.zip :as z]
            [rewrite-clj.node.protocols :as np]
            [taoensso.timbre :as log]
            [com.fulcrologic.porting.parsing.util :as util]))

(def lifecycles #{:componentDidMount :componentWillMount :componentDidUpdate :initLocalState
                  :componentWillReceiveProps :componentWillUpdate :UNSAFE_componentWillMount
                  :UNSAFE_componentWillReceiveProps :UNSAFE_componentWillUpdate})

(defn next-vector [zloc]
  (z/find-next zloc z/next (fn [l] (and (not (np/printable-only? l)) (vector? (z/sexpr l))))))

(defn add-this
  "Finds the lambda at key `k` in the map at zloc, and adds `this` as the first argument to it."
  [this-sym k zloc]
  (or (some-> zloc
        z/down
        (z/find-value k)
        (next-vector)
        (z/insert-child this-sym))
    zloc))

(defn update-lifecycle-transform
  "Find defsc forms, capture the `this` binding, and add it to the correct lifecycle methods"
  [env]
  (let [f (ft/current-form env)]
    (if (and (ft/within env 'fulcro.client.primitives/defsc) (map? f))
      (let [this-sym (some-> env :zloc z/up next-vector z/down z/sexpr)
            new-env  (reduce
                       (fn [{:keys [zloc] :as e} k]
                         (let [zloc (z/edit-node zloc (partial add-this (or this-sym 'this) k))]
                           (assoc e :zloc zloc)))
                       env
                       lifecycles)]
        (when (contains? f :protocols)
          (util/report-warning! "The :protocols key is no longer supported in Fulcro 3. Please see Developer's Guide." f))
        new-env)
      env)))

(def ns-warnings
  {'fulcro.client.localized-dom "Fulcro CSS is now an extra dep. Remember to add it to your project."
   'localized-dom               "Fulcro CSS is now an extra dep. Remember to add it to your project."
   'fulcro.websockets           "Fulcro websockets is now an extra dependency. Remember to add it to your project."})

(defn warn-missing-deps
  [env]
  (let [form (ft/current-form env)]
    (when (and (ft/within env 'ns) (symbol? form) (contains? ns-warnings form))
      (util/report-warning! (get ns-warnings form))))
  env)
