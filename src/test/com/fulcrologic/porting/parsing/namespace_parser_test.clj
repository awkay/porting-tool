(ns com.fulcrologic.porting.parsing.namespace-parser-test
  (:require
    [com.fulcrologic.porting.rewrite-clj.zip :as z]
    [com.fulcrologic.porting.parsing.namespace-parser :as nsp]
    [fulcro-spec.core :refer [specification assertions behavior]]
    [com.fulcrologic.porting.parsing.form-traversal :as ft]
    [taoensso.timbre :as log]))

(specification "namespace parser"
  (behavior "CLJC Parsing (clj)"
    (let [loc     (z/of-string "(ns sample\n  (:use\n    [clojure.java.ui])\n  (:require\n    #? (:clj [clojure.edn :as edn :refer [f g h]] :cljs [boo :as bah])\n    simple.namespace [clojure.java.io :as io :refer :all]))")
          ns-form (ft/loc->form loc :clj)
          env     (nsp/parse-namespace {} ns-form)
          {:keys [ns->alias nsalias->ns raw-sym->fqsym]} env]
      (assertions
        "Adds resolution for aliased nses"
        (get nsalias->ns 'io) => 'clojure.java.io
        (get nsalias->ns 'edn) => 'clojure.edn
        "Adds resolution from ns to alias"
        (get ns->alias 'clojure.java.io) => 'io
        (get ns->alias 'clojure.edn) => 'edn
        "Adds resolution for global syms"
        (get raw-sym->fqsym 'f) => 'clojure.edn/f
        (get raw-sym->fqsym 'h) => 'clojure.edn/h)))
  (behavior "CLJC Parsing (cljs)"
    (let [loc     (z/of-string "(ns sample\n  (:use\n    [clojure.java.ui])\n  (:require\n    #? (:clj [clojure.edn :as edn :refer [f g h]] :cljs [boo :as bah])\n    [clojure.java.io :as io :refer :all]))")
          ns-form (ft/loc->form loc :cljs)
          env     (nsp/parse-namespace {} ns-form)
          {:keys [nsalias->ns raw-sym->fqsym]} env]
      (assertions
        "Detects the correct differences"
        (nsalias->ns 'io) => 'clojure.java.io
        (nsalias->ns 'bah) => 'boo)))
  (behavior "complex requires"
    (let [loc     (z/of-string "(ns boo
                                          (:require
                                            [clojure.io :as io]
                                            [com.fulcrologic.fulcro
                                                     [component :as comp :refer [defsc]]
                                                     [application :as app]]))")
          ns-form (ft/loc->form loc :clj)
          env     (nsp/parse-namespace {} ns-form)
          {:keys [nsalias->ns raw-sym->fqsym]} env]
      (assertions
        "can resolve prefixed globals"
        (raw-sym->fqsym 'defsc) => 'com.fulcrologic.fulcro.component/defsc
        "Adds resolution for aliased nses"
        (nsalias->ns 'comp) => 'com.fulcrologic.fulcro.component
        (nsalias->ns 'app) => 'com.fulcrologic.fulcro.application))))
