(ns com.fulcrologic.porting.parsing.util-test
  (:require
    [com.fulcrologic.porting.parsing.util :as util]
    [fulcro-spec.core :refer [specification assertions behavior]]))

(declare =>)

(specification "find-maplike-vals"
  (assertions
    "Can find the desired values in a recursive data structure"
    (util/find-maplike-vals {:x [1 2 {:k 1 :x [:k 2]}]} :k) => #{1 2}
    "returns a set"
    (set? (util/find-maplike-vals {:x [1 2 {:k 1}]} :k)) => true
    (set? (util/find-maplike-vals {} :k)) => true
    "finds values even if they are in nested matches"
    (util/find-maplike-vals {:x [1 2 {:k 1 :other {:k 2}}]} :k) => #{1 2}))

(specification "clear-raw-syms"
  (behavior "Clears raw symbols from the parsing environments of a processing env"
    (let [processing-env (util/processing-env {:feature-context :cljs
                                               :parsing-envs    {:cljs {:raw-sym->fqsym {'s1 'foo/s1
                                                                                         's2 'foo/s2}}}})]
      (assertions
        "Removes simple symbols from the raw symbol lookup in a feature context"
        (util/clear-raw-syms processing-env #{'s1})
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

(specification "require-for"
  (behavior "returns a valid require clause"
    (let [processing-env (util/processing-env
                           {:feature-context :cljs
                            :config          {:cljs {:namespace->alias {'com.boo.sns 'sns}}}})]
      (assertions
        "returns the simple lib name if no aliases are configured"
        (util/require-for processing-env 'com.boo.other) => 'com.boo.other
        "can resolve fully-qualified smbols to a shorter version using feature context aliases"
        (util/require-for processing-env 'com.boo.sns) => '[com.boo.sns :as sns]))))

(specification "bound-syms"
  (behavior "finds symbol bindings from binding expressions"
    (assertions
      "can detect simple symbols"
      (util/bound-syms 'a) => #{'a}
      "can detect symbols in a vector destructure"
      (util/bound-syms '[a b]) => #{'a 'b}
      "can detect symbols in a vector destructure :as clause"
      (util/bound-syms '[:as c]) => #{'c}
      "can detect symbols in a map destructure's :keys"
      (util/bound-syms '{:keys [a b]}) => #{'a 'b}
      "can detect symbols in a map destructure's :or clause"
      (util/bound-syms '{:keys [a b] :or {a s}}) => #{'a 'b 's}
      "can detect symbols in a map destructure's :as clause"
      (util/bound-syms '{:keys [a b] :as x}) => #{'a 'b 'x}
      "can detect symbols in a map destructure's key area"
      (util/bound-syms '{L :a}) => #{'L})))
