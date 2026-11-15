;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.widgets
  "Design Studio widgets that are not part of one page: the guard refusal
  card, the first steps after sign-up, the workspace MCP tip and its
  interactive guide, the Craft dock with its quick prompt, and the way to
  the sign-in for a signed-out visitor.
  Mounted once, from `app.main.ui/app`."
  (:require
   [app.main.data.auth :as da]
   [app.main.repo :as rp]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.error-boundary :refer [error-boundary*]]
   [app.main.ui.studio.craft :refer [craft*]]
   [app.main.ui.studio.first-steps :refer [first-steps*]]
   [app.main.ui.studio.mcp-tip :refer [mcp-tip*]]
   [app.main.ui.studio.mcp-tour :refer [mcp-tour*]]
   [app.main.ui.studio.refusal-card :refer [refusal-card*]]
   [beicon.v2.core :as rx]
   [cuerdas.core :as str]
   [rumext.v2 :as mf]))

(defn- members-only?
  "Dashboard and settings pages need an account. Shared view links work
  without one and the workspace has its own sign-in dialog, so only these
  two kinds of page move."
  [section]
  (and (keyword? section)
       (let [section (name section)]
         (or (str/starts-with? section "dashboard-")
             (str/starts-with? section "settings-")))))

(mf/defc silent*
  "Fallback of the widgets' own error boundary: a fault in a widget is
  reported like any other and never takes the editor down with it."
  {::mf/private true}
  []
  nil)

(mf/defc global-widgets*
  [{:keys [route profile has-exception]}]
  (let [section        (-> route :data :name)
        ;; Only a signed-in profile is kept in the state, so this is false
        ;; for a signed-out visitor and, for a moment, while signing in.
        authenticated? (da/is-authenticated? profile)]

    ;; Signed out on a dashboard or settings page, upstream 2.18.7 shows its
    ;; first-run survey (dashboard without a team) or a sign-in dialog over
    ;; an error page, never the sign-in page. Send the visitor there. Only a
    ;; clear "signed out" from the backend counts: a fault leaves the page
    ;; alone.
    (mf/with-effect [section authenticated?]
      (when (and (not authenticated?) (members-only? section))
        (let [sub (->> (rp/cmd! :get-profile)
                       (rx/subs! (fn [profile]
                                   (when-not (da/is-authenticated? profile)
                                     (st/emit! (rt/nav :auth-login))))
                                 (fn [_] nil)))]
          (fn []
            (rx/dispose! sub)))))

    [:> error-boundary* {:fallback silent*}
     [:> refusal-card*]
     (when (and authenticated? (not has-exception))
       [:*
        [:> first-steps* {:section section}]
        [:> mcp-tour* {:section section}]
        ;; F3-B: the native Craft dock and the quick prompt at the selection.
        [:> craft* {:section section}]
        (when (= section :workspace)
          [:> mcp-tip*])])]))
