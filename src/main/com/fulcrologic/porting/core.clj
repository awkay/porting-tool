(ns com.fulcrologic.porting.core
  (:require
    [com.fulcrologic.porting.specs :as pspec]
    [ghostwheel.core :refer [>defn =>]]
    [clojure.string :as str]
    [clojure.pprint :refer [pprint]]
    [com.fulcrologic.porting.transforms.rename :as rename]
    [com.fulcrologic.porting.specs :as pspec]
    [com.fulcrologic.porting.parsing.form-traversal :as ft]
    [com.fulcrologic.porting.parsing.namespace-parser :as nsparser]
    [com.fulcrologic.porting.parsing.util :as util]
    [com.fulcrologic.porting.rewrite-clj.zip :as z]
    [taoensso.timbre :as log]))

(defn do-processing-passes [filename processing-env]
  (let [state     (atom {})
        ;; PASS 1: You have access to a state-atom that you can swap! against to record information
        env1      (loop [env (assoc processing-env :pass 1 :state-atom state)]
                    (let [{:keys [zloc] :as new-env} (ft/process-form env)]
                      (if-let [next-loc (z/right zloc)]
                        (recur (assoc new-env :zloc next-loc))
                        new-env)))
        ;; PASS 2: You have the *value* as :state of the previous pass's information
        final-env (loop [env (assoc processing-env :pass 2 :state @state)]
                    (let [{new-loc :zloc :as new-env} (ft/process-form env)]
                      (if-let [next-loc (z/right new-loc)]
                        (recur (assoc new-env :zloc next-loc))
                        (do
                          (spit filename (with-out-str (z/print-root new-loc)))
                          (assoc new-env :zloc (z/root new-loc))))))]
    (dissoc final-env :state :pass)))

(>defn process-single
  [filename config lang]
  [string? ::pspec/config #{:clj :cljs} => ::pspec/processing-env]
  (let [processing-env (util/processing-env {:parsing-envs    {lang {}}
                                             :zloc            (z/of-file filename)
                                             :config          config
                                             :feature-context lang})]
    (do-processing-passes filename processing-env)))


(>defn process-cljc
  [filename config]
  [string? ::pspec/config => ::pspec/processing-env]
  (let [processing-env (util/processing-env {:zloc            (z/of-file filename)
                                             :config          config
                                             :feature-context :agnostic})]
    (do-processing-passes filename processing-env)))

(>defn process-file
  "Process a clj/cljs/cljc file using the given config."
  [filename config]
  [string? ::pspec/config => any?]
  (binding [util/*current-file* filename]
    (try
      (cond
        (str/ends-with? filename ".cljs") (process-single filename config :cljs)
        (str/ends-with? filename ".cljc") (process-cljc filename config)
        (str/ends-with? filename ".clj") (process-single filename config :clj)
        :else (util/report-error! (str "Cannot determine language from file " filename)))
      (catch Exception e
        (log/error e "Processing died due to an exception")
        (util/report-error! (str "Processing aborted for" filename))))))

(defn record-aliases [env]
  (let [f (ft/current-form env)]
    (if (and (list? f) (= 'ns (first f)))
      (let [clj-form      (ft/current-form env :clj)
            cljs-form     (ft/current-form env :cljs)
            agnostic-form (ft/current-form env :none)
            new-env       (-> env
                            (update-in [:parsing-envs :clj] nsparser/parse-namespace clj-form)
                            (update-in [:parsing-envs :cljs] nsparser/parse-namespace cljs-form)
                            (update-in [:parsing-envs :agnostic] nsparser/parse-namespace agnostic-form))]
        new-env)
      env)))
