(ns com.fulcrologic.porting.input
  (:refer-clojure :exclude [read read-string])
  (:require
    [clojure.tools.reader :as edn]
    [clojure.tools.reader.reader-types :as readers]
    [clojure.pprint :refer [pprint]]
    [com.fulcrologic.porting.parsing.namespace-parser :refer [parse-namespace]]
    [com.fulcrologic.porting.parsing.util :refer [*current-file*]]
    [clojure.java.io :as io])
  (:import (java.io StringReader)))

(defn cljc->map [s] (vary-meta (apply hash-map s) assoc ::cljc-form? true))

(defn read [pbr lang]
  (let [read-form #(edn/read {:read-cond :allow
                              :features  #{lang}
                              :eof       ::END} pbr)
        result    (atom [])]
    (loop [form (read-form)]
      (when (not= ::END form)
        (swap! result conj form)
        (recur (read-form))))
    @result))

(defn read-string
  "Read forms from a string. `lang` should be :cljs or :clj. If the content is CLJC, you will receive
  the forms for the specified lang."
  [s lang]
  (binding [*current-file* "literal string"]
    (let [rdr (StringReader. s)
          pbr (readers/indexing-push-back-reader rdr)]
      (read pbr lang))))

(defn read-file [filename lang]
  "Read forms from a file. `lang` should be :cljs or :clj. If the content is CLJC, you will receive
  the forms for the specified lang."
  (binding [*current-file* filename]
    (let [s   (io/input-stream filename)
          rdr (io/reader s)
          pbr (readers/indexing-push-back-reader rdr)]
      (read pbr lang))))

(comment
  (read-file "/Users/tonykay/fulcrologic/porting-tool/resources/sample.clj" :clj))
