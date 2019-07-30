(ns com.fulcrologic.porting.input
  (:refer-clojure :exclude [read read-string])
  (:require
    [clojure.tools.reader :as edn]
    [clojure.tools.reader.reader-types :as readers]
    [clojure.pprint :as pp :refer [pprint]]
    [ghostwheel.core :refer [>defn => >defn-]]
    [com.fulcrologic.porting.specs :as pspec]
    [com.fulcrologic.porting.parsing.namespace-parser :refer [parse-namespace]]
    [com.fulcrologic.porting.parsing.util :refer [*current-file*]]
    [clojure.java.io :as io])
  (:import (java.io StringReader)))

(>defn- read
  [pbr lang]
  [any? ::pspec/feature => any?]
  (let [read-form (cond
                    (= :agnostic lang) #(edn/read {:read-cond :preserve
                                                   :features  #{:clj :cljs}
                                                   :eof       ::END} pbr)
                    (= :none lang) #(edn/read {:eof       ::END
                                               :read-cond :allow} pbr)
                    :else #(edn/read {:read-cond :allow
                                      :features  #{lang}
                                      :eof       ::END} pbr))
        result    (atom [])]
    (loop [form (read-form)]
      (when (not= ::END form)
        (swap! result conj form)
        (recur (read-form))))
    @result))

(defn read-string
  "Read forms from a string. `lang` should be :cljs, :clj, or :agnostic. When using :agnostic reader conditionals
  will be preserved."
  [s lang]
  [string? ::pspec/feature => any?]
  (binding [*current-file* "literal string"]
    (let [rdr (StringReader. s)
          pbr (readers/indexing-push-back-reader rdr)]
      (read pbr lang))))

(defn read-file
  [filename lang]
  [string? ::pspec/feature => any?]
  "Read forms from a file. `lang` should be :cljs, :clj, :agnostic, or :none. When using :agnostic reader conditionals
  will be preserved, when using :none they will be elided."
  (binding [*current-file* filename]
    (let [s   (io/input-stream filename)
          rdr (io/reader s)
          pbr (readers/indexing-push-back-reader rdr)]
      (read pbr lang))))
