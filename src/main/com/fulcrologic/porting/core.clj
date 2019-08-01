(ns com.fulcrologic.porting.core
  (:require
    [com.fulcrologic.porting.specs :as pspec]
    [ghostwheel.core :refer [>defn =>]]
    [clojure.pprint :as pp :refer [pprint]]
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [rewrite-clj.parser :as rw]
    [rewrite-clj.zip :as z]
    [com.fulcrologic.porting.input :as input]
    [com.fulcrologic.porting.transforms.rename :as rename]
    [com.fulcrologic.porting.specs :as pspec]
    [com.fulcrologic.porting.parsing.form-traversal :as ft]
    [com.fulcrologic.porting.parsing.namespace-parser :as nsparser]
    [com.fulcrologic.porting.parsing.util :as util]
    [clojure.pprint :as pprint]
    [clojure.tools.reader :as reader]
    [clojure.tools.reader.impl.errors :as err]))

(defn do-processing-passes [processing-env forms]
  (let [state         (atom {})
        ;; PASS 1: You have access to a state-atom that you can swap! against to record information
        _             (reduce
                        (fn [result f]
                          (conj result (ft/process-form
                                         (assoc processing-env
                                           :pass 1
                                           :state-atom state) f)))
                        []
                        forms)
        ;; PASS 2: You have the *value* as :state of the previous pass's information
        updated-forms (reduce
                        (fn [result f]
                          (conj result (ft/process-form (assoc processing-env
                                                          :pass 2
                                                          :state @state) f)))
                        []
                        forms)]
    updated-forms))

(>defn process-single
  [filename config lang]
  [string? ::pspec/config #{:clj :cljs} => any?]
  (let [forms          (input/read-file filename lang)
        nsform         (first (filter #(= 'ns (first %)) forms))
        parsing-env    (nsparser/parse-namespace {} nsform)
        processing-env (util/processing-env {:parsing-envs    {lang parsing-env}
                                             :config          config
                                             :feature-context lang})]
    (do-processing-passes processing-env forms)))


(>defn process-cljc
  [filename config]
  [string? ::pspec/config => any?]
  (let [clj-forms          (input/read-file filename :clj)
        cljs-forms         (input/read-file filename :cljs)
        cljc-forms         (input/read-file filename :agnostic)
        common-forms       (input/read-file filename :none)
        clj-nsform         (first (filter #(= 'ns (first %)) clj-forms))
        cljs-nsform        (first (filter #(= 'ns (first %)) cljs-forms))
        common-nsform      (first (filter #(= 'ns (first %)) common-forms))
        clj-parsing-env    (nsparser/parse-namespace {} clj-nsform)
        cljs-parsing-env   (nsparser/parse-namespace {} cljs-nsform)
        common-parsing-env (nsparser/parse-namespace {} common-nsform)
        processing-env     (util/processing-env {:parsing-envs    {:clj      clj-parsing-env
                                                                   :agnostic common-parsing-env
                                                                   :cljs     cljs-parsing-env}
                                                 :config          config
                                                 :feature-context :agnostic})]
    (do-processing-passes processing-env cljc-forms)))

(defn pprint-vector [avec]
  (when (::syntax-quoted? (meta avec))
    (print "`"))
  (pprint/pprint-logical-block :prefix "[" :suffix "]"
    (pprint/print-length-loop [aseq (seq avec)]
      (when aseq
        (pprint/write-out (first aseq))
        (when (next aseq)
          (.write ^java.io.Writer *out* " ")
          (pprint/pprint-newline :linear)
          (recur (next aseq)))))))


(def pprint-array (pprint/formatter-out "~<[~;~@{~w~^, ~:_~}~;]~:>"))

(defn read-tagged [rdr initch opts pending-forms]
  (let [tag (#'reader/read* rdr true nil opts pending-forms)]
    (if-not (symbol? tag)
      (err/throw-bad-reader-tag rdr tag))
    (tagged-literal tag (#'reader/read* rdr true nil opts pending-forms))))

(defn mess-around-with-other-peoples-crap! []
  (alter-var-root #'clojure.tools.reader/read-tagged (constantly read-tagged))
  (alter-var-root #'clojure.tools.reader/read-syntax-quote
    (constantly (fn [r b o p]
                  (vary-meta (#'clojure.tools.reader/read* r true nil o p) assoc ::syntax-quoted? true))))

  (defmethod pprint/code-dispatch clojure.lang.IPersistentVector [v]
    (pprint-vector v))

  (defmethod pprint/code-dispatch clojure.lang.ReaderConditional [{:keys [form splicing?]}]
    (pprint/write-out (if splicing? (symbol "#?@") (symbol "#?")))
    (pprint/pprint-logical-block :prefix "(" :suffix ")"
      (let [forms (partition 2 form)]
        (loop [fs (seq forms)]
          (when fs
            (let [[feature form] (first fs)]
              (pprint/write-out feature)
              (.write ^java.io.Writer *out* " ")
              (pprint/write-out form)
              (when (next fs)
                (pprint/pprint-newline :linear)
                (recur (next fs))))))))))

(>defn process-file
  "Process a clj/cljs/cljc file using the given config."
  [filename config]
  [string? ::pspec/config => any?]
  (mess-around-with-other-peoples-crap!)
  (binding [util/*current-file*        filename
            pp/*print-pprint-dispatch* pp/code-dispatch]
    (try
      (doseq [f (cond
                  (str/ends-with? filename ".cljs") (process-single filename config :cljs)
                  (str/ends-with? filename ".cljc") (process-cljc filename config)
                  (str/ends-with? filename ".clj") (process-single filename config :clj)
                  :else (util/report-error! (str "Cannot determine language from file " filename)))]
        (pprint f)
        (println))
      (catch Exception e
        (log/error e "Processing aborted for" filename)))))

(comment
  (process-file "./resources/sample.clj" {:clj {:fqname-old->new  {'clojure.edn/f           'com.boo/f
                                                                   'clojure.edn/g           'other.ns/g
                                                                   'clojure.edn/read-string 'my-reader/read-it}
                                                :transforms       [rename/rename-artifacts-transform]
                                                :namespace->alias {'com.boo   'boo
                                                                   'other.ns  'other
                                                                   'my-reader 'r}}})

  (let [in (rw/parse-file-all "./resources/trial.cljc")
        data (z/of-file "./resources/trial.cljc")]
    (z/right (z/find-value data z/next 'ns))
    )


  (let [base   {:fqname-old->new    {'fulcro.client.primitives/defsc        'com.fulcrologic.fulcro.components/defsc
                                     'fulcro.client.primitives/get-computed 'com.fulcrologic.fulcro.components/get-computed}
                :namespace-old->new {'fulcro.client.data-fetch 'com.fulcrologic.fulcro.data-fetch
                                     'fulcro.client.mutations  'com.fulcrologic.fulcro.mutations}
                :transforms         [rename/rename-artifacts-transform
                                     rename/rename-namespaces-transform
                                     rename/add-missing-namespaces-transform]
                :namespace->alias   {'com.fulcrologic.fulcro.components 'comp
                                     'com.fulcrologic.fulcro.data-fetch 'df
                                     'com.fulcrologic.fulcro.mutations  'm
                                     'com.fulcrologic.fulcro.dom        'dom
                                     'com.fulcrologic.fulcro.dom-server 'dom}}
        config {:agnostic base
                :cljs     (merge base {:namespace-old->new {'fulcro.client.dom 'com.fulcrologic.fulcro.dom}})
                :clj      (merge base {:namespace-old->new {'fulcro.client.dom-server 'com.fulcrologic.fulcro.dom-server}})}]
    (process-file "./resources/trial.cljc" config)))
