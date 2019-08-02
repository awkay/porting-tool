(ns com.fulcrologic.porting.core
  (:require
    [com.fulcrologic.porting.specs :as pspec]
    [ghostwheel.core :refer [>defn =>]]
    [clojure.pprint :as pp :refer [pprint]]
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [rewrite-clj.parser :as rw]
    [rewrite-clj.zip :as z]
    ;[com.fulcrologic.porting.transforms.rename :as rename]
    [com.fulcrologic.porting.specs :as pspec]
    [com.fulcrologic.porting.parsing.form-traversal :as ft]
    [com.fulcrologic.porting.parsing.util :as util]))

(defn do-processing-passes [processing-env]
  (let [state     (atom {})
        ;; PASS 1: You have access to a state-atom that you can swap! against to record information
        _         (loop [env (assoc processing-env :pass 1 :state-atom state)]
                    (let [loc (ft/current-loc env)]
                      (ft/process-form env)
                      (when-let [next-loc (z/right loc)]
                        (recur (assoc env :zloc next-loc)))))
        ;; PASS 2: You have the *value* as :state of the previous pass's information
        final-env (loop [env (assoc processing-env :pass 2 :state @state)]
                    (let [loc (ft/current-loc env)]
                      (ft/process-form env)
                      (if-let [next-loc (z/right loc)]
                        (recur (assoc env :zloc next-loc))
                        (do
                          #_(z/print-root loc)
                          (assoc env :zloc (z/root loc))))))]
    (dissoc final-env :state :pass)))

(>defn process-single
  [filename config lang]
  [string? ::pspec/config #{:clj :cljs} => ::pspec/processing-env]
  (let [processing-env (util/processing-env {:parsing-envs    {lang {}}
                                             :zloc            (z/of-file filename)
                                             :config          config
                                             :feature-context lang})]
    (do-processing-passes processing-env)))


(>defn process-cljc
  [filename config]
  [string? ::pspec/config => ::pspec/processing-env]
  (let [clj-parsing-env    {} #_(nsparser/parse-namespace {} clj-nsform)
        cljs-parsing-env   {} #_(nsparser/parse-namespace {} cljs-nsform)
        common-parsing-env {} #_(nsparser/parse-namespace {} common-nsform)
        processing-env     (util/processing-env {:parsing-envs    {:clj      clj-parsing-env
                                                                   :agnostic common-parsing-env
                                                                   :cljs     cljs-parsing-env}
                                                 :zloc            (z/of-file filename)
                                                 :config          config
                                                 :feature-context :agnostic})]
    (do-processing-passes processing-env)))

(>defn process-file
  "Process a clj/cljs/cljc file using the given config."
  [filename config]
  [string? ::pspec/config => any?]
  (binding [util/*current-file*        filename
            pp/*print-pprint-dispatch* pp/code-dispatch]
    (try
      (cond
        (str/ends-with? filename ".cljs") (process-single filename config :cljs)
        (str/ends-with? filename ".cljc") (process-cljc filename config)
        (str/ends-with? filename ".clj") (process-single filename config :clj)
        :else (util/report-error! (str "Cannot determine language from file " filename)))
      (catch Exception e
        (log/error e "Processing aborted for" filename)))))

(defn record-aliases [env]
  (if (and (ft/within env 'ns) (ft/within env :require) (= :require (ft/current-form env)))
    (let [f   (ft/current-form env)
          loc (ft/current-loc env)]
      (log/info "REQUIRE" f)
      env))



  env)

(comment
  (process-file "./resources/sample.clj" {:clj {:fqname-old->new  {'clojure.edn/f           'com.boo/f
                                                                   'clojure.edn/g           'other.ns/g
                                                                   'clojure.edn/read-string 'my-reader/read-it}
                                                :transforms       [rename/rename-artifacts-transform]
                                                :namespace->alias {'com.boo   'boo
                                                                   'other.ns  'other
                                                                   'my-reader 'r}}})

  (let [in   (rw/parse-file-all "./resources/trial.cljc")
        data (z/of-file "./resources/trial.cljc")]
    (z/right (z/find-value data z/next 'ns))
    )


  (let [base   {:fqname-old->new    {'fulcro.client.primitives/defsc        'com.fulcrologic.fulcro.components/defsc
                                     'fulcro.client.primitives/get-computed 'com.fulcrologic.fulcro.components/get-computed}
                :namespace-old->new {'fulcro.client.data-fetch 'com.fulcrologic.fulcro.data-fetch
                                     'fulcro.client.mutations  'com.fulcrologic.fulcro.mutations}

                :transforms         [record-aliases]
                :namespace->alias   {'com.fulcrologic.fulcro.components 'comp
                                     'com.fulcrologic.fulcro.data-fetch 'df
                                     'com.fulcrologic.fulcro.mutations  'm
                                     'com.fulcrologic.fulcro.dom        'dom
                                     'com.fulcrologic.fulcro.dom-server 'dom}}
        config {:agnostic base
                :cljs     (merge base {:namespace-old->new {'fulcro.client.dom 'com.fulcrologic.fulcro.dom}})
                :clj      (merge base {:namespace-old->new {'fulcro.client.dom-server 'com.fulcrologic.fulcro.dom-server}})}]
    (process-file "./resources/trial.cljc" config)
    :ok))
