(ns com.fulcrologic.porting.core
  (:require
    [com.fulcrologic.porting.specs :as pspec]
    [ghostwheel.core :refer [>defn =>]]
    [clojure.pprint :as pp :refer [pprint]]
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [com.fulcrologic.porting.input :as input]
    [com.fulcrologic.porting.transforms.rename :as rename]
    [com.fulcrologic.porting.specs :as pspec]
    [com.fulcrologic.porting.parsing.form-traversal :as ft]
    [com.fulcrologic.porting.parsing.namespace-parser :as nsparser]
    [com.fulcrologic.porting.parsing.util :as util]))

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

(>defn process-file
  "Process a clj/cljs/cljc file using the given config."
  [filename config]
  [string? ::pspec/config => any?]
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


  (let [base {:fqname-old->new  {'fulcro.client.primitives/defsc        'com.fulcrologic.fulcro.components/defsc
                                 'fulcro.client.primitives/get-computed 'com.fulcrologic.fulcro.components/get-computed}
              :transforms       [rename/rename-artifacts-transform
                                 rename/rename-namespaces-transform
                                 rename/add-missing-namespaces-transform]
              :namespace->alias {'com.fulcrologic.fulcro.components 'comp
                                 'com.fulcrologic.fulcro.dom        'dom
                                 'com.fulcrologic.fulcro.dom-server 'dom}}
        config {:agnostic base
                :cljs     (merge base {:namespace-old->new {'fulcro.client.dom 'com.fulcrologic.fulcro.dom}})
                :clj      (merge base {:namespace-old->new {'fulcro.client.dom-server 'com.fulcrologic.fulcro.dom-server}})}]
    ))
