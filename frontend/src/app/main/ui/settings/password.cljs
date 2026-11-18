;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns app.main.ui.settings.password
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.schema :as sm]
   [app.main.data.notifications :as ntf]
   [app.main.data.profile :as udu]
   [app.main.store :as st]
   [app.main.ui.components.forms :as fm]
   [app.main.ui.studio.guard :refer [guard*]]
   [app.main.ui.studio.settings-notes :refer [password-note*]]
   [app.main.ui.studio.settings-shell.page :refer [page-head*]]
   [app.main.ui.studio.shell-state :as ds-state]
   [app.util.dom :as dom]
   [app.util.i18n :as i18n :refer [tr]]
   [rumext.v2 :as mf]))

(defn- on-error
  [form error]
  (let [data (ex-data error)]
    (case (:code data)
      :old-password-not-match
      (swap! form assoc-in [:extra-errors :password-old]
             {:message (tr "errors.wrong-old-password")})

      :email-as-password
      (swap! form assoc-in [:extra-errors :password-1]
             {:message (tr "errors.email-as-password")})

      :weak-password
      (let [details (:details data)
            options (when (seq details)
                      (mapv tr details))]
        (swap! form assoc-in [:extra-errors :password-1]
               {:message (tr "errors.weak-password")
                :options options}))

      (let [msg (tr "generic.error")]
        (st/emit! (ntf/error msg))))))

(defn- on-success
  [form]
  (reset! form nil)
  (let [password-old-node (dom/get-element "password-old")
        msg (tr "dashboard.notifications.password-saved")]
    (dom/clean-value! password-old-node)
    (dom/focus! password-old-node)
    (st/emit! (ntf/success msg))))

(defn- on-submit
  [form event]
  (dom/prevent-default event)
  (let [params (with-meta (:clean-data @form)
                 {:on-success (partial on-success form)
                  :on-error (partial on-error form)})]
    (st/emit! (udu/update-password params))))

(def schema:password-form
  [:and
   [:map {:title "PasswordForm"}
    [:password-1 ::sm/password]
    [:password-2 ::sm/password]
    ;; The old password is validated by the backend, so it only needs to be
    ;; present here; it may predate the current minimum length policy.
    [:password-old [::sm/text {:max 500}]]]
   [:fn {:error/code "errors.password-invalid-confirmation"
         :error/field :password-2}
    (fn [{:keys [password-1 password-2]}]
      (= password-1 password-2))]])

(mf/defc password-form*
  []
  (let [initial (mf/with-memo []
                  {:password-old ""
                   :password-1 ""
                   :password-2 ""})
        form    (fm/use-form :schema schema:password-form
                             :initial initial)]

    [:& fm/form {:class (stl/css :password-form)
                 :on-submit on-submit
                 :form form}
     [:div {:class (stl/css :fields-row)}
      [:& fm/input
       {:type "password"
        :name :password-old
        :auto-focus? true
        :label (tr "labels.old-password")}]]

     [:div {:class (stl/css :fields-row)}
      [:& fm/input
       {:type "password"
        :name :password-1
        :show-success? true
        :label (tr "labels.new-password")}]]

     [:div {:class (stl/css :fields-row)}
      [:& fm/input
       {:type "password"
        :name :password-2
        :show-success? true
        :label (tr "labels.confirm-password")}]]

     [:> fm/submit-button*
      {:label (tr "dashboard.password-change")
       :data-testid "submit-password"
       :class (stl/css :update-btn)}]]))

;; --- Password Page

(mf/defc password-page*
  []
  (mf/with-effect []
    (dom/set-html-title (tr "title.settings.password")))

  (let [shell   (ds-state/use-state)
        backup? (and shell (ds-state/backup? shell))]
    [:section {:class (stl/css :dashboard-settings)
               :aria-labelledby "password-section-title"}
     [:div {:class (stl/css :form-container)}
      ;; Design Studio seam (STUDIO-SEAMS.md, P3-A): the page header of
      ;; every Settings page.
      [:> page-head* {:id "password-section-title"
                      :title (if backup?
                               (tr "ds.settings.password-title")
                               (tr "labels.password"))
                      :subtitle (tr "ds.settings.password.subtitle")}]
      [:> guard* {}
       [:> password-note* {:state shell}]]
      [:> password-form*]]]))
