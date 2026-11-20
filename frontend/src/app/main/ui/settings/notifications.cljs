;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns app.main.ui.settings.notifications
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.common.schema :as sm]
   [app.main.data.profile :as dp]
   [app.main.store :as st]
   [app.main.ui.components.forms :as fm]
   [app.main.ui.studio.settings-shell.page :refer [page-head* card*]]
   [app.util.dom :as dom]
   [app.util.i18n :as i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def default-notification-settings
  {:dashboard-comments :all
   :email-comments :partial
   :email-invites :all})

(defn- on-submit
  [form _event]
  (let [params (:clean-data @form)]
    (st/emit! (dp/update-notifications params))))

(def ^:private schema:notifications-form
  [:map {:title "NotificationsForm"}
   [:dashboard-comments [::sm/one-of #{:all :partial :none}]]
   [:email-comments [::sm/one-of #{:all :partial :none}]]
   [:email-invites [::sm/one-of #{:all :partial :none}]]])

(mf/defc notifications-page*
  [{:keys [profile]}]
  (let [settings (mf/with-memo [profile]
                   (-> (merge default-notification-settings
                              (-> profile :props :notifications))
                       (update-vals d/name)))
        form     (fm/use-form :schema schema:notifications-form
                              :initial settings)]

    (mf/with-effect []
      (dom/set-html-title (tr "title.settings.notifications")))

    ;; Design Studio seam (STUDIO-SEAMS.md, P3D): one table card, the
    ;; events as rows and the channels (In-app, Email) as columns; each
    ;; cell is a select with the options and values of the radios it
    ;; replaces. Same form, schema, save call and test id.
    [:section {:class (stl/css :notifications-page)}
     [:& fm/form {:class (stl/css :notifications-form)
                  :on-submit on-submit
                  :form form}
      [:div {:class (stl/css :form-container)}
       [:> page-head* {:title (tr "labels.notifications")
                       :subtitle (tr "ds.settings.notifications.subtitle")}]
       [:> card* {}
        [:table {:data-ds-settings "matrix"}
         [:thead
          [:tr
           [:th {:scope "col" :data-ds-settings "col"} (tr "ds.settings.notifications.event")]
           [:th {:scope "col" :data-ds-settings "col"} (tr "ds.settings.notifications.in-app")]
           [:th {:scope "col" :data-ds-settings "col"} (tr "ds.settings.notifications.email")]]]
         [:tbody
          [:tr
           [:th {:scope "row" :data-ds-settings "event"}
            (tr "dashboard.settings.notifications.dashboard-comments.title")]
           [:td {:data-testid "ds-notify-dashboard-comments"}
            [:& fm/select
             {:options [{:label (tr "dashboard.settings.notifications.dashboard-comments.all") :value "all"}
                        {:label (tr "dashboard.settings.notifications.dashboard-comments.partial") :value "partial"}
                        {:label (tr "dashboard.settings.notifications.dashboard-comments.none") :value "none"}]
              :name :dashboard-comments}]]
           [:td {:data-testid "ds-notify-email-comments"}
            [:& fm/select
             {:options [{:label (tr "dashboard.settings.notifications.email-comments.all") :value "all"}
                        {:label (tr "dashboard.settings.notifications.email-comments.partial") :value "partial"}
                        {:label (tr "dashboard.settings.notifications.email-comments.none") :value "none"}]
              :name :email-comments}]]]
          [:tr
           [:th {:scope "row" :data-ds-settings "event"}
            (tr "dashboard.settings.notifications.email-invites.title")]
           ;; No in-app setting for invites today.
           [:td {:data-ds-settings "none"}
            [:span {:aria-label (tr "ds.settings.notifications.no-setting")} "—"]]
           [:td {:data-testid "ds-notify-email-invites"}
            [:& fm/select
             {:options [{:label (tr "dashboard.settings.notifications.email-invites.all") :value "all"}
                        ;; This type of notifications doesnt't exist yet
                        ;; {:label "Only invites and requests that my response" :value "partial"}
                        {:label (tr "dashboard.settings.notifications.email-invites.none") :value "none"}]
              :name :email-invites}]]]]]
        [:div {:data-ds-settings "foot"}
         [:> fm/submit-button*
          {:label (tr "dashboard.settings.notifications.submit")
           :disabled (= (:data @form) (:initial @form))
           :data-testid "submit-settings"
           :class (stl/css :update-btn)}]]]]]]))
