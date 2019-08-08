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

(let [base {:fqname-old->new    {
                                 'fulcro.client.data-fetch/append-to                       'com.fulcrologic.fulcro.algorithms.data-targeting/append-to
                                 'fulcro.client.data-fetch/data-state?                        'com.fulcrologic.fulcro.data-fetch/data-state?
                                 'fulcro.client.data-fetch/elide-query-nodes                        'com.fulcrologic.fulcro.algorithms.misc/elide-query-nodes
                                 'fulcro.client.data-fetch/failed?                         'com.fulcrologic.fulcro.data-fetch/failed?
                                 'fulcro.client.data-fetch/load                            'com.fulcrologic.fulcro.data-fetch/load!
                                 'fulcro.client.data-fetch/load-field                      'com.fulcrologic.fulcro.data-fetch/load-field!
                                 'fulcro.client.data-fetch/load-params*                    'com.fulcrologic.fulcro.data-fetch/load-params*
                                 'fulcro.client.data-fetch/loading?                        'com.fulcrologic.fulcro.data-fetch/loading?
                                 'fulcro.client.data-fetch/marker-table                        'com.fulcrologic.fulcro.data-fetch/marker-table
                                 'fulcro.client.data-fetch/multiple-targets                'com.fulcrologic.fulcro.algorithms.data-targeting/multiple-targets
                                 'fulcro.client.data-fetch/prepend-to                      'com.fulcrologic.fulcro.algorithms.data-targeting/prepend-to
                                 'fulcro.client.data-fetch/ready?                          'com.fulcrologic.fulcro.data-fetch/ready?
                                 'fulcro.client.data-fetch/refresh!                        'com.fulcrologic.fulcro.data-fetch/refresh!
                                 'fulcro.client.data-fetch/replace-at                      'com.fulcrologic.fulcro.algorithms.data-targeting/replace-at
                                 'fulcro.client.impl.data-targeting/append-to              'com.fulcrologic.fulcro.algorithms.data-targeting/append-to
                                 'fulcro.client.impl.data-targeting/multiple-targets       'com.fulcrologic.fulcro.algorithms.data-targeting/multiple-targets
                                 'fulcro.client.impl.data-targeting/multiple-targets?      'com.fulcrologic.fulcro.algorithms.data-targeting/multiple-targets?
                                 'fulcro.client.impl.data-targeting/prepend-to             'com.fulcrologic.fulcro.algorithms.data-targeting/prepend-to
                                 'fulcro.client.impl.data-targeting/replace-at             'com.fulcrologic.fulcro.algorithms.data-targeting/replace-at
                                 'fulcro.client.primitives/any->reconciler                 'com.fulcrologic.fulcro.components/any->app
                                 'fulcro.client.primitives/app-root                        'com.fulcrologic.fulcro.application/app-root
                                 'fulcro.client.primitives/ast->query                      'edn-query-language.core/ast->query
                                 'fulcro.client.primitives/classname->class                'com.fulcrologic.fulcro.components/registry-key->class
                                 'fulcro.client.primitives/component->state-map            'com.fulcrologic.fulcro.components/component->state-map
                                 'fulcro.client.primitives/component-pre-merge             'com.fulcrologic.fulcro.algorithms.merge/component-pre-merge
                                 'fulcro.client.primitives/component?                      'com.fulcrologic.fulcro.components/component?
                                 'fulcro.client.primitives/computed                        'com.fulcrologic.fulcro.components/computed
                                 'fulcro.client.primitives/db->tree                        'com.fulcrologic.fulcro.algorithms.denormalize/db->tree
                                 'fulcro.client.primitives/defsc                           'com.fulcrologic.fulcro.components/defsc
                                 'fulcro.client.primitives/force-root-render!              'com.fulcrologic.fulcro.application/force-root-render!
                                 'fulcro.client.primitives/get-computed                    'com.fulcrologic.fulcro.components/get-computed
                                 'fulcro.client.primitives/get-ident                       'com.fulcrologic.fulcro.components/get-ident
                                 'fulcro.client.primitives/get-initial-state               'com.fulcrologic.fulcro.components/get-initial-state
                                 'fulcro.client.primitives/get-query                       'com.fulcrologic.fulcro.components/get-query
                                 'fulcro.client.primitives/get-state                       'com.fulcrologic.fulcro.components/get-state
                                 'fulcro.client.primitives/integrate-ident                 'com.fulcrologic.fulcro.algorithms.merge/integrate-ident*
                                 'fulcro.client.primitives/merge-alternate-union-elements  'com.fulcrologic.fulcro.algorithms.merge/merge-alternate-union-elements
                                 'fulcro.client.primitives/merge-alternate-union-elements! 'com.fulcrologic.fulcro.algorithms.merge/merge-alternate-union-elements!
                                 'fulcro.client.primitives/merge-component                 'com.fulcrologic.fulcro.algorithms.merge/merge-component
                                 'fulcro.client.primitives/merge-component!                'com.fulcrologic.fulcro.algorithms.merge/merge-component!
                                 'fulcro.client.primitives/merge-mutation-joins            'com.fulcrologic.fulcro.algorithms.merge/merge-mutation-joins
                                 'fulcro.client.primitives/pre-merge-transform             'com.fulcrologic.fulcro.algorithms.merge/pre-merge-transform
                                 'fulcro.client.primitives/query->ast                      'edn-query-language.core/query->ast
                                 'fulcro.client.primitives/query->ast1                     'edn-query-language.core/query->ast1
                                 'fulcro.client.primitives/set-state!                      'com.fulcrologic.fulcro.components/set-state!
                                 'fulcro.client.primitives/tempid                          'com.fulcrologic.fulcro.algorithms.tempid/tempid
                                 'fulcro.client.primitives/tempid?                         'com.fulcrologic.fulcro.algorithms.tempid/tempid?
                                 'fulcro.client.primitives/transact!                       'com.fulcrologic.fulcro.components/transact!
                                 'fulcro.client.primitives/tree->db                        'com.fulcrologic.fulcro.algorithms.normalize/tree->db
                                 'fulcro.client.primitives/update-state!                   'com.fulcrologic.fulcro.components/update-state!
                                 'fulcro.client.primitives/with-parent-context             'com.fulcrologic.fulcro.components/with-parent-context
                                 ;'fulcro.client.primitives/integrate-ident!    'com.fulcrologic.fulcro.algorithms.merge/missing
                                 }

            :namespace-old->new {'fulcro.client.data-fetch 'com.fulcrologic.fulcro.data-fetch
                                 'fulcro.client.dom-common 'com.fulcrologic.fulcro.dom-common
                                 'fulcro.client.mutations  'com.fulcrologic.fulcro.mutations}

            :deleted-namespaces '#{fulcro.client.primitives
                                   fulcro.client.routing
                                   fulcro.client.impl.parser
                                   fulcro.client.cards
                                   fulcro.client.util
                                   fulcro.client.network
                                   fulcro.client.websockets
                                   fulcro.client.logging
                                   fulcro.client.impl.protocols
                                   fulcro.client.impl.application
                                   fulcro.client.impl.data-fetch
                                   fulcro.client.impl.data-targeting}

            :transforms         [core/record-aliases
                                 rename/flatten-nested-libspecs-transform
                                 rename/rename-artifacts-transform
                                 rename/rename-namespaces-transform
                                 rename/add-missing-namespaces-transform
                                 fulcro/update-lifecycle-transform
                                 ;fulcro/warn-missing-deps
                                 rename/delete-namespaces-transform]
            :namespace->alias   {'com.fulcrologic.fulcro.algorithms.tempid              'tempid
                                 'com.fulcrologic.fulcro.algorithms.normalize           'fnorm
                                 'com.fulcrologic.fulcro.algorithms.denormalize         'fdn
                                 'com.fulcrologic.fulcro.algorithms.merge               'merge
                                 'com.fulcrologic.fulcro.algorithms.tx-processing       'txn
                                 'com.fulcrologic.fulcro.algorithms.application-helpers 'ah
                                 'com.fulcrologic.fulcro.algorithms.form-state          'fs
                                 'com.fulcrologic.fulcro.algorithms.misc                'fmisc
                                 'com.fulcrologic.fulcro.algorithms.data-targeting      'targeting
                                 'com.fulcrologic.fulcro.application                    'app
                                 'com.fulcrologic.fulcro.components                     'comp
                                 'com.fulcrologic.fulcro.data-fetch                     'df
                                 'com.fulcrologic.fulcro.mutations                      'm
                                 'com.fulcrologic.fulcro.dom                            'dom
                                 'com.fulcrologic.fulcro.dom-server                     'dom
                                 'edn-query-language.core                               'eql}}]
  (def fulcro-port-config {:agnostic base
                           :cljs     (merge base {:namespace-old->new {'fulcro.client.dom           'com.fulcrologic.fulcro.dom
                                                                       'fulcro.client.localized-dom 'com.fulcrologic.garden-css.dom}})
                           :clj      (merge base {:namespace-old->new {'fulcro.client.dom-server           'com.fulcrologic.fulcro.dom-server
                                                                       'fulcro.client.localized-dom-server 'com.fulcrologic.garden-css.dom-server}})}))

(defn -main [& args]
  (log/merge-config! {:output-fn log/default-output-fn})
  (log/info (str/join "," args))
  (doseq [dir args]
    (doseq [f (source-files dir)
            :let [filename (.getAbsolutePath f)]]
      (log/info "================================================================================")
      (log/info "Porting" filename)
      (core/process-file filename fulcro-port-config))))

(comment
  (-main "resources"))
