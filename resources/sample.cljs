(ns sample
  (:require
    [fulcro.client.dom :as dom]
    [fulcro.client.primitives :as prim :refer [defsc]]))

(prim/defsc Component [this props]
  {:ident [:x :x]}
  (dom/div "Hello world"))

