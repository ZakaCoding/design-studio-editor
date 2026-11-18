;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns app.main.ui.auth.login
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.logging :as log]
   [app.common.schema :as sm]
   [app.config :as cf]
   [app.main.data.auth :as da]
   [app.main.data.notifications :as ntf]
   [app.main.repo :as rp]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.components.button-link :as bl]
   [app.main.ui.components.forms :as fm]
   [app.main.ui.components.link :as lk]
   [app.main.ui.ds.foundations.assets.raw-svg :refer [raw-svg*] :as raw-icons]
   [app.main.ui.ds.notifications.context-notification :refer [context-notification*]]
   [app.main.ui.studio.auth :as sa]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [app.util.storage :as s]
   [beicon.v2.core :as rx]
   [rumext.v2 :as mf]))

(def ^:const show-sso-login-buttons?
  (some (partial contains? cf/flags)
        [:login-with-google
         :login-with-github
         :login-with-gitlab
         :login-with-oidc]))

(mf/defc demo-warning*
  []
  [:> context-notification*
   {:level :warning}
   (tr "auth.demo-warning")])

(defn create-demo-profile
  []
  (st/emit! (da/create-demo-profile)))

(defn- store-login-redirect
  [callback-url]
  (binding [s/*sync* true]
    ;; Save the current login raw uri for later redirect user back to
    ;; the same page, we need it to be synchronous because the user is
    ;; going to be redirected instantly to the oidc provider uri
    (swap! s/session assoc :login-redirect (or callback-url (rt/get-current-href)))))

(defn- clear-login-redirect
  []
  (binding [s/*sync* true]
    (swap! s/session dissoc :login-redirect)))

(defn- login-with-sso
  [provider params]
  (let [params (assoc params :provider provider)]
    (st/emit! (da/login-with-sso params))))

(def ^:private schema:login-form
  [:map {:title "LoginForm"}
   [:email [::sm/email {:error/code "errors.invalid-email"}]]
   [:password {:optional true} [:string {:min 1}]]
   [:invitation-token {:optional true}
    [:string {:min 1}]]])

(mf/defc login-form*
  [{:keys [params handle-redirect on-success-callback on-recovery-request origin] :as props}]
  (let [initial (mf/with-memo [params] params)

        form    (fm/use-form :schema schema:login-form
                             :initial initial)

        error   (mf/use-state false)

        ;; Design Studio (P3-B): drawn on the button only, between the
        ;; submit and its answer.
        busy*   (mf/use-state false)

        show-password-field* (mf/use-state #(not (contains? cf/flags :login-with-custom-sso)))
        show-password-field? (deref show-password-field*)

        callback-url (:callback-url params)

        on-error
        (fn [cause]
          (reset! busy* false)
          (let [cause (ex-data cause)]
            (cond
              (and (= :restriction (:type cause))
                   (= :profile-blocked (:code cause)))
              (reset! error (tr "errors.profile-blocked"))

              (and (= :restriction (:type cause))
                   (= :ldap-not-initialized (:code cause)))
              (st/emit! (ntf/error (tr "errors.ldap-disabled")))

              (and (= :restriction (:type cause))
                   (= :admin-only-profile (:code cause)))
              (reset! error (tr "errors.profile-blocked"))

              (and (= :validation (:type cause))
                   (= :wrong-credentials (:code cause)))
              (reset! error (tr "errors.wrong-credentials"))

              (and (= :validation (:type cause))
                   (= :account-without-password (:code cause)))
              (reset! error (tr "errors.wrong-credentials"))

              :else
              (reset! error (tr "errors.generic")))))

        on-success
        (fn [data]
          (reset! busy* false)
          (when (fn? on-success-callback)
            (on-success-callback data)))

        on-submit
        (mf/use-fn
         (mf/deps show-password-field? params)
         (fn [form _event]
           (reset! error nil)

           (let [data (:clean-data @form)]
             (if show-password-field?
               (let [params (-> (merge params data)
                                (with-meta {:on-error on-error
                                            :on-success on-success}))]
                 (reset! busy* true)
                 (st/emit! (da/login params)))

               (let [params (merge params data)]
                 (->> (rp/cmd! :get-sso-provider {:email (:email params)})
                      (rx/map :id)
                      (rx/catch (fn [cause]
                                  (log/error :hint "error on retrieving sso provider"
                                             :cause cause)
                                  (rx/of nil)))
                      (rx/subs! (fn [sso-provider-id]
                                  (if sso-provider-id
                                    (let [params {:provider sso-provider-id}]
                                      (st/emit! (da/login-with-sso params)))
                                    (reset! show-password-field* true))))))))))

        on-submit-ldap
        (mf/use-fn
         (mf/deps form)
         (fn [event]
           (dom/prevent-default event)
           (dom/stop-propagation event)

           (reset! error nil)
           (let [params (:clean-data @form)
                 params (with-meta params
                          {:on-error on-error
                           :on-success on-success})]
             (st/emit! (da/login-with-ldap params)))))

        on-recovery-request
        (or on-recovery-request
            #(st/emit! (rt/nav :auth-recovery-request)))]


    (mf/with-effect [handle-redirect callback-url]
      (if (or handle-redirect callback-url)
        (store-login-redirect callback-url)
        (clear-login-redirect)))

    ;; Design Studio (WP5): same form, state and events; F1 · 08 markup.
    (let [password-login? (or (contains? cf/flags :login)
                              (contains? cf/flags :login-with-password))
          forgot-link     (when (and (not= origin :viewer) password-login?)
                            ;; `mf/html`: hiccup in a binding is not compiled by
                            ;; the component macro; as a raw vector React rendered
                            ;; its keyword and the sign-in page crashed (#31).
                            (mf/html
                             [:> lk/link* {:action on-recovery-request
                                           :class sa/link-class
                                           :data-testid "forgot-password"}
                              (tr "ds.signin.forgot")]))]
      [:*
       (when-let [message @error]
         [:> sa/alert* {} message])

       [:& fm/form {:on-submit on-submit
                    :class sa/form-class
                    :form form}

        [:> sa/field* {:field :email
                       :type "email"
                       :label (tr "ds.signin.email")
                       :placeholder (tr "ds.signin.email-placeholder")
                       :auto-complete "username"
                       :auto-focus (not (contains? cf/flags :login-with-custom-sso))}]

        (when show-password-field?
          [:> sa/field* {:field :password
                         :type "password"
                         :label (tr "ds.signin.password")
                         :auto-complete "current-password"
                         :auto-focus (contains? cf/flags :login-with-custom-sso)
                         :aside forgot-link}])

        (when-not show-password-field?
          forgot-link)

        (when password-login?
          [:> sa/submit* {:label (tr "ds.signin.submit")
                          :busy (deref busy*)
                          :data-testid "login-submit"}])

        (when (contains? cf/flags :login-with-ldap)
          [:> sa/submit* {:label (tr "auth.login-with-ldap-submit")
                          :on-click on-submit-ldap}])]])))

(defn raw-icon
  [id]
  (mf/html
   [:> raw-svg* {:id id :class (stl/css :sso-icon)}]))

(mf/defc sso-buttons*
  [{:keys [params] :as props}]
  (let [login-with-google
        (mf/use-fn
         (mf/deps params)
         #(login-with-sso "google" params))

        login-with-github
        (mf/use-fn
         (mf/deps params)
         #(login-with-sso "github" params))

        login-with-gitlab
        (mf/use-fn
         (mf/deps params)
         #(login-with-sso "gitlab" params))

        login-with-oidc
        (mf/use-fn
         (mf/deps params)
         #(login-with-sso "oidc" params))]

    [:div {:class (stl/css :sso-row)}
     (when (contains? cf/flags :login-with-google)
       [:> bl/button-link* {:on-click login-with-google
                            :icon (raw-icon raw-icons/brand-google)
                            :label (tr "auth.login-with-google-submit")
                            :class (stl/css :sso-btn)}])

     (when (contains? cf/flags :login-with-github)
       [:> bl/button-link* {:on-click login-with-github
                            :icon (raw-icon raw-icons/brand-github)
                            :label (tr "auth.login-with-github-submit")
                            :class (stl/css :sso-btn)}])

     (when (contains? cf/flags :login-with-gitlab)
       [:> bl/button-link* {:on-click login-with-gitlab
                            :icon (raw-icon raw-icons/brand-gitlab)
                            :label (tr "auth.login-with-gitlab-submit")
                            :class (stl/css :sso-btn)}])

     ;; Design Studio (WP5): OIDC is ZakaCoding ID. With open sign-up it is
     ;; the way in for members who joined before email + password.
     (when (contains? cf/flags :login-with-oidc)
       [:> sa/sso-button* {:on-click login-with-oidc
                           :label (if (sa/registration-open?)
                                    (tr "ds.signin.sso-existing")
                                    (tr "ds.auth.sso.continue"))}])]))

(mf/defc login-dialog*
  [{:keys [params] :as props}]
  ;; Design Studio (WP5): the form comes first and ZakaCoding ID below it.
  (let [form? (or (contains? cf/flags :login)
                  (contains? cf/flags :login-with-password)
                  (contains? cf/flags :login-with-ldap))]
    [:> sa/scope* {}
     (when form?
       [:> login-form* props])

     (when show-sso-login-buttons?
       [:*
        (when form?
          [:> sa/divider* {}])
        [:> sso-buttons* {:params params}]])]))

(mf/defc login-page*
  [{:keys [params]}]
  ;; Design Studio (WP5): "create an account" is a link to /start/signup,
  ;; because Penpot's register form is refused (ADR 0017).
  [:> sa/page* {:title (tr "ds.signin.title")
                :title-testid "login-title"
                :subtitle (tr "ds.signin.tagline")}

   (when (contains? cf/flags :demo-warning)
     [:> demo-warning*])

   [:> login-dialog* {:params params}]

   [:> sa/signup-row* {}]])
