(ns com.fulcrologic.porting.specs
  (:require
    [clojure.test.check.generators :as tcgen]
    [clojure.spec.gen.alpha :as gen]
    [clojure.spec.alpha :as s]))

(s/def ::feature #{:clj :cljs :agnostic})

(s/def ::namespace (s/with-gen simple-symbol? #(s/gen '#{com.company.thing other.thing some.thing clojure.core cljs.test})))
(s/def ::artifact-name (s/with-gen qualified-symbol? #(s/gen '#{com.company.thing/grapple other.thing/log some.thing/boo clojure.core/reduce cljs.test/deftest})))
(s/def ::artifact-move (s/map-of ::artifact-name ::artifact-name))
(s/def ::artifact-delete (s/every ::artifact-name :kind set?))
(s/def ::namespace-move (s/map-of ::namespace ::namespace))
(s/def ::namespace-delete (s/every ::namespace :kind set?))
(s/def ::form-predicate (s/fspec
                          :args (s/cat :form any?)
                          :ret boolean?
                          :gen #(s/gen #{(fn predicate [_] true)})))

(s/def ::nsalias->ns (s/map-of simple-symbol? ::namespace))
(s/def ::raw-sym->fqsym (s/map-of simple-symbol? ::artifact-name))

(s/def ::parsing-env (s/keys :opt-un [::nsalias->ns
                                      ::raw-sym->fqsym]))

(s/def ::processing-env (s/map-of ::feature ::parsing-env))

(s/def ::transform-function (s/fspec
                              :args (s/cat :env ::env :form any?)
                              :ret any?
                              :gen #(s/gen #{(fn xform [e f] f)})))

(s/def ::form-transform (s/tuple ::form-predicate ::transform-function))
(s/def ::transforms (s/every ::form-transform :kind vector?))
(s/def ::lang-config (s/keys
                       :opt-un [::artifact-move
                                ::namespace-move
                                ::artifact-delete
                                ::namespace-delete
                                ::transforms]))

(s/def ::config (s/map-of ::feature ::lang-config))

(comment
  (gen/sample (s/gen ::config))


  (s/explain-str ::config
    {:clj {:transforms [
                        [(fn [f] 32) (fn [e f] f)]
                        ]}})
  )


