(ns com.fulcrologic.porting.fulcro2to3
  (:require
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [clojure.java.io :as io]
    [com.fulcrologic.porting.core :as core]
    [com.fulcrologic.porting.transforms.fulcro :as fulcro]
    [com.fulcrologic.porting.transforms.rename :as rename])
  (:gen-class)
  (:import (java.io File)))

(defn source-files
  "Given a starting path, returns a lazy seq of all of the source files."
  [^String starting-dir]
  (let [source? (fn [^File f]
                  (let [nm (.getName f)]
                    (or (str/ends-with? nm ".cljs")
                      (str/ends-with? nm ".clj")
                      (str/ends-with? nm ".cljc"))))]
    (filter source? (file-seq (io/file starting-dir)))))

(let [base {:fqname-old->new    {'fulcro.client.primitives/defsc        'com.fulcrologic.fulcro.components/defsc
                                 'fulcro.client.primitives/get-computed 'com.fulcrologic.fulcro.components/get-computed}
            :namespace-old->new {'fulcro.client.data-fetch 'com.fulcrologic.fulcro.data-fetch
                                 'fulcro.client.mutations  'com.fulcrologic.fulcro.mutations}

            :transforms         [rename/flatten-nested-libspecs-transform
                                 core/record-aliases
                                 rename/rename-artifacts-transform
                                 rename/rename-namespaces-transform
                                 rename/add-missing-namespaces-transform
                                 fulcro/update-lifecycle-transform
                                 fulcro/warn-missing-deps]
            :namespace->alias   {'com.fulcrologic.fulcro.components 'comp
                                 'com.fulcrologic.fulcro.data-fetch 'df
                                 'com.fulcrologic.fulcro.mutations  'm
                                 'com.fulcrologic.fulcro.dom        'dom
                                 'com.fulcrologic.fulcro.dom-server 'dom}}]
  (def fulcro-port-config {:agnostic base
                           :cljs     (merge base {:namespace-old->new {'fulcro.client.dom           'com.fulcrologic.fulcro.dom
                                                                       'fulcro.client.localized-dom 'com.fulcrologic.garden-css.dom
                                                                       }})
                           :clj      (merge base {:namespace-old->new {'fulcro.client.dom-server           'com.fulcrologic.fulcro.dom-server
                                                                       'fulcro.client.localized-dom-server 'com.fulcrologic.garden-css.dom-server
                                                                       }})}))

(defn -main [& args]
  ;(log/merge-config! {:output-fn output-fn})
  (log/info (str/join "," args))
  (doseq [dir args]
    (doseq [f (source-files dir)
            :let [filename (.getAbsolutePath f)]]
      (log/info "================================================================================")
      (log/info "Porting" filename)
      (core/process-file filename fulcro-port-config))))

(comment
  (-main "resources"))
