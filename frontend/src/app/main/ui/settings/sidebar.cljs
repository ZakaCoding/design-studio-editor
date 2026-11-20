;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns app.main.ui.settings.sidebar
  (:require-macros [app.main.style :as stl])
  (:require
   [app.config :as cf]
   [app.main.data.common :as dcm]
   [app.main.data.event :as ev]
   [app.main.data.modal :as modal]
   [app.main.data.team :as dtm]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.dashboard.sidebar :refer [profile-section*]]
   [app.main.ui.studio.dashboard-shell.scope :as ds-scope]
   [app.main.ui.studio.settings-shell.icons :as ds-icons]
   [app.main.ui.studio.settings-shell.page :as ds-page]
   [app.main.ui.studio.shell-state :as ds-state]
   [app.util.i18n :as i18n :refer [tr]]
   [app.util.keyboard :as kbd]
   [rumext.v2 :as mf]))

;; FIXME: move to common
(def ^:private go-settings-profile
  #(st/emit! (rt/nav :settings-profile)))

(def ^:private go-settings-feedback
  #(st/emit! (rt/nav :settings-feedback)))

(def ^:private go-settings-password
  #(st/emit! (rt/nav :settings-password)))

(def ^:private go-settings-options
  #(st/emit! (rt/nav :settings-options)))

(def ^:private go-settings-subscription
  #(st/emit! (rt/nav :settings-subscription)))

(def ^:private go-settings-integrations
  #(st/emit! (rt/nav :settings-integrations)))

(def ^:private go-settings-notifications
  #(st/emit! (rt/nav :settings-notifications)))

(def ^:private go-settings-shortcuts
  #(st/emit! (rt/nav :settings-shortcuts)))

(defn- show-release-notes
  [event]
  (let [version (:main cf/version)]
    (st/emit! (ev/event {::ev/name "show-release-notes" :version version}))

    (if (and (kbd/alt? event) (kbd/mod? event))
      (st/emit! (modal/show {:type :onboarding}))
      (st/emit! (modal/show {:type :release-notes :version version})))))

(mf/defc sidebar-content*
  [{:keys [profile section]}]
  (let [profile?       (= section :settings-profile)
        password?      (= section :settings-password)
        options?       (= section :settings-options)
        feedback?      (= section :settings-feedback)
        subscription?  (= section :settings-subscription)
        integrations?  (= section :settings-integrations)
        notifications? (= section :settings-notifications)
        shortcuts?     (= section :settings-shortcuts)
        shell          (ds-state/use-state)
        team-id        (or (dtm/get-last-team-id)
                           (:default-team-id profile))

        go-dashboard
        (mf/use-fn
         (mf/deps team-id)
         #(st/emit! (dcm/go-to-dashboard-recent :team-id team-id)))]

    [:div {:class (stl/css :sidebar-content)}
     [:div {:class (stl/css :sidebar-content-section)}
      [:button {:class (stl/css :back-to-dashboard)
                :on-click go-dashboard}
       ;; Design Studio seam (STUDIO-SEAMS.md): "‹ Home" (F1 · 10)
       [:> ds-icons/back* {:size "m"}]
       [:span {:class (stl/css :back-text)} (tr "ds.shell.nav.home")]]]

     [:hr {:class (stl/css :sidebar-separator)}]

     ;; Design Studio seam (STUDIO-SEAMS.md): the sidebar title of F1 · 10
     [:h2 {:data-ds-dash "settings-title"} (tr "ds.shell.settings.title")]

     ;; Design Studio seam (STUDIO-SEAMS.md, P3-A): the rows are the
     ;; dashboard sidebar's (a line icon, the raised current row); the
     ;; pages, the handlers and upstream's test ids are unchanged.
     [:nav {:class (stl/css :sidebar-content-section)
            :aria-label (tr "labels.settings")}
      [:ul {:class (stl/css :sidebar-nav-settings)}
       [:> ds-page/nav-row* {:label (tr "labels.profile")
                             :icon ds-icons/user*
                             :is-current profile?
                             :on-click go-settings-profile
                             :test-id "ds-settings-nav-profile"}]

       [:> ds-page/nav-row* {:label (if (and shell (ds-state/backup? shell))
                                      (tr "ds.settings.password-side")
                                      (tr "labels.password"))
                             :icon ds-icons/lock*
                             :is-current password?
                             :on-click go-settings-password
                             :test-id "ds-settings-nav-password"}]

       [:> ds-page/nav-row* {:label (tr "labels.notifications")
                             :icon ds-icons/bell*
                             :is-current notifications?
                             :on-click go-settings-notifications
                             :test-id "ds-settings-nav-notifications"}]

       (when (contains? cf/flags :custom-shortcuts)
         [:> ds-page/nav-row* {:label (tr "label.shortcuts")
                               :icon ds-icons/keyboard*
                               :is-current shortcuts?
                               :on-click go-settings-shortcuts
                               :test-id "ds-settings-nav-shortcuts"}])

       [:> ds-page/nav-row* {:label (tr "ds.settings.options-side")
                             :icon ds-icons/globe*
                             :is-current options?
                             :on-click go-settings-options
                             :test-id "settings-profile"}]

       (when (or (contains? cf/flags :subscriptions)
                 (contains? cf/flags :admin-console))
         [:> ds-page/nav-row* {:label (tr "ds.plans.side")
                               :icon ds-icons/card*
                               :is-current subscription?
                               :on-click go-settings-subscription
                               :test-id "settings-subscription"}])

       (when (or (contains? cf/flags :access-tokens)
                 (contains? cf/flags :mcp))
         [:> ds-page/nav-row* {:label (tr "ds.settings.ai-access-side")
                               :icon ds-icons/plug*
                               :is-current integrations?
                               :on-click go-settings-integrations
                               :test-id "settings-integrations"}])

       [:hr {:class (stl/css :sidebar-separator)
             :data-ds-settings "nav-rule"}]

       [:> ds-page/nav-row* {:label (tr "labels.release-notes")
                             :icon ds-icons/notes*
                             :is-current false
                             :on-click show-release-notes
                             :test-id "release-notes"}]

       (when (contains? cf/flags :user-feedback)
         [:> ds-page/nav-row* {:label (tr "labels.contact-us")
                               :icon ds-icons/message*
                               :is-current feedback?
                               :on-click go-settings-feedback
                               :test-id "ds-settings-nav-feedback"}])]]]))

(mf/defc sidebar*
  {::mf/wrap [mf/memo]}
  [{:keys [profile section]}]
  ;; Design Studio seam (STUDIO-SEAMS.md): the light Design Studio chrome
  ;; (scope of `studio/dashboard_shell/chrome.scss`) while Settings is open;
  ;; P3-A: and the scope of `studio/settings_shell/polish.scss`
  (ds-scope/use-scope)
  (ds-page/use-scope)
  [:aside {:class (stl/css :dashboard-sidebar :settings)
           :data-ds-settings "sidebar"}
   [:> sidebar-content* {:profile profile
                         :section section}]
   [:> profile-section* {:profile profile}]])
