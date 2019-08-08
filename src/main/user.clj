(ns user
  (:require
    [clojure.tools.namespace.repl :as tools-ns :refer [disable-reload! refresh clear set-refresh-dirs]]
    [expound.alpha :as expound]
    [clojure.spec.alpha :as s]
    [taoensso.timbre :as log]
    [clojure.string :as str]
    [com.fulcrologic.porting.rewrite-clj.zip :as z]
    [clojure.java.io :as io]))

(set-refresh-dirs "src/main" "src/test")
(alter-var-root #'s/*explain-out* (constantly expound/printer))

(comment

  (let [;ast (p/parse-file-all "./resources/trial.cljc")
        root-loc (z/of-string "#?()")
        #_#_first-dep-loc (-> root-loc (z/down) (z/right) (z/right) (z/down) (z/right))]
    (-> root-loc z/down z/right z/down z/sexpr)
    #_(-> root-loc (z/down) (z/right) (z/right) (z/down) (z/right) (z/right) (z/right) (z/right))
    #_(-> root-loc (z/down) (z/right) (z/right) (z/down) (z/right)
        (z/insert-right '[a.b :as c])
        (z/append-newline)
        (z/root-string))

    #_(loop [loc root-loc]
        (if (z/end? loc)
          (z/root-string loc)
          (do (println (z/node loc))
              (recur (z/next loc)))))
    ))
