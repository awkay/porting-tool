(ns com.fulcrologic.porting.specs
  (:require
    [rewrite-clj.zip :as z]
    [clojure.test.check.generators :as tcgen]
    [clojure.core.specs.alpha :as specs]
    [clojure.spec.gen.alpha :as gen]
    [clojure.spec.alpha :as s]))

(s/def ::feature #{:clj :cljs :agnostic :none})

(s/def ::namespace (s/with-gen simple-symbol? #(s/gen '#{com.company.thing other.thing some.thing clojure.core cljs.test})))
(s/def ::artifact-name (s/with-gen qualified-symbol? #(s/gen '#{com.company.thing/grapple other.thing/log some.thing/boo clojure.core/reduce cljs.test/deftest})))
(s/def ::fqname-old->new (s/map-of ::artifact-name ::artifact-name))
(s/def ::artifact-delete (s/every ::artifact-name :kind set?))
(s/def ::namespace-old->new (s/map-of ::namespace ::namespace))
(s/def ::namespace-delete (s/every ::namespace :kind set?))
(s/def ::namespace->alias (s/map-of ::namespace simple-symbol?))

(s/def ::form-predicate (s/fspec
                          :args (s/cat :form any?)
                          :ret boolean?
                          :gen #(s/gen #{(fn predicate [_] (rand-nth [true false]))})))

(s/def ::nsalias->ns (s/map-of simple-symbol? ::namespace))
(s/def ::ns->alias (s/map-of ::namespace simple-symbol?))
(s/def ::raw-sym->fqsym (s/map-of simple-symbol? ::artifact-name))

(s/def ::parsing-env (s/keys :opt-un [::nsalias->ns
                                      ::ns->alias
                                      ::raw-sym->fqsym]))

(s/def ::parsing-envs (s/map-of ::feature ::parsing-env))
(s/def ::feature-context ::feature)


(s/def ::let-forms (s/every symbol? :kind set?))
(s/def ::defn-forms (s/every symbol? :kind set?))

(s/def ::transforms (s/every fn? :kind vector?))
(s/def ::lang-config (s/keys
                       :opt-un [::fqname-old->new
                                ::namespace-old->new
                                ::artifact-delete
                                ::namespace->alias
                                ::namespace-delete
                                ::transforms
                                ::let-forms
                                ::defn-forms]))

(s/def ::config (s/map-of ::feature ::lang-config))
(s/def ::current-ns ::namespace)
(s/def ::zloc (s/with-gen #(or (map? %) (vector? %)) #(s/gen #{(z/of-string "(list)")})))

(s/def ::processing-env (s/keys
                          :req-un [::parsing-envs
                                   ::config
                                   ::zloc
                                   ::feature-context]
                          :opt-un [::current-ns]))

(s/def ::let-like (s/spec (s/cat :sym symbol? :bindings ::specs/bindings :body (s/* any?))))
(s/def ::defn-like (s/spec (s/cat :sym symbol? :args ::specs/defn-args)))

(comment
  (gen/sample (s/gen ::config))

  )


