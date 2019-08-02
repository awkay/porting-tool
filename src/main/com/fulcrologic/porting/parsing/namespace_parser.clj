(ns com.fulcrologic.porting.parsing.namespace-parser
  (:require
    [ghostwheel.core :refer [>defn >defn- =>]]
    [com.fulcrologic.porting.parsing.util :refer [report-error! report-warning! *current-form*]]
    [com.fulcrologic.porting.specs :as pspec]
    [clojure.pprint :refer [pprint]]
    [clojure.core.specs.alpha :as specs]
    [clojure.spec.alpha :as s]
    [taoensso.timbre :as log]
    [clojure.spec.gen.alpha :as gen]))

(comment
  :raw-sym->fqsym
  :nsalias->ns
  :ns->alias)
