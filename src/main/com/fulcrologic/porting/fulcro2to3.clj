(ns com.fulcrologic.porting.fulcro2to3
  (:require
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.tools.reader.reader-types :refer [push-back-reader]]
    [clojure.tools.reader :as reader]
    [clojure.tools.reader.edn :as edn]
    [com.fulcrologic.porting.core :as core]
    [com.fulcrologic.porting.transforms.fulcro :as fulcro]
    [com.fulcrologic.porting.transforms.rename :as rename]
    [com.fulcrologic.porting.rewrite-clj.zip :as z]
    [com.fulcrologic.porting.parsing.form-traversal :as ft])
  (:gen-class)
  (:import (java.io File)))

(def rename-map
  '{fulcro-css.css/get-css                                             com.fulcrologic.fulcro-css.css/get-css
    fulcro-css.css/get-css-rules                                       com.fulcrologic.fulcro-css.css/get-css-rules
    fulcro-css.css/localize-css                                        com.fulcrologic.fulcro-css.css/localize-css
    fulcro-css.css/localize-selector                                   com.fulcrologic.fulcro-css.css/localize-selector
    fulcro-css.css/raw-css                                             com.fulcrologic.fulcro-css.css/raw-css
    fulcro-css.css-implementation/cssify                               com.fulcrologic.fulcro-css.css-implementation/cssify
    fulcro-css.css-implementation/fq-component                         :NOT-PORTED
    fulcro-css.css-implementation/implements-protocol?                 :NOT-PORTED
    fulcro-css.css-implementation/local-class                          com.fulcrologic.fulcro-css.css-implementation/local-class
    fulcro-css.css-implementation/set-classname                        com.fulcrologic.fulcro-css.css-implementation/set-classname
    fulcro-css.css-injection/StyleElement                              com.fulcrologic.fulcro-css.css-injection/StyleElement
    fulcro-css.css-injection/component-css-includes-with-depth         com.fulcrologic.fulcro-css.css-injection/component-css-includes-with-depth
    fulcro-css.css-injection/find-css-nodes                            com.fulcrologic.fulcro-css.css-injection/find-css-nodes
    fulcro-css.css-injection/upsert-css                                com.fulcrologic.fulcro-css.css-injection/upsert-css
    fulcro-css.css-injection/style-element                             com.fulcrologic.fulcro-css.css-injection/style-element
    fulcro.checksums/adler32                                           com.fulcrologic.fulcro.dom-server/adler32
    fulcro.checksums/assign-react-checksum                             com.fulcrologic.fulcro.dom-server/assign-react-checksum
    fulcro.client/-abort-items-on-queue                                :NOT-PORTED
    fulcro.client/-initialize                                          :NOT-PORTED
    fulcro.client/-mutation-query?                                     :NOT-PORTED
    fulcro.client/Application                                          :NOT-PORTED
    fulcro.client/clear-queue                                          :NOT-PORTED
    fulcro.client/get-url                                              :NOT-PORTED
    fulcro.client/make-fulcro-client                                   com.fulcrologic.fulcro.application/fulcro-app
    fulcro.client/make-fulcro-test-client                              :NOT-PORTED
    fulcro.client/mount*                                               :NOT-PORTED
    fulcro.client/new-fulcro-client                                    com.fulcrologic.fulcro.application/fulcro-app
    fulcro.client/new-fulcro-test-client                               :NOT-PORTED
    fulcro.client/refresh*                                             :NOT-PORTED
    fulcro.client/register-tool                                        :NOT-PORTED
    fulcro.client/reset-history-impl                                   :NOT-PORTED
    fulcro.client.cards/fulcro-application                             :NOT-PORTED
    fulcro.client.data-fetch/append-to                                 com.fulcrologic.fulcro.algorithms.data-targeting/append-to
    fulcro.client.data-fetch/bool?                                     :NOT-PORTED
    fulcro.client.data-fetch/computed-refresh                          :NOT-PORTED
    fulcro.client.data-fetch/elide-query-nodes                         com.fulcrologic.fulcro.algorithms.misc/elide-query-nodes
    fulcro.client.data-fetch/load                                      com.fulcrologic.fulcro.data-fetch/load!
    fulcro.client.data-fetch/load-field                                com.fulcrologic.fulcro.data-fetch/load-field!
    fulcro.client.data-fetch/load-action                               :NOT-PORTED
    fulcro.client.data-fetch/load-mutation                             :NOT-PORTED
    fulcro.client.data-fetch/load-params*                              com.fulcrologic.fulcro.data-fetch/load-params*
    fulcro.client.data-fetch/multiple-targets                          com.fulcrologic.fulcro.algorithms.data-targeting/multiple-targets
    fulcro.client.data-fetch/prepend-to                                com.fulcrologic.fulcro.algorithms.data-targeting/prepend-to
    fulcro.client.data-fetch/replace-at                                com.fulcrologic.fulcro.algorithms.data-targeting/replace-at
    fulcro.client.dom/clj-map->js-object                               com.fulcrologic.fulcro.dom/clj-map->js-object
    fulcro.client.dom/convert-props                                    com.fulcrologic.fulcro.dom/convert-props
    fulcro.client.dom/gen-client-dom-fns                               com.fulcrologic.fulcro.dom/gen-client-dom-fns
    fulcro.client.dom/gen-dom-macro                                    com.fulcrologic.fulcro.dom/gen-dom-macro
    fulcro.client.dom/gen-dom-macros                                   com.fulcrologic.fulcro.dom/gen-dom-macros
    fulcro.client.dom/is-form-element?                                 com.fulcrologic.fulcro.dom/is-form-element?
    fulcro.client.dom/macro-create-element                             com.fulcrologic.fulcro.dom/macro-create-element
    fulcro.client.dom/macro-create-element*                            com.fulcrologic.fulcro.dom/macro-create-element*
    fulcro.client.dom/macro-create-wrapped-form-element                com.fulcrologic.fulcro.dom/macro-create-wrapped-form-element
    fulcro.client.dom/render                                           com.fulcrologic.fulcro.dom/render
    fulcro.client.dom/syntax-error                                     com.fulcrologic.fulcro.dom/syntax-error
    fulcro.client.dom/wrap-form-element                                com.fulcrologic.fulcro.dom/wrap-form-element
    fulcro.client.dom-common/classes->str                              com.fulcrologic.fulcro.dom-common/classes->str
    fulcro.client.dom-common/gen-docstring                             com.fulcrologic.fulcro.dom-common/gen-docstring
    fulcro.client.dom-common/interpret-classes                         com.fulcrologic.fulcro.dom-common/interpret-classes
    fulcro.client.dom-server/Element                                   com.fulcrologic.fulcro.dom-server/Element
    fulcro.client.dom-server/ReactEmpty                                com.fulcrologic.fulcro.dom-server/ReactEmpty
    fulcro.client.dom-server/ReactText                                 com.fulcrologic.fulcro.dom-server/ReactText
    fulcro.client.dom-server/Text                                      com.fulcrologic.fulcro.dom-server/Text
    fulcro.client.dom-server/append!                                   com.fulcrologic.fulcro.dom-server/append!
    fulcro.client.dom-server/camel->other-case                         com.fulcrologic.fulcro.dom-server/camel->other-case
    fulcro.client.dom-server/coerce-attr-key                           com.fulcrologic.fulcro.dom-server/coerce-attr-key
    fulcro.client.dom-server/component?                                :NOT-PORTED
    fulcro.client.dom-server/container-tag?                            com.fulcrologic.fulcro.dom-server/container-tag?
    fulcro.client.dom-server/create-element                            com.fulcrologic.fulcro.dom-server/create-element
    fulcro.client.dom-server/element                                   com.fulcrologic.fulcro.dom-server/element
    fulcro.client.dom-server/element?                                  com.fulcrologic.fulcro.dom-server/element?
    fulcro.client.dom-server/escape-html                               com.fulcrologic.fulcro.dom-server/escape-html
    fulcro.client.dom-server/gen-all-tags                              com.fulcrologic.fulcro-css.localized-dom-server/gen-all-tags
    fulcro.client.dom-server/gen-tag-fn                                com.fulcrologic.fulcro-css.localized-dom-server/gen-tag-fn
    fulcro.client.dom-server/node                                      com.fulcrologic.fulcro.dom-server/node
    fulcro.client.dom-server/normalize-styles!                         com.fulcrologic.fulcro.dom-server/normalize-styles!
    fulcro.client.dom-server/react-text-node                           com.fulcrologic.fulcro.dom-server/react-text-node
    fulcro.client.dom-server/render-attr-map!                          com.fulcrologic.fulcro.dom-server/render-attr-map!
    fulcro.client.dom-server/render-attribute!                         com.fulcrologic.fulcro.dom-server/render-attribute!
    fulcro.client.dom-server/render-element!                           com.fulcrologic.fulcro.dom-server/render-element!
    fulcro.client.dom-server/render-styles!                            com.fulcrologic.fulcro.dom-server/render-styles!
    fulcro.client.dom-server/render-to-str                             com.fulcrologic.fulcro.dom-server/render-to-str
    fulcro.client.dom-server/render-unescaped-html!                    com.fulcrologic.fulcro.dom-server/render-unescaped-html!
    fulcro.client.dom-server/render-xml-attribute!                     com.fulcrologic.fulcro.dom-server/render-xml-attribute!
    fulcro.client.dom-server/text-node                                 com.fulcrologic.fulcro.dom-server/text-node
    fulcro.client.impl.application/-enqueue                            :NOT-PORTED
    fulcro.client.impl.application/-send-payload                       :NOT-PORTED
    fulcro.client.impl.application/detect-errant-remotes               :NOT-PORTED
    fulcro.client.impl.application/enqueue-mutations                   :NOT-PORTED
    fulcro.client.impl.application/enqueue-reads                       :NOT-PORTED
    fulcro.client.impl.application/fallback-handler                    :NOT-PORTED
    fulcro.client.impl.application/generate-reconciler                 :NOT-PORTED
    fulcro.client.impl.application/initialize-global-error-callbacks   :NOT-PORTED
    fulcro.client.impl.application/is-sequential?                      :NOT-PORTED
    fulcro.client.impl.application/read-local                          :NOT-PORTED
    fulcro.client.impl.application/real-send                           :NOT-PORTED
    fulcro.client.impl.application/send-with-history-tracking          :NOT-PORTED
    fulcro.client.impl.application/server-send                         :NOT-PORTED
    fulcro.client.impl.application/split-mutations                     :NOT-PORTED
    fulcro.client.impl.application/start-network-sequential-processing :NOT-PORTED
    fulcro.client.impl.application/write-entry-point                   :NOT-PORTED
    fulcro.client.impl.data-fetch/-error-callback                      :NOT-PORTED
    fulcro.client.impl.data-fetch/-loaded-callback                     :NOT-PORTED
    fulcro.client.impl.data-fetch/-place-load-marker                   :NOT-PORTED
    fulcro.client.impl.data-fetch/-place-load-markers                  :NOT-PORTED
    fulcro.client.impl.data-fetch/-remove-marker                       :NOT-PORTED
    fulcro.client.impl.data-fetch/-set-global-loading!                 :NOT-PORTED
    fulcro.client.impl.data-fetch/-tick!                               :NOT-PORTED
    fulcro.client.impl.data-fetch/callback-env                         :NOT-PORTED
    fulcro.client.impl.data-fetch/data-field                           :NOT-PORTED
    fulcro.client.impl.data-fetch/data-ident                           :NOT-PORTED
    fulcro.client.impl.data-fetch/data-marker                          :NOT-PORTED
    fulcro.client.impl.data-fetch/data-marker?                         :NOT-PORTED
    fulcro.client.impl.data-fetch/data-params                          :NOT-PORTED
    fulcro.client.impl.data-fetch/data-path                            :NOT-PORTED
    fulcro.client.impl.data-fetch/data-query                           :NOT-PORTED
    fulcro.client.impl.data-fetch/data-query-key                       :NOT-PORTED
    fulcro.client.impl.data-fetch/data-refresh                         :NOT-PORTED
    fulcro.client.impl.data-fetch/data-remote                          :NOT-PORTED
    fulcro.client.impl.data-fetch/data-state?                          com.fulcrologic.fulcro.data-fetch/data-state?
    fulcro.client.impl.data-fetch/data-target                          :NOT-PORTED
    fulcro.client.impl.data-fetch/data-uuid                            :NOT-PORTED
    fulcro.client.impl.data-fetch/dedupe-by                            :NOT-PORTED
    fulcro.client.impl.data-fetch/earliest-load-time                   :NOT-PORTED
    ;; move?
    fulcro.client.impl.data-fetch/elide-ast-nodes                      com.fulcrologic.fulcro.algorithms.misc/elide-ast-nodes
    fulcro.client.impl.data-fetch/full-query                           :NOT-PORTED
    fulcro.client.impl.data-fetch/inject-query-params                  :NOT-PORTED
    fulcro.client.impl.data-fetch/is-deferred-transaction?             :NOT-PORTED
    fulcro.client.impl.data-fetch/is-direct-table-load?                :NOT-PORTED
    fulcro.client.impl.data-fetch/make-data-state                      :NOT-PORTED
    fulcro.client.impl.data-fetch/mark-loading                         :NOT-PORTED
    fulcro.client.impl.data-fetch/mark-parallel-loading!               :NOT-PORTED
    fulcro.client.impl.data-fetch/mark-ready                           :NOT-PORTED
    fulcro.client.impl.data-fetch/optional                             :NOT-PORTED
    fulcro.client.impl.data-fetch/ready-state                          :NOT-PORTED
    fulcro.client.impl.data-fetch/relocate-targeted-results!           :NOT-PORTED
    fulcro.client.impl.data-fetch/split-items-ready-to-load            :NOT-PORTED
    fulcro.client.impl.data-targeting/append-target?                   com.fulcrologic.fulcro.algorithms.data-targeting/append-target?
    fulcro.client.impl.data-targeting/integrate-ident                  com.fulcrologic.fulcro.algorithms.merge/integrate-ident*
    fulcro.client.impl.data-targeting/multiple-targets?                com.fulcrologic.fulcro.algorithms.data-targeting/multiple-targets?
    fulcro.client.impl.data-targeting/prepend-target?                  com.fulcrologic.fulcro.algorithms.data-targeting/prepend-target?
    fulcro.client.impl.data-targeting/process-target                   com.fulcrologic.fulcro.algorithms.data-targeting/process-target
    fulcro.client.impl.data-targeting/replacement-target?              com.fulcrologic.fulcro.algorithms.data-targeting/replacement-target?
    fulcro.client.impl.data-targeting/special-target?                  com.fulcrologic.fulcro.algorithms.data-targeting/special-target?
    fulcro.client.impl.parser/ast->expr                                edn-query-language.core/ast->expr
    fulcro.client.impl.parser/call->ast                                edn-query-language.core/call->ast
    fulcro.client.impl.parser/dispatch                                 :NOT-PORTED
    fulcro.client.impl.parser/expr->ast                                edn-query-language.core/expr->ast
    fulcro.client.impl.parser/ident->ast                               edn-query-language.core/ident->ast
    fulcro.client.impl.parser/join->ast                                edn-query-language.core/join->ast
    fulcro.client.impl.parser/keyword->ast                             edn-query-language.core/keyword->ast
    fulcro.client.impl.parser/parameterize                             edn-query-language.core/parameterize
    fulcro.client.impl.parser/parser                                   :NOT-PORTED
    fulcro.client.impl.parser/path-meta                                :NOT-PORTED
    fulcro.client.impl.parser/query->ast                               edn-query-language.core/query->ast
    fulcro.client.impl.parser/rethrow?                                 :NOT-PORTED
    fulcro.client.impl.parser/substitute-root-path-for-ident           :NOT-PORTED
    fulcro.client.impl.parser/symbol->ast                              edn-query-language.core/symbol->ast
    fulcro.client.impl.parser/union->ast                               edn-query-language.core/union->ast
    fulcro.client.impl.parser/union-entry->ast                         edn-query-language.core/union-entry->ast
    fulcro.client.impl.parser/wrap-expr                                edn-query-language.core/wrap-expr
    fulcro.client.localized-dom/emit-tag                               com.fulcrologic.fulcro-css.localized-dom/emit-tag
    fulcro.client.localized-dom-common/add-kwprops-to-props            com.fulcrologic.fulcro-css.localized-dom-common/add-kwprops-to-props
    fulcro.client.logging/value-message                                :NOT-PORTED
    fulcro.client.mutations/defmutation*                               :NOT-PORTED
    fulcro.client.network/make-xhrio                                   com.fulcrologic.fulcro.networking.http-remote/make-xhrio
    fulcro.client.primitives/-register-component!                      :NOT-PORTED
    fulcro.client.primitives/add-basis-time                            :NOT-PORTED
    fulcro.client.primitives/add-basis-time*                           :NOT-PORTED
    fulcro.client.primitives/classname->class                          com.fulcrologic.fulcro.components/registry-key->class
    fulcro.client.primitives/collect-statics                           :NOT-PORTED
    fulcro.client.primitives/computed-initial-state?                   com.fulcrologic.fulcro.components/computed-initial-state?
    fulcro.client.primitives/get-basis-time                            com.fulcrologic.fulcro.algorithms.denormalize/denormalization-time
    fulcro.client.primitives/get-current-time                          com.fulcrologic.fulcro.application/basis-t
    fulcro.client.primitives/get-history                               :NOT-PORTED
    fulcro.client.primitives/get-initial-state                         com.fulcrologic.fulcro.components/get-initial-state
    fulcro.client.primitives/get-network-activity                      :NOT-PORTED
    fulcro.client.primitives/has-ident?                                com.fulcrologic.fulcro.components/has-ident?
    fulcro.client.primitives/has-initial-app-state?                    com.fulcrologic.fulcro.components/has-initial-app-state?
    fulcro.client.primitives/has-pre-merge?                            com.fulcrologic.fulcro.components/has-pre-merge?
    fulcro.client.primitives/has-query?                                com.fulcrologic.fulcro.components/has-query?
    fulcro.client.primitives/pre-merge                                 com.fulcrologic.fulcro.components/pre-merge
    fulcro.client.primitives/reshape                                   :NOT-PORTED
    fulcro.client.primitives/validate-statics                          :NOT-PORTED
    fulcro.client.routing/bad-route                                    com.fulcrologic.fulcro.routing.legacy-ui-routers/bad-route
    fulcro.client.routing/defrouter                                    :NOT-PORTED
    fulcro.client.util/base64-decode                                   :NOT-PORTED
    fulcro.client.util/base64-encode                                   :NOT-PORTED
    fulcro.client.util/first-node                                      :NOT-PORTED
    fulcro.client.util/force-render                                    :NOT-PORTED
    fulcro.client.util/react-instance?                                 :NOT-PORTED
    fulcro.client.util/strip-parameters                                :NOT-PORTED
    fulcro.client.util/transit-clj->str                                com.fulcrologic.fulcro.algorithms.transit/transit-clj->str
    fulcro.client.util/transit-str->clj                                com.fulcrologic.fulcro.algorithms.transit/transit-str->clj
    fulcro.easy-server/Handler                                         :NOT-PORTED
    fulcro.easy-server/WebServer                                       :NOT-PORTED
    fulcro.easy-server/api                                             :NOT-PORTED
    fulcro.easy-server/app-namify-api                                  :NOT-PORTED
    fulcro.easy-server/build-handler                                   :NOT-PORTED
    fulcro.easy-server/handler                                         :NOT-PORTED
    fulcro.easy-server/index                                           :NOT-PORTED
    fulcro.easy-server/make-fulcro-server                              :NOT-PORTED
    fulcro.easy-server/make-fulcro-test-server                         :NOT-PORTED
    fulcro.easy-server/make-web-server                                 :NOT-PORTED
    fulcro.easy-server/not-found-handler                               :NOT-PORTED
    fulcro.easy-server/route-handler                                   :NOT-PORTED
    fulcro.easy-server/wrap-connection                                 :NOT-PORTED
    fulcro.easy-server/wrap-extra-routes                               :NOT-PORTED
    fulcro.events/F10?                                                 com.fulcrologic.fulcro.dom.events/F10?
    fulcro.events/F11?                                                 com.fulcrologic.fulcro.dom.events/F11?
    fulcro.events/F12?                                                 com.fulcrologic.fulcro.dom.events/F12?
    fulcro.events/F1?                                                  com.fulcrologic.fulcro.dom.events/F1?
    fulcro.events/F2?                                                  com.fulcrologic.fulcro.dom.events/F2?
    fulcro.events/F3?                                                  com.fulcrologic.fulcro.dom.events/F3?
    fulcro.events/F4?                                                  com.fulcrologic.fulcro.dom.events/F4?
    fulcro.events/F5?                                                  com.fulcrologic.fulcro.dom.events/F5?
    fulcro.events/F6?                                                  com.fulcrologic.fulcro.dom.events/F6?
    fulcro.events/F7?                                                  com.fulcrologic.fulcro.dom.events/F7?
    fulcro.events/F8?                                                  com.fulcrologic.fulcro.dom.events/F8?
    fulcro.events/F9?                                                  com.fulcrologic.fulcro.dom.events/F9?
    fulcro.events/alt?                                                 com.fulcrologic.fulcro.dom.events/alt?
    fulcro.events/ctrl?                                                com.fulcrologic.fulcro.dom.events/ctrl?
    fulcro.events/delete?                                              com.fulcrologic.fulcro.dom.events/delete?
    fulcro.events/down-arrow?                                          com.fulcrologic.fulcro.dom.events/down-arrow?
    fulcro.events/end?                                                 com.fulcrologic.fulcro.dom.events/end?
    fulcro.events/enter-key?                                           com.fulcrologic.fulcro.dom.events/enter-key?
    fulcro.events/enter?                                               com.fulcrologic.fulcro.dom.events/enter?
    fulcro.events/escape-key?                                          com.fulcrologic.fulcro.dom.events/escape-key?
    fulcro.events/escape?                                              com.fulcrologic.fulcro.dom.events/escape?
    fulcro.events/home?                                                com.fulcrologic.fulcro.dom.events/home?
    fulcro.events/is-key?                                              com.fulcrologic.fulcro.dom.events/is-key?
    fulcro.events/left-arrow?                                          com.fulcrologic.fulcro.dom.events/left-arrow?
    fulcro.events/page-down?                                           com.fulcrologic.fulcro.dom.events/page-down?
    fulcro.events/page-up?                                             com.fulcrologic.fulcro.dom.events/page-up?
    fulcro.events/right-arrow?                                         com.fulcrologic.fulcro.dom.events/right-arrow?
    fulcro.events/shift?                                               com.fulcrologic.fulcro.dom.events/shift?
    fulcro.events/tab?                                                 com.fulcrologic.fulcro.dom.events/tab?
    fulcro.events/up-arrow?                                            com.fulcrologic.fulcro.dom.events/up-arrow?
    fulcro.gettext/block->translation                                  :NOT-PORTED
    fulcro.gettext/deploy-translations                                 :NOT-PORTED
    fulcro.gettext/extract-strings                                     :NOT-PORTED
    fulcro.gettext/get-blocks                                          :NOT-PORTED
    fulcro.gettext/is-header?                                          :NOT-PORTED
    fulcro.gettext/strip-comments                                      :NOT-PORTED
    fulcro.gettext/stripquotes                                         :NOT-PORTED
    fulcro.history/add-or-increment                                    :NOT-PORTED
    fulcro.history/compressible-tx                                     :NOT-PORTED
    fulcro.history/compressible-tx?                                    :NOT-PORTED
    fulcro.history/current-step                                        :NOT-PORTED
    fulcro.history/focus-end                                           :NOT-PORTED
    fulcro.history/focus-next                                          :NOT-PORTED
    fulcro.history/focus-previous                                      :NOT-PORTED
    fulcro.history/focus-start                                         :NOT-PORTED
    fulcro.history/gc-history                                          :NOT-PORTED
    fulcro.history/get-step                                            :NOT-PORTED
    fulcro.history/history-navigator                                   :NOT-PORTED
    fulcro.history/is-timestamp?                                       :NOT-PORTED
    fulcro.history/last-tx-time                                        :NOT-PORTED
    fulcro.history/nav-position                                        :NOT-PORTED
    fulcro.history/new-history                                         :NOT-PORTED
    fulcro.history/oldest-active-network-request                       :NOT-PORTED
    fulcro.history/ordered-steps                                       :NOT-PORTED
    fulcro.history/record-history-step                                 :NOT-PORTED
    fulcro.history/remote-activity-finished                            :NOT-PORTED
    fulcro.history/remote-activity-started                             :NOT-PORTED
    fulcro.i18n/load-locale                                            :NOT-PORTED
    fulcro.i18n/change-locale                                          :NOT-PORTED
    fulcro.i18n/ensure-locale-loaded!                                  :NOT-PORTED
    fulcro.i18n/Locale                                                 :NOT-PORTED
    fulcro.i18n/LocaleSelector                                         :NOT-PORTED
    fulcro.i18n/t                                                      :NOT-PORTED
    fulcro.i18n/tr                                                     :NOT-PORTED
    fulcro.i18n/tr-ssr                                                 :NOT-PORTED
    fulcro.i18n/trc                                                    :NOT-PORTED
    fulcro.i18n/trf                                                    :NOT-PORTED
    fulcro.i18n/tr-unsafe                                              :NOT-PORTED
    fulcro.i18n/trc-unsafe                                             :NOT-PORTED
    fulcro.i18n/ui-locale-selector                                     :NOT-PORTED
    fulcro.i18n/with-locale                                            :NOT-PORTED
    fulcro.logging/should-log?                                         :NOT-PORTED
    fulcro.logging/system-log-level                                    :NOT-PORTED
    fulcro.server/Config                                               :NOT-PORTED
    fulcro.server/FulcroApiHandler                                     :NOT-PORTED
    fulcro.server/ServerEmulator                                       :NOT-PORTED
    fulcro.server/arg-assertion                                        :NOT-PORTED
    fulcro.server/assert-user                                          :NOT-PORTED
    fulcro.server/augment-map                                          com.fulcrologic.fulcro.server.api-middleware/augment-map
    fulcro.server/augment-response                                     com.fulcrologic.fulcro.server.api-middleware/augment-response
    fulcro.server/defmutation                                          :NOT-PORTED
    fulcro.server/defquery-entity                                      :NOT-PORTED
    fulcro.server/defquery-root                                        :NOT-PORTED
    fulcro.server/fulcro-parser                                        :NOT-PORTED
    fulcro.server/fulcro-system                                        :NOT-PORTED
    fulcro.server/generate-response                                    com.fulcrologic.fulcro.server.api-middleware/generate-response
    fulcro.server/handle-api-request                                   com.fulcrologic.fulcro.server.api-middleware/handle-api-request
    fulcro.server/legal-origin?                                        :NOT-PORTED
    fulcro.server/load-config                                          com.fulcrologic.fulcro.server.config/load-config!
    fulcro.server/load-edn                                             com.fulcrologic.fulcro.server.config/load-edn!
    fulcro.server/new-config                                           :NOT-PORTED
    fulcro.server/new-server-emulator                                  com.fulcrologic.fulcro.networking.mock-server-remote/mock-http-server
    fulcro.server/parser-mutate-error->response                        :NOT-PORTED
    fulcro.server/parser-read-error->response                          :NOT-PORTED
    fulcro.server/process-errors                                       :NOT-PORTED
    fulcro.server/raise-response                                       :NOT-PORTED
    fulcro.server/raw-config                                           :NOT-PORTED
    fulcro.server/reader                                               com.fulcrologic.fulcro.server.api-middleware/reader
    fulcro.server/serialize-exception                                  :NOT-PORTED
    fulcro.server/server-read                                          :NOT-PORTED
    fulcro.server/transitive-join                                      :NOT-PORTED
    fulcro.server/unknow-error->response                               :NOT-PORTED
    fulcro.server/valid-response?                                      :NOT-PORTED
    fulcro.server/wrap-protect-origins                                 :NOT-PORTED
    fulcro.server/wrap-transit-body                                    :NOT-PORTED
    fulcro.server/wrap-transit-params                                  com.fulcrologic.fulcro.server.api-middleware/wrap-transit-params
    fulcro.server/wrap-transit-response                                com.fulcrologic.fulcro.server.api-middleware/wrap-transit-response
    fulcro.server/writer                                               com.fulcrologic.fulcro.server.api-middleware/writer
    ;; add?
    fulcro.server-render/initial-state->script-tag                     :NOT-PORTED
    fulcro.support-viewer/SupportViewer                                :NOT-PORTED
    fulcro.support-viewer/get-target-app                               :NOT-PORTED
    fulcro.support-viewer/nav                                          :NOT-PORTED
    fulcro.support-viewer/start-fulcro-support-viewer                  :NOT-PORTED
    fulcro.tempid/tempid                                               com.fulcrologic.fulcro.algorithms.tempid/tempid
    fulcro.tempid/tempid?                                              com.fulcrologic.fulcro.algorithms.tempid/tempid?
    fulcro.tempid/tag                                                  com.fulcrologic.fulcro.algorithms.tempid/tag
    fulcro.ui.bootstrap3/PopOver                                       :NOT-PORTED
    fulcro.ui.bootstrap3/PopOverContent                                :NOT-PORTED
    fulcro.ui.bootstrap3/PopOverTarget                                 :NOT-PORTED
    fulcro.ui.bootstrap3/PopOverTitle                                  :NOT-PORTED
    fulcro.ui.bootstrap3/address                                       :NOT-PORTED
    fulcro.ui.bootstrap3/alert                                         :NOT-PORTED
    fulcro.ui.bootstrap3/badge                                         :NOT-PORTED
    fulcro.ui.bootstrap3/breadcrumb-item                               :NOT-PORTED
    fulcro.ui.bootstrap3/breadcrumbs                                   :NOT-PORTED
    fulcro.ui.bootstrap3/button                                        :NOT-PORTED
    fulcro.ui.bootstrap3/button-group                                  :NOT-PORTED
    fulcro.ui.bootstrap3/button-toolbar                                :NOT-PORTED
    fulcro.ui.bootstrap3/caption                                       :NOT-PORTED
    fulcro.ui.bootstrap3/carousel-ident                                :NOT-PORTED
    fulcro.ui.bootstrap3/close-button                                  :NOT-PORTED
    fulcro.ui.bootstrap3/col                                           :NOT-PORTED
    fulcro.ui.bootstrap3/collapse-ident                                :NOT-PORTED
    fulcro.ui.bootstrap3/container                                     :NOT-PORTED
    fulcro.ui.bootstrap3/container-fluid                               :NOT-PORTED
    fulcro.ui.bootstrap3/dropdown                                      :NOT-PORTED
    fulcro.ui.bootstrap3/dropdown-divider                              :NOT-PORTED
    fulcro.ui.bootstrap3/dropdown-ident                                :NOT-PORTED
    fulcro.ui.bootstrap3/dropdown-item                                 :NOT-PORTED
    fulcro.ui.bootstrap3/dropdown-item-ident                           :NOT-PORTED
    fulcro.ui.bootstrap3/form-horizontal                               :NOT-PORTED
    fulcro.ui.bootstrap3/get-abs-position                              :NOT-PORTED
    fulcro.ui.bootstrap3/glyphicon                                     :NOT-PORTED
    fulcro.ui.bootstrap3/img                                           :NOT-PORTED
    fulcro.ui.bootstrap3/inline-ul                                     :NOT-PORTED
    fulcro.ui.bootstrap3/jumbotron                                     :NOT-PORTED
    fulcro.ui.bootstrap3/label                                         :NOT-PORTED
    fulcro.ui.bootstrap3/labeled-input                                 :NOT-PORTED
    fulcro.ui.bootstrap3/lead                                          :NOT-PORTED
    fulcro.ui.bootstrap3/modal-ident                                   :NOT-PORTED
    fulcro.ui.bootstrap3/nav-ident                                     :NOT-PORTED
    fulcro.ui.bootstrap3/nav-link                                      :NOT-PORTED
    fulcro.ui.bootstrap3/nav-link-ident                                :NOT-PORTED
    fulcro.ui.bootstrap3/pager                                         :NOT-PORTED
    fulcro.ui.bootstrap3/pager-next                                    :NOT-PORTED
    fulcro.ui.bootstrap3/pager-previous                                :NOT-PORTED
    fulcro.ui.bootstrap3/pagination                                    :NOT-PORTED
    fulcro.ui.bootstrap3/pagination-entry                              :NOT-PORTED
    fulcro.ui.bootstrap3/panel                                         :NOT-PORTED
    fulcro.ui.bootstrap3/panel-body                                    :NOT-PORTED
    fulcro.ui.bootstrap3/panel-footer                                  :NOT-PORTED
    fulcro.ui.bootstrap3/panel-group                                   :NOT-PORTED
    fulcro.ui.bootstrap3/panel-heading                                 :NOT-PORTED
    fulcro.ui.bootstrap3/panel-title                                   :NOT-PORTED
    fulcro.ui.bootstrap3/plain-ul                                      :NOT-PORTED
    fulcro.ui.bootstrap3/progress-bar                                  :NOT-PORTED
    fulcro.ui.bootstrap3/quotation                                     :NOT-PORTED
    fulcro.ui.bootstrap3/row                                           :NOT-PORTED
    fulcro.ui.bootstrap3/set-active-nav-link*                          :NOT-PORTED
    fulcro.ui.bootstrap3/set-dropdown-item-active*                     :NOT-PORTED
    fulcro.ui.bootstrap3/table                                         :NOT-PORTED
    fulcro.ui.bootstrap3/thumbnail                                     :NOT-PORTED
    fulcro.ui.bootstrap3/well                                          :NOT-PORTED
    fulcro.ui.clip-geometry/Point                                      :NOT-PORTED
    fulcro.ui.clip-geometry/Rectangle                                  :NOT-PORTED
    fulcro.ui.clip-geometry/diff-translate                             :NOT-PORTED
    fulcro.ui.clip-geometry/diff-translate-rect                        :NOT-PORTED
    fulcro.ui.clip-geometry/draw-rect                                  :NOT-PORTED
    fulcro.ui.clip-geometry/event->dom-coords                          :NOT-PORTED
    fulcro.ui.clip-geometry/height                                     :NOT-PORTED
    fulcro.ui.clip-geometry/inside-rect?                               :NOT-PORTED
    fulcro.ui.clip-geometry/max-rect                                   :NOT-PORTED
    fulcro.ui.clip-geometry/new-handle                                 :NOT-PORTED
    fulcro.ui.clip-geometry/rect-midpoint                              :NOT-PORTED
    fulcro.ui.clip-geometry/width                                      :NOT-PORTED
    fulcro.ui.clip-tool/ClipTool                                       :NOT-PORTED
    fulcro.ui.clip-tool/PreviewClip                                    :NOT-PORTED
    fulcro.ui.clip-tool/change-cursor                                  :NOT-PORTED
    fulcro.ui.clip-tool/constrain-corner                               :NOT-PORTED
    fulcro.ui.clip-tool/constrain-size                                 :NOT-PORTED
    fulcro.ui.clip-tool/dragLR                                         :NOT-PORTED
    fulcro.ui.clip-tool/dragUL                                         :NOT-PORTED
    fulcro.ui.clip-tool/generate-url                                   :NOT-PORTED
    fulcro.ui.clip-tool/mouseDown                                      :NOT-PORTED
    fulcro.ui.clip-tool/mouseMoved                                     :NOT-PORTED
    fulcro.ui.clip-tool/mouseUp                                        :NOT-PORTED
    fulcro.ui.clip-tool/pan                                            :NOT-PORTED
    fulcro.ui.clip-tool/refresh-clip-region                            :NOT-PORTED
    fulcro.ui.clip-tool/refresh-image                                  :NOT-PORTED
    fulcro.ui.clip-tool/set-initial-clip                               :NOT-PORTED
    fulcro.ui.clip-tool/translate-clip-region                          :NOT-PORTED
    fulcro.ui.elements/first-node-of-type                              :NOT-PORTED
    fulcro.ui.elements/update-frame-content                            :NOT-PORTED
    fulcro.ui.file-upload/handle-file-upload                           :NOT-PORTED
    fulcro.ui.form-state/FormConfig                                    com.fulcrologic.fulcro.algorithms.form-state/FormConfig
    fulcro.ui.form-state/add-form-config                               com.fulcrologic.fulcro.algorithms.form-state/add-form-config
    fulcro.ui.form-state/add-form-config*                              com.fulcrologic.fulcro.algorithms.form-state/add-form-config*
    fulcro.ui.form-state/clear-complete*                               com.fulcrologic.fulcro.algorithms.form-state/clear-complete*
    fulcro.ui.form-state/delete-form-state*                            com.fulcrologic.fulcro.algorithms.form-state/delete-form-state*
    fulcro.ui.form-state/dirty-fields                                  com.fulcrologic.fulcro.algorithms.form-state/dirty-fields
    fulcro.ui.form-state/dirty?                                        com.fulcrologic.fulcro.algorithms.form-state/dirty?
    fulcro.ui.form-state/entity->pristine*                             com.fulcrologic.fulcro.algorithms.form-state/entity->pristine*
    fulcro.ui.form-state/form-config                                   com.fulcrologic.fulcro.algorithms.form-state/form-config
    fulcro.ui.form-state/form-id                                       com.fulcrologic.fulcro.algorithms.form-state/form-id
    fulcro.ui.form-state/get-form-fields                               com.fulcrologic.fulcro.algorithms.form-state/get-form-fields
    fulcro.ui.form-state/immediate-subforms                            com.fulcrologic.fulcro.algorithms.form-state/immediate-subforms
    fulcro.ui.form-state/invalid-spec?                                 com.fulcrologic.fulcro.algorithms.form-state/invalid-spec?
    fulcro.ui.form-state/make-validator                                com.fulcrologic.fulcro.algorithms.form-state/make-validator
    fulcro.ui.form-state/mark-complete*                                com.fulcrologic.fulcro.algorithms.form-state/mark-complete*
    fulcro.ui.form-state/no-spec-or-valid?                             com.fulcrologic.fulcro.algorithms.form-state/no-spec-or-valid?
    fulcro.ui.form-state/pristine->entity*                             com.fulcrologic.fulcro.algorithms.form-state/pristine->entity*
    fulcro.ui.form-state/valid-spec?                                   com.fulcrologic.fulcro.algorithms.form-state/valid-spec?
    fulcro.ui.forms/assert-or-fail                                     :NOT-PORTED
    fulcro.ui.forms/build-form                                         :NOT-PORTED
    fulcro.ui.forms/checkbox-input                                     :NOT-PORTED
    fulcro.ui.forms/commit-state                                       :NOT-PORTED
    fulcro.ui.forms/css-class                                          :NOT-PORTED
    fulcro.ui.forms/current-validity                                   :NOT-PORTED
    fulcro.ui.forms/current-value                                      :NOT-PORTED
    fulcro.ui.forms/default-state                                      :NOT-PORTED
    fulcro.ui.forms/defvalidator                                       :NOT-PORTED
    fulcro.ui.forms/dirty-field?                                       :NOT-PORTED
    fulcro.ui.forms/dropdown-input                                     :NOT-PORTED
    fulcro.ui.forms/element-names                                      :NOT-PORTED
    fulcro.ui.forms/fail!                                              :NOT-PORTED
    fulcro.ui.forms/field-config                                       :NOT-PORTED
    fulcro.ui.forms/field-type                                         :NOT-PORTED
    fulcro.ui.forms/form-component                                     :NOT-PORTED
    fulcro.ui.forms/form-ident                                         :NOT-PORTED
    fulcro.ui.forms/form-reduce                                        :NOT-PORTED
    fulcro.ui.forms/get-form-spec                                      :NOT-PORTED
    fulcro.ui.forms/get-forms                                          :NOT-PORTED
    fulcro.ui.forms/get-on-form-change-mutation                        :NOT-PORTED
    fulcro.ui.forms/get-original-data                                  :NOT-PORTED
    fulcro.ui.forms/html5-input                                        :NOT-PORTED
    fulcro.ui.forms/id-field                                           :NOT-PORTED
    fulcro.ui.forms/iform?                                             :NOT-PORTED
    fulcro.ui.forms/init-form                                          :NOT-PORTED
    fulcro.ui.forms/init-many                                          :NOT-PORTED
    fulcro.ui.forms/init-one                                           :NOT-PORTED
    fulcro.ui.forms/initialized-state                                  :NOT-PORTED
    fulcro.ui.forms/initialized?                                       :NOT-PORTED
    fulcro.ui.forms/integer-input                                      :NOT-PORTED
    fulcro.ui.forms/invalid?                                           :NOT-PORTED
    fulcro.ui.forms/is-form?                                           :NOT-PORTED
    fulcro.ui.forms/is-subform?                                        :NOT-PORTED
    fulcro.ui.forms/on-form-change                                     :NOT-PORTED
    fulcro.ui.forms/option                                             :NOT-PORTED
    fulcro.ui.forms/placeholder                                        :NOT-PORTED
    fulcro.ui.forms/radio-input                                        :NOT-PORTED
    fulcro.ui.forms/reduce-forms                                       :NOT-PORTED
    fulcro.ui.forms/reset-entity                                       :NOT-PORTED
    fulcro.ui.forms/server-initialized?                                :NOT-PORTED
    fulcro.ui.forms/set-current-value                                  :NOT-PORTED
    fulcro.ui.forms/subform-element                                    :NOT-PORTED
    fulcro.ui.forms/text-input                                         :NOT-PORTED
    fulcro.ui.forms/textarea-input                                     :NOT-PORTED
    fulcro.ui.forms/ui-field?                                          :NOT-PORTED
    fulcro.ui.forms/update-current-value                               :NOT-PORTED
    fulcro.ui.forms/update-forms                                       :NOT-PORTED
    fulcro.ui.forms/valid?                                             :NOT-PORTED
    fulcro.ui.forms/validatable-fields                                 :NOT-PORTED
    fulcro.ui.forms/validator                                          :NOT-PORTED
    fulcro.ui.forms/validator-args                                     :NOT-PORTED
    fulcro.ui.icons/concat-class-string                                com.fulcrologic.fulcro.dom.icons/concat-class-string
    fulcro.ui.icons/concat-state-string                                com.fulcrologic.fulcro.dom.icons/concat-state-string
    fulcro.ui.icons/icon                                               com.fulcrologic.fulcro.dom.icons/icon
    fulcro.ui.icons/title-case                                         com.fulcrologic.fulcro.dom.icons/title-case
    fulcro.util/atom?                                                  com.fulcrologic.fulcro.algorithms.misc/atom?
    fulcro.util/conform!                                               com.fulcrologic.fulcro.algorithms.misc/conform!
    fulcro.util/deep-merge                                             com.fulcrologic.fulcro.algorithms.misc/deep-merge
    fulcro.util/force-children                                         com.fulcrologic.fulcro.algorithms.misc/force-children
    fulcro.util/ident?                                                 edn-query-language.core/ident?
    fulcro.util/join-entry                                             com.fulcrologic.fulcro.algorithms.misc/join-entry
    fulcro.util/join-key                                               com.fulcrologic.fulcro.algorithms.misc/join-key
    fulcro.util/join-value                                             com.fulcrologic.fulcro.algorithms.misc/join-value
    fulcro.util/join?                                                  com.fulcrologic.fulcro.algorithms.misc/join?
    fulcro.util/mutation-join?                                         com.fulcrologic.fulcro.algorithms.misc/mutation-join?
    fulcro.util/mutation-key                                           :NOT-PORTED
    fulcro.util/mutation?                                              :NOT-PORTED
    fulcro.util/recursion?                                             com.fulcrologic.fulcro.algorithms.misc/recursion?
    fulcro.util/resolve-externs                                        :NOT-PORTED
    fulcro.util/soft-invariant                                         :NOT-PORTED
    fulcro.util/union?                                                 com.fulcrologic.fulcro.algorithms.misc/union?
    fulcro.util/unique-ident?                                          :NOT-PORTED
    fulcro.util/unique-key                                             :NOT-PORTED
    fulcro.websockets/EasyServerAdapter                                :NOT-PORTED
    fulcro.websockets/Websockets                                       com.fulcrologic.fulcro.networking.websockets/Websockets
    fulcro.websockets/make-easy-server-adapter                         :NOT-PORTED
    fulcro.websockets/make-websocket-networking                        :NOT-PORTED
    fulcro.websockets/make-websockets                                  com.fulcrologic.fulcro.networking.websockets/make-websockets
    fulcro.websockets/reconnect!                                       com.fulcrologic.fulcro.networking.websockets/reconnect!
    fulcro.websockets/sente-event-handler                              com.fulcrologic.fulcro.networking.websockets/sente-event-handler
    fulcro.websockets/wrap-api                                         com.fulcrologic.fulcro.networking.websockets/wrap-api
    fulcro.websockets.components.channel-server/ChannelServer          :NOT-PORTED
    fulcro.websockets.components.channel-server/SimpleChannelServer    :NOT-PORTED
    fulcro.websockets.components.channel-server/add-listener           :NOT-PORTED
    fulcro.websockets.components.channel-server/make-channel-server    :NOT-PORTED
    fulcro.websockets.components.channel-server/notify-listeners       :NOT-PORTED
    fulcro.websockets.components.channel-server/remove-listener        :NOT-PORTED
    fulcro.websockets.components.channel-server/route-handlers         :NOT-PORTED
    fulcro.websockets.components.channel-server/simple-channel-server  :NOT-PORTED
    fulcro.websockets.components.channel-server/valid-client-id?       :NOT-PORTED
    fulcro.websockets.components.channel-server/valid-origin?          :NOT-PORTED
    fulcro.websockets.components.channel-server/wrap-web-socket        :NOT-PORTED
    fulcro.websockets.networking/ChannelClient                         :NOT-PORTED
    fulcro.websockets.networking/make-channel-client                   :NOT-PORTED
    fulcro.websockets.networking/start-router!                         :NOT-PORTED
    fulcro.websockets.networking/stop-router!                          :NOT-PORTED
    fulcro.websockets.transit-packer/make-packer                       com.fulcrologic.fulcro.networking.transit-packer/make-packer})

(defn source-files
  "Given a starting path, returns a lazy seq of all of the source files."
  [^String starting-dir]
  (let [source? (fn [^File f]
                  (let [nm (.getName f)]
                    (or (str/ends-with? nm ".cljs")
                      (str/ends-with? nm ".clj")
                      (str/ends-with? nm ".cljc"))))]
    (filter source? (file-seq (io/file starting-dir)))))

(let [base {:fqname-old->new    {
                                 'fulcro.client.data-fetch/append-to                       'com.fulcrologic.fulcro.algorithms.data-targeting/append-to
                                 'fulcro.client.data-fetch/data-state?                     'com.fulcrologic.fulcro.data-fetch/data-state?
                                 'fulcro.client.data-fetch/failed?                         'com.fulcrologic.fulcro.data-fetch/failed?
                                 'fulcro.client.data-fetch/load                            'com.fulcrologic.fulcro.data-fetch/load!
                                 'fulcro.client.data-fetch/load-field                      'com.fulcrologic.fulcro.data-fetch/load-field!
                                 'fulcro.client.data-fetch/load-params*                    'com.fulcrologic.fulcro.data-fetch/load-params*
                                 'fulcro.client.data-fetch/loading?                        'com.fulcrologic.fulcro.data-fetch/loading?
                                 'fulcro.client.data-fetch/marker-table                    'com.fulcrologic.fulcro.data-fetch/marker-table
                                 'fulcro.client.data-fetch/multiple-targets                'com.fulcrologic.fulcro.algorithms.data-targeting/multiple-targets
                                 'fulcro.client.data-fetch/prepend-to                      'com.fulcrologic.fulcro.algorithms.data-targeting/prepend-to
                                 'fulcro.client.data-fetch/ready?                          'com.fulcrologic.fulcro.data-fetch/ready?
                                 'fulcro.client.data-fetch/refresh!                        'com.fulcrologic.fulcro.data-fetch/refresh!
                                 'fulcro.client.data-fetch/replace-at                      'com.fulcrologic.fulcro.algorithms.data-targeting/replace-at
                                 'fulcro.client.impl.data-targeting/append-to              'com.fulcrologic.fulcro.algorithms.data-targeting/append-to
                                 'fulcro.client.impl.data-targeting/multiple-targets       'com.fulcrologic.fulcro.algorithms.data-targeting/multiple-targets
                                 'fulcro.client.impl.data-targeting/multiple-targets?      'com.fulcrologic.fulcro.algorithms.data-targeting/multiple-targets?
                                 'fulcro.client.impl.data-targeting/prepend-to             'com.fulcrologic.fulcro.algorithms.data-targeting/prepend-to
                                 'fulcro.client.impl.data-targeting/replace-at             'com.fulcrologic.fulcro.algorithms.data-targeting/replace-at
                                 'fulcro.client.primitives/any->reconciler                 'com.fulcrologic.fulcro.components/any->app
                                 'fulcro.client.primitives/app-root                        'com.fulcrologic.fulcro.application/app-root
                                 'fulcro.client.primitives/ast->query                      'edn-query-language.core/ast->query
                                 'fulcro.client.primitives/classname->class                'com.fulcrologic.fulcro.components/registry-key->class
                                 'fulcro.client.primitives/component->state-map            'com.fulcrologic.fulcro.components/component->state-map
                                 'fulcro.client.primitives/component-pre-merge             'com.fulcrologic.fulcro.algorithms.merge/component-pre-merge
                                 'fulcro.client.primitives/component?                      'com.fulcrologic.fulcro.components/component?
                                 'fulcro.client.primitives/computed                        'com.fulcrologic.fulcro.components/computed
                                 'fulcro.client.primitives/db->tree                        'com.fulcrologic.fulcro.algorithms.denormalize/db->tree
                                 'fulcro.client.primitives/defsc                           'com.fulcrologic.fulcro.components/defsc
                                 'fulcro.client.primitives/force-root-render!              'com.fulcrologic.fulcro.application/force-root-render!
                                 'fulcro.client.primitives/get-computed                    'com.fulcrologic.fulcro.components/get-computed
                                 'fulcro.client.primitives/get-ident                       'com.fulcrologic.fulcro.components/get-ident
                                 'fulcro.client.primitives/get-initial-state               'com.fulcrologic.fulcro.components/get-initial-state
                                 'fulcro.client.primitives/get-query                       'com.fulcrologic.fulcro.components/get-query
                                 'fulcro.client.primitives/get-state                       'com.fulcrologic.fulcro.components/get-state
                                 'fulcro.client.primitives/integrate-ident                 'com.fulcrologic.fulcro.algorithms.merge/integrate-ident*
                                 'fulcro.client.primitives/merge-alternate-union-elements  'com.fulcrologic.fulcro.algorithms.merge/merge-alternate-union-elements
                                 'fulcro.client.primitives/merge-alternate-union-elements! 'com.fulcrologic.fulcro.algorithms.merge/merge-alternate-union-elements!
                                 'fulcro.client.primitives/merge-component                 'com.fulcrologic.fulcro.algorithms.merge/merge-component
                                 'fulcro.client.primitives/merge-component!                'com.fulcrologic.fulcro.algorithms.merge/merge-component!
                                 'fulcro.client.primitives/merge-mutation-joins            'com.fulcrologic.fulcro.algorithms.merge/merge-mutation-joins
                                 'fulcro.client.primitives/pre-merge-transform             'com.fulcrologic.fulcro.algorithms.merge/pre-merge-transform
                                 'fulcro.client.primitives/query->ast                      'edn-query-language.core/query->ast
                                 'fulcro.client.primitives/query->ast1                     'edn-query-language.core/query->ast1
                                 'fulcro.client.primitives/set-state!                      'com.fulcrologic.fulcro.components/set-state!
                                 'fulcro.client.primitives/tempid                          'com.fulcrologic.fulcro.algorithms.tempid/tempid
                                 'fulcro.client.primitives/tempid?                         'com.fulcrologic.fulcro.algorithms.tempid/tempid?
                                 'fulcro.client.primitives/transact!                       'com.fulcrologic.fulcro.components/transact!
                                 'fulcro.client.primitives/tree->db                        'com.fulcrologic.fulcro.algorithms.normalize/tree->db
                                 'fulcro.client.primitives/update-state!                   'com.fulcrologic.fulcro.components/update-state!
                                 'fulcro.client.primitives/with-parent-context             'com.fulcrologic.fulcro.components/with-parent-context
                                 ;'fulcro.client.primitives/integrate-ident!    nil
                                 }

            :namespace-old->new '{fulcro.client.data-fetch           com.fulcrologic.fulcro.data-fetch
                                  fulcro.client.dom-common           com.fulcrologic.fulcro.dom-common
                                  fulcro.client.dom                  com.fulcrologic.fulcro.dom
                                  fulcro.incubator.dynamic-routing   com.fulcrologic.fulcro.routing.dynamic-routing
                                  fulcro.incubator.ui-state-machines com.fulcrologic.fulcro.ui-state-machines
                                  fulcro.websockets                  com.fulcrologic.fulcro.networking.websockets
                                  fulcro.client.mutations            com.fulcrologic.fulcro.mutations}

            :deleted-namespaces '#{fulcro.client.primitives
                                   fulcro.client.routing
                                   fulcro.client
                                   fulcro.client.impl.parser
                                   fulcro.client.cards
                                   fulcro.client.util
                                   fulcro.client.network
                                   fulcro.client.websockets
                                   fulcro.client.logging
                                   fulcro.client.impl.protocols
                                   fulcro.client.impl.application
                                   fulcro.client.impl.data-fetch
                                   fulcro.client.impl.data-targeting}

            :transforms         [core/record-aliases
                                 rename/flatten-nested-libspecs-transform
                                 rename/rename-namespaces-transform
                                 rename/rename-artifacts-transform
                                 rename/add-missing-namespaces-transform
                                 fulcro/update-lifecycle-transform
                                 ;fulcro/warn-missing-deps
                                 rename/delete-namespaces-transform]
            :namespace->alias   {'com.fulcrologic.fulcro.algorithms.tempid              'tempid
                                 'com.fulcrologic.fulcro.algorithms.normalize           'fnorm
                                 'com.fulcrologic.fulcro.algorithms.denormalize         'fdn
                                 'com.fulcrologic.fulcro.algorithms.merge               'merge
                                 'com.fulcrologic.fulcro.algorithms.tx-processing       'txn
                                 'com.fulcrologic.fulcro.algorithms.application-helpers 'ah
                                 'com.fulcrologic.fulcro.algorithms.form-state          'fs
                                 'com.fulcrologic.fulcro.algorithms.data-targeting      'targeting
                                 'com.fulcrologic.fulcro.application                    'app
                                 'com.fulcrologic.fulcro.components                     'comp
                                 'com.fulcrologic.fulcro.data-fetch                     'df
                                 'com.fulcrologic.fulcro.mutations                      'm
                                 'com.fulcrologic.fulcro.dom                            'dom
                                 'com.fulcrologic.fulcro.dom-server                     'dom
                                 'edn-query-language.core                               'eql}}]
  (def fulcro-port-config {:agnostic base
                           :cljs     (merge base {:namespace-old->new {'fulcro.client.dom           'com.fulcrologic.fulcro.dom
                                                                       'fulcro.client.localized-dom 'com.fulcrologic.garden-css.dom}})
                           :clj      (merge base {:namespace-old->new {'fulcro.client.dom-server           'com.fulcrologic.fulcro.dom-server
                                                                       'fulcro.client.localized-dom-server 'com.fulcrologic.garden-css.dom-server}})}))

(defn -main [& args]
  (log/merge-config! {:output-fn log/default-output-fn})
  (log/info (str/join "," args))
  (doseq [dir args]
    (doseq [f (source-files dir)
            :let [filename (.getAbsolutePath f)]]
      (log/info "================================================================================")
      (log/info "Porting" filename)
      (core/process-file filename fulcro-port-config))))

(defn reader-cond-loc->map [loc]
  (->> loc
    z/down
    z/right
    z/sexpr
    (partition 2)
    (into {} (map vec))))

(def defn-form? '#{defn >defn defmacro defrecord defsc})
(defn loc-of-function? [loc]
  (if (ft/reader-cond? loc)
    (let [form (-> (reader-cond-loc->map loc) vals first)]
      (and (list? form) (defn-form? (some-> form first name symbol))))
    (and loc (z/list? loc) (defn-form? (some-> loc (z/sexpr) first name symbol)))))

(defn function-locations [f]
  (let [f      (io/file f)
        zipper (z/of-file f)
        ns     (-> zipper z/down z/next z/sexpr)]
    (loop [floc (z/find-next zipper loc-of-function?) functions (sorted-map)]
      (let [floc (if (ft/reader-cond? floc)
                   (-> floc z/down z/right z/down z/right)
                   floc)]
        (if floc
          (let [fname     (-> floc z/down z/right z/sexpr)
                functions (update functions (symbol (name fname)) (fnil conj (sorted-set))
                            (symbol (name ns) (name fname)))]
            (recur (z/find-next floc loc-of-function?) functions))
          functions)))))

(defn all-function-locations [source-dir]
  (reduce
    (fn [result f]
      (merge result (function-locations f)))
    (sorted-map)
    (source-files source-dir)))

(defn get-fulcro-remappings []
  (let [simple->new-fqnames (all-function-locations "../fulcro3/src/main")
        css->new-fqnames    (all-function-locations "../fulcro-garden-css/src/main")
        ws->new-fqnames     (all-function-locations "../fulcro3-websockets/src/main")
        simple->new-fqnames (merge simple->new-fqnames css->new-fqnames ws->new-fqnames)
        simple->old-fqnames (all-function-locations "../fulcro/src/main")]
    (reduce
      (fn [result k]
        (let [old-names (simple->old-fqnames k)
              old-name  (first old-names)
              new-names (simple->new-fqnames k)
              new-name  (first new-names)]
          (cond
            (> (count old-names) 1) (assoc result old-name :DUPLICATE-OLD)
            (empty? new-names) (assoc result old-name :NOT-PORTED)
            (> (count new-names) 1) (assoc result old-name :NOT-PORTED)
            :else (assoc result old-name new-name))))
      (sorted-map)
      (keys simple->old-fqnames))))

(comment
  (-main "/Users/tonykay/fulcrologic/ucv/src/main/ucv/util.cljc"))
