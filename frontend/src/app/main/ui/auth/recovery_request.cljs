;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns app.main.ui.auth.recovery-request
  (:require
   [app.common.schema :as sm]
   [app.main.data.notifications :as ntf]
   [app.main.data.profile :as du]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.components.forms :as fm]
   [app.main.ui.components.link :as lk]
   [app.main.ui.studio.auth :as sa]
   [app.util.i18n :as i18n :refer [tr]]
   [beicon.v2.core :as rx]
   [rumext.v2 :as mf]))

(def ^:private schema:recovery-request-form
  [:map {:title "RecoverRequestForm"}
   [:email ::sm/email]])

(mf/defc recovery-form*
  [{:keys [on-success-callback]}]
  (let [form      (fm/use-form :schema schema:recovery-request-form
                               :initial {})
        submitted (mf/use-state false)

        default-success-finish
        (mf/use-fn
         #(st/emit! (ntf/info (tr "auth.notifications.recovery-token-sent"))))

        on-success
        (mf/use-fn
         (fn [cdata _]
           (reset! submitted false)
           (if (nil? on-success-callback)
             (default-success-finish)
             (on-success-callback (:email cdata)))))

        on-error
        (mf/use-fn
         (fn [data cause]
           (reset! submitted false)
           (let [code (-> cause ex-data :code)]
             (case code
               :profile-not-verified
               (rx/of (ntf/error (tr "auth.notifications.profile-not-verified")))

               :profile-is-muted
               (rx/of (ntf/error (tr "errors.profile-is-muted")))

               (:email-has-permanent-bounces
                :email-has-complaints)
               (rx/of (ntf/error (tr "errors.email-has-permanent-bounces" (:email data))))

               (rx/throw cause)))))

        on-submit
        (mf/use-fn
         (fn []
           (reset! submitted true)
           (let [cdata  (:clean-data @form)
                 params (with-meta cdata
                          {:on-success #(on-success cdata %)
                           :on-error #(on-error cdata %)})]
             (reset! form nil)
             (st/emit! (du/request-profile-recovery params)))))]

    ;; Design Studio (WP5): same form and events; F1 · 09 markup.
    [:& fm/form {:on-submit on-submit
                 :class sa/form-class
                 :form form}
     [:> sa/field* {:field :email
                    :label (tr "ds.signin.email")
                    :placeholder (tr "ds.signin.email-placeholder")
                    :type "text"
                    :auto-complete "username"
                    :auto-focus true}]

     [:> sa/submit* {:label (tr "ds.recovery.request.submit")
                     :data-testid "recovery-resquest-submit"}]]))


;; --- Recovery Request Page

(mf/defc recovery-request-page*
  [{:keys [params on-success-callback go-back-callback]}]
  (let [default-go-back #(st/emit! (rt/nav :auth-login))
        go-back (or go-back-callback default-go-back)]
    ;; Design Studio (WP5): F1 · 09 "Lupa password". For 15 minutes after a
    ;; profile changes Penpot sends no reset mail and answers as if it had
    ;; (ADR 0017), so with open sign-up the screen says so.
    [:> sa/scope* {}
     [:> sa/page* {:title (tr "ds.auth.reset.title")
                   :subtitle (tr "ds.auth.reset.subtitle")}
      [:> recovery-form* {:params params :on-success-callback on-success-callback}]
      (when (sa/registration-open?)
        [:> sa/note* {:level "warning"
                      :data-testid "ds-reset-wait"}
         (tr "ds.auth.reset.wait-note")])
      [:div
       [:> lk/link* {:action go-back
                     :class sa/link-class
                     :data-testid "go-back-link"}
        (tr "ds.auth.back-to-sign-in")]]]]))


(mf/defc recovery-sent-page*
  [{:keys [email]}]
  ;; Design Studio (WP5): same message in our markup.
  [:> sa/scope* {}
   [:> sa/page* {:title (tr "ds.recovery.sent.title")
                 :subtitle (tr "ds.recovery.sent.text" email)}
    [:> sa/note* {} (tr "ds.recovery.sent.hint")]]])
