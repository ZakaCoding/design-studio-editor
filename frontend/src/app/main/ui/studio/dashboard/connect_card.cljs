;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard.connect-card
  "The \"Connect AI\" card of the dashboard (F1 · 03, 03b) and the status
  bar it shrinks to once the account is connected (F1 · 03c). The flow
  itself is in `app.main.ui.studio.dashboard.connect`."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.config :as cf]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.ds.foundations.assets.icon :as i]
   [app.main.ui.studio.dashboard.connect :as connect]
   [app.main.ui.studio.dashboard.rules :as rules]
   [app.main.ui.studio.ds :as sds]
   [app.util.clipboard :as clipboard]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def ^:private failure-text
  {:session   "ds.connect.error.session"
   :no-access "ds.connect.error.no-access"
   :busy      "ds.connect.error.busy"
   :failed    "ds.connect.error.failed"})

(defn- use-copy
  "Returns `[copied? on-copy]` for `text`; `copied?` falls back after a
  moment."
  [text]
  (let [copied* (mf/use-state false)
        timer   (mf/use-ref nil)
        on-copy (mf/use-fn
                 (mf/deps text)
                 (fn [_]
                   (-> (clipboard/to-clipboard text)
                       (.then (fn []
                                (reset! copied* true)
                                (js/clearTimeout (mf/ref-val timer))
                                (mf/set-ref-val! timer (js/setTimeout #(reset! copied* false) 2400))))
                       (.catch (constantly nil)))))]
    (mf/with-effect []
      (fn [] (js/clearTimeout (mf/ref-val timer))))
    [(deref copied*) on-copy]))

(mf/defc address*
  "The assistant address with its copy button. The address is public: it
  holds no key."
  {::mf/private true}
  [{:keys [uri is-disabled]}]
  (let [[copied? on-copy] (use-copy uri)]
    [:div {:class (stl/css :address)}
     [:code {:class (stl/css :address-value)
             :data-testid "ds-connect-address"}
      uri]
     [:> sds/button* {:variant "secondary"
                      :icon-id (if copied? i/tick i/clipboard)
                      :disabled is-disabled
                      :on-click on-copy
                      :data-testid "ds-connect-copy"}
      (if copied?
        (tr "ds.connect.copied")
        (tr "ds.connect.copy"))]
     ;; Announced once, without moving focus.
     [:span {:class (stl/css :visually-hidden) :role "status"}
      (when copied? (tr "ds.connect.copied"))]]))

(mf/defc step*
  {::mf/private true}
  [{:keys [number title state children]}]
  ;; state: "open", "done" or "waiting"
  [:li {:class (stl/css-case :step true
                             :step-done (= state "done")
                             :step-waiting (= state "waiting"))
        :aria-current (when (= state "open") "step")}
   [:span {:class (stl/css :step-mark) :aria-hidden true}
    (if (= state "done")
      [:> sds/icon* {:icon-id i/tick :size "s"}]
      number)]
   [:div {:class (stl/css :step-body)}
    [:h3 {:class (stl/css :step-title)}
     title
     (when (= state "done")
       [:span {:class (stl/css :visually-hidden)} (str " (" (tr "ds.connect.step-done") ")")])]
    children]])

(mf/defc status-bar*
  "F1 · 03c: one line once connected, and the reminder after \"Not now\"."
  {::mf/private true}
  [{:keys [mode uri on-open]}]
  (let [connected?        (= mode :connected)
        [copied? on-copy] (use-copy uri)
        on-manage         (mf/use-fn #(st/emit! (rt/nav :settings-integrations)))]
    [:section {:class (stl/css-case :bar true :bar-connected connected?)
               :aria-label (tr "ds.connect.label")
               :data-testid "ds-connect-bar"
               :data-ds-connect (if connected? "connected" "later")}
     [:span {:class (stl/css :bar-dot) :aria-hidden true}]
     [:p {:class (stl/css :bar-text)}
      [:strong (if connected?
                 (tr "ds.connect.bar.connected")
                 (tr "ds.connect.bar.later"))]
      [:span (if connected?
               (tr "ds.connect.bar.connected-note")
               (tr "ds.connect.bar.later-note"))]]
     (if connected?
       [:div {:class (stl/css :bar-actions)}
        [:> sds/button* {:variant "ghost"
                         :size "sm"
                         :icon-id (if copied? i/tick i/clipboard)
                         :on-click on-copy
                         :data-testid "ds-connect-bar-copy"}
         (if copied?
           (tr "ds.connect.copied")
           (tr "ds.connect.bar.copy"))]
        [:> sds/button* {:variant "ghost"
                         :size "sm"
                         :on-click on-manage
                         :data-testid "ds-connect-manage"}
         (tr "ds.connect.bar.manage")]
        [:span {:class (stl/css :visually-hidden) :role "status"}
         (when copied? (tr "ds.connect.copied"))]]
       [:div {:class (stl/css :bar-actions)}
        [:> sds/button* {:variant "secondary"
                         :size "sm"
                         :on-click on-open
                         :data-testid "ds-connect-reopen"}
         (tr "ds.connect.bar.open")]])]))

(mf/defc connect-card*
  "Renders nothing until Studio says the account has AI access."
  []
  (let [status*    (mf/use-state nil)
        ;; Step 1 finished in this visit: keep step 2 on screen until the
        ;; member closes the card.
        activated* (mf/use-state false)
        dismissed* (mf/use-state connect/dismissed?)
        busy*      (mf/use-state false)
        problem*   (mf/use-state nil)

        uri        (mf/with-memo []
                     (rules/assistant-uri (.-origin js/location)))

        mode       (rules/card-mode {:enabled (contains? cf/flags :mcp)
                                       :status (deref status*)
                                       :activated (deref activated*)
                                       :dismissed (deref dismissed*)})

        on-activate
        (mf/use-fn
         (fn [_]
           (reset! busy* true)
           (reset! problem* nil)
           (-> (connect/activate!)
               (.then (fn [outcome]
                        (reset! busy* false)
                        (if (= :connected outcome)
                          (do
                            (swap! status* assoc :connected true)
                            (reset! activated* true))
                          (reset! problem* outcome)))))))

        on-later
        (mf/use-fn
         (fn [_]
           (connect/set-dismissed! true)
           (reset! dismissed* true)))

        on-open
        (mf/use-fn
         (fn [_]
           (connect/set-dismissed! false)
           (reset! dismissed* false)))

        on-done
        (mf/use-fn #(reset! activated* false))]

    (mf/with-effect []
      (let [alive (volatile! true)]
        (-> (connect/fetch-status)
            (.then (fn [status]
                     (when @alive
                       (reset! status* status)))))
        (fn [] (vreset! alive false))))

    (case mode
      :hidden
      nil

      (:connected :later)
      [:> status-bar* {:mode mode :uri uri :on-open on-open}]

      (let [pairing? (= mode :pairing)
            problem  (deref problem*)]
        [:section {:class (stl/css :card)
                   :aria-labelledby "ds-connect-title"
                   :data-testid "ds-connect-card"
                   :data-ds-connect (if pairing? "pairing" "card")}
         [:header {:class (stl/css :card-header)}
          [:> sds/badge* {:tone "craft" :is-mono true} (tr "ds.connect.badge")]
          [:h2 {:id "ds-connect-title" :class (stl/css :card-title)}
           (tr "ds.connect.title")]
          [:p {:class (stl/css :card-lead)} (tr "ds.connect.lead")]]

         [:ol {:class (stl/css :steps)}
          [:> step* {:number 1
                     :title (tr "ds.connect.step-1.title")
                     :state (if pairing? "done" "open")}
           [:p {:class (stl/css :step-text)}
            (if pairing?
              (tr "ds.connect.step-1.done")
              (tr "ds.connect.step-1.text"))]
           (when-not pairing?
             [:> sds/button* {:variant "primary"
                              :size "lg"
                              :is-loading (deref busy*)
                              :on-click on-activate
                              :data-testid "ds-connect-activate"}
              (tr "ds.connect.step-1.cta")])
           (when (and (not pairing?) (some? problem))
             [:p {:class (stl/css :problem) :role "alert"}
              (tr (get failure-text problem "ds.connect.error.failed"))])]

          [:> step* {:number 2
                     :title (tr "ds.connect.step-2.title")
                     :state (if pairing? "open" "waiting")}
           [:p {:class (stl/css :step-text)} (tr "ds.connect.step-2.text")]
           [:> address* {:uri uri :is-disabled (not pairing?)}]
           (when pairing?
             [:p {:class (stl/css :step-text)} (tr "ds.connect.step-2.after")])]]

         [:footer {:class (stl/css :card-footer)}
          [:a {:class (stl/css :guide)
               :href connect/guide-uri
               :target "_blank"
               :rel "noopener"}
           (tr "ds.connect.guide")]
          (if pairing?
            [:> sds/button* {:variant "secondary"
                             :on-click on-done
                             :data-testid "ds-connect-done"}
             (tr "ds.connect.done")]
            [:> sds/button* {:variant "ghost"
                             :on-click on-later
                             :data-testid "ds-connect-later"}
             (tr "ds.connect.later")])]]))))

(mf/defc home-card*
  "The card as the first row of the projects list (mounted from
  `app.main.ui.dashboard.projects`)."
  []
  [:div {:class (stl/css :home-row)}
   [:> connect-card*]])
