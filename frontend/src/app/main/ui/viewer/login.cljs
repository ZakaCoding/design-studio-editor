;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns app.main.ui.viewer.login
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.logging :as log]
   [app.main.data.modal :as modal]
   [app.main.store :as st]
   [app.main.ui.auth.login :refer [login-dialog*]]
   [app.main.ui.auth.recovery-request :refer [recovery-request-page* recovery-sent-page*]]
   [app.main.ui.components.link :as lk]
   [app.main.ui.studio.auth :as sa]
   [app.util.dom :as dom]
   [app.util.i18n :as i18n :refer [tr]]
   [rumext.v2 :as mf]))

(log/set-level! :warn)

(mf/defc login-register-modal
  {::mf/register modal/components
   ::mf/register-as :login-register}
  [_]
  ;; Design Studio (F4-B, F1 · 11): the viewer's share-link sign-in uses our
  ;; dialog and lockup, the same one a signed-out dashboard visit gets.
  ;; "Create an account" is a link to /start/signup (ADR 0017), so Penpot's
  ;; register form is no longer reachable from here.
  (let [user-email       (mf/use-state "")
        current-section* (mf/use-state :login)
        current-section  (deref current-section*)

        set-section-recovery
        (mf/use-fn #(reset! current-section* :recovery-request))

        go-back-to-login
        (mf/use-fn #(reset! current-section* :login))

        close
        (fn [event]
          (dom/prevent-default event)
          (st/emit! (modal/hide)))

        recovery-email-sent
        (fn [email]
          (reset! user-email email)
          (reset! current-section* :recovery-email-sent))

        success-login
        (fn []
          (.reload js/window.location true))]

    [:div {:class (stl/css :modal-overlay)}
     [:> sa/modal* {:on-close close
                    :close-label (tr "labels.close")}
      [:*
       (case current-section
         :login
         [:> sa/page* {:title (tr "ds.auth.modal.title")
                       :subtitle (tr "ds.viewer.login.tagline")}
          [:> login-dialog* {:on-recovery-request set-section-recovery
                             :on-success-callback success-login
                             :origin :viewer}]
          ;; Design Studio (P3-B): Penpot's dialog had this link under its
          ;; form; without it the reset section below could not be reached.
          [:div
           [:> lk/link* {:action set-section-recovery
                         :class sa/link-class
                         :data-testid "forgot-password"}
            (tr "ds.signin.forgot")]]
          [:> sa/signup-row* {}]]

         :recovery-request
         [:> recovery-request-page* {:go-back-callback go-back-to-login
                                     :on-success-callback recovery-email-sent}]

         :recovery-email-sent
         [:> recovery-sent-page* {:email @user-email
                                  :on-back go-back-to-login}])]]]))
