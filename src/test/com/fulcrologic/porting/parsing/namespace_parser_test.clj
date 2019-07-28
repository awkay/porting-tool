(ns com.fulcrologic.porting.parsing.namespace-parser-test
  (:require
    [com.fulcrologic.porting.input :as input]
    [com.fulcrologic.porting.parsing.namespace-parser :as nsp]
    [fulcro-spec.core :refer [specification assertions behavior]]))

(specification "namespace parser"
  (behavior "CLJC Parsing (clj)"
    (let [clj-forms (input/read-string "(ns sample\n  (:use\n    [clojure.java.ui])\n  (:require\n    #? (:clj [clojure.edn :as edn :refer [f g h]] :cljs [boo :as bah])\n    simple.namespace [clojure.java.io :as io :refer :all]))"
                      :clj)
          env       (nsp/parse-namespace {} (first clj-forms))
          {:keys [nsalias->ns raw-sym->fqsym]} env]
      (assertions
        "Adds resolution for aliased nses"
        (nsalias->ns 'io) => 'clojure.java.io
        (nsalias->ns 'edn) => 'clojure.edn
        "Adds resolution for global syms"
        (raw-sym->fqsym 'f) => 'clojure.edn/f
        (raw-sym->fqsym 'h) => 'clojure.edn/h)))
  (behavior "CLJC Parsing (cljs)"
    (let [forms (input/read-string "(ns sample\n  (:use\n    [clojure.java.ui])\n  (:require\n    #? (:clj [clojure.edn :as edn :refer [f g h]] :cljs [boo :as bah])\n    [clojure.java.io :as io :refer :all]))"
                  :cljs)
          env   (nsp/parse-namespace {} (first forms))
          {:keys [nsalias->ns raw-sym->fqsym]} env]
      (assertions
        "Detects the correct differences"
        (nsalias->ns 'io) => 'clojure.java.io
        (nsalias->ns 'bah) => 'boo)))
  (behavior "complex requires"
    (let [clj-forms (input/read-string "(ns boo
                                          (:require
                                            [clojure.io :as io]
                                            [com.fulcrologic.fulcro
                                                     [component :as comp :refer [defsc]]
                                                     [application :as app]]))"
                      :clj)
          env       (nsp/parse-namespace {} (first clj-forms))
          {:keys [nsalias->ns raw-sym->fqsym]} env]
      (assertions
        "can resolve prefixed globals"
        (raw-sym->fqsym 'defsc) => 'com.fulcrologic.fulcro.component/defsc
        "Adds resolution for aliased nses"
        (nsalias->ns 'comp) => 'com.fulcrologic.fulcro.component
        (nsalias->ns 'app) => 'com.fulcrologic.fulcro.application))))
