;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns app.main.ui.auth.recovery
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.schema :as sm]
   [app.main.data.notifications :as ntf]
   [app.main.data.profile :as du]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.components.forms :as fm]
   [app.main.ui.studio.auth :as sa]
   [app.util.i18n :as i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def ^:private schema:recovery-form
  [:and
   [:map {:title "RecoveryForm"}
    [:token ::sm/text]
    [:password-1 ::sm/password]
    [:password-2 ::sm/password]]
   [:fn {:error/code "errors.password-invalid-confirmation"
         :error/field :password-2}
    (fn [{:keys [password-1 password-2]}]
      (= password-1 password-2))]])

(defn- on-error
  [form on-used-link error]
  (let [{:keys [type code] :as edata} (ex-data error)]
    (if (= [:validation :weak-password] [type code])
      (let [details (:details edata)
            options (when (seq details)
                      (mapv tr details))]
        (swap! form assoc-in [:extra-errors :password-1]
               {:message (tr "errors.weak-password")
                :options options}))

      ;; Design Studio (WP5): a reset link works once. Besides Penpot's
      ;; message the screen turns into F1 · 09 "Link sudah dipakai".
      (let [msg (tr "errors.invalid-recovery-token")]
        (st/emit! (ntf/error msg))
        (when (fn? on-used-link)
          (on-used-link))))))

(defn- on-success
  [_]
  (st/emit! (ntf/info (tr "auth.notifications.password-changed-successfully"))
            (rt/nav :auth-login)))

(defn- on-submit
  [on-used-link form _event]
  (let [mdata  {:on-error (partial on-error form on-used-link)
                :on-success on-success}
        params {:token (get-in @form [:clean-data :token])
                :password (get-in @form [:clean-data :password-2])}]
    (st/emit! (du/recover-profile (with-meta params mdata)))))

(mf/defc recovery-form*
  [{:keys [params on-used-link]}]
  (let [form (fm/use-form :schema schema:recovery-form
                          :initial params)

        handle-submit
        (mf/use-fn
         (mf/deps on-used-link)
         (fn [form event]
           (on-submit on-used-link form event)))]

    ;; Design Studio (WP5): same schema (password rules) and event.
    [:& fm/form {:on-submit handle-submit
                 :class sa/form-class
                 :form form}

     [:> sa/field* {:field :password-1
                    :type "password"
                    :label (tr "ds.recovery.new.password")
                    :auto-complete "new-password"
                    :auto-focus true}]

     [:> sa/field* {:field :password-2
                    :type "password"
                    :label (tr "ds.recovery.new.confirm")
                    :auto-complete "new-password"}]

     [:> sa/submit* {:label (tr "ds.recovery.new.submit")
                     :data-testid "recovery-submit"}]]))

;; --- Recovery Request Page

(mf/defc recovery-page*
  [{:keys [params]}]
  ;; Design Studio (WP5): says what this screen does (Penpot reuses "Forgot
  ;; password?" here), and a refused link becomes "Link sudah dipakai".
  (let [used*        (mf/use-state false)
        on-used-link (mf/use-fn #(reset! used* true))]
    (if (deref used*)
      [:> sa/used-link* {}]
      [:> sa/page* {:title (tr "ds.recovery.new.title")
                    :subtitle (tr "ds.recovery.new.subtitle")}
       [:> recovery-form* {:params params
                           :on-used-link on-used-link}]
       [:div
        [:a {:on-click #(st/emit! (rt/nav :auth-login))
             :class sa/link-class
             :data-testid "go-back-link"}
         (tr "ds.recovery.back")]]])))
