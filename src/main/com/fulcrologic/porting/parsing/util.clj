(ns com.fulcrologic.porting.parsing.util)

(def ^:dynamic *current-file* "unknown")
(def ^:dynamic *current-form* nil)

(defn compile-warning!
  ([message] (compile-warning! message *current-form*))
  ([message form]
   (let [{:keys [line column]} (meta form)]
     (println (str *current-file* " " line ":" column " - " message)))))

(defn compile-error!
  ([message] (compile-error! message *current-form*))
  ([message form]
   (compile-warning! message form)
   (throw (ex-info "Failed" {}))))
