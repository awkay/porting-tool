(ns com.fulcrologic.porting.parsing.form-traversal-test
  (:require
    [com.fulcrologic.porting.parsing.form-traversal :as tr]
    [com.fulcrologic.porting.parsing.util :as util]
    [rewrite-clj.zip.base :as zb]
    [rewrite-clj.custom-zipper.core :as zc]
    [fulcro-spec.core :refer [specification assertions behavior when-mocking! component]]
    [clojure.string :as str]))

;;(specification "let traversal"
;;  (behavior "analyzes the bindings"
;;    (let [processing-env (util/processing-env
;;                           {:feature-context :agnostic
;;                            :parsing-envs    {:agnostic {:raw-sym->fqsym {'a 'com.boo/a
;;                                                                          'x 'com.boo/x
;;                                                                          'b 'com.boo/b}}}})
;;          let-like-form  '(enc/when-let [{[a c] :list
;;                                          x :x} v] (f x) (g a))
;;          expected-env   (update-in processing-env [:parsing-envs :agnostic :raw-sym->fqsym] dissoc 'a 'x)]
;;      (when-mocking!
;;        (tr/process-form e f) =1x=> (do
;;                                      (assertions
;;                                        "processes each body element without those in the global raw symbol resolution"
;;                                        e => expected-env)
;;                                      f)
;;        (tr/process-form e f) => f
;;
;;        (tr/process-let processing-env let-like-form)))
;;
;;    (let [processing-env (util/processing-env
;;                           {:feature-context :agnostic
;;                            :parsing-envs    {:agnostic {:raw-sym->fqsym {'a 'com.boo/a
;;                                                                          'b 'com.boo/b}}}})
;;          let-like-form  '(enc/when-let [a 2] (f x) (g a))
;;          expected-env   (update-in processing-env [:parsing-envs :agnostic :raw-sym->fqsym] dissoc 'a)]
;;      (when-mocking!
;;        (tr/process-form e f) =1x=> (do
;;                                      (assertions
;;                                        "processes each body element without those in the global raw symbol resolution"
;;                                        e => expected-env)
;;                                      f)
;;        (tr/process-form e f) => f
;;
;;        (tr/process-let processing-env let-like-form)))
;;
;;    (let [processing-env (util/processing-env
;;                           {:feature-context :agnostic
;;                            :parsing-envs    {:agnostic {:raw-sym->fqsym {'a 'com.boo/a
;;                                                                          'b 'com.boo/b}}}})
;;          let-like-form  '(enc/when-let [a a b 2] (f x) (g a))]
;;      (when-mocking!
;;        (util/report-warning! m f) => (assertions
;;                                         "logs warnings about binding overlaps"
;;                                         (str/includes? m "#{a} are bound AND used") => true)
;;        (tr/process-form e f) => f
;;
;;        (tr/process-let processing-env let-like-form)))))
;;
;;(specification "defn traversal"
;;  (behavior "analyzes the bindings"
;;    (let [processing-env (util/processing-env
;;                           {:feature-context :agnostic
;;                            :parsing-envs    {:agnostic {:raw-sym->fqsym {'a 'com.boo/a
;;                                                                          'b 'com.boo/b}}}})
;;          defn-like-form '(defn f
;;                            ([x [e1 e2] {b :foo}] body)
;;                            ([] body))
;;          expected-env   (update-in processing-env [:parsing-envs :agnostic :raw-sym->fqsym] dissoc 'b)]
;;      (when-mocking!
;;        (tr/process-form e f) =1x=> (do
;;                                      (assertions
;;                                        "processes each body element without those in the global raw symbol resolution"
;;                                        e => expected-env)
;;                                      f)
;;        (tr/process-form e f) => f
;;
;;        (tr/process-defn processing-env defn-like-form)))
;;    (let [processing-env (util/processing-env
;;                           {:feature-context :agnostic
;;                            :parsing-envs    {:agnostic {:raw-sym->fqsym {'a 'com.boo/a
;;                                                                          'b 'com.boo/b}}}})
;;          defn-like-form '(defn f
;;                            ([{:keys [y z]
;;                               :or   {y b}}] body)
;;                            ([] body))]
;;      (when-mocking!
;;        (tr/process-form e f) => f
;;        (util/report-warning! m f)
;;        =1x=> (do
;;                (assertions
;;                  "warns about aliasing raw syms"
;;                  (str/includes? m "that have been aliased") => true)
;;                f)
;;
;;        (tr/process-defn processing-env defn-like-form)))))
;;

(specification "process-list"
  (let [env (util/processing-env
              {:zloc (zb/of-string "()")})]
    (assertions
      "Has no effect for empty lists"
      (tr/process-list env) => env))

  (let [env (util/processing-env
              {:zloc (zb/of-string "(1 2 3)")})]
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
                {:zloc (zb/of-string "#{}")})]
      (assertions
        "Has no effect for empty sets"
        (tr/process-sequence env) => env))

    (let [env (util/processing-env
                {:zloc (zb/of-string "#{1 2 3}")})]
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
                {:zloc (zb/of-string "{}")})]
      (assertions
        "Has no effect for empty vectors"
        (tr/process-sequence env) => env))
    (let [env (util/processing-env
                {:zloc (zb/of-string "{:a 1 :b 2}")})]
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
                {:zloc (zb/of-string "[]")})]
      (assertions
        "Has no effect for empty vectors"
        (tr/process-sequence env) => env))
    (let [env (util/processing-env
                {:zloc (zb/of-string "[1 2 3]")})]
      (when-mocking!
        (tr/process-form e) =3x=> (do
                                    (assertions
                                      "walks the elements one at a time"
                                      (contains? #{1 2 3} (tr/current-form e)) => true)
                                    e)

        (assertions
          "Returns an env as the original location"
          (tr/process-sequence env) => env)))))

(specification "process-reader-conditional" :focus
  (let [env (util/processing-env
              {:zloc (zb/of-string "#?()")})]
    (assertions
      "Tolerates empty reader conditionals"
      (tr/process-form env) => env))
  (let [env (util/processing-env
              {:zloc (zb/of-string "#?(:clj a :cljs b)")})]
    (assertions
      "Returns the env at the original position"
      (tr/process-form env) => env))

  (let [env (util/processing-env
              {:zloc (zb/of-string "#?(:clj a :cljs b)")})]
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
