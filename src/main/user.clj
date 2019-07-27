(ns user
  (:require
    [clojure.tools.namespace.repl :as tools-ns :refer [disable-reload! refresh clear set-refresh-dirs]]
    [expound.alpha :as expound]
    [clojure.spec.alpha :as s]))

(set-refresh-dirs "src/main" "src/test")
(alter-var-root #'s/*explain-out* (constantly expound/printer))
