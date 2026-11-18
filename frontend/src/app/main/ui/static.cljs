;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns app.main.ui.static
  (:require-macros [app.main.style :as stl])
  (:require
   ["rxjs" :as rxjs]
   [app.common.data :as d]
   [app.common.exceptions :as ex]
   [app.common.uuid :as uuid]
   [app.main.data.auth :refer [is-authenticated?]]
   [app.main.data.common :as dcm]
   [app.main.data.nitrate :as dnt]
   [app.main.errors :as errors]
   [app.main.refs :as refs]
   [app.main.repo :as rp]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.auth.login :refer [login-dialog*]]
   [app.main.ui.auth.recovery-request :refer [recovery-request-page* recovery-sent-page*]]
   [app.main.ui.auth.register :as register]
   [app.main.ui.ds.foundations.assets.raw-svg :refer [raw-svg*]]
   [app.main.ui.ds.product.loader :refer [loader*]]
   [app.main.ui.studio.auth :as sa]
   [app.main.ui.studio.ds.button :as sbtn]
   [app.main.ui.studio.ds.logo :refer [logo* logo-mark*]]
   [app.main.ui.studio.error-page :refer [error-shell*]]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [app.util.timers :as tm]
   [app.util.webapi :as wapi]
   [beicon.v2.core :as rx]
   [cuerdas.core :as str]
   [rumext.v2 :as mf]))

;; FIXME: this is a workaround until we export this class on beicon library
(def TimeoutError rxjs/TimeoutError)

(mf/defc error-container*
  "Design Studio seam (STUDIO-SEAMS.md): the page is `studio.error-page/error-shell*`."
  [{:keys [children code primary-home? actions]}]
  (let [profile     (mf/deref refs/profile)
        profile-id  (:id profile)
        on-nav-root (mf/use-fn
                     (mf/deps profile-id profile)
                     (fn []
                       (if (and profile-id (some? (:default-team-id profile)))
                         (st/emit! (dcm/go-to-dashboard-recent
                                    :team-id (:default-team-id profile)))
                         (st/emit! (rt/nav-root)))))]
    [:> error-shell* {:code code
                      :on-home on-nav-root
                      :primary-home? primary-home?
                      :actions actions}
     children]))

(mf/defc invalid-token
  [{:keys [reason]}]
  ;; Map the specific failure reason to actionable copy. Falls back to
  ;; the generic invitation-invalid message when the reason is missing
  ;; or unknown so the UX never regresses for unhandled cases.
  ;;
  ;; The branches use `tr` with literal keys (instead of `(tr key-var)`)
  ;; so the i18n usage scanner can statically track every key.
  [:> error-container* {:primary-home? true}
   (case reason
     :email-mismatch
     [:*
      [:div {:class (stl/css :main-message)} (tr "errors.invite-email-mismatch")]]

     :token-expired
     [:*
      [:div {:class (stl/css :main-message)} (tr "errors.invite-expired")]]

     [:*
      [:div {:class (stl/css :main-message)} (tr "errors.invite-invalid")]
      [:div {:class (stl/css :desc-message)} (tr "errors.invite-invalid.info")]])])

(mf/defc login-modal*
  {::mf/private true}
  []
  (let [current-section  (mf/use-state :login)
        user-email       (mf/use-state "")
        register-token   (mf/use-state "")

        set-section
        (mf/use-fn
         (fn [event]
           (let [section (-> (dom/get-current-target event)
                             (dom/get-data "section")
                             (keyword))]
             (reset! current-section section))))

        set-section-recovery
        (mf/use-fn
         #(reset! current-section :recovery-request))

        set-section-login
        (mf/use-fn
         #(reset! current-section :login))

        success-login
        (mf/use-fn
         #(st/emit! (rt/reload true)))

        success-register
        (mf/use-fn
         (fn [data]
           (cond
             (:invitation-token data)
             (st/emit! (rt/nav :auth-verify-token {:token (:invitation-token data)}))

             (:is-active data)
             (st/emit! (rt/reload true))

             :else
             (do
               (reset! user-email (:email data))
               (reset! current-section :register-email-sent)))))

        register-email-sent
        (mf/use-fn
         (fn [data]
           (reset! user-email (if (string? data) data (:email data)))
           (reset! current-section :register-email-sent)))

        recovery-email-sent
        (mf/use-fn
         (fn [email]
           (reset! user-email email)
           (reset! current-section :recovery-email-sent)))

        on-nav-root
        (mf/use-fn #(st/emit! (rt/nav :auth-login {})))]

    ;; Design Studio (WP5): our dialog and lockup. "Create an account" is a
    ;; link to /start/signup (ADR 0017), so the register sections below are
    ;; no longer reachable from here; they stay for a small upstream diff.
    [:div {:class (stl/css :overlay)}
     [:> sa/modal* {:on-close on-nav-root
                    :close-label (tr "labels.close")}
      [:*
       (case @current-section
         :login
         [:> sa/page* {:title (tr "ds.auth.modal.title")
                       :subtitle (tr "ds.signin.modal.tagline")}
          [:> login-dialog* {:on-recovery-request set-section-recovery
                             :on-success-callback success-login
                             :handle-redirect true}]
          [:> sa/signup-row* {}]]

         :register
         [:*
          [:div {:class (stl/css :logo-title)} (tr "not-found.login.signup-free")]
          [:div {:class (stl/css :logo-subtitle)} (tr "not-found.login.start-using")]
          [:> register/register-methods* {:on-success-callback success-register :hide-separator true}]
          #_[:hr {:class (stl/css :separator)}]
          [:div {:class (stl/css :separator)}]
          [:div {:class (stl/css :change-section)}
           (tr "auth.already-have-account")
           " "
           [:a {:data-section "login"
                :on-click set-section} (tr "auth.login-here")]]
          [:div {:class (stl/css :links)}
           [:hr {:class (stl/css :separator)}]
           [:> register/terms-service-privacy-policy*]]]

         :register-validate
         [:div {:class (stl/css :form-container)}
          [:> register/register-form*
           {:params {:token @register-token}
            :on-success-callback register-email-sent}]
          [:div {:class (stl/css :links)}
           [:div {:class (stl/css :register)}
            [:a {:data-section "register"
                 :on-click set-section}
             (tr "labels.go-back")]]]]

         :register-email-sent
         [:div {:class (stl/css :form-container)}
          [:> register/register-success-page* {:params {:email @user-email :hide-logo true}}]]

         :recovery-request
         [:> recovery-request-page* {:go-back-callback set-section-login
                                     :on-success-callback recovery-email-sent}]

         :recovery-email-sent
         [:div {:class (stl/css :form-container)}
          [:> recovery-sent-page* {:email @user-email}]])]]]))

(mf/defc request-dialog*
  [{:keys [title content button-text on-button-click cancel-text on-close]}]
  (let [on-click (or on-button-click on-close)]
    ;; Design Studio (P3-A): the light DS dialog of the dashboard (card
    ;; radius 16, sentence-case title, soft and black pills) wherever it
    ;; appears: over the dashboard, the workspace and the viewer.
    [:div {:class (stl/css :overlay :overlay-request)}
     [:div {:class (stl/css :dialog)
            :role "dialog"
            :aria-modal "true"
            :data-testid "ds-request-dialog"}
      [:button {:class (stl/css :modal-close)
                :type "button"
                :aria-label (tr "labels.close")
                :on-click on-close}
       [:svg {:width 16 :height 16 :view-box "0 0 24 24" :aria-hidden true}
        [:path {:d "M6 6l12 12M18 6L6 18"}]]]
      [:h2 {:class (stl/css :dialog-title)} title]
      (for [[index content] (d/enumerate content)]
        [:p {:key index :class (stl/css :dialog-text)} content])
      [:div {:class (stl/css :sign-info)}
       (when cancel-text
         [:button {:class (stl/css :cancel-button)
                   :type "button"
                   :on-click on-close}
          cancel-text])
       [:button {:class (stl/css :accept-button)
                 :type "button"
                 :data-testid "ds-request-accept"
                 :on-click on-click}
        button-text]]]]))

(mf/defc request-access*
  [{:keys [file-id team-id is-default is-workspace profile]}]
  (let [requested*  (mf/use-state {:sent false :already-requested false})
        requested   (deref requested*)

        on-close
        (mf/use-fn
         (mf/deps profile)
         (fn []
           (let [team-id (:default-team-id profile)]
             (st/emit! (dcm/go-to-dashboard-recent :team-id team-id)))))

        on-success
        (mf/use-fn
         #(reset! requested* {:sent true :already-requested false}))

        on-error
        (mf/use-fn
         #(reset! requested* {:sent true :already-requested true}))

        on-request-access
        (mf/use-fn
         (mf/deps file-id team-id is-workspace)
         (fn []
           (let [params (if (some? file-id)
                          {:file-id file-id
                           :is-viewer (not is-workspace)}
                          {:team-id team-id})
                 mdata  {:on-success on-success
                         :on-error on-error}]
             (st/emit! (dcm/create-team-access-request
                        (with-meta params mdata))))))]

    (cond
      is-default
      [:> request-dialog* {:title (tr "not-found.no-permission.project")
                           :button-text (tr "not-found.no-permission.go-dashboard")
                           :on-close on-close}]

      (and (some? file-id) (:already-requested requested))
      [:> request-dialog* {:title (tr "not-found.no-permission.already-requested.file")
                           :content [(tr "not-found.no-permission.already-requested.or-others.file")]
                           :button-text (tr "not-found.no-permission.go-dashboard")
                           :on-close on-close}]

      (:already-requested requested)
      [:> request-dialog* {:title (tr "not-found.no-permission.already-requested.project")
                           :content [(tr "not-found.no-permission.already-requested.or-others.project")]
                           :button-text (tr "not-found.no-permission.go-dashboard")
                           :on-close on-close}]

      (:sent requested)
      [:> request-dialog* {:title (tr "not-found.no-permission.done.success")
                           :content [(tr "not-found.no-permission.done.remember")]
                           :button-text (tr "not-found.no-permission.go-dashboard")
                           :on-close on-close}]

      (some? file-id)
      [:> request-dialog* {:title (tr "not-found.no-permission.file")
                           :content [(tr "not-found.no-permission.you-can-ask.file")
                                     (tr "not-found.no-permission.if-approves")]
                           :button-text (tr "ds.access.ask")
                           :on-button-click on-request-access
                           :cancel-text (tr "not-found.no-permission.go-dashboard")
                           :on-close on-close}]

      (some? team-id)
      [:> request-dialog* {:title (tr "not-found.no-permission.project")
                           :content [(tr "not-found.no-permission.you-can-ask.project")
                                     (tr "not-found.no-permission.if-approves")]
                           :button-text (tr "ds.access.ask")
                           :on-button-click on-request-access
                           :cancel-text (tr "not-found.no-permission.go-dashboard")
                           :on-close on-close}])))

(mf/defc not-found*
  []
  [:> error-container* {:code "404" :primary-home? true}
   [:div {:class (stl/css :main-message)} (tr "ds.error.404.title")]
   [:div {:class (stl/css :desc-message)} (tr "ds.error.404.text")]])

(mf/defc bad-gateway*
  []
  (let [handle-retry
        (mf/use-fn
         (fn [] (st/emit! (rt/assign-exception nil))))]
    [:> error-container* {:code "502"
                          :actions (mf/html
                                    [:> sbtn/button* {:variant "primary" :on-click handle-retry}
                                     (tr "labels.retry")])}
     [:div {:class (stl/css :main-message)} (tr "ds.error.unavailable.title")]
     [:div {:class (stl/css :desc-message)} (tr "ds.error.unavailable.text")]]))

(mf/defc service-unavailable*
  []
  (let [on-click (mf/use-fn #(st/emit! (rt/assign-exception nil)))]
    [:> error-container* {:code "503"
                          :actions (mf/html
                                    [:> sbtn/button* {:variant "primary" :on-click on-click}
                                     (tr "labels.retry")])}
     [:div {:class (stl/css :main-message)} (tr "ds.error.unavailable.title")]
     [:div {:class (stl/css :desc-message)} (tr "ds.error.unavailable.text")]]))

(mf/defc nitrate-unavailable*
  []
  [:section {:class (stl/css :nitrate-unavailable-layout)}
   [:div {:class (stl/css :nitrate-unavailable-content)}
    [:> raw-svg* {:id "logo-nitrate-unavailable" :class (stl/css :nitrate-unavailable-logo)}]
    [:p {:class (stl/css :nitrate-unavailable-message)}
     (tr "labels.nitrate-unavailable.main-message")]]
   [:p {:class (stl/css :nitrate-unavailable-footer)}
    (tr "labels.copyright-period")]])

(mf/defc nitrate-not-configured-page*
  []
  [:section {:class (stl/css :nitrate-unavailable-layout)}
   [:div {:class (stl/css :nitrate-unavailable-content)}
    [:> raw-svg* {:id "logo-nitrate-unavailable" :class (stl/css :nitrate-unavailable-logo)}]
    [:div {:class (stl/css :nitrate-unavailable-message)}
     (tr "labels.nitrate-not-configured.main-message")]
    [:div {:class (stl/css :nitrate-not-configured-message)}
     (tr "labels.nitrate-not-configured.desc-message")]
    [:div {:class (stl/css :nitrate-not-configured-message)}
     [:span
      (tr "labels.nitrate-not-configured.learn-more")
      " "
      [:a {:href "https://help.penpot.app/technical-guide/getting-started/docker/#update-penpot"
           :target "_blank"
           :rel "noopener noreferrer"}
       (tr "labels.nitrate-not-configured.technical-guide")]]]]

   [:p {:class (stl/css :nitrate-unavailable-footer)}
    (tr "labels.copyright-period")]])

(mf/defc webgl-context-lost*
  []
  (let [on-reload (mf/use-fn #(js/location.reload))]
    [:> error-container* {:actions (mf/html
                                    [:> sbtn/button* {:variant "primary" :on-click on-reload}
                                     (tr "labels.reload-page")])}
     [:div {:class (stl/css :main-message)} (tr "errors.webgl-context-lost.main-message")]
     [:div {:class (stl/css :desc-message)} (tr "errors.webgl-context-lost.desc-message")]]))

(mf/defc internal-error*
  [{:keys [on-reset report] :as props}]
  (let [report-uri (mf/use-ref nil)
        on-reset   (or on-reset #(st/emit! (rt/assign-exception nil)))

        support-contact-click
        (mf/use-fn
         (mf/deps on-reset report)
         (fn []
           (tm/schedule on-reset)
           (let [error-report-id (uuid/next)
                 error-href (rt/get-current-href)]
             (set! errors/last-report {:id error-report-id :content report})
             (st/emit!
              (rt/nav :settings-feedback {:type "issue"
                                          :error-report-id error-report-id
                                          :error-href error-href})))))

        on-download
        (mf/use-fn
         (fn [event]
           (dom/prevent-default event)
           (when-let [uri (mf/ref-val report-uri)]
             (dom/trigger-download-uri "report" "text/plain" uri))))]

    (mf/with-effect [report]
      (when (some? report)
        (set! errors/last-report report)
        (let [report (wapi/create-blob report "text/plain")
              uri    (wapi/create-uri report)]
          (mf/set-ref-val! report-uri uri)
          (fn []
            (wapi/revoke-uri uri)))))

    [:> error-container* {:code "500"
                          :actions (mf/html
                                    [:*
                                     [:> sbtn/button* {:variant "secondary"
                                                       :type "button"
                                                       :class (stl/css :support-btn)
                                                       :on-click support-contact-click}
                                      (tr "labels.contact-support")]
                                     [:> sbtn/button* {:variant "primary"
                                                       :type "button"
                                                       :class (stl/css :retry-btn)
                                                       :on-click on-reset}
                                      (tr "labels.retry")]])}
     [:div {:class (stl/css :main-message)} (tr "ds.error.500.title")]

     [:div {:class (stl/css :desc-message)}
      [:p {:class (stl/css :desc-text)} (tr "ds.error.500.text")]]

     (when (some? report)
       [:a {:class (stl/css :download-link) :on-click on-download} (tr "labels.download" "report.txt")])]))

(defn- load-info
  "Load exception page info"
  [path-params]
  (let [default {:loaded true}
        stream  (cond
                  (:file-id path-params)
                  (->> (rp/cmd! :get-file-info {:id (:file-id path-params)})
                       (rx/map (fn [info]
                                 {:loaded true
                                  :file-id (:id info)})))

                  (:team-id path-params)
                  (->> (rp/cmd! :get-team-info {:id (:team-id path-params)})
                       (rx/map (fn [info]
                                 {:loaded true
                                  :team-id (:id info)
                                  :team-default (:is-default info)})))

                  :else
                  (rx/of default))]

    (->> stream
         (rx/timeout 3000)
         (rx/catch (fn [cause]
                     (if (instance? TimeoutError cause)
                       (rx/of default)
                       (rx/throw cause)))))))

;; Design Studio (P3-A): what stands behind the sign-in and the
;; request-access dialogs is a light outline of the page the link points
;; to (editor, viewer or dashboard) with our mark. Upstream drew Penpot's
;; dark sidebar, its viewer header and a file named "Penpot file" there,
;; for a member who cannot open the page anyway.
(mf/defc context-wrapper*
  [{:keys [is-workspace is-dashboard is-viewer children]}]
  [:*
   (cond
     is-workspace
     [:div {:class (stl/css :workspace)
            :data-testid "ds-outline-workspace"}
      [:div {:class (stl/css :workspace-left)}
       [:> logo-mark* {:size 28}]
       [:div {:class (stl/css :outline-lines) :aria-hidden true}
        [:div {:class (stl/css :outline-line-short)}]
        [:div {:class (stl/css :outline-line)}]]]
      [:div {:class (stl/css :workspace-right)}]]

     is-viewer
     [:div {:class (stl/css :viewer)
            :data-testid "ds-outline-viewer"}
      [:div {:class (stl/css :viewer-island)}
       [:> logo-mark* {:size 28}]
       [:div {:class (stl/css :outline-lines) :aria-hidden true}
        [:div {:class (stl/css :outline-line-short)}]
        [:div {:class (stl/css :outline-line)}]]]]

     is-dashboard
     [:div {:class (stl/css :dashboard)
            :data-testid "ds-outline-dashboard"}
      [:div {:class (stl/css :dashboard-sidebar)}
       [:> logo* {:size "sm"}]
       [:div {:class (stl/css :outline-lines :outline-nav) :aria-hidden true}
        [:div {:class (stl/css :outline-line)}]
        [:div {:class (stl/css :outline-line-short)}]
        [:div {:class (stl/css :outline-line)}]]]]

     ;; Signed out, the route has no page yet: a plain light ground (it
     ;; was the dark page background).
     :else
     [:div {:class (stl/css :outline-plain)}])

   children])

(mf/defc sso-error-section*
  "Shown in place of the dashboard/workspace (same static skeleton and
  `request-dialog*` used by the no-permission dialogs) when the organization
  SSO exchange with the identity provider fails."
  {::mf/private true}
  [{:keys [organization-id team-id profile is-workspace is-dashboard organization-name]}]
  (let [clean-url
        (mf/with-memo []
          (-> (rt/get-current-href)
              (dom/remove-query-param :sso-error)
              (dom/remove-query-param :organization-id)))

        _ (mf/with-effect []
            ;; Consume the marker once: scrub it from the URL bar so a
            ;; browser refresh doesn't keep re-showing this dialog.
            (dom/replace-history-state! clean-url))

        on-close
        (mf/use-fn
         (mf/deps profile)
         (fn []
           ;; Land on the user's own default team
           (st/emit! (rt/assign-exception nil)
                     (dcm/go-to-dashboard-recent :team-id (:default-team-id profile)))))

        on-retry
        (mf/use-fn
         (mf/deps organization-id team-id clean-url)
         (fn []
           (st/emit! (rt/assign-exception nil))
           (if (or team-id organization-id)
             ;; Retry with team-id and/or organization-id to trigger SSO check
             (st/emit! (dnt/retry-organization-sso {:team-id team-id
                                                    :organization-id organization-id
                                                    :dest-url clean-url}))
             ;; Fallback: just navigate to clean URL
             (st/emit! (rt/nav-raw :uri clean-url)))))]

    [:> context-wrapper* {:is-dashboard (or is-dashboard (not is-workspace))
                          :is-workspace is-workspace
                          :profile profile}
     [:> request-dialog* {:title (tr "labels.sso-error.title", organization-name)
                          :content [(tr "labels.sso-error.desc-message")]
                          :button-text (tr "labels.sso-error.retry")
                          :on-button-click on-retry
                          :cancel-text (tr "not-found.no-permission.go-dashboard")
                          :on-close on-close}]]))

(mf/defc exception-section*
  {::mf/private true}
  [{:keys [data] :as props}]
  (let [type        (get data :type)
        cause       (get data ::errors/instance)
        environment (errors/environment-error? cause)

        report      (mf/with-memo [cause]
                      (when (ex/exception? cause)
                        (errors/generate-report cause {:format (if environment :compact :full)})))

        props       (mf/spread-props props {:report report})]

    (mf/with-effect [report type cause]
      (when (and (ex/exception? cause)
                 (not (contains? #{:not-found :authentication} type)))
        ;; Environment pages are audit-only: they use the canonical
        ;; `handled-exception` event instead of `exception-page`.
        (errors/submit-report :event-name (if environment "handled-exception" "exception-page")
                              :report report
                              :hint (ex/get-hint cause)
                              :cause cause)))

    (case type
      :not-found
      [:> not-found* {}]

      :authentication
      [:> not-found* {}]

      :bad-gateway
      [:> bad-gateway* props]

      :service-unavailable
      [:> service-unavailable*]

      :nitrate-unavailable
      [:> nitrate-unavailable*]

      :nitrate-not-configured
      [:> nitrate-not-configured-page*]

      :sso-error
      [:> sso-error-section* {:organization-id (get data :organization-id)
                              :organization-name (get data :organization-name)
                              :team-id (get data :team-id)
                              :profile (mf/deref refs/profile)
                              :is-workspace (get data :is-workspace false)
                              :is-dashboard (get data :is-dashboard true)}]

      [:> internal-error* props])))

(mf/defc exception-page*
  [{:keys [data route] :as props}]

  (let [type        (:type data)
        path        (:path route)
        params      (:query-params route)

        workspace?  (str/includes? path "workspace")
        dashboard?  (str/includes? path "dashboard")
        view?       (str/includes? path "view")

        ;; We store the request access info int this state
        info*       (mf/use-state nil)
        info        (deref info*)

        profile     (mf/deref refs/profile)

        auth-error? (= type :authentication)
        not-found?  (= type :not-found)

        authenticated? (is-authenticated? profile)

        ;; Keeps whether the user was authenticated when this component first mounted.
        initial-authenticated? (mf/with-memo [] authenticated?)

        request-access?
        (and
         (or workspace? dashboard? view?)
         (or (some? (:file-id info))
             (some? (:team-id info))))]

    (mf/with-effect [params info]
      (when-not (:loaded info)
        (->> (load-info params)
             (rx/subs! (partial reset! info*)
                       (partial reset! info* {:loaded true})))))


    (if (or auth-error? not-found?)
      (cond
        (not authenticated?)
        [:> context-wrapper*
         {:is-workspace workspace?
          :is-dashboard dashboard?
          :is-viewer view?
          :profile profile}
         [:> login-modal* {}]]

        ;; The user was not authenticated when exception-page first
        ;; mounted, but they have just logged in via the login modal.
        ;; Show a loading indicator to prevent briefly flashing the
        ;; "no permission" dialog.
        (not initial-authenticated?)
        [:> loader* {:title (tr "labels.loading") :overlay true}]

        :else
        (when (get info :loaded false)
          (if request-access?
            [:> context-wrapper* {:is-workspace workspace?
                                  :is-dashboard dashboard?
                                  :is-viewer view?
                                  :profile profile}
             [:> request-access* {:file-id (:file-id info)
                                  :team-id  (:team-id info)
                                  :is-default (:team-default info)
                                  :profile profile
                                  :is-workspace workspace?}]]
            [:> exception-section* props])))

      [:> exception-section* props])))
