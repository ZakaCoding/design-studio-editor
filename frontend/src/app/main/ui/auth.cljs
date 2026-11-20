;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns app.main.ui.auth
  (:require
   [app.common.data.macros :as dm]
   [app.main.data.auth :as da]
   [app.main.store :as st]
   [app.main.ui.auth.login :refer [login-page*]]
   [app.main.ui.auth.recovery :refer [recovery-page*]]
   [app.main.ui.auth.recovery-request :refer [recovery-request-page*]]
   [app.main.ui.auth.register :refer [register-page* register-success-page* register-validate-page* terms-service-privacy-policy*]]
   [app.main.ui.studio.auth :as sa]
   [app.util.dom :as dom]
   [app.util.i18n :as i18n :refer [tr]]
   [rumext.v2 :as mf]))

(mf/defc auth*
  [{:keys [route]}]
  (let [section
        (dm/get-in route [:data :name])

        is-register
        (or (= section :auth-register)
            (= section :auth-register-validate)
            (= section :register-validate-page)
            (= section :auth-register-success))
        params
        (:query-params route)

        error
        (:error params)]

    (mf/with-effect []
      (dom/set-html-title (tr "title.default")))

    (mf/with-effect [error]
      (when error
        (st/emit! (da/show-redirect-error error))))

    ;; Design Studio (WP5): the F1 · 08 shell replaces Penpot's logo and
    ;; illustration. With open sign-up the register routes leave for
    ;; /start/signup (ADR 0017); the shell does that.
    [:> sa/shell* {:section (name section)
                   :leave-to-signup is-register}
     [:*
      (case section
        :auth-register
        [:> register-page* {:params params}]

        :auth-register-success
        [:> register-success-page* {:params params}]

        :auth-register-validate
        [:> register-validate-page* {:params params}]

        :auth-login
        [:> login-page* {:params params}]

        :auth-recovery-request
        [:> recovery-request-page*]

        :auth-recovery
        [:> recovery-page* {:params params}])

      (when (= section :auth-register)
        [:> terms-service-privacy-policy*])]]))


(mf/defc auth-page*
  {::mf/lazy-load true}
  [props]
  [:> auth* props])
