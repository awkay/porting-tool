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

(defn find-map-vals
  "Recursively searches `data` for maps that contain `k`. Returns all such values at those `k`."
  [data k]
  (let [result (atom #{})]
    (clojure.walk/prewalk
      (fn [ele]
        (when (and (map? ele) (contains? ele k))
          (swap! result conj (get ele k)))
        ele)
      data)
    @result))

(defn clear-raw-syms
  "returns an env with the global resolution of the given syms elided."
  [env syms]
  (reduce
    (fn [e s]
      (update e :raw-sym->fqsym dissoc s))
    env
    syms))
