;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.first-steps
  "First steps after the email-first sign-up: the member opened the link in
  their mail, so now they choose a password; once that step closes, one card
  says what to do next. The card shows once per browser."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.main.data.common :as dcm]
   [app.main.data.dashboard :as dd]
   [app.main.data.event :as-alias ev]
   [app.main.store :as st]
   [app.main.ui.studio.shell-client :as shell]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def ^:private studio-uri "/studio/")
(def ^:private plans-uri "/studio/craft/plans")

;; --- Creating the first file

(defn- default-project
  "Where the first design goes: the drafts of the team on screen, else the
  member's own drafts."
  [state]
  (let [profile (:profile state)
        team-id (:current-team-id state)
        drafts  (when (some? team-id)
                  (d/seek #(and (:is-default %) (= team-id (:team-id %)))
                          (vals (:projects state))))]
    (if (some? drafts)
      {:project-id (:id drafts) :team-id team-id}
      {:project-id (:default-project-id profile)
       :team-id    (:default-team-id profile)})))

(defn- create-first-file!
  "Create a file with Penpot's own event and open it in the editor."
  []
  (let [{:keys [project-id team-id]} (default-project @st/state)]
    (when (and (uuid? project-id) (uuid? team-id))
      (let [on-success
            (fn [{:keys [id data]}]
              (st/emit! (dcm/go-to-workspace :team-id team-id
                                             :file-id id
                                             :page-id (get-in data [:pages 0]))))]
        (st/emit! (-> (dd/create-file (with-meta {:project-id project-id}
                                        {:on-success on-success}))
                      (with-meta {::ev/origin "studio:first-steps"})))))))

;; --- Password

(mf/defc password-dialog*
  {::mf/private true}
  [{:keys [on-done]}]
  (let [value*   (mf/use-state "")
        ;; nil, :weak, :failed or :exists
        problem* (mf/use-state nil)
        problem  (deref problem*)
        busy*    (mf/use-state false)
        exists?  (= problem :exists)

        on-change
        (mf/use-fn #(reset! value* (.. % -target -value)))

        on-submit
        (mf/use-fn
         (mf/deps (deref value*) on-done)
         (fn [event]
           (.preventDefault event)
           (reset! busy* true)
           (reset! problem* nil)
           (-> (shell/save-password (deref value*))
               (.then (fn [status]
                        (reset! busy* false)
                        (cond
                          (<= 200 status 299) (on-done)
                          (= status 409)      (reset! problem* :exists)
                          (= status 400)      (reset! problem* :weak)
                          :else               (reset! problem* :failed)))))))]

    [:div {:class (stl/css :global/ds-pass-veil :veil)
           :role "dialog"
           :aria-modal "true"
           :aria-label (tr "ds.first-steps.password.title")}
     [:form {:class (stl/css :global/ds-pass :card)
             :on-submit on-submit}
      [:h2 (tr "ds.first-steps.password.title")]
      [:p (tr "ds.first-steps.password.lead")]
      [:label {:for "ds-pass-input"} (tr "ds.first-steps.password.label")]
      [:input {:type "password"
               :id "ds-pass-input"
               :required true
               :min-length 8
               :max-length 256
               :auto-complete "new-password"
               :auto-focus true
               :value (deref value*)
               :on-change on-change}]
      [:p {:class (stl/css :rules)} (tr "ds.first-steps.password.rules")]
      [:p {:class (stl/css :global/ds-pass-status :status)
           :role "status"}
       (case problem
         :weak   (tr "ds.first-steps.password.weak")
         :failed (tr "ds.first-steps.password.failed")
         :exists (tr "ds.first-steps.password.exists")
         nil)]
      [:div {:class (stl/css :actions)}
       [:button {:type "button"
                 :class (stl/css :global/ds-pass-later :secondary)
                 :on-click on-done}
        (if exists?
          (tr "ds.first-steps.password.ok")
          (tr "ds.first-steps.password.later"))]
       (when-not exists?
         [:button {:type "submit"
                   :class (stl/css :global/ds-pass-save :primary)
                   :disabled (deref busy*)}
          (tr "ds.first-steps.password.save")])]]]))

;; --- Welcome

(mf/defc welcome-card*
  {::mf/private true}
  [{:keys [on-close]}]
  (let [on-create
        (mf/use-fn
         (mf/deps on-close)
         (fn []
           (on-close)
           (create-first-file!)))

        on-veil-click
        (mf/use-fn
         (mf/deps on-close)
         (fn [event]
           (when (identical? (.-target event) (.-currentTarget event))
             (on-close))))]

    (mf/with-effect [on-close]
      (let [on-key-down (fn [event]
                          (when (= "Escape" (.-key event))
                            (on-close)))]
        (.addEventListener js/document "keydown" on-key-down)
        (fn []
          (.removeEventListener js/document "keydown" on-key-down))))

    [:div {:class (stl/css :global/ds-welcome-veil :veil)
           :role "dialog"
           :aria-modal "true"
           :aria-labelledby "ds-welcome-title"
           :on-click on-veil-click}
     [:div {:class (stl/css :global/ds-welcome :card)}
      [:h2 {:id "ds-welcome-title"} (tr "ds.first-steps.welcome.title")]
      [:p (tr "ds.first-steps.welcome.lead")]
      [:p {:class (stl/css :craft)}
       (tr "ds.first-steps.welcome.craft")
       " "
       [:a {:href plans-uri} (tr "ds.first-steps.welcome.plans")]]
      [:div {:class (stl/css :actions :actions-wrap)}
       [:a {:class (stl/css :secondary)
            :href studio-uri}
        (tr "ds.first-steps.welcome.ai")]
       [:button {:type "button"
                 :class (stl/css :global/ds-welcome-create :primary)
                 :auto-focus true
                 :on-click on-create}
        (tr "ds.first-steps.welcome.create")]]]]))

;; --- Flow

(mf/defc first-steps*
  "Mounted for a signed-in member on every page. `section` is the route
  name: the page that opens the mail link has no session yet, so the
  question is asked again as the member moves on, until Studio answered."
  [{:keys [section]}]
  (let [;; nil, :password or :welcome
        stage*  (mf/use-state nil)
        stage   (deref stage*)
        asked   (mf/use-ref false)
        asking  (mf/use-ref false)

        on-password-done
        (mf/use-fn
         (fn []
           ;; Shown once per browser; nothing here is sent anywhere.
           (if (some? (shell/store-get :local "welcomed"))
             (reset! stage* nil)
             (do
               (shell/store-set! :local "welcomed" true)
               (reset! stage* :welcome)))))

        on-welcome-close
        (mf/use-fn #(reset! stage* nil))]

    (mf/with-effect [section]
      (when (and (some? section)
                 (not= section :auth-verify-token)
                 (not (mf/ref-val asked))
                 (not (mf/ref-val asking)))
        (mf/set-ref-val! asking true)
        (-> (shell/fetch-state)
            (.then
             (fn [state]
               (cond
                 ;; Studio can't say yet: ask again on the next page.
                 (nil? state)
                 nil

                 ;; With sign-up off, nobody has a password to choose.
                 (not (:open state))
                 (mf/set-ref-val! asked true)

                 :else
                 (-> (shell/fetch-password-needed)
                     (.then
                      (fn [{:keys [status needed]}]
                        ;; No session yet, or the server can't say: ask
                        ;; again on the next page.
                        (when-not (or (= status 0) (= status 401) (>= status 500))
                          (mf/set-ref-val! asked true)
                          (when needed
                            (reset! stage* :password)))))))))
            (.then (fn [_] (mf/set-ref-val! asking false))))))

    (case stage
      :password [:> password-dialog* {:on-done on-password-done}]
      :welcome  [:> welcome-card* {:on-close on-welcome-close}]
      nil)))
