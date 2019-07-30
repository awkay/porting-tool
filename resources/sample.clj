(ns sample
  (:use
    [clojure.java.ui])
  (:require
    [clojure.edn :as edn :refer [f g h]]
    [clojure.java.io :as io :refer :all]))

(defn some-function [arg]
  (f)
  (edn/read-string "" {})
  (map g []))


