;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.mcp-tip
  "Workspace MCP onboarding tip: a card at the bottom of the canvas, left of
  the design panel, that invites the member to connect their AI assistant
  and starts the interactive guide. When it shows is decided by
  `app.main.ui.studio.mcp-cadence/decide`."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.refs :as refs]
   [app.main.ui.studio.mcp-cadence :as cadence]
   [app.main.ui.studio.mcp-tour :as tour]
   [app.main.ui.studio.shell-client :as shell]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def ^:private max-timeout
  "Longest delay `setTimeout` takes."
  (dec (js/Math.pow 2 31)))

(def ^:private leave-ms 220)

(defn- remembered
  "What this browser remembers about the tip, in the shape `decide` reads."
  [state]
  (let [local   (shell/store-get :local "toast")
        session (shell/store-get :session "toast")
        field   (fn [obj k] (when (object? obj) (unchecked-get obj k)))]
    {:connected  (:connected state)
     :ai-access  (:ai-access state)
     :snoozed    (field local "snoozed")
     :last-day   (field local "lastDay")
     :dismissed  (field session "dismissed")
     :last-shown (field session "lastShown")}))

(defn- right-offset
  "Keep the tip on the canvas, left of the right-hand design panel."
  []
  (let [panel (.querySelector js/document "[data-testid=\"right-sidebar\"]")
        width (some-> panel (.getBoundingClientRect) (.-width))
        width (if (and (number? width) (< width (/ (.-innerWidth js/window) 2))) width 0)]
    (str (+ width 16) "px")))

(mf/defc scene*
  {::mf/private true}
  []
  [:svg {:class (stl/css :scene)
         :view-box "0 0 280 120"
         :aria-hidden "true"
         :focusable "false"}
   [:rect {:x 1 :y 1 :width 278 :height 118 :rx 14 :class (stl/css :sc-bg)}]
   [:g
    [:rect {:x 12 :y 20 :width 140 :height 34 :rx 12 :class (stl/css :sc-bubble)}]
    [:circle {:cx 27 :cy 37 :r 7 :class (stl/css :sc-ai)}]
    [:text {:x 39 :y 40.5 :class (stl/css :sc-txt)} (tr "ds.mcp-tip.scene-ai")]
    [:g {:class (stl/css :sc-dots)}
     [:circle {:cx 30 :cy 68 :r 3}]
     [:circle {:cx 40 :cy 68 :r 3}]
     [:circle {:cx 50 :cy 68 :r 3}]]]
   [:path {:d "M152 37 C163 37 161 58 172 58" :class (stl/css :sc-wire)}]
   [:circle {:r 3.5 :class (stl/css :sc-pulse)}
    [:animateMotion {:dur "2.4s"
                     :repeatCount "indefinite"
                     :path "M152 37 C163 37 161 58 172 58"}]]
   [:rect {:x 118 :y 76 :width 38 :height 18 :rx 9 :class (stl/css :sc-chip)}]
   [:text {:x 137 :y 88.5 :text-anchor "middle" :class (stl/css :sc-chiptxt)} "MCP"]
   [:g
    [:rect {:x 172 :y 14 :width 96 :height 92 :rx 8 :class (stl/css :sc-board)}]
    [:text {:x 180 :y 28 :class (stl/css :sc-cap)} (tr "ds.mcp-tip.scene-canvas")]
    [:rect {:x 180 :y 36 :width 80 :height 30 :rx 4 :class (stl/css :sc-frame)}]
    [:rect {:x 180 :y 72 :width 50 :height 7 :rx 3.5 :class (stl/css :sc-line :sc-line-1)}]
    [:rect {:x 180 :y 84 :width 34 :height 7 :rx 3.5 :class (stl/css :sc-line :sc-line-2)}]
    [:g {:class (stl/css :sc-cursor)}
     [:path {:d "M0 0l10 4-4.4 1.6L3.6 10z"}]
     [:rect {:x 9 :y 9 :width 18 :height 11 :rx 5.5}]
     [:text {:x 18 :y 17.5 :text-anchor "middle"} "AI"]]]])

(mf/defc mcp-tip*
  []
  (let [profile      (mf/deref refs/profile)
        mcp-enabled? (-> profile :props :mcp-enabled boolean)
        touring?     (some? (mf/deref tour/step-ref))

        ;; nil: not on screen; :in: showing; :out: leaving (fade)
        phase*       (mf/use-state nil)
        phase        (deref phase*)
        offset*      (mf/use-state "16px")
        hidden-tab*  (mf/use-state (boolean (.-hidden js/document)))

        still?       (mf/with-memo [] (shell/reduced-motion?))

        ;; Timers and flags that outlive a render.
        alive        (mf/use-ref true)
        showing      (mf/use-ref false)
        show-timer   (mf/use-ref nil)
        hide-timer   (mf/use-ref nil)
        schedule-ref (mf/use-ref nil)
        hide-ref     (mf/use-ref nil)

        clear!
        (mf/use-fn
         (fn [ref]
           (some-> (mf/ref-val ref) js/clearTimeout)
           (mf/set-ref-val! ref nil)))

        release
        (mf/use-fn
         (fn []
           (clear! hide-timer)
           (mf/set-ref-val! hide-timer
                            (js/setTimeout #((mf/ref-val hide-ref) :timeout)
                                           (:visible-for cadence/cadence)))))

        hold
        (mf/use-fn #(clear! hide-timer))

        show
        (mf/use-fn
         (fn []
           (when (and (mf/ref-val alive) (not (mf/ref-val showing)))
             (shell/store-merge! :session "toast" #js {:lastShown (js/Date.now)})
             (shell/store-merge! :local "toast" #js {:lastDay (shell/today)})
             (mf/set-ref-val! showing true)
             (reset! offset* (right-offset))
             (reset! phase* :out)
             ;; One frame later, so the card animates in.
             (js/requestAnimationFrame
              (fn []
                (when (and (mf/ref-val alive) (mf/ref-val showing))
                  (reset! phase* :in))))
             (release))))

        schedule
        (mf/use-fn
         (mf/deps touring?)
         (fn schedule []
           (clear! show-timer)
           (when (and (mf/ref-val alive) (not (mf/ref-val showing)) (not touring?))
             (-> (shell/fetch-state)
                 (.then
                  (fn [state]
                    ;; AI access state unknown: never advertise a page that
                    ;; may not work.
                    (when (and (some? state) (mf/ref-val alive) (not (mf/ref-val showing)))
                      (let [decision (cadence/decide (remembered state) (js/Date.now) (shell/today))
                            retry-in (:retry-in decision)]
                        (clear! show-timer)
                        (cond
                          (:show decision)
                          (mf/set-ref-val!
                           show-timer
                           (js/setTimeout
                            (fn []
                              (cond
                                (not (mf/ref-val alive))
                                nil

                                ;; Wait until the member looks at the tab.
                                (.-hidden js/document)
                                (.addEventListener js/document "visibilitychange"
                                                   #((mf/ref-val schedule-ref))
                                                   #js {:once true})

                                (nil? @tour/step-ref)
                                (show)))
                            (:delay decision)))

                          (some? retry-in)
                          (mf/set-ref-val!
                           show-timer
                           (js/setTimeout #((mf/ref-val schedule-ref))
                                          (min retry-in max-timeout))))))))))))

        hide
        (mf/use-fn
         (fn [why]
           (clear! hide-timer)
           (when (= why :manual)
             (shell/store-merge! :session "toast" #js {:dismissed (shell/today)}))
           (when (mf/ref-val showing)
             (mf/set-ref-val! showing false)
             (reset! phase* :out)
             (js/setTimeout (fn []
                              (when (and (mf/ref-val alive) (not (mf/ref-val showing)))
                                (reset! phase* nil)))
                            (if (shell/reduced-motion?) 0 leave-ms)))
           (when (not= why :manual)
             ((mf/ref-val schedule-ref)))))

        on-guide
        (mf/use-fn
         (fn []
           (hide :manual)
           (tour/start!)))

        on-later
        (mf/use-fn
         (fn []
           (shell/store-merge! :local "toast" #js {:snoozed (shell/today)})
           (hide :manual)))

        on-close
        (mf/use-fn #(hide :manual))

        on-key-down
        (mf/use-fn
         (fn [event]
           (when (= "Escape" (.-key event))
             (.stopPropagation event)
             (hide :manual))))]

    (mf/set-ref-val! schedule-ref schedule)
    (mf/set-ref-val! hide-ref hide)

    ;; Decide on open, and again whenever the guide starts or ends.
    (mf/with-effect [schedule]
      (schedule))

    (mf/with-effect []
      (mf/set-ref-val! alive true)
      (let [on-resize     #(reset! offset* (right-offset))
            on-visibility #(reset! hidden-tab* (boolean (.-hidden js/document)))]
        (.addEventListener js/window "resize" on-resize)
        (.addEventListener js/document "visibilitychange" on-visibility)
        (fn []
          (mf/set-ref-val! alive false)
          (clear! show-timer)
          (clear! hide-timer)
          (.removeEventListener js/window "resize" on-resize)
          (.removeEventListener js/document "visibilitychange" on-visibility))))

    (when (some? phase)
      [:aside {:class (stl/css-case :global/ds-toast true
                                    :global/ds-in (= phase :in)
                                    :toast true
                                    :toast-in (= phase :in)
                                    :still still?
                                    :paused (deref hidden-tab*))
               :style {"--ds-right" (deref offset*)}
               :role "region"
               :aria-label (tr "ds.mcp-tip.label")
               :on-key-down on-key-down
               :on-pointer-enter hold
               :on-pointer-leave release
               :on-focus hold
               :on-blur release}
       [:button {:type "button"
                 :class (stl/css :scene-button)
                 :aria-label (tr "ds.mcp-tip.cta")
                 :on-click on-guide}
        [:> scene*]]
       [:div {:class (stl/css :body)}
        [:p {:class (stl/css :title)}
         (if mcp-enabled? (tr "ds.mcp-tip.title-enabled") (tr "ds.mcp-tip.title"))]
        [:p {:class (stl/css :text)}
         (if mcp-enabled? (tr "ds.mcp-tip.body-enabled") (tr "ds.mcp-tip.body"))]
        [:div {:class (stl/css :actions)}
         [:button {:type "button"
                   :class (stl/css :global/ds-t-cta :cta)
                   :on-click on-guide}
          (tr "ds.mcp-tip.cta")]
         [:button {:type "button"
                   :class (stl/css :global/ds-t-never :later)
                   :on-click on-later}
          (tr "ds.mcp-tip.later")]]]
       [:button {:type "button"
                 :class (stl/css :global/ds-t-close :close)
                 :aria-label (tr "ds.mcp-tip.close")
                 :on-click on-close}
        [:svg {:view-box "0 0 16 16" :aria-hidden "true" :focusable "false"}
         [:path {:d "M4 4l8 8M12 4l-8 8"}]]]])))
