;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard.connect-card
  "The \"Connect AI\" card of the dashboard (F1 · 03, 03b, 12) and the
  status bar it shrinks to once an AI app is connected (F1 · 03c). The
  flow itself is in `app.main.ui.studio.dashboard.connect`.

  The two steps sit side by side: step 1 turns AI on for the account (one
  click), step 2 is the address for the member's AI app, with the three
  kinds of app to choose from. The card says how many steps are done and
  turns into the status bar by itself when the app connects."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.config :as cf]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.studio.dashboard.connect :as connect]
   [app.main.ui.studio.dashboard.icons :as icons]
   [app.main.ui.studio.dashboard.rules :as rules]
   [app.util.clipboard :as clipboard]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def ^:private failure-text
  {:session   "ds.connect.error.session"
   :no-access "ds.connect.error.no-access"
   :busy      "ds.connect.error.busy"
   :failed    "ds.connect.error.failed"})

(def ^:private assistant-copy
  {:claude {:name "ds.connect.app.claude"
            :note "ds.connect.app.claude-note"
            :hint "ds.connect.hint.claude"
            :waiting "ds.connect.waiting.claude"}
   :codex  {:name "ds.connect.app.codex"
            :note "ds.connect.app.codex-note"
            :hint "ds.connect.hint.codex"
            :waiting "ds.connect.waiting.codex"}
   :other  {:name "ds.connect.app.other"
            :note "ds.connect.app.other-note"
            :hint "ds.connect.hint.other"
            :waiting "ds.connect.waiting.other"}})

;; How often, and for how long, the open card asks Studio whether the AI
;; app has connected ("jadi hijau sendiri saat tersambung").
(def ^:private poll-ms 5000)
(def ^:private poll-max 120)

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
     [:> icons/link* {:size "m" :class (stl/css :address-icon)}]
     [:code {:class (stl/css :address-value)
             :title uri
             :data-testid "ds-connect-address"}
      (rules/display-uri uri)]
     [:button {:class (stl/css :copy)
               :type "button"
               :disabled is-disabled
               :on-click on-copy
               :data-testid "ds-connect-copy"}
      (if copied?
        (tr "ds.connect.copied")
        (tr "ds.connect.copy"))]
     ;; Announced once, without moving focus.
     [:span {:class (stl/css :visually-hidden) :role "status"}
      (when copied? (tr "ds.connect.copied"))]]))

(mf/defc choices*
  "Step 2: which AI app the member connects. It changes the instruction
  under the address, nothing else."
  {::mf/private true}
  [{:keys [selected is-disabled on-select]}]
  (let [on-click
        (mf/use-fn
         (mf/deps on-select)
         (fn [event]
           (on-select (keyword (.getAttribute (.-currentTarget event) "data-app")))))]
    [:div {:class (stl/css :choices)
           :role "radiogroup"
           :aria-label (tr "ds.connect.apps")}
     (for [app rules/assistants]
       (let [copy      (get assistant-copy app)
             selected? (= app selected)]
         [:button {:key (name app)
                   :class (stl/css-case :choice true
                                        :choice-selected selected?)
                   :type "button"
                   :role "radio"
                   :aria-checked selected?
                   :disabled is-disabled
                   :data-app (name app)
                   :data-testid "ds-connect-app"
                   :on-click on-click}
          [:span {:class (stl/css :choice-name)} (tr (:name copy))]
          [:span {:class (stl/css :choice-note)} (tr (:note copy))]]))]))

(mf/defc status-bar*
  "F1 · 03c: one line once an AI app is connected."
  {::mf/private true}
  [{:keys [assistant on-add]}]
  (let [on-manage (mf/use-fn #(st/emit! (rt/nav :settings-integrations)))
        named?    (some? assistant)]
    [:section {:class (stl/css :bar)
               :aria-label (tr "ds.connect.label")
               :data-testid "ds-connect-bar"
               :data-ds-connect "connected"}
     [:span {:class (stl/css :bar-dot) :aria-hidden true}]
     [:p {:class (stl/css :bar-text)}
      [:strong
       (if named?
         (tr "ds.connect.bar.app-connected" (:name assistant))
         (tr "ds.connect.bar.connected"))]
      [:span
       (cond
         (not named?)                   (tr "ds.connect.bar.auto")
         (= :write (:access assistant)) (tr "ds.connect.bar.write")
         :else                          (tr "ds.connect.bar.read"))]]
     [:div {:class (stl/css :bar-actions)}
      [:button {:class (stl/css :bar-manage)
                :type "button"
                :on-click on-manage
                :data-testid "ds-connect-manage"}
       (tr "ds.connect.bar.manage")]
      [:button {:class (stl/css :bar-add)
                :type "button"
                :on-click on-add
                :data-testid "ds-connect-add"}
       [:> icons/plus* {:size "m"}]
       (tr "ds.connect.bar.add")]]]))

(mf/defc connect-card*
  "Renders nothing until Studio says the account has AI access."
  []
  (let [status*    (mf/use-state nil)
        ;; Step 1 finished in this visit.
        activated* (mf/use-state false)
        ;; "+ Aplikasi AI lain": step 2 again, from the status bar.
        adding*    (mf/use-state false)
        busy*      (mf/use-state false)
        problem*   (mf/use-state nil)
        ;; The app the member picked; nil follows the connected one.
        app*       (mf/use-state nil)

        status     (deref status*)
        adding?    (deref adding*)

        uri        (mf/with-memo []
                     (rules/assistant-uri (.-origin js/location)))

        mode       (rules/card-mode {:enabled (contains? cf/flags :mcp)
                                     :status status
                                     :activated (deref activated*)
                                     :adding adding?})
        pairing?   (= mode :pairing)
        app        (or (deref app*)
                       (if adding?
                         :claude
                         (rules/assistant-choice (:name (:assistant status)))))
        copy       (get assistant-copy app)
        problem    (deref problem*)

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

        on-manage
        (mf/use-fn #(st/emit! (rt/nav :settings-integrations)))

        on-add
        (mf/use-fn
         (fn [_]
           (reset! app* nil)
           (reset! adding* true)))

        on-close
        (mf/use-fn #(reset! adding* false))

        on-select
        (mf/use-fn #(reset! app* %))]

    (mf/with-effect []
      (let [alive (volatile! true)]
        (-> (connect/fetch-status)
            (.then (fn [status]
                     (when @alive
                       (reset! status* status)))))
        (fn [] (vreset! alive false))))

    ;; While step 2 is open, ask now and then whether the app has connected;
    ;; the card then becomes the status bar by itself. Not while the tab is
    ;; hidden, and not for ever.
    (mf/with-effect [pairing? adding?]
      (when (and pairing? (not adding?))
        (let [alive (volatile! true)
              asked (volatile! 0)
              timer (volatile! nil)
              ask   (fn []
                      (when (and @alive (not (.-hidden js/document)))
                        (vswap! asked inc)
                        (-> (connect/fetch-status)
                            (.then (fn [fresh]
                                     (when (and @alive (some? (:assistant fresh)))
                                       (reset! status* fresh)))))))]
          (vreset! timer (js/setInterval
                          (fn []
                            (if (>= @asked poll-max)
                              (js/clearInterval @timer)
                              (ask)))
                          poll-ms))
          (fn []
            (vreset! alive false)
            (js/clearInterval @timer)))))

    ;; `cond`, not `case`: the component macro compiled a `case` with a
    ;; default branch around hiccup into code that threw for every mode.
    (cond
      (= mode :hidden)
      nil

      (= mode :connected)
      [:> status-bar* {:assistant (:assistant status) :on-add on-add}]

      :else
      [:section {:class (stl/css :card)
                 :aria-labelledby "ds-connect-title"
                 :data-testid "ds-connect-card"
                 :data-ds-connect (if pairing? "pairing" "card")}
       [:header {:class (stl/css :card-header)}
        [:div {:class (stl/css :card-heading)}
         [:h2 {:id "ds-connect-title" :class (stl/css :card-title)}
          (tr "ds.connect.title")]
         [:p {:class (stl/css :card-lead)} (tr "ds.connect.lead")]]
        (if adding?
          [:button {:class (stl/css :card-close)
                    :type "button"
                    :on-click on-close
                    :data-testid "ds-connect-done"}
           (tr "ds.connect.done")]
          [:span {:class (stl/css :progress)
                  :data-testid "ds-connect-progress"}
           (tr "ds.connect.progress" (str (rules/steps-done mode)))])]

       [:div {:class (stl/css :steps)}
        ;; Step 1
        [:div {:class (stl/css-case :step-one true
                                    :step-one-done pairing?)
               :data-testid "ds-connect-step-1"}
         [:div {:class (stl/css :step-head)}
          [:span {:class (stl/css :mark) :aria-hidden true}
           (if pairing?
             [:> icons/check* {:size "s"}]
             "1")]
          [:h3 {:class (stl/css :step-title)}
           (tr "ds.connect.step-1.title")
           (when pairing?
             [:span {:class (stl/css :visually-hidden)}
              (str " (" (tr "ds.connect.step-done") ")")])]
          [:button {:class (stl/css :switch)
                    :type "button"
                    :role "switch"
                    :aria-checked pairing?
                    :aria-label (tr "ds.connect.step-1.switch")
                    :aria-busy (when (deref busy*) true)
                    :on-click (if pairing? on-manage on-activate)
                    :data-testid "ds-connect-switch"}
           [:span {:class (stl/css :switch-knob) :aria-hidden true}]]]
         [:p {:class (stl/css :step-text)}
          (if pairing?
            (tr "ds.connect.step-1.done")
            (tr "ds.connect.step-1.text"))]
         (if pairing?
           [:p {:class (stl/css :active)}
            [:> icons/check* {:size "s"}]
            (tr "ds.connect.step-1.active")]
           [:button {:class (stl/css :activate)
                     :type "button"
                     :aria-busy (when (deref busy*) true)
                     :disabled (deref busy*)
                     :on-click on-activate
                     :data-testid "ds-connect-activate"}
            (tr "ds.connect.step-1.cta")])
         (when (and (not pairing?) (some? problem))
           [:p {:class (stl/css :problem) :role "alert"}
            (tr (get failure-text problem "ds.connect.error.failed"))])]

        ;; Step 2
        [:div {:class (stl/css-case :step-two true
                                    :step-two-locked (not pairing?))
               :data-testid "ds-connect-step-2"}
         [:div {:class (stl/css :step-head)}
          [:span {:class (stl/css :mark :mark-two) :aria-hidden true} "2"]
          [:h3 {:class (stl/css :step-title)} (tr "ds.connect.step-2.title")]]
         [:div {:class (stl/css :step-two-body)}
          [:div {:class (stl/css :apps)}
           [:> choices* {:selected app
                         :is-disabled (not pairing?)
                         :on-select on-select}]
           (if pairing?
             [:p {:class (stl/css :waiting) :role "status"}
              [:span {:class (stl/css :waiting-dot) :aria-hidden true}]
              (tr (:waiting copy))]
             [:p {:class (stl/css :locked)} (tr "ds.connect.step-2.locked")])]
          [:div {:class (stl/css :link)}
           [:> address* {:uri uri :is-disabled (not pairing?)}]
           [:p {:class (stl/css :hint)}
            (tr (:hint copy))
            " "
            [:a {:class (stl/css :guide)
                 :href (get connect/guides app)
                 :target "_blank"
                 :rel "noopener"
                 :tab-index (when-not pairing? -1)}
             (tr "ds.connect.guide")]]]]]]])))

(mf/defc home-card*
  "The card as the first row of the projects list (mounted from
  `app.main.ui.dashboard.projects`)."
  []
  [:div {:class (stl/css :home-row)}
   [:> connect-card*]])
