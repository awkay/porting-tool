(ns com.fulcrologic.porting.parsing.form-traversal-test
  (:require
    [com.fulcrologic.porting.parsing.form-traversal :as tr]
    [com.fulcrologic.porting.parsing.util :as util]
    [fulcro-spec.core :refer [specification assertions behavior when-mocking!]]
    [clojure.string :as str]))

(specification "let traversal"
  (behavior "analyzes the bindings"
    (let [processing-env (util/processing-env
                           {:feature-context :agnostic
                            :parsing-envs    {:agnostic {:raw-sym->fqsym {'a 'com.boo/a
                                                                          'x 'com.boo/x
                                                                          'b 'com.boo/b}}}})
          let-like-form  '(enc/when-let [{[a c] :list
                                          x :x} v] (f x) (g a))
          expected-env   (update-in processing-env [:parsing-envs :agnostic :raw-sym->fqsym] dissoc 'a 'x)]
      (when-mocking!
        (tr/process-form e f) =1x=> (do
                                      (assertions
                                        "processes each body element without those in the global raw symbol resolution"
                                        e => expected-env)
                                      f)
        (tr/process-form e f) => f

        (tr/process-let processing-env let-like-form)))

    (let [processing-env (util/processing-env
                           {:feature-context :agnostic
                            :parsing-envs    {:agnostic {:raw-sym->fqsym {'a 'com.boo/a
                                                                          'b 'com.boo/b}}}})
          let-like-form  '(enc/when-let [a 2] (f x) (g a))
          expected-env   (update-in processing-env [:parsing-envs :agnostic :raw-sym->fqsym] dissoc 'a)]
      (when-mocking!
        (tr/process-form e f) =1x=> (do
                                      (assertions
                                        "processes each body element without those in the global raw symbol resolution"
                                        e => expected-env)
                                      f)
        (tr/process-form e f) => f

        (tr/process-let processing-env let-like-form)))

    (let [processing-env (util/processing-env
                           {:feature-context :agnostic
                            :parsing-envs    {:agnostic {:raw-sym->fqsym {'a 'com.boo/a
                                                                          'b 'com.boo/b}}}})
          let-like-form  '(enc/when-let [a a b 2] (f x) (g a))]
      (when-mocking!
        (util/report-warning! m f) => (assertions
                                         "logs warnings about binding overlaps"
                                         (str/includes? m "#{a} are bound AND used") => true)
        (tr/process-form e f) => f

        (tr/process-let processing-env let-like-form)))))

(specification "defn traversal"
  (behavior "analyzes the bindings"
    (let [processing-env (util/processing-env
                           {:feature-context :agnostic
                            :parsing-envs    {:agnostic {:raw-sym->fqsym {'a 'com.boo/a
                                                                          'b 'com.boo/b}}}})
          defn-like-form '(defn f
                            ([x [e1 e2] {b :foo}] body)
                            ([] body))
          expected-env   (update-in processing-env [:parsing-envs :agnostic :raw-sym->fqsym] dissoc 'b)]
      (when-mocking!
        (tr/process-form e f) =1x=> (do
                                      (assertions
                                        "processes each body element without those in the global raw symbol resolution"
                                        e => expected-env)
                                      f)
        (tr/process-form e f) => f

        (tr/process-defn processing-env defn-like-form)))
    (let [processing-env (util/processing-env
                           {:feature-context :agnostic
                            :parsing-envs    {:agnostic {:raw-sym->fqsym {'a 'com.boo/a
                                                                          'b 'com.boo/b}}}})
          defn-like-form '(defn f
                            ([{:keys [y z]
                               :or   {y b}}] body)
                            ([] body))]
      (when-mocking!
        (tr/process-form e f) => f
        (util/report-warning! m f)
        =1x=> (do
                (assertions
                  "warns about aliasing raw syms"
                  (str/includes? m "that have been aliased") => true)
                f)

        (tr/process-defn processing-env defn-like-form)))))
