(ns com.fulcrologic.porting.rewrite-clj.zip
  (:refer-clojure :exclude [next find replace remove
                            seq? map? vector? list? set?
                            print map get assoc])
  (:require
    [clojure.walk]
    [rewrite-clj.zip
     [base :as base]
     [edit :as edit]
     [find :as find]
     [insert :as insert]
     [move :as move]
     [remove :as remove]
     [seq :as seq]
     [subedit :as subedit]
     [walk :as walk]
     [whitespace :as ws]]
    [rewrite-clj.potemkin :refer [import-vars]]
    [rewrite-clj
     [parser :as p]
     [node :as node]]
    [rewrite-clj.custom-zipper.core :as z]))

;; ## API Facade

;; ## Base Operations

(defmacro ^:private defbase
  [sym base]
  (let [{:keys [arglists]} (meta
                             (ns-resolve
                               (symbol (namespace base))
                               (symbol (name base))))
        sym (with-meta
              sym
              {:doc      (format "Directly call '%s' on the given arguments." base)
               :arglists `(quote ~arglists)})]
    `(def ~sym ~base)))

(defbase right* rewrite-clj.custom-zipper.core/right)
(defbase left* rewrite-clj.custom-zipper.core/left)
(defbase up* rewrite-clj.custom-zipper.core/up)
(defbase down* rewrite-clj.custom-zipper.core/down)
(defbase next* rewrite-clj.custom-zipper.core/next)
(defbase prev* rewrite-clj.custom-zipper.core/prev)
(defbase rightmost* rewrite-clj.custom-zipper.core/rightmost)
(defbase leftmost* rewrite-clj.custom-zipper.core/leftmost)
(defbase replace* rewrite-clj.custom-zipper.core/replace)
(defbase edit* rewrite-clj.custom-zipper.core/edit)
(defbase remove* rewrite-clj.custom-zipper.core/remove)
(defbase insert-left* rewrite-clj.custom-zipper.core/insert-left)
(defbase insert-right* rewrite-clj.custom-zipper.core/insert-right)

(do
  (do
    (def node (clojure.core/deref #'rewrite-clj.custom-zipper.core/node))
    (clojure.core/alter-meta!
      (var node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.custom-zipper.core/node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.custom-zipper.core/node (var node))
    #'rewrite-clj.custom-zipper.core/node)
  (do
    (def position (clojure.core/deref #'rewrite-clj.custom-zipper.core/position))
    (clojure.core/alter-meta!
      (var position)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.custom-zipper.core/position) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.custom-zipper.core/position (var position))
    #'rewrite-clj.custom-zipper.core/position)
  (do
    (def root (clojure.core/deref #'rewrite-clj.custom-zipper.core/root))
    (clojure.core/alter-meta!
      (var root)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.custom-zipper.core/root) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.custom-zipper.core/root (var root))
    #'rewrite-clj.custom-zipper.core/root)
  (do
    (def child-sexprs (clojure.core/deref #'rewrite-clj.zip.base/child-sexprs))
    (clojure.core/alter-meta!
      (var child-sexprs)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.base/child-sexprs) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.base/child-sexprs (var child-sexprs))
    #'rewrite-clj.zip.base/child-sexprs)
  (do
    (def edn* (clojure.core/deref #'rewrite-clj.zip.base/edn*))
    (clojure.core/alter-meta!
      (var edn*)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.base/edn*) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.base/edn* (var edn*))
    #'rewrite-clj.zip.base/edn*)
  (do
    (def edn (clojure.core/deref #'rewrite-clj.zip.base/edn))
    (clojure.core/alter-meta!
      (var edn)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.base/edn) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.base/edn (var edn))
    #'rewrite-clj.zip.base/edn)
  (do
    (def tag (clojure.core/deref #'rewrite-clj.zip.base/tag))
    (clojure.core/alter-meta!
      (var tag)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.base/tag) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.base/tag (var tag))
    #'rewrite-clj.zip.base/tag)
  (do
    (def sexpr (clojure.core/deref #'rewrite-clj.zip.base/sexpr))
    (clojure.core/alter-meta!
      (var sexpr)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.base/sexpr) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.base/sexpr (var sexpr))
    #'rewrite-clj.zip.base/sexpr)
  (do
    (def length (clojure.core/deref #'rewrite-clj.zip.base/length))
    (clojure.core/alter-meta!
      (var length)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.base/length) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.base/length (var length))
    #'rewrite-clj.zip.base/length)
  (do
    (def value (clojure.core/deref #'rewrite-clj.zip.base/value))
    (clojure.core/alter-meta!
      (var value)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.base/value) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.base/value (var value))
    #'rewrite-clj.zip.base/value)
  (do
    (def of-file (clojure.core/deref #'rewrite-clj.zip.base/of-file))
    (clojure.core/alter-meta!
      (var of-file)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.base/of-file) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.base/of-file (var of-file))
    #'rewrite-clj.zip.base/of-file)
  (do
    (def of-string (clojure.core/deref #'rewrite-clj.zip.base/of-string))
    (clojure.core/alter-meta!
      (var of-string)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.base/of-string) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.base/of-string (var of-string))
    #'rewrite-clj.zip.base/of-string)
  (do
    (def string (clojure.core/deref #'rewrite-clj.zip.base/string))
    (clojure.core/alter-meta!
      (var string)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.base/string) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.base/string (var string))
    #'rewrite-clj.zip.base/string)
  (do
    (def root-string (clojure.core/deref #'rewrite-clj.zip.base/root-string))
    (clojure.core/alter-meta!
      (var root-string)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.base/root-string) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.base/root-string (var root-string))
    #'rewrite-clj.zip.base/root-string)
  (do
    (def print (clojure.core/deref #'rewrite-clj.zip.base/print))
    (clojure.core/alter-meta!
      (var print)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.base/print) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.base/print (var print))
    #'rewrite-clj.zip.base/print)
  (do
    (def print-root (clojure.core/deref #'rewrite-clj.zip.base/print-root))
    (clojure.core/alter-meta!
      (var print-root)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.base/print-root) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.base/print-root (var print-root))
    #'rewrite-clj.zip.base/print-root)
  (do
    (def replace (clojure.core/deref #'rewrite-clj.zip.edit/replace))
    (clojure.core/alter-meta!
      (var replace)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.edit/replace) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.edit/replace (var replace))
    #'rewrite-clj.zip.edit/replace)
  (do
    (def edit (clojure.core/deref #'rewrite-clj.zip.edit/edit))
    (clojure.core/alter-meta!
      (var edit)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.edit/edit) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.edit/edit (var edit))
    #'rewrite-clj.zip.edit/edit)
  (do
    (def splice (clojure.core/deref #'rewrite-clj.zip.edit/splice))
    (clojure.core/alter-meta!
      (var splice)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.edit/splice) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.edit/splice (var splice))
    #'rewrite-clj.zip.edit/splice)
  (do
    (def prefix (clojure.core/deref #'rewrite-clj.zip.edit/prefix))
    (clojure.core/alter-meta!
      (var prefix)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.edit/prefix) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.edit/prefix (var prefix))
    #'rewrite-clj.zip.edit/prefix)
  (do
    (def suffix (clojure.core/deref #'rewrite-clj.zip.edit/suffix))
    (clojure.core/alter-meta!
      (var suffix)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.edit/suffix) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.edit/suffix (var suffix))
    #'rewrite-clj.zip.edit/suffix)
  (do
    (def find (clojure.core/deref #'rewrite-clj.zip.find/find))
    (clojure.core/alter-meta!
      (var find)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.find/find) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.find/find (var find))
    #'rewrite-clj.zip.find/find)
  (do
    (def find-next (clojure.core/deref #'rewrite-clj.zip.find/find-next))
    (clojure.core/alter-meta!
      (var find-next)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.find/find-next) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.find/find-next (var find-next))
    #'rewrite-clj.zip.find/find-next)
  (do
    (def find-depth-first (clojure.core/deref #'rewrite-clj.zip.find/find-depth-first))
    (clojure.core/alter-meta!
      (var find-depth-first)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.find/find-depth-first) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.find/find-depth-first (var find-depth-first))
    #'rewrite-clj.zip.find/find-depth-first)
  (do
    (def find-next-depth-first (clojure.core/deref #'rewrite-clj.zip.find/find-next-depth-first))
    (clojure.core/alter-meta!
      (var find-next-depth-first)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.find/find-next-depth-first) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.find/find-next-depth-first (var find-next-depth-first))
    #'rewrite-clj.zip.find/find-next-depth-first)
  (do
    (def find-tag (clojure.core/deref #'rewrite-clj.zip.find/find-tag))
    (clojure.core/alter-meta!
      (var find-tag)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.find/find-tag) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.find/find-tag (var find-tag))
    #'rewrite-clj.zip.find/find-tag)
  (do
    (def find-next-tag (clojure.core/deref #'rewrite-clj.zip.find/find-next-tag))
    (clojure.core/alter-meta!
      (var find-next-tag)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.find/find-next-tag) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.find/find-next-tag (var find-next-tag))
    #'rewrite-clj.zip.find/find-next-tag)
  (do
    (def find-value (clojure.core/deref #'rewrite-clj.zip.find/find-value))
    (clojure.core/alter-meta!
      (var find-value)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.find/find-value) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.find/find-value (var find-value))
    #'rewrite-clj.zip.find/find-value)
  (do
    (def find-next-value (clojure.core/deref #'rewrite-clj.zip.find/find-next-value))
    (clojure.core/alter-meta!
      (var find-next-value)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.find/find-next-value) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.find/find-next-value (var find-next-value))
    #'rewrite-clj.zip.find/find-next-value)
  (do
    (def find-token (clojure.core/deref #'rewrite-clj.zip.find/find-token))
    (clojure.core/alter-meta!
      (var find-token)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.find/find-token) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.find/find-token (var find-token))
    #'rewrite-clj.zip.find/find-token)
  (do
    (def find-next-token (clojure.core/deref #'rewrite-clj.zip.find/find-next-token))
    (clojure.core/alter-meta!
      (var find-next-token)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.find/find-next-token) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.find/find-next-token (var find-next-token))
    #'rewrite-clj.zip.find/find-next-token)
  (do
    (def insert-right (clojure.core/deref #'rewrite-clj.zip.insert/insert-right))
    (clojure.core/alter-meta!
      (var insert-right)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.insert/insert-right) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.insert/insert-right (var insert-right))
    #'rewrite-clj.zip.insert/insert-right)
  (do
    (def insert-left (clojure.core/deref #'rewrite-clj.zip.insert/insert-left))
    (clojure.core/alter-meta!
      (var insert-left)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.insert/insert-left) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.insert/insert-left (var insert-left))
    #'rewrite-clj.zip.insert/insert-left)
  (do
    (def insert-child (clojure.core/deref #'rewrite-clj.zip.insert/insert-child))
    (clojure.core/alter-meta!
      (var insert-child)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.insert/insert-child) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.insert/insert-child (var insert-child))
    #'rewrite-clj.zip.insert/insert-child)
  (do
    (def append-child (clojure.core/deref #'rewrite-clj.zip.insert/append-child))
    (clojure.core/alter-meta!
      (var append-child)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.insert/append-child) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.insert/append-child (var append-child))
    #'rewrite-clj.zip.insert/append-child)
  (do
    (def left (clojure.core/deref #'rewrite-clj.zip.move/left))
    (clojure.core/alter-meta!
      (var left)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.move/left) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.move/left (var left))
    #'rewrite-clj.zip.move/left)
  (do
    (def right (clojure.core/deref #'rewrite-clj.zip.move/right))
    (clojure.core/alter-meta!
      (var right)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.move/right) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.move/right (var right))
    #'rewrite-clj.zip.move/right)
  (do
    (def up (clojure.core/deref #'rewrite-clj.zip.move/up))
    (clojure.core/alter-meta!
      (var up)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.move/up) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.move/up (var up))
    #'rewrite-clj.zip.move/up)
  (do
    (def down (clojure.core/deref #'rewrite-clj.zip.move/down))
    (clojure.core/alter-meta!
      (var down)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.move/down) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.move/down (var down))
    #'rewrite-clj.zip.move/down)
  (do
    (def prev (clojure.core/deref #'rewrite-clj.zip.move/prev))
    (clojure.core/alter-meta!
      (var prev)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.move/prev) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.move/prev (var prev))
    #'rewrite-clj.zip.move/prev)
  (do
    (def next (clojure.core/deref #'rewrite-clj.zip.move/next))
    (clojure.core/alter-meta!
      (var next)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.move/next) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.move/next (var next))
    #'rewrite-clj.zip.move/next)
  (do
    (def leftmost (clojure.core/deref #'rewrite-clj.zip.move/leftmost))
    (clojure.core/alter-meta!
      (var leftmost)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.move/leftmost) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.move/leftmost (var leftmost))
    #'rewrite-clj.zip.move/leftmost)
  (do
    (def rightmost (clojure.core/deref #'rewrite-clj.zip.move/rightmost))
    (clojure.core/alter-meta!
      (var rightmost)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.move/rightmost) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.move/rightmost (var rightmost))
    #'rewrite-clj.zip.move/rightmost)
  (do
    (def leftmost? (clojure.core/deref #'rewrite-clj.zip.move/leftmost?))
    (clojure.core/alter-meta!
      (var leftmost?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.move/leftmost?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.move/leftmost? (var leftmost?))
    #'rewrite-clj.zip.move/leftmost?)
  (do
    (def rightmost? (clojure.core/deref #'rewrite-clj.zip.move/rightmost?))
    (clojure.core/alter-meta!
      (var rightmost?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.move/rightmost?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.move/rightmost? (var rightmost?))
    #'rewrite-clj.zip.move/rightmost?)
  (do
    (def end? (clojure.core/deref #'rewrite-clj.zip.move/end?))
    (clojure.core/alter-meta!
      (var end?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.move/end?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.move/end? (var end?))
    #'rewrite-clj.zip.move/end?)
  (do
    (def remove (clojure.core/deref #'rewrite-clj.zip.remove/remove))
    (clojure.core/alter-meta!
      (var remove)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.remove/remove) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.remove/remove (var remove))
    #'rewrite-clj.zip.remove/remove)
  (do
    (def seq? (clojure.core/deref #'rewrite-clj.zip.seq/seq?))
    (clojure.core/alter-meta!
      (var seq?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.seq/seq?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.seq/seq? (var seq?))
    #'rewrite-clj.zip.seq/seq?)
  (do
    (def list? (clojure.core/deref #'rewrite-clj.zip.seq/list?))
    (clojure.core/alter-meta!
      (var list?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.seq/list?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.seq/list? (var list?))
    #'rewrite-clj.zip.seq/list?)
  (do
    (def vector? (clojure.core/deref #'rewrite-clj.zip.seq/vector?))
    (clojure.core/alter-meta!
      (var vector?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.seq/vector?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.seq/vector? (var vector?))
    #'rewrite-clj.zip.seq/vector?)
  (do
    (def set? (clojure.core/deref #'rewrite-clj.zip.seq/set?))
    (clojure.core/alter-meta!
      (var set?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.seq/set?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.seq/set? (var set?))
    #'rewrite-clj.zip.seq/set?)
  (do
    (def map? (clojure.core/deref #'rewrite-clj.zip.seq/map?))
    (clojure.core/alter-meta!
      (var map?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.seq/map?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.seq/map? (var map?))
    #'rewrite-clj.zip.seq/map?)
  (do
    (def map (clojure.core/deref #'rewrite-clj.zip.seq/map))
    (clojure.core/alter-meta!
      (var map)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.seq/map) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.seq/map (var map))
    #'rewrite-clj.zip.seq/map)
  (do
    (def map-keys (clojure.core/deref #'rewrite-clj.zip.seq/map-keys))
    (clojure.core/alter-meta!
      (var map-keys)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.seq/map-keys) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.seq/map-keys (var map-keys))
    #'rewrite-clj.zip.seq/map-keys)
  (do
    (def map-vals (clojure.core/deref #'rewrite-clj.zip.seq/map-vals))
    (clojure.core/alter-meta!
      (var map-vals)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.seq/map-vals) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.seq/map-vals (var map-vals))
    #'rewrite-clj.zip.seq/map-vals)
  (do
    (def get (clojure.core/deref #'rewrite-clj.zip.seq/get))
    (clojure.core/alter-meta!
      (var get)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.seq/get) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.seq/get (var get))
    #'rewrite-clj.zip.seq/get)
  (do
    (def assoc (clojure.core/deref #'rewrite-clj.zip.seq/assoc))
    (clojure.core/alter-meta!
      (var assoc)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.seq/assoc) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.seq/assoc (var assoc))
    #'rewrite-clj.zip.seq/assoc)
  (do
    (def edit-node (clojure.core/deref #'rewrite-clj.zip.subedit/edit-node))
    (clojure.core/alter-meta!
      (var edit-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.subedit/edit-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.subedit/edit-node (var edit-node))
    #'rewrite-clj.zip.subedit/edit-node)
  (do
    (def edit-> #'rewrite-clj.zip.subedit/edit->)
    (clojure.core/alter-meta!
      (var edit->)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.subedit/edit->) :name))
    (. (var edit->) setMacro)
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.subedit/edit-> (var edit->))
    #'rewrite-clj.zip.subedit/edit->)
  (do
    (def edit->> #'rewrite-clj.zip.subedit/edit->>)
    (clojure.core/alter-meta!
      (var edit->>)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.subedit/edit->>) :name))
    (. (var edit->>) setMacro)
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.subedit/edit->> (var edit->>))
    #'rewrite-clj.zip.subedit/edit->>)
  (do
    (def subedit-node (clojure.core/deref #'rewrite-clj.zip.subedit/subedit-node))
    (clojure.core/alter-meta!
      (var subedit-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.subedit/subedit-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.subedit/subedit-node (var subedit-node))
    #'rewrite-clj.zip.subedit/subedit-node)
  (do
    (def subedit-> #'rewrite-clj.zip.subedit/subedit->)
    (clojure.core/alter-meta!
      (var subedit->)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.subedit/subedit->) :name))
    (. (var subedit->) setMacro)
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.subedit/subedit-> (var subedit->))
    #'rewrite-clj.zip.subedit/subedit->)
  (do
    (def subedit->> #'rewrite-clj.zip.subedit/subedit->>)
    (clojure.core/alter-meta!
      (var subedit->>)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.subedit/subedit->>) :name))
    (. (var subedit->>) setMacro)
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.subedit/subedit->> (var subedit->>))
    #'rewrite-clj.zip.subedit/subedit->>)
  (do
    (def prewalk (clojure.core/deref #'rewrite-clj.zip.walk/prewalk))
    (clojure.core/alter-meta!
      (var prewalk)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.walk/prewalk) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.walk/prewalk (var prewalk))
    #'rewrite-clj.zip.walk/prewalk)
  (do
    (def postwalk (clojure.core/deref #'rewrite-clj.zip.walk/postwalk))
    (clojure.core/alter-meta!
      (var postwalk)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.walk/postwalk) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.walk/postwalk (var postwalk))
    #'rewrite-clj.zip.walk/postwalk)
  (do
    (def whitespace? (clojure.core/deref #'rewrite-clj.zip.whitespace/whitespace?))
    (clojure.core/alter-meta!
      (var whitespace?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.whitespace/whitespace?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.whitespace/whitespace? (var whitespace?))
    #'rewrite-clj.zip.whitespace/whitespace?)
  (do
    (def linebreak? (clojure.core/deref #'rewrite-clj.zip.whitespace/linebreak?))
    (clojure.core/alter-meta!
      (var linebreak?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.whitespace/linebreak?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.whitespace/linebreak? (var linebreak?))
    #'rewrite-clj.zip.whitespace/linebreak?)
  (do
    (def whitespace-or-comment? (clojure.core/deref #'rewrite-clj.zip.whitespace/whitespace-or-comment?))
    (clojure.core/alter-meta!
      (var whitespace-or-comment?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.whitespace/whitespace-or-comment?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.whitespace/whitespace-or-comment? (var whitespace-or-comment?))
    #'rewrite-clj.zip.whitespace/whitespace-or-comment?)
  (do
    (def skip (clojure.core/deref #'rewrite-clj.zip.whitespace/skip))
    (clojure.core/alter-meta!
      (var skip)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.whitespace/skip) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.whitespace/skip (var skip))
    #'rewrite-clj.zip.whitespace/skip)
  (do
    (def skip-whitespace (clojure.core/deref #'rewrite-clj.zip.whitespace/skip-whitespace))
    (clojure.core/alter-meta!
      (var skip-whitespace)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.whitespace/skip-whitespace) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.whitespace/skip-whitespace (var skip-whitespace))
    #'rewrite-clj.zip.whitespace/skip-whitespace)
  (do
    (def skip-whitespace-left (clojure.core/deref #'rewrite-clj.zip.whitespace/skip-whitespace-left))
    (clojure.core/alter-meta!
      (var skip-whitespace-left)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.whitespace/skip-whitespace-left) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.whitespace/skip-whitespace-left (var skip-whitespace-left))
    #'rewrite-clj.zip.whitespace/skip-whitespace-left)
  (do
    (def prepend-space (clojure.core/deref #'rewrite-clj.zip.whitespace/prepend-space))
    (clojure.core/alter-meta!
      (var prepend-space)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.whitespace/prepend-space) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.whitespace/prepend-space (var prepend-space))
    #'rewrite-clj.zip.whitespace/prepend-space)
  (do
    (def append-space (clojure.core/deref #'rewrite-clj.zip.whitespace/append-space))
    (clojure.core/alter-meta!
      (var append-space)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.whitespace/append-space) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.whitespace/append-space (var append-space))
    #'rewrite-clj.zip.whitespace/append-space)
  (do
    (def prepend-newline (clojure.core/deref #'rewrite-clj.zip.whitespace/prepend-newline))
    (clojure.core/alter-meta!
      (var prepend-newline)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.whitespace/prepend-newline) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.whitespace/prepend-newline (var prepend-newline))
    #'rewrite-clj.zip.whitespace/prepend-newline)
  (do
    (def append-newline (clojure.core/deref #'rewrite-clj.zip.whitespace/append-newline))
    (clojure.core/alter-meta!
      (var append-newline)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.zip.whitespace/append-newline) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.zip.whitespace/append-newline (var append-newline))
    #'rewrite-clj.zip.whitespace/append-newline))
