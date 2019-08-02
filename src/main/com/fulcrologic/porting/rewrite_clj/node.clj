(ns com.fulcrologic.porting.rewrite-clj.node
  (:require [rewrite-clj.node
             coerce
             comment
             fn
             forms
             integer
             keyword
             meta
             protocols
             quote
             reader-macro
             regex
             seq
             string
             token
             uneval
             whitespace]
            [rewrite-clj.potemkin :refer [import-vars]]))

;; ## API Facade
(do
  (do
    (def coerce (clojure.core/deref #'rewrite-clj.node.protocols/coerce))
    (clojure.core/alter-meta!
      (var coerce)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.protocols/coerce) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.protocols/coerce (var coerce))
    #'rewrite-clj.node.protocols/coerce)
  (do
    (def children (clojure.core/deref #'rewrite-clj.node.protocols/children))
    (clojure.core/alter-meta!
      (var children)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.protocols/children) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.protocols/children (var children))
    #'rewrite-clj.node.protocols/children)
  (do
    (def child-sexprs (clojure.core/deref #'rewrite-clj.node.protocols/child-sexprs))
    (clojure.core/alter-meta!
      (var child-sexprs)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.protocols/child-sexprs) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.protocols/child-sexprs (var child-sexprs))
    #'rewrite-clj.node.protocols/child-sexprs)
  (do
    (def concat-strings (clojure.core/deref #'rewrite-clj.node.protocols/concat-strings))
    (clojure.core/alter-meta!
      (var concat-strings)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.protocols/concat-strings) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.protocols/concat-strings (var concat-strings))
    #'rewrite-clj.node.protocols/concat-strings)
  (do
    (def inner? (clojure.core/deref #'rewrite-clj.node.protocols/inner?))
    (clojure.core/alter-meta!
      (var inner?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.protocols/inner?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.protocols/inner? (var inner?))
    #'rewrite-clj.node.protocols/inner?)
  (do
    (def leader-length (clojure.core/deref #'rewrite-clj.node.protocols/leader-length))
    (clojure.core/alter-meta!
      (var leader-length)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.protocols/leader-length) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.protocols/leader-length (var leader-length))
    #'rewrite-clj.node.protocols/leader-length)
  (do
    (def length (clojure.core/deref #'rewrite-clj.node.protocols/length))
    (clojure.core/alter-meta!
      (var length)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.protocols/length) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.protocols/length (var length))
    #'rewrite-clj.node.protocols/length)
  (do
    (def printable-only? (clojure.core/deref #'rewrite-clj.node.protocols/printable-only?))
    (clojure.core/alter-meta!
      (var printable-only?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.protocols/printable-only?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.protocols/printable-only? (var printable-only?))
    #'rewrite-clj.node.protocols/printable-only?)
  (do
    (def replace-children (clojure.core/deref #'rewrite-clj.node.protocols/replace-children))
    (clojure.core/alter-meta!
      (var replace-children)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.protocols/replace-children) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.protocols/replace-children (var replace-children))
    #'rewrite-clj.node.protocols/replace-children)
  (do
    (def sexpr (clojure.core/deref #'rewrite-clj.node.protocols/sexpr))
    (clojure.core/alter-meta!
      (var sexpr)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.protocols/sexpr) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.protocols/sexpr (var sexpr))
    #'rewrite-clj.node.protocols/sexpr)
  (do
    (def sexprs (clojure.core/deref #'rewrite-clj.node.protocols/sexprs))
    (clojure.core/alter-meta!
      (var sexprs)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.protocols/sexprs) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.protocols/sexprs (var sexprs))
    #'rewrite-clj.node.protocols/sexprs)
  (do
    (def string (clojure.core/deref #'rewrite-clj.node.protocols/string))
    (clojure.core/alter-meta!
      (var string)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.protocols/string) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.protocols/string (var string))
    #'rewrite-clj.node.protocols/string)
  (do
    (def tag (clojure.core/deref #'rewrite-clj.node.protocols/tag))
    (clojure.core/alter-meta!
      (var tag)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.protocols/tag) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.protocols/tag (var tag))
    #'rewrite-clj.node.protocols/tag)
  (do
    (def comment-node (clojure.core/deref #'rewrite-clj.node.comment/comment-node))
    (clojure.core/alter-meta!
      (var comment-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.comment/comment-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.comment/comment-node (var comment-node))
    #'rewrite-clj.node.comment/comment-node)
  (do
    (def comment? (clojure.core/deref #'rewrite-clj.node.comment/comment?))
    (clojure.core/alter-meta!
      (var comment?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.comment/comment?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.comment/comment? (var comment?))
    #'rewrite-clj.node.comment/comment?)
  (do
    (def fn-node (clojure.core/deref #'rewrite-clj.node.fn/fn-node))
    (clojure.core/alter-meta!
      (var fn-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.fn/fn-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.fn/fn-node (var fn-node))
    #'rewrite-clj.node.fn/fn-node)
  (do
    (def forms-node (clojure.core/deref #'rewrite-clj.node.forms/forms-node))
    (clojure.core/alter-meta!
      (var forms-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.forms/forms-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.forms/forms-node (var forms-node))
    #'rewrite-clj.node.forms/forms-node)
  (do
    (def integer-node (clojure.core/deref #'rewrite-clj.node.integer/integer-node))
    (clojure.core/alter-meta!
      (var integer-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.integer/integer-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.integer/integer-node (var integer-node))
    #'rewrite-clj.node.integer/integer-node)
  (do
    (def keyword-node (clojure.core/deref #'rewrite-clj.node.keyword/keyword-node))
    (clojure.core/alter-meta!
      (var keyword-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.keyword/keyword-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.keyword/keyword-node (var keyword-node))
    #'rewrite-clj.node.keyword/keyword-node)
  (do
    (def meta-node (clojure.core/deref #'rewrite-clj.node.meta/meta-node))
    (clojure.core/alter-meta!
      (var meta-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.meta/meta-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.meta/meta-node (var meta-node))
    #'rewrite-clj.node.meta/meta-node)
  (do
    (def raw-meta-node (clojure.core/deref #'rewrite-clj.node.meta/raw-meta-node))
    (clojure.core/alter-meta!
      (var raw-meta-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.meta/raw-meta-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.meta/raw-meta-node (var raw-meta-node))
    #'rewrite-clj.node.meta/raw-meta-node)
  (do
    (def regex-node (clojure.core/deref #'rewrite-clj.node.regex/regex-node))
    (clojure.core/alter-meta!
      (var regex-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.regex/regex-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.regex/regex-node (var regex-node))
    #'rewrite-clj.node.regex/regex-node)
  (do
    (def deref-node (clojure.core/deref #'rewrite-clj.node.reader-macro/deref-node))
    (clojure.core/alter-meta!
      (var deref-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.reader-macro/deref-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.reader-macro/deref-node (var deref-node))
    #'rewrite-clj.node.reader-macro/deref-node)
  (do
    (def eval-node (clojure.core/deref #'rewrite-clj.node.reader-macro/eval-node))
    (clojure.core/alter-meta!
      (var eval-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.reader-macro/eval-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.reader-macro/eval-node (var eval-node))
    #'rewrite-clj.node.reader-macro/eval-node)
  (do
    (def reader-macro-node (clojure.core/deref #'rewrite-clj.node.reader-macro/reader-macro-node))
    (clojure.core/alter-meta!
      (var reader-macro-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.reader-macro/reader-macro-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.reader-macro/reader-macro-node (var reader-macro-node))
    #'rewrite-clj.node.reader-macro/reader-macro-node)
  (do
    (def var-node (clojure.core/deref #'rewrite-clj.node.reader-macro/var-node))
    (clojure.core/alter-meta!
      (var var-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.reader-macro/var-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.reader-macro/var-node (var var-node))
    #'rewrite-clj.node.reader-macro/var-node)
  (do
    (def list-node (clojure.core/deref #'rewrite-clj.node.seq/list-node))
    (clojure.core/alter-meta!
      (var list-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.seq/list-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.seq/list-node (var list-node))
    #'rewrite-clj.node.seq/list-node)
  (do
    (def map-node (clojure.core/deref #'rewrite-clj.node.seq/map-node))
    (clojure.core/alter-meta!
      (var map-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.seq/map-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.seq/map-node (var map-node))
    #'rewrite-clj.node.seq/map-node)
  (do
    (def namespaced-map-node (clojure.core/deref #'rewrite-clj.node.seq/namespaced-map-node))
    (clojure.core/alter-meta!
      (var namespaced-map-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.seq/namespaced-map-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.seq/namespaced-map-node (var namespaced-map-node))
    #'rewrite-clj.node.seq/namespaced-map-node)
  (do
    (def set-node (clojure.core/deref #'rewrite-clj.node.seq/set-node))
    (clojure.core/alter-meta!
      (var set-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.seq/set-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.seq/set-node (var set-node))
    #'rewrite-clj.node.seq/set-node)
  (do
    (def vector-node (clojure.core/deref #'rewrite-clj.node.seq/vector-node))
    (clojure.core/alter-meta!
      (var vector-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.seq/vector-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.seq/vector-node (var vector-node))
    #'rewrite-clj.node.seq/vector-node)
  (do
    (def string-node (clojure.core/deref #'rewrite-clj.node.string/string-node))
    (clojure.core/alter-meta!
      (var string-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.string/string-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.string/string-node (var string-node))
    #'rewrite-clj.node.string/string-node)
  (do
    (def quote-node (clojure.core/deref #'rewrite-clj.node.quote/quote-node))
    (clojure.core/alter-meta!
      (var quote-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.quote/quote-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.quote/quote-node (var quote-node))
    #'rewrite-clj.node.quote/quote-node)
  (do
    (def syntax-quote-node (clojure.core/deref #'rewrite-clj.node.quote/syntax-quote-node))
    (clojure.core/alter-meta!
      (var syntax-quote-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.quote/syntax-quote-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.quote/syntax-quote-node (var syntax-quote-node))
    #'rewrite-clj.node.quote/syntax-quote-node)
  (do
    (def unquote-node (clojure.core/deref #'rewrite-clj.node.quote/unquote-node))
    (clojure.core/alter-meta!
      (var unquote-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.quote/unquote-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.quote/unquote-node (var unquote-node))
    #'rewrite-clj.node.quote/unquote-node)
  (do
    (def unquote-splicing-node (clojure.core/deref #'rewrite-clj.node.quote/unquote-splicing-node))
    (clojure.core/alter-meta!
      (var unquote-splicing-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.quote/unquote-splicing-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.quote/unquote-splicing-node (var unquote-splicing-node))
    #'rewrite-clj.node.quote/unquote-splicing-node)
  (do
    (def token-node (clojure.core/deref #'rewrite-clj.node.token/token-node))
    (clojure.core/alter-meta!
      (var token-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.token/token-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.token/token-node (var token-node))
    #'rewrite-clj.node.token/token-node)
  (do
    (def uneval-node (clojure.core/deref #'rewrite-clj.node.uneval/uneval-node))
    (clojure.core/alter-meta!
      (var uneval-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.uneval/uneval-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.uneval/uneval-node (var uneval-node))
    #'rewrite-clj.node.uneval/uneval-node)
  (do
    (def comma-separated (clojure.core/deref #'rewrite-clj.node.whitespace/comma-separated))
    (clojure.core/alter-meta!
      (var comma-separated)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.whitespace/comma-separated) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.whitespace/comma-separated (var comma-separated))
    #'rewrite-clj.node.whitespace/comma-separated)
  (do
    (def line-separated (clojure.core/deref #'rewrite-clj.node.whitespace/line-separated))
    (clojure.core/alter-meta!
      (var line-separated)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.whitespace/line-separated) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.whitespace/line-separated (var line-separated))
    #'rewrite-clj.node.whitespace/line-separated)
  (do
    (def linebreak? (clojure.core/deref #'rewrite-clj.node.whitespace/linebreak?))
    (clojure.core/alter-meta!
      (var linebreak?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.whitespace/linebreak?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.whitespace/linebreak? (var linebreak?))
    #'rewrite-clj.node.whitespace/linebreak?)
  (do
    (def newlines (clojure.core/deref #'rewrite-clj.node.whitespace/newlines))
    (clojure.core/alter-meta!
      (var newlines)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.whitespace/newlines) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.whitespace/newlines (var newlines))
    #'rewrite-clj.node.whitespace/newlines)
  (do
    (def newline-node (clojure.core/deref #'rewrite-clj.node.whitespace/newline-node))
    (clojure.core/alter-meta!
      (var newline-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.whitespace/newline-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.whitespace/newline-node (var newline-node))
    #'rewrite-clj.node.whitespace/newline-node)
  (do
    (def spaces (clojure.core/deref #'rewrite-clj.node.whitespace/spaces))
    (clojure.core/alter-meta!
      (var spaces)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.whitespace/spaces) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.whitespace/spaces (var spaces))
    #'rewrite-clj.node.whitespace/spaces)
  (do
    (def whitespace-node (clojure.core/deref #'rewrite-clj.node.whitespace/whitespace-node))
    (clojure.core/alter-meta!
      (var whitespace-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.whitespace/whitespace-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.whitespace/whitespace-node (var whitespace-node))
    #'rewrite-clj.node.whitespace/whitespace-node)
  (do
    (def whitespace? (clojure.core/deref #'rewrite-clj.node.whitespace/whitespace?))
    (clojure.core/alter-meta!
      (var whitespace?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.whitespace/whitespace?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.whitespace/whitespace? (var whitespace?))
    #'rewrite-clj.node.whitespace/whitespace?)
  (do
    (def comma-node (clojure.core/deref #'rewrite-clj.node.whitespace/comma-node))
    (clojure.core/alter-meta!
      (var comma-node)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.whitespace/comma-node) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.whitespace/comma-node (var comma-node))
    #'rewrite-clj.node.whitespace/comma-node)
  (do
    (def comma? (clojure.core/deref #'rewrite-clj.node.whitespace/comma?))
    (clojure.core/alter-meta!
      (var comma?)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.whitespace/comma?) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.whitespace/comma? (var comma?))
    #'rewrite-clj.node.whitespace/comma?)
  (do
    (def whitespace-nodes (clojure.core/deref #'rewrite-clj.node.whitespace/whitespace-nodes))
    (clojure.core/alter-meta!
      (var whitespace-nodes)
      clojure.core/merge
      (clojure.core/dissoc (clojure.core/meta #'rewrite-clj.node.whitespace/whitespace-nodes) :name))
    (rewrite-clj.potemkin/link-vars #'rewrite-clj.node.whitespace/whitespace-nodes (var whitespace-nodes))
    #'rewrite-clj.node.whitespace/whitespace-nodes))

;; ## Predicates

(defn whitespace-or-comment?
  "Check whether the given node represents whitespace or comment."
  [node]
  (or (whitespace? node)
    (comment? node)))

;; ## Value

(defn ^{:deprecated "0.4.0"} value
  "DEPRECATED: Get first child as a pair of tag/sexpr (if inner node),
   or just the node's own sexpr. (use explicit analysis of `children`
   `child-sexprs` instead) "
  [node]
  (if (inner? node)
    (some-> (children node)
      (first)
      ((juxt tag sexpr)))
    (sexpr node)))
