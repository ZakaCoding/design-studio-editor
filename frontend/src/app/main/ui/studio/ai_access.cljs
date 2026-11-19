;; Design Studio (F1 · 10 "Akses AI"): the content of Settings > AI access.
;; Not connected: the two-step Connect flow of the dashboard (F4-A). Connected:
;; the switch card, the connected apps, the Craft credits and the privacy note
;; of the board. Unavailable: a link to Studio. State comes from
;; `GET /studio/api/editor-shell/state` (shell-state); only booleans, the app
;; names and dates, and the `member` summary are read. No token, key or link
;; is ever shown or fetched here.

(ns app.main.ui.studio.ai-access
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.main.broadcast :as mbc]
   [app.main.data.modal :as modal]
   [app.main.data.profile :as du]
   [app.main.refs :as refs]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.ds.foundations.assets.icon :as i]
   [app.main.ui.studio.dashboard.connect :as connect]
   [app.main.ui.studio.dashboard.connect-card :as cc]
   [app.main.ui.studio.dashboard.rules :as rules]
   [app.main.ui.studio.ds :as sds]
   [app.main.ui.studio.settings-shell.account :as acc]
   [app.main.ui.studio.settings-shell.page :refer [card* row*]]
   [app.main.ui.studio.shell-state :as ss]
   [app.util.clipboard :as clipboard]
   [app.util.i18n :as i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def manage-uri
  "Studio: pause or change an existing connection, see why AI access is
  unavailable. Starting a connection does not go there."
  "/studio/")

(defn access-status
  "Which of the four states the card shows, from the shell state map:
  :loading (nil), :unavailable (the server could not answer, or this account has
  no AI access), :connected or :not-connected."
  [state]
  (cond
    (nil? state)                :loading
    (not (true? (:aiAccess state))) :unavailable
    (true? (:connected state))  :connected
    :else                       :not-connected))

(defn starts-connect-flow?
  "True when the card hosts the Connect flow instead of linking to Studio."
  [status]
  (= :not-connected status))

(def ^:private status-copy
  {:connected     {:tone "success" :badge "ds.ai-access.connected"
                   :text "ds.ai-access.connected-text" :cta "ds.ai-access.manage"}
   :unavailable   {:tone "neutral" :badge "ds.ai-access.unavailable"
                   :text "ds.ai-access.unavailable-text" :cta "ds.ai-access.manage"}})

;; --- Pure helpers

(defn app-initial
  "The letter in an app's avatar."
  [name]
  (let [name (some-> name str .trim)]
    (if (seq name) (.toUpperCase (subs name 0 1)) "?")))

(defn app-tone
  "Avatar colour class by app name: the two assistants the board names get
  their own, anything else the Craft indigo."
  [name]
  (let [name (some-> name str .toLowerCase)]
    (cond
      (= name "claude") "claude"
      (= name "codex")  "codex"
      :else             "other")))

(defn credits-ratio
  "0..1 for the bar, or nil without numbers."
  [{:keys [left total]}]
  (when (and (number? left) (number? total) (pos? total))
    (-> (/ left total) (max 0) (min 1))))

(defn- locale-tag [] (if (= "id" (deref i18n/locale)) "id-ID" "en-US"))

(defn- parse-date
  [value]
  (when (or (string? value) (number? value))
    (let [date (js/Date. value)]
      (when-not (js/isNaN (.getTime date)) date))))

(defn- short-date
  "\"9 Nov\". Through Intl.DateTimeFormat: inside the editor
  Date.prototype.toLocaleDateString is replaced by toDateString upstream,
  which printed \"Mon Nov 09 2026\"."
  [value]
  (when-let [date (parse-date value)]
    (.format (js/Intl.DateTimeFormat. (locale-tag) #js {:day "numeric" :month "short"}) date)))

(defn- ago
  [value]
  (when-let [date (parse-date value)]
    (let [secs (js/Math.round (/ (- (.getTime date) (js/Date.now)) 1000))
          fmt  (js/Intl.RelativeTimeFormat. (locale-tag) #js {:numeric "auto"})
          abs  (js/Math.abs secs)]
      (cond
        (< abs 60)     (.format fmt secs "second")
        (< abs 3600)   (.format fmt (js/Math.round (/ secs 60)) "minute")
        (< abs 86400)  (.format fmt (js/Math.round (/ secs 3600)) "hour")
        :else          (.format fmt (js/Math.round (/ secs 86400)) "day")))))

(defn- plan-name
  [plan]
  (cond
    (= plan "monthly")  "Monthly"
    (= plan "lifetime") "Lifetime"
    :else nil))

;; --- Parts

(mf/defc sparkle-icon*
  {::mf/private true}
  [{:keys [class]}]
  [:> sds/icon* {:class class :size "l" :view-box "0 0 24 24"}
   [:path {:d "M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z"}]
   [:path {:d "M19 16v4M17 18h4"}]])

(mf/defc lock-icon*
  {::mf/private true}
  [{:keys [class]}]
  [:> sds/icon* {:class class :size "m" :view-box "0 0 24 24"}
   [:rect {:x 5 :y 11 :width 14 :height 9 :rx 2}]
   [:path {:d "M8 11V8a4 4 0 0 1 8 0v3"}]])

(mf/defc switch-card*
  "P3D: one 64 px card, the title and one line of state left, the switch
  right; what turning it off does is the switch's tooltip (and the
  confirm dialog says it again)."
  {::mf/private true}
  [{:keys [on? busy? problem on-toggle]}]
  [:section {:class (stl/css-case :switch-card true :switch-card-off (not on?))
             :data-testid "ds-ai-access"
             :data-status (if on? "connected" "paused")}
   [:div {:class (stl/css :switch-text)}
    [:h2 {:class (stl/css :switch-title)} (tr "ds.ai.switch.title")]
    [:p {:class (stl/css :switch-state)}
     (if on? (tr "ds.ai.switch.on") (tr "ds.ai.switch.off"))]
    (when (some? problem)
      [:p {:class (stl/css :problem) :role "alert"}
       (tr "ds.connect.error.failed")])]
   [:button {:class (stl/css-case :switch true :switch-on on?)
             :type "button"
             :role "switch"
             :aria-checked (if on? "true" "false")
             :aria-label (tr "ds.ai.switch.title")
             :title (if on? (tr "ds.ai.switch.on-note") (tr "ds.ai.switch.off-note"))
             :disabled busy?
             :data-testid "ds-ai-switch"
             :on-click on-toggle}
    [:span {:class (stl/css :switch-knob)}]]])

(mf/defc app-row*
  {::mf/private true}
  [{:keys [app on-revoke]}]
  (let [label    (or (:name app) (tr "ds.ai.apps.generic"))
        since    (short-date (:connectedAt app))
        used     (ago (:lastUsedAt app))
        access   (:access app)
        info     (cond
                   (and since used) (tr "ds.ai.apps.meta" since used)
                   since            (tr "ds.ai.apps.meta-since" since)
                   :else            (tr "ds.ai.apps.generic-meta"))]
    [:li {:class (stl/css :app) :data-testid "ds-ai-app"}
     [:span {:class (stl/css-case :avatar true
                                  :avatar-claude (= "claude" (app-tone (:name app)))
                                  :avatar-codex (= "codex" (app-tone (:name app))))
             :aria-hidden "true"}
      (app-initial label)]
     [:div {:class (stl/css :app-text)}
      [:p {:class (stl/css :app-name)} label]
      [:p {:class (stl/css :app-meta)} info]]
     (when (some? access)
       [:span {:class (stl/css :access)}
        (if (= access "edit") (tr "ds.ai.apps.edit") (tr "ds.ai.apps.view"))])
     [:button {:class (stl/css :revoke)
               :type "button"
               :data-testid "ds-ai-revoke"
               :on-click on-revoke}
      (tr "ds.ai.apps.revoke")]]))

(mf/defc add-app*
  {::mf/private true}
  []
  (let [open*   (mf/use-state false)
        copied* (mf/use-state false)
        uri     (mf/with-memo [] (rules/assistant-uri (.-origin js/location)))
        on-open (mf/use-fn #(swap! open* not))
        on-copy (mf/use-fn
                 (mf/deps uri)
                 (fn [_]
                   (-> (clipboard/to-clipboard uri)
                       (.then (fn []
                                (reset! copied* true)
                                (js/setTimeout #(reset! copied* false) 2400)))
                       (.catch (constantly nil)))))]
    [:div {:class (stl/css :add)}
     [:button {:class (stl/css :add-button)
               :type "button"
               :aria-expanded (if @open* "true" "false")
               :data-testid "ds-ai-add"
               :on-click on-open}
      [:> sds/icon* {:size "m" :view-box "0 0 24 24"}
       [:path {:d "M12 5v14M5 12h14"}]]
      (tr "ds.ai.apps.add")]
     (when @open*
       [:div {:class (stl/css :add-panel)}
        [:p {:class (stl/css :add-text)} (tr "ds.ai.apps.add-text")]
        [:div {:class (stl/css :address)}
         [:code {:class (stl/css :address-value)} uri]
         [:> sds/button* {:variant "secondary"
                          :size "sm"
                          :icon-id i/clipboard
                          :on-click on-copy}
          (if @copied* (tr "ds.connect.copied") (tr "ds.connect.copy"))]]])]))

(mf/defc craft-card*
  "P3D: Craft in one row (the Account panel already shows the meter)."
  {::mf/private true}
  [{:keys [member]}]
  (let [credits (:credits member)
        ratio   (credits-ratio credits)
        plan    (plan-name (:plan member))
        renews  (short-date (:renewsAt member))
        on-plans (mf/use-fn #(st/emit! (rt/nav :settings-subscription)))]
    [:> card* {:class (stl/css :craft) :test-id "ds-ai-craft"}
     [:> row* {:label (tr "ds.ai.craft.title")
               :class (stl/css :craft-row)
               :action (mf/html
                        [:button {:class (stl/css :craft-link) :type "button" :on-click on-plans}
                         (if (and (some? ratio) (some? plan))
                           (tr "ds.ai.craft.link")
                           (tr "ds.ai.craft.see"))
                         " →"])}
      [:> sparkle-icon* {:class (stl/css :craft-icon)}]
      (if (and (some? ratio) (some? plan))
        [:p {:class (stl/css :craft-text)}
         [:strong (tr "ds.ai.craft.left" (acc/format-number (:left credits)) (acc/format-number (:total credits)))]
         [:span " · "
          (if (some? renews)
            (tr "ds.ai.craft.plan-renews" plan renews)
            (tr "ds.ai.craft.plan" plan))]]
        [:p {:class (stl/css :craft-text)}
         [:strong (tr "ds.ai.craft.free")]
         [:span " · " (tr "ds.ai.craft.free-text")]])]]))

(mf/defc privacy-note*
  "P3D: one line."
  {::mf/private true}
  []
  [:aside {:class (stl/css :privacy)}
   [:> lock-icon* {:class (stl/css :privacy-icon)}]
   [:p {:class (stl/css :privacy-text)}
    (tr "ds.ai.privacy") " "
    [:a {:class (stl/css :privacy-link) :href manage-uri}
     (tr "ds.ai.privacy.history") " →"]]])

(mf/defc connected-view*
  {::mf/private true}
  [{:keys [state]}]
  (let [tokens*  (mf/deref refs/access-tokens)
        profile  (mf/deref refs/profile)
        paused?  (false? (-> profile :props :mcp-enabled))
        on?      (not paused?)
        busy*    (mf/use-state false)
        problem* (mf/use-state nil)

        apps     (let [apps (:apps state)]
                   (if (and (sequential? apps) (seq apps)) apps [{}]))

        do-revoke
        (mf/use-fn
         (mf/deps tokens*)
         (fn []
           (doseq [token (filter #(= "mcp" (:type %)) tokens*)]
             (st/emit! (du/delete-access-token
                        (with-meta {:id (:id token)}
                          {:on-success #(st/emit! (du/fetch-access-tokens))}))))
           (st/emit! (du/update-profile-props {:mcp-enabled false})
                     (mbc/event :mcp/disable {}))))

        on-revoke
        (mf/use-fn
         (mf/deps do-revoke)
         (fn []
           (st/emit! (modal/show {:type :confirm
                                  :title (tr "ds.ai.revoke.title")
                                  :message (tr "ds.ai.revoke.message")
                                  :accept-label (tr "ds.ai.apps.revoke")
                                  :on-accept do-revoke}))))

        on-toggle
        (mf/use-fn
         (mf/deps on? on-revoke)
         (fn [_]
           (if on?
             (on-revoke)
             (do
               (reset! busy* true)
               (reset! problem* nil)
               (-> (connect/activate!)
                   (.then (fn [outcome]
                            (reset! busy* false)
                            (when-not (= :connected outcome)
                              (reset! problem* outcome)))))))))]
    [:div {:class (stl/css :connected)}
     [:> switch-card* {:on? on?
                       :busy? (deref busy*)
                       :problem (deref problem*)
                       :on-toggle on-toggle}]
     (when on?
       ;; P3D: the apps are 48 px rows of one card, "Connect another app"
       ;; in its foot.
       [:> card* {:title (tr "ds.ai.apps.title")
                  :class (stl/css :apps)
                  :foot (mf/html [:> add-app* {}])}
        [:ul {:class (stl/css :app-list)}
         (for [[idx app] (d/enumerate apps)]
           [:> app-row* {:key idx :app app :on-revoke on-revoke}])]])
     (when (true? (:craft state))
       [:> craft-card* {:member (:member state)}])
     [:> privacy-note* {}]]))

(mf/defc ai-access-card*
  []
  (let [state  (ss/use-state)
        status (access-status state)
        copy   (get status-copy status)]
    (cond
      (starts-connect-flow? status)
      ;; The same card as the dashboard: step 1 is one click here, in the
      ;; editor; step 2 is the address to paste in the assistant.
      [:div {:class (stl/css :connect) :data-testid "ds-ai-access" :data-status "not-connected"}
       [:> cc/connect-card* {}]]

      (= status :connected)
      [:> connected-view* {:state state}]

      (some? copy)
      [:> sds/card* {:class (stl/css :card)
                     :padding "md"
                     :data-testid "ds-ai-access"
                     :data-status (name status)}
       [:div {:class (stl/css :head)}
        [:h3 {:class (stl/css :title)} (tr "ds.ai-access.title")]
        [:> sds/badge* {:tone (:tone copy)}
         (tr (:badge copy))]]
       [:p {:class (stl/css :text)} (tr (:text copy))]
       [:div
        [:> sds/button* {:variant "secondary"
                         :to manage-uri
                         :data-testid "ds-ai-access-manage"}
         (tr (:cta copy))]]])))
