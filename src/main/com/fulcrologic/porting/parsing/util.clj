(ns com.fulcrologic.porting.parsing.util)

(def ^:dynamic *current-file*)

(defn compile-error! [message form]
  (let [{:keys [line column]} (meta form)]
    (println (str *current-file* " " line ":" column " - " message))
    (throw (ex-info "Failed" {}))))
