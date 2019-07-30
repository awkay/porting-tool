(ns trial
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.data-fetch :as df]
    [fulcro.client.mutations :as m]
    #?(:cljs [fulcro.client.dom :as dom]
       :clj  [fulcro.client.dom-server :as dom])))

(m/defmutation some-mutation [params]
  (action [{:keys [reconciler state] :as env}]
    (df/load-action env :root nil)
    #?(:cljs (js/setTimeout
               (fn []
                 (prim/transact! reconciler `[(f ~params)]))))
    (swap! state assoc-in [:x 1] {}))
  (remote [env] (df/remote-load env)))

(defsc Component [this props]
  {}
  (let [computed (prim/get-computed this)]
    (dom/div
      (dom/div "Hi"))))
