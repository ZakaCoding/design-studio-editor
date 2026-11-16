;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.mcp-tip
  "Workspace MCP onboarding tip: a compact card at the bottom of the canvas
  that invites the member to connect their AI assistant and starts the
  interactive guide. It sits at the bottom right of the canvas, in line
  with the Craft pill; while Craft is open it sits at the bottom left of
  the canvas (owner, 16 Nov). Always above the row of the floating toolbar,
  and on the other side when the selection is under it. When it shows is
  decided by `app.main.ui.studio.mcp-cadence/decide`."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.ui.studio.craft.quick-prompt :as qp]
   [app.main.ui.studio.ds :as sds]
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

(def ^:private card-width 300)

(def ^:private gap
  "Gap between the tip and the edges of the canvas area."
  12)

(def ^:private lift
  "Distance from the bottom of the window to the tip: the 20 px under the
  floating toolbar and the Craft pill, their 48 px and a 12 px gap, so the
  tip never touches that row."
  80)

(defn- overlaps?
  [a b]
  (and (< (:x a) (+ (:x b) (:width b)))
       (< (:x b) (+ (:x a) (:width a)))
       (< (:y a) (+ (:y b) (:height b)))
       (< (:y b) (+ (:y a) (:height a)))))

(defn- placement
  "Where the tip sits: `{:side :left | :right, :offset \"<n>px\"}`, the
  distance from that edge of the window. Always above the row of the
  floating toolbar and the Craft pill, inside the canvas area (the
  workspace shell's `viewport-overlay` mount point).

  While Craft is open (`<html data-ds-craft-open>`, set by the dock) it
  sits at the left edge of the canvas area, right of Layers or its rail;
  else at the right edge, in line with the Craft pill. When the selection
  is under that place and the other side is free, it takes the other
  side. `height` is the tip's own height in px."
  [height]
  (let [root   (.-documentElement js/document)
        area   (some-> (.getElementById js/document "ds-viewport-overlay")
                       (.getBoundingClientRect))
        pill   (some-> (.querySelector js/document "#ds-viewport-overlay [data-testid=\"ds-craft-toggle\"], #ds-craft-dock [data-testid=\"ds-craft-toggle\"]")
                       (.getBoundingClientRect))
        vw     (.-innerWidth js/window)
        vh     (.-innerHeight js/window)
        left   (if (some? area) (+ (.-left area) gap) 16)
        right  (cond
                 (and (some? pill) (pos? (.-width pill))) (- vw (.-right pill))
                 (some? area) (+ (- vw (.-right area)) gap)
                 :else 16)
        card   (fn [side]
                 {:x (if (= side :left) left (- vw right card-width))
                  :y (- vh lift height)
                  :width card-width
                  :height height})
        under  (:rect (qp/selection))
        free?  (fn [side]
                 (or (nil? under) (not (overlaps? (card side) under))))
        prefer (if (and (.hasAttribute root "data-ds-craft-open") (some? area)) :left :right)
        other  (if (= prefer :left) :right :left)
        side   (if (and (not (free? prefer)) (free? other)) other prefer)]
    {:side side
     :offset (str (js/Math.round (if (= side :left) left right)) "px")}))

(mf/defc mcp-tip*
  []
  (let [touring?     (some? (mf/deref tour/step-ref))

        ;; nil: not on screen; :in: showing; :out: leaving (fade)
        phase*       (mf/use-state nil)
        phase        (deref phase*)
        place*       (mf/use-state {:side :right :offset "16px"})
        place        (deref place*)
        card-ref     (mf/use-ref nil)
        hidden-tab* (mf/use-state (boolean (.-hidden js/document)))

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

        ;; Before the first render the tip has no height yet: 150 px is
        ;; about what it measures.
        place!
        (mf/use-fn
         (fn []
           (let [node   (mf/ref-val card-ref)
                 height (if (some? node) (.-offsetHeight node) 0)]
             (reset! place* (placement (if (pos? height) height 150))))))

        show
        (mf/use-fn
         (fn []
           (when (and (mf/ref-val alive) (not (mf/ref-val showing)))
             (shell/store-merge! :session "toast" #js {:lastShown (js/Date.now)})
             (shell/store-merge! :local "toast" #js {:lastDay (shell/today)})
             (mf/set-ref-val! showing true)
             (place!)
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
      (let [on-resize     #(place!)
            on-visibility #(reset! hidden-tab* (boolean (.-hidden js/document)))]
        (.addEventListener js/window "resize" on-resize)
        (.addEventListener js/document "visibilitychange" on-visibility)
        (fn []
          (mf/set-ref-val! alive false)
          (clear! show-timer)
          (clear! hide-timer)
          (.removeEventListener js/window "resize" on-resize)
          (.removeEventListener js/document "visibilitychange" on-visibility))))

    ;; While the tip is on screen the panels can still change under it:
    ;; the file finishes loading, Craft opens or closes, Layers becomes a
    ;; rail. Look again a few times a second; an unchanged place renders
    ;; nothing.
    (mf/with-effect [(some? phase)]
      (when (some? phase)
        (let [timer (js/setInterval #(place!) 400)]
          #(js/clearInterval timer))))

    (when (some? phase)
      [:aside {:class (stl/css-case :global/ds-toast true
                                    :global/ds-in (= phase :in)
                                    :toast true
                                    :toast-in (= phase :in)
                                    :toast-left (= (:side place) :left)
                                    :still still?
                                    :paused (deref hidden-tab*))
               :ref card-ref
               :style {"--ds-offset" (:offset place)}
               :data-side (name (:side place))
               :role "region"
               :aria-label (tr "ds.mcp-tip.label")
               :on-key-down on-key-down
               :on-pointer-enter hold
               :on-pointer-leave release
               :on-focus hold
               :on-blur release}
       [:div {:class (stl/css :head)}
        ;; The MCP plug, the mark of the MCP pill's menu.
        [:span {:class (stl/css :mark)
                :aria-hidden "true"}
         [:svg {:view-box "0 0 16 16" :focusable "false"}
          [:path {:d "M6 1.75v3M10 1.75v3M4.25 4.75h7.5v2.5a3.75 3.75 0 0 1-7.5 0zM8 11v3.25"}]]]
        [:p {:class (stl/css :title)}
         (tr "ds.mcp-tip.title")]
        [:button {:type "button"
                  :class (stl/css :global/ds-t-close :close)
                  :aria-label (tr "ds.mcp-tip.close")
                  :title (tr "ds.mcp-tip.close")
                  :on-click on-close}
         [:svg {:view-box "0 0 16 16" :aria-hidden "true" :focusable "false"}
          [:path {:d "M4.5 4.5l7 7M11.5 4.5l-7 7"}]]]]
       [:p {:class (stl/css :text)}
        (tr "ds.mcp-tip.body")]
       [:div {:class (stl/css :actions)}
        [:> sds/button* {:class (stl/css :global/ds-t-cta :cta)
                         :size "sm"
                         :on-click on-guide}
         (tr "ds.mcp-tip.cta")]
        [:button {:type "button"
                  :class (stl/css :global/ds-t-never :later)
                  :on-click on-later}
         (tr "ds.mcp-tip.later")]]])))
