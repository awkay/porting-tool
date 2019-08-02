(ns com.fulcrologic.porting.parsing.form-traversal-test
  (:require
    [com.fulcrologic.porting.parsing.form-traversal :as tr]
    [com.fulcrologic.porting.parsing.util :as util]
    [rewrite-clj.zip :as z]
    [fulcro-spec.core :refer [specification assertions behavior when-mocking! component]]
    [clojure.string :as str]))

(specification "let traversal" :focus
  (behavior "analyzes the bindings"
    (let [processing-env (util/processing-env
                           {:feature-context :agnostic
                            :zloc            (z/of-string "(let [{[a c] :list x :x} v] (f x) (g a))")
                            :parsing-envs    {:agnostic {:raw-sym->fqsym {'a 'com.boo/a
                                                                          'x 'com.boo/x
                                                                          'b 'com.boo/b}}}})]
      (when-mocking!
        (tr/process-sequence e) =1x=> (let [raw-syms (set (keys (get-in e [:parsing-envs :agnostic :raw-sym->fqsym])))]
                                        (assertions
                                          "retains the non-shadowed aliases"
                                          (contains? raw-syms 'b) => true
                                          "processes the sequence without the shadowed raw syms"
                                          (contains? raw-syms 'a) => false
                                          (contains? raw-syms 'x) => false)
                                        e)

        (tr/process-let processing-env)))

    (let [processing-env (util/processing-env
                           {:feature-context :agnostic
                            :zloc            (z/of-string "(let [a a b 2] (f x) (g a))")
                            :parsing-envs    {:agnostic {:raw-sym->fqsym {'a 'com.boo/a
                                                                          'b 'com.boo/b}}}})]
      (when-mocking!
        (util/report-warning! m f) => (assertions
                                        "logs warnings about binding overlaps"
                                        (str/includes? m "The global") => true)
        (tr/process-sequence e) =1x=> e

        (tr/process-let processing-env)))))

(specification "defn traversal"
  (behavior "analyzes the bindings"
    (let [processing-env (util/processing-env
                           {:feature-context :agnostic
                            :zloc            (z/of-string "(defn f ([x [e1 e2] {b :foo}] body) ([] body))")
                            :parsing-envs    {:agnostic {:raw-sym->fqsym {'a 'com.boo/a
                                                                          'b 'com.boo/b}}}})]
      (when-mocking!
        (tr/process-sequence e) =1x=> (let [raw-syms (set (keys (get-in e [:parsing-envs :agnostic :raw-sym->fqsym])))]
                                        (assertions
                                          "retains non-shadowed syms"
                                          (contains? raw-syms 'a) => true
                                          "processes the sequence without the shadowed syms"
                                          (contains? raw-syms 'b) => false)
                                        e)

        (tr/process-defn processing-env)))))


(specification "process-list"
  (let [env (util/processing-env
              {::tr/path (list)
               :zloc     (z/of-string "()")})]
    (assertions
      "Has no effect for empty lists"
      (tr/process-list env) => env))

  (let [env (util/processing-env
              {:zloc (z/of-string "(1 2 3)")})]
    (when-mocking!
      (tr/process-form e) =1x=> (do
                                  (assertions
                                    "walks the elements one at a time"
                                    (tr/current-form e) => 1)
                                  e)
      (tr/process-form e) =2x=> e

      (assertions
        "Returns an env as the original location"
        (tr/process-list env) => env))))

(specification "process-sequence"
  (component "sets"
    (let [env (util/processing-env
                {:zloc (z/of-string "#{}")})]
      (assertions
        "Has no effect for empty sets"
        (tr/process-sequence env) => env))

    (let [env (util/processing-env
                {:zloc (z/of-string "#{1 2 3}")})]
      (when-mocking!
        (tr/process-form e) =3x=> (do
                                    (assertions
                                      "walks the elements one at a time"
                                      (contains? #{1 2 3} (tr/current-form e)) => true)
                                    e)

        (assertions
          "Returns an env as the original location"
          (tr/process-sequence env) => env))))
  (component "maps"
    (let [env (util/processing-env
                {:zloc (z/of-string "{}")})]
      (assertions
        "Has no effect for empty vectors"
        (tr/process-sequence env) => env))
    (let [env (util/processing-env
                {:zloc (z/of-string "{:a 1 :b 2}")})]
      (when-mocking!
        (tr/process-form e) =4x=> (do
                                    (assertions
                                      "walks the k and v one at a time"
                                      (contains? #{:a :b 1 2} (tr/current-form e)) => true)
                                    e)

        (assertions
          "Returns an env as the original location"
          (tr/process-sequence env) => env))))
  (component "vectors"
    (let [env (util/processing-env
                {:zloc (z/of-string "[]")})]
      (assertions
        "Has no effect for empty vectors"
        (tr/process-sequence env) => env))
    (let [env (util/processing-env
                {:zloc (z/of-string "[1 2 3]")})]
      (when-mocking!
        (tr/process-form e) =3x=> (do
                                    (assertions
                                      "walks the elements one at a time"
                                      (contains? #{1 2 3} (tr/current-form e)) => true)
                                    e)

        (assertions
          "Returns an env as the original location"
          (tr/process-sequence env) => env)))))

(specification "process-reader-conditional"
  (let [env (util/processing-env
              {:zloc (z/of-string "#?()")})]
    (assertions
      "Tolerates empty reader conditionals"
      (tr/process-form env) => env))
  (let [env (util/processing-env
              {:zloc (z/of-string "#?(:clj a :cljs b)")})]
    (assertions
      "Returns the env at the original position"
      (tr/process-form env) => env))

  (let [env (util/processing-env
              {:zloc (z/of-string "#?(:clj a :cljs b)")})]
    (when-mocking!
      (tr/process-form e) =1x=> (do
                                  (assertions
                                    "processes the clojure side with a clj processing context"
                                    (:feature-context e) => :clj
                                    "on the correct form"
                                    (tr/current-form e) => 'a)
                                  e)
      (tr/process-form e) =1x=> (do
                                  (assertions
                                    "processes the cljs side with a cljs processing context"
                                    (:feature-context e) => :cljs
                                    "on the correct form"
                                    (tr/current-form e) => 'b)
                                  e)

      (assertions
        "Returns an env as the original location"
        (tr/process-reader-conditional env) => env))))

(comment
  (let [z (z/of-string "(ns a (:require #?(:clj 1 :cljs 2)))
  (defn f [a] #?(:clj 42 :cljs 43))")
        actual (tr/loc->form z :clj)]

    actual
    ))
