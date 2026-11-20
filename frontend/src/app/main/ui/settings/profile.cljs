;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns app.main.ui.settings.profile
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.schema :as sm]
   [app.config :as cf]
   [app.main.data.modal :as modal]
   [app.main.data.notifications :as ntf]
   [app.main.data.profile :as du]
   [app.main.refs :as refs]
   [app.main.store :as st]
   [app.main.ui.components.file-uploader :refer [file-uploader]]
   [app.main.ui.components.forms :as fm]
   [app.main.ui.ds.foundations.assets.icon :as i :refer [icon*]]
   [app.main.ui.studio.guard :refer [guard*]]
   [app.main.ui.studio.settings-notes :refer [email-note*]]
   [app.main.ui.studio.settings-shell.page :refer [page-head* card* row*]]
   [app.main.ui.studio.shell-state :as ds-state]
   [app.util.dom :as dom]
   [app.util.i18n :as i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def ^:private schema:profile-form
  [:map {:title "ProfileForm"}
   [:fullname [::sm/text {:max 250}]]
   [:email ::sm/email]])

(defn- on-submit
  [form _event]
  (let [data       (:clean-data @form)
        on-success (fn [_]
                     (swap! form assoc :touched {})
                     (st/emit! (ntf/success (tr "notifications.profile-saved"))))]
    (st/emit! (du/update-profile data)
              (du/persist-profile {:on-success on-success}))))

;; --- Profile Form

(mf/defc profile-form*
  {::mf/private true}
  []
  (let [profile (mf/deref refs/profile)
        form    (fm/use-form :schema schema:profile-form
                             :initial profile)

        shell   (ds-state/use-state)
        open?   (ds-state/open? shell)

        on-show-change-email
        (mf/use-fn
         #(modal/show! :change-email {}))]

    ;; Design Studio seam (STUDIO-SEAMS.md, P3D): the fields are rows of
    ;; the profile card (label left, control right), Save sits in the
    ;; card's foot; same form, fields, handlers and test ids.
    [:& fm/form {:on-submit on-submit
                 :form form
                 :class (stl/css :profile-form)}
     [:> row* {:label (tr "ds.settings.profile.name") :for "fullname"}
      [:div {:class (stl/css :fields-row)}
       [:& fm/input
        {:type "text"
         :name :fullname
         ;; Design Studio (P3-A): our own labels, "Name" and "Email"
         :label (tr "ds.settings.profile.name")}]]]

     [:> row* {:label (tr "ds.settings.profile.email")
               :action (when-not open?
                         (mf/html
                          [:div {:class (stl/css :change-email)}
                           [:a {:on-click on-show-change-email
                                :role "button"
                                :tab-index 0
                                :data-ds-settings "row-button"}
                            (tr "dashboard.change-email")]]))}
      [:div {:class (stl/css :options)
             :data-ds-settings "email"}
       [:span {:data-ds-settings "email-value"} (:email profile)]
       (when open?
         [:> guard* {}
          [:> email-note*]])]]

     [:div {:data-ds-settings "foot"}
      [:> fm/submit-button*
       {:label (tr "dashboard.save-settings")
        :disabled (empty? (:touched @form))
        :class (stl/css :btn-primary)}]]]))

(mf/defc delete-account-row*
  {::mf/private true}
  []
  (let [on-show-delete-account
        (mf/use-fn
         #(modal/show! :delete-account {}))]
    ;; Design Studio seam (STUDIO-SEAMS.md, P3D): deleting the account is
    ;; one row of its own card; same dialog, same test id.
    [:> card* {:tone "danger"}
     [:> row* {:label (tr "ds.settings.delete.title")
               :hint (tr "ds.settings.delete.text")
               :is-wide true
               :action (mf/html
                        [:a {:on-click on-show-delete-account
                             :role "button"
                             :tab-index 0
                             :data-ds-settings "danger-button"
                             :data-testid "remove-acount-btn"}
                         (tr "ds.settings.delete.action")])}]]))

;; --- Profile Photo Form

(mf/defc profile-photo-form*
  {::mf/private true}
  []
  (let [input-ref  (mf/use-ref nil)
        profile    (mf/deref refs/profile)
        has-photo? (some? (:photo-id profile))

        photo
        (mf/with-memo [profile]
          (cf/resolve-profile-photo-url profile))

        on-image-click
        (mf/use-fn
         #(dom/click (mf/ref-val input-ref)))

        on-file-selected
        (fn [file]
          (st/emit! (du/update-photo file)))

        on-delete-click
        (mf/use-fn
         (fn [event]
           (dom/stop-propagation event)
           (st/emit! (modal/show
                      {:type :confirm
                       :title (tr "labels.delete-profile-photo.title")
                       :message (tr "labels.delete-profile-photo.message")
                       :accept-label (tr "labels.delete")
                       :on-accept (fn [_] (st/emit! du/delete-photo))}))))]

    ;; Design Studio seam (STUDIO-SEAMS.md, P3-A, P3D): the photo is the
    ;; first row of the profile card, a 40 px avatar and "Change photo"
    ;; (it was a 148 px disc with the action only on hover).
    [:form {:class (stl/css :avatar-form)
            :data-ds-settings "photo"}
     [:> row* {:label (tr "ds.settings.photo.title")
               :hint (tr "ds.settings.photo.hint")}
      [:div {:class (stl/css :image-change-field)}
       [:span {:class (stl/css :update-overlay)
               :on-click on-image-click} (tr "labels.update")]
       [:img {:src photo :alt ""}]
       (when has-photo?
         [:button {:type "button"
                   :class (stl/css :delete-overlay)
                   :title (tr "labels.delete")
                   :aria-label (tr "labels.delete")
                   :on-click on-delete-click
                   :data-testid "profile-image-delete"}
          [:> icon* {:icon-id i/delete :size "m"}]])
       [:& file-uploader {:accept "image/jpeg,image/png"
                          :multi false
                          :ref input-ref
                          :on-selected on-file-selected
                          :data-testid "profile-image-input"}]]
      [:button {:type "button"
                :data-ds-settings "photo-button"
                :on-click on-image-click}
       (tr "ds.settings.photo.change")]]]))

;; --- Profile Page

(mf/defc profile-page*
  []
  (mf/with-effect []
    (dom/set-html-title (tr "title.settings.profile")))

  [:section {:class (stl/css :dashboard-settings)
             :aria-labelledby "profile-section-title"}
   [:div {:class (stl/css :form-container)}
    [:> page-head* {:id "profile-section-title"
                    :title (tr "labels.profile")
                    :subtitle (tr "ds.settings.profile.subtitle")}]
    [:> card* {:class (stl/css :profile-card)}
     [:> profile-photo-form*]
     [:> profile-form*]]
    [:> delete-account-row*]]])
