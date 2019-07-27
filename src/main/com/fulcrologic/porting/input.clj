(ns com.fulcrologic.porting.input
  (:require
    [clojure.tools.reader :as edn]
    [clojure.tools.reader.reader-types :as readers]
    [clojure.pprint :refer [pprint]]
    [com.fulcrologic.porting.parsing.namespace-parser :refer [parse-namespace]]
    [com.fulcrologic.porting.parsing.util :refer [*current-file*]]
    [clojure.java.io :as io])
  (:import (java.io StringReader)))

(defn cljc->map [s] (vary-meta (apply hash-map s) assoc ::cljc-form? true))

(defn read-string [s]
  (binding [*current-file* "literal string"]
    (let [rdr       (StringReader. s)
          pbr       (readers/indexing-push-back-reader rdr)
          read-form #(edn/read {:read-cond :allow
                                :features  #{:clj}
                                :eof       ::END} pbr)
          result    (atom [])]
      (loop [form (read-form)] (conj result form))
      @result)))

(defn read-file [filename]
  (binding [*current-file* filename]
    (let [rdr       (StringReader. s)
          pbr       (readers/indexing-push-back-reader rdr)
          read-form #(edn/read {:read-cond :allow
                                :features  #{:clj}
                                :eof       ::END} pbr)
          result    (atom [])]
      (loop [form (read-form)] (conj result form))
      @result)))

(defn read-file [filename]
  (binding [*current-file* filename]
    (let [s         (io/input-stream filename)
          rdr       (io/reader s)
          pbr       (readers/indexing-push-back-reader rdr)
          read-form #(edn/read {:read-cond :allow
                                :features  #{:clj}
                                :eof       ::END} pbr)
          state     (atom {})]
      (loop [form (read-form)]
        (when (not= ::END form)
          (when (and (sequential? form) (= 'ns (first form)))
            (parse-namespace form state))
          (recur (read-form))))
      @state)))

(comment
  (read-forms "/Users/tonykay/fulcrologic/porting-tool/resources/sample.clj"))
