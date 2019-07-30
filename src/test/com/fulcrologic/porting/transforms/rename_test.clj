(ns com.fulcrologic.porting.transforms.rename-test
  (:require
    [com.fulcrologic.porting.transforms.rename :as rename]
    [fulcro-spec.core :refer [specification assertions behavior component when-mocking]]
    [com.fulcrologic.porting.parsing.util :as util]
    [clojure.string :as str]))

(specification "rename transform"
  (component "predicate"
    (assertions
      "detects symbols"
      (= symbol? (first rename/rename-artifacts-transform)) => true))
  (component "Transform"
    (let [env   (util/processing-env
                  {:feature-context :clj
                   :parsing-envs    {:clj {:nsalias->ns    {'prim  'fulcro.client.primitives
                                                            'thing 'other.ns}
                                           :ns->alias      {'com.computation.comp 'comp
                                                            'other.ns             'thing}
                                           :raw-sym->fqsym {'defsc 'fulcro.client.primitives/defsc}}}
                   :config          {:clj {:namespace->alias {'com.fulcrologic.fulcro.components 'comp
                                                              'com.thing                         'thing}
                                           :fqname-old->new  {'fulcro.client.primitives/defsc
                                                              'com.fulcrologic.fulcro.components/defsc

                                                              'com.a/x
                                                              'com.thing/x

                                                              'com.a/a
                                                              'com.b/a}}}})
          xform (second rename/rename-artifacts-transform)]
      (assertions
        "Remaps fq symbols"
        (xform env 'fulcro.client.primitives/defsc) => 'comp/defsc
        "Remaps symbols according to the configuration for the lang context"
        (xform env 'com.a/a) => 'com.b/a
        "Re-aliases the target name if there is a new alias defined"
        (xform env 'prim/defsc) => 'comp/defsc
        (xform env 'fulcro.client.primitives/defsc) => 'comp/defsc)

      (when-mocking
        (util/report-error! m f) => (assertions
                                       "issues an error if there is a conflicting alias already in the file's ns"
                                       (str/includes? m "has namespace alias `thing`, but it refers to `other.ns`") => true)
        (xform env 'com.a/x)))))
