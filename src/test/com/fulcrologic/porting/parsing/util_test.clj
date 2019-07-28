(ns com.fulcrologic.porting.parsing.util-test
  (:require
    [com.fulcrologic.porting.parsing.util :as util]
    [fulcro-spec.core :refer [specification assertions behavior]]))

(declare =>)

(specification "find-map-vals"
  (assertions
    "Can find the desired values in a recursive data structure"
    (util/find-map-vals {:x [1 2 {:k 1}]} :k) => #{1}
    "returns a set"
    (set? (util/find-map-vals {:x [1 2 {:k 1}]} :k)) => true
    (set? (util/find-map-vals {} :k)) => true
    "finds values even if they are in nested matches"
    (util/find-map-vals {:x [1 2 {:k 1 :other {:k 2}}]} :k) => #{1 2}))

(specification "clear-raw-syms"
  (behavior "Clears raw symbols from the parsing environments of a processing env"
    (let [processing-env (util/processing-env {:parsing-envs {:clj      {:raw-sym->fqsym {'c  'foo/c
                                                                                          'c2 'foo/c2}}
                                                              :cljs     {:raw-sym->fqsym {'s1 'foo/s1
                                                                                          's2 'foo/s2}}
                                                              :agnostic {:raw-sym->fqsym {'a1 'foo/a
                                                                                          'a2 'foo/a2}}}})]
      (assertions
        "Only affects the feature languages specified"
        (util/clear-raw-syms processing-env #{'s1} #{:clj}) => processing-env
        (util/clear-raw-syms processing-env #{'s1} #{:agnostic}) => processing-env
        (util/clear-raw-syms processing-env #{'s1} #{:cljs})
        => (update-in processing-env [:parsing-envs :cljs :raw-sym->fqsym] dissoc 's1)))))

(specification "sym->fqsym"
  (behavior "resolves a known symbol"
    (let [processing-env (util/processing-env
                           {:feature-context :cljs
                            :parsing-envs    {:clj      {:raw-sym->fqsym {'c  'foo/c
                                                                          'c2 'foo/c2}}
                                              :cljs     {:nsalias->ns    {'sns 'com.boo.sns}
                                                         :raw-sym->fqsym {'s1 'foo/s1
                                                                          's2 'foo/s2}}
                                              :agnostic {:raw-sym->fqsym {'a1 'foo/a
                                                                          'a2 'foo/a2}}}})]
      (assertions
        "can resolve simple smbols for their context"
        (util/sym->fqsym processing-env 's1) => 'foo/s1
        (util/sym->fqsym (assoc processing-env :feature-context :agnostic) 'a1) => 'foo/a
        "can resolve symbols that have an aliased ns"
        (util/sym->fqsym processing-env 'sns/some-function) => 'com.boo.sns/some-function))))

(specification "fqsym->aliased-sym"
  (behavior "resolves a known symbol"
    (let [processing-env (util/processing-env
                           {:feature-context :cljs
                            :config          {:cljs {:namespace-aliases {'com.boo.sns 'sns}}}
                            :parsing-envs    {:cljs {:nsalias->ns    {'sns 'com.boo.sns}
                                                     :raw-sym->fqsym {}}}})]
      (assertions
        "can resolve fully-qualified smbols to a shorter version using feature context aliases"
        (util/fqsym->aliased-sym processing-env 'com.boo.sns/foo) => 'sns/foo))))

(specification "require-for"
  (behavior "returns a valid require clause"
    (let [processing-env (util/processing-env
                           {:feature-context :cljs
                            :config          {:cljs {:namespace-aliases {'com.boo.sns 'sns}}}})]
      (assertions
        "returns the simple lib name if no aliases are configured"
        (util/require-for processing-env 'com.boo.other) => 'com.boo.other
        "can resolve fully-qualified smbols to a shorter version using feature context aliases"
        (util/require-for processing-env 'com.boo.sns) => '[com.boo.sns :as sns]))))
