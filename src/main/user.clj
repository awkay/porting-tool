(ns user
  (:require
    [clojure.tools.namespace.repl :as tools-ns :refer [disable-reload! refresh clear set-refresh-dirs]]
    [expound.alpha :as expound]
    [clojure.spec.alpha :as s]
    [taoensso.timbre :as log]
    [clojure.string :as str]))

(set-refresh-dirs "src/main" "src/test" "../fulcro-spec/src/main")
(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn output-fn
  "Default (fn [data]) -> string output fn.
  Use`(partial default-output-fn <opts-map>)` to modify default opts."
  ([data] (output-fn nil data))
  ([opts data]
   (let [{:keys [level ?err #_vargs msg_ ?ns-str ?file hostname_
                 timestamp_ ?line]} data]
     (str
       (str/upper-case (name level)) " "
       (force msg_)))))

(log/merge-config! {:output-fn output-fn})

(comment

  )
