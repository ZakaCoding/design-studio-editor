;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.widgets
  "Design Studio widgets that are not part of one page: the guard refusal
  card, the first steps after sign-up, the workspace MCP tip and its
  interactive guide. Mounted once, from `app.main.ui/app`."
  (:require
   [app.main.data.auth :as da]
   [app.main.ui.error-boundary :refer [error-boundary*]]
   [app.main.ui.studio.first-steps :refer [first-steps*]]
   [app.main.ui.studio.mcp-tip :refer [mcp-tip*]]
   [app.main.ui.studio.mcp-tour :refer [mcp-tour*]]
   [app.main.ui.studio.refusal-card :refer [refusal-card*]]
   [rumext.v2 :as mf]))

(mf/defc silent*
  "Fallback of the widgets' own error boundary: a fault in a widget is
  reported like any other and never takes the editor down with it."
  {::mf/private true}
  []
  nil)

(mf/defc global-widgets*
  [{:keys [route profile has-exception]}]
  (let [section        (-> route :data :name)
        authenticated? (da/is-authenticated? profile)]
    [:> error-boundary* {:fallback silent*}
     [:> refusal-card*]
     (when (and authenticated? (not has-exception))
       [:*
        [:> first-steps* {:section section}]
        [:> mcp-tour* {:section section}]
        (when (= section :workspace)
          [:> mcp-tip*])])]))
