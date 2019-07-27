(ns sample
  (:use
    [clojure.java.ui])
  (:require
    #? (:clj [clojure.edn :as edn :refer [f g h]] :cljs [boo :as bah])
    [clojure.java.io :as io :refer :all]))

#?(:clj 1 :cljs 2)
(io/make-input-stream)

(edn/read)
