(ns trial
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    #?(:cljs [fulcro.client.dom :as dom]
       :clj  [fulcro.client.dom-server :as dom])))

(defsc Component [this props]
  {}
  (let [computed (prim/get-computed this)]
    (dom/div
      (dom/div "Hi"))))
