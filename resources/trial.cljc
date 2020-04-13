(ns trial
  (:require
    [fulcro.client
     [primitives :as prim :refer [defsc]]
     [data-fetch :as df]
     [mutations :as m]]
    #?(:cljs [fulcro.client.dom :as dom]
       :clj  [fulcro.client.dom-server :as dom])))

(comment
  "hello")

;; comment
(m/defmutation some-mutation [params]
  (action [{:keys [reconciler state] :as env}]              ; comment 2
    (df/load-action env :root nil)
    (let [defsc 2]
      defsc)
    #(first %2)
    #_abc
    (#'some-specials
      \a
      'b
      @state
      #js {:a 1})

    #?(:cljs (js/setTimeout
               (fn []
                 (prim/transact! reconciler `[(f ~params)]))))
    (swap! state assoc-in [:x 1] {}))
  (remote [env] (df/remote-load env)))

(defsc Component [t props]
  {:componentDidMount (fn [] ...)}
  (let [computed (prim/get-computed this)]
    (dom/div
      (dom/div "Hi"))))

