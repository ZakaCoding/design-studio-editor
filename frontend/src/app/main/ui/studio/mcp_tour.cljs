;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.mcp-tour
  "Interactive MCP guide: a blinking \"click here\" ring walks the member to
  Main menu → MCP server → Manage → turn MCP on → copy the editor link, then
  hands over to AI access. The member performs every click; the guide only
  points, and the ring never takes pointer events.

  The step lives in session storage, so it survives the route change from
  the workspace to Settings > Integrations and a reload."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.data.event :as-alias ev]
   [app.main.data.modal :as-alias modal]
   [app.main.refs :as refs]
   [app.main.store :as st]
   [app.main.ui.studio.mcp-cadence :as cadence]
   [app.main.ui.studio.shell-client :as shell]
   [app.util.i18n :refer [tr]]
   [beicon.v2.core :as rx]
   [potok.v2.core :as ptk]
   [rumext.v2 :as mf]))

(def ^:private studio-uri "/studio/")
(def ^:private guide-uri "/start/guides/ai-agent-mcp")

;; --- Step

(defn- stored-step
  []
  (let [step (some-> (shell/store-get :session "tour") (unchecked-get "step"))
        step (when (string? step) (keyword step))]
    (when (cadence/step? step)
      step)))

(defonce step-ref
  (atom (stored-step)))

(defn set-step!
  [step]
  (if (some? step)
    (shell/store-set! :session "tour" #js {:step (name step) :at (js/Date.now)})
    (shell/store-remove! :session "tour"))
  (reset! step-ref step))

(defn start!
  []
  (set-step! :menu))

;; --- What is on screen
;;
;; The workspace entries are found by the ids upstream gives them (the MCP
;; entry is in the DOM only while the main menu is open); the Integrations
;; controls by the `data-testid` our seam adds (STUDIO-SEAMS.md).

(defn- query
  [selector]
  (.querySelector js/document selector))

(defn- main-menu-button
  "The workspace main menu toggle: the only toggle button in the header of
  the left sidebar. Its name is a tooltip that exists only on hover (2.18),
  so the translated `label` is a second way to find it, not the first."
  [label]
  (or (query "[data-testid=\"left-sidebar\"] header button[aria-pressed]")
      (->> (.querySelectorAll js/document "button[aria-pressed][aria-label]")
           (array-seq)
           (filter #(= label (.getAttribute % "aria-label")))
           (first))))

(def ^:private modal-selector "[data-testid=\"ds-integrations-modal\"]")

(defn- link-copy-button
  "The copy button of the editor link on the page (the key window has a copy
  button of its own, for the key)."
  []
  (->> (.querySelectorAll js/document "[data-testid=\"ds-input-copy\"]")
       (array-seq)
       (remove #(some? (.closest % modal-selector)))
       (first)))

(defn- last-button
  [node]
  (last (array-seq (.querySelectorAll node "button"))))

;; --- Placement

(defn- position!
  [ring tip target]
  (when (and (some? ring) (some? tip))
    (let [style (.-style tip)]
      (if (or (nil? target) (not (.-isConnected target)) (.-hidden ring))
        (do
          (.removeProperty style "--x")
          (.removeProperty style "--y"))
        (let [rect      (.getBoundingClientRect target)
              pad       6
              ww        (.-innerWidth js/window)
              wh        (.-innerHeight js/window)
              th        (.-offsetHeight tip)
              ;; Near the top edge (the Main menu button) the label sits
              ;; beside the ring and the card below it.
              near-top? (< (.-top rect) 44)
              ;; Otherwise the card goes to the right of the target when
              ;; there is room, else below it.
              tw        (min 300 (- ww 32))
              right?    (and (not near-top?)
                             (<= (+ (.-right rect) 16 tw) (- ww 16)))
              x         (if right?
                          (+ (.-right rect) 16)
                          (max 16 (min (.-left rect) (- ww tw 16))))
              y         (if right?
                          (max 16 (min (- (.-top rect) 8) (- wh th 16)))
                          (min (+ (.-bottom rect) (if near-top? 18 14)) (- wh th 16)))
              rstyle    (.-style ring)]
          (.setProperty rstyle "--x" (str (- (.-left rect) pad) "px"))
          (.setProperty rstyle "--y" (str (- (.-top rect) pad) "px"))
          (.setProperty rstyle "--w" (str (+ (.-width rect) (* pad 2)) "px"))
          (.setProperty rstyle "--h" (str (+ (.-height rect) (* pad 2)) "px"))
          (.setAttribute ring "data-below" (if near-top? "true" "false"))
          (.setProperty style "--x" (str x "px"))
          (.setProperty style "--y" (str y "px")))))))

;; --- Component

(defn- step-text
  [step modal?]
  (cond
    (= step :done) (tr "ds.mcp-tour.done-text")
    modal?         (tr "ds.mcp-tour.step-4-modal")
    :else
    (case step
      :menu   (tr "ds.mcp-tour.step-1")
      :mcp    (tr "ds.mcp-tour.step-2")
      :manage (tr "ds.mcp-tour.step-3")
      :enable (tr "ds.mcp-tour.step-4")
      :copy   (tr "ds.mcp-tour.step-5"))))

(mf/defc mcp-tour*
  [{:keys [section]}]
  (let [step          (mf/deref step-ref)
        profile       (mf/deref refs/profile)
        mcp-enabled?  (-> profile :props :mcp-enabled boolean)
        workspace?    (= section :workspace)
        integrations? (= section :settings-integrations)
        menu-label    (tr "shortcut-subsection.main-menu")

        ;; What the guide currently shows, as a primitive so an unchanged
        ;; screen does not render again: "<step>|<m when the key window is
        ;; open>|<t when there is a control to point at>".
        view*         (mf/use-state nil)
        view          (deref view*)
        shown-step    (mf/use-ref nil)
        modal-ref     (mf/use-ref false)
        target-ref    (mf/use-ref nil)
        ring-ref      (mf/use-ref nil)
        tip-ref       (mf/use-ref nil)

        still?        (mf/with-memo [] (shell/reduced-motion?))

        read
        (mf/use-fn
         (mf/deps step workspace? integrations? mcp-enabled? menu-label)
         (fn []
           (if (nil? step)
             (do
               (mf/set-ref-val! target-ref nil)
               (reset! view* nil))
             (let [manage (when workspace? (query "#mcp-menu-nav-to-integrations"))
                   entry  (when workspace? (query "#file-menu-mcp"))
                   copy   (when integrations? (link-copy-button))
                   step   (cadence/advance step {:workspace? workspace?
                                                 :integrations? integrations?
                                                 :menu-open? (some? entry)
                                                 :manage? (some? manage)
                                                 :mcp-enabled? mcp-enabled?
                                                 :copy? (some? copy)})
                   ;; While the key window is open, point at its main button
                   ;; instead of the switch on the page.
                   modal  (when (and integrations? (= step :enable))
                            (query modal-selector))
                   target (cond
                            (some? modal)
                            (last-button modal)

                            workspace?
                            (case step
                              :menu   (main-menu-button menu-label)
                              :mcp    entry
                              :manage manage
                              nil)

                            integrations?
                            (case step
                              :enable (query "[data-testid=\"ds-mcp-switch\"]")
                              :copy   copy
                              nil))]
               (when (not= step @step-ref)
                 (set-step! step))
               (mf/set-ref-val! target-ref target)
               (mf/set-ref-val! shown-step step)
               (mf/set-ref-val! modal-ref (some? modal))
               (reset! view* (when (or (some? target) (some? modal) (= step :done))
                               (str (name step) "|" (when modal "m") "|" (when target "t"))))))))

        on-end
        (mf/use-fn #(set-step! nil))]

    ;; Re-read on every change of the step, the route or the profile.
    (mf/with-effect [read]
      (read))

    ;; While the guide runs, re-read on every DOM batch: menus open and
    ;; close, the Integrations page loads, the key window appears.
    (mf/with-effect [read (some? step)]
      (when (some? step)
        (let [queued   (volatile! false)
              frame    (volatile! nil)
              on-batch (fn []
                         (when-not @queued
                           (vreset! queued true)
                           (vreset! frame (js/requestAnimationFrame
                                           (fn []
                                             (vreset! queued false)
                                             (read))))))
              observer (js/MutationObserver. on-batch)]
          (.observe observer (.-body js/document) #js {:childList true :subtree true})
          (fn []
            (.disconnect observer)
            (some-> @frame js/cancelAnimationFrame)))))

    ;; Follow the control: it moves with scrolling, resizing and animations.
    (mf/with-effect [view]
      (when (some? view)
        (let [frame  (volatile! nil)
              follow (fn follow []
                       (position! (mf/ref-val ring-ref) (mf/ref-val tip-ref) (mf/ref-val target-ref))
                       (vreset! frame (js/requestAnimationFrame follow)))]
          (follow)
          (fn []
            (some-> @frame js/cancelAnimationFrame)))))

    ;; Copying the editor link completes the in-editor part of the guide.
    (mf/with-effect [step]
      (when (= step :copy)
        (let [sub (->> st/stream
                       (rx/filter (ptk/type? ::ev/event))
                       (rx/filter #(= "copy-mcp-url" (::ev/name (deref %))))
                       (rx/subs! (fn [_]
                                   (js/setTimeout #(set-step! :done) 0))))]
          (fn []
            (rx/dispose! sub)))))

    ;; Escape ends the guide, unless a Penpot window is open and takes it.
    (mf/with-effect [(some? view)]
      (when (some? view)
        (let [on-key-down
              (fn [event]
                (when (and (= "Escape" (.-key event))
                           (nil? (::modal/modal @st/state)))
                  (set-step! nil)))]
          (.addEventListener js/document "keydown" on-key-down)
          (fn []
            (.removeEventListener js/document "keydown" on-key-down)))))

    (when (some? view)
      (let [step    (mf/ref-val shown-step)
            modal?  (mf/ref-val modal-ref)
            target? (some? (mf/ref-val target-ref))
            done?   (= step :done)]
        [:*
         [:div {:class (stl/css-case :global/ds-coach true
                                     :coach true
                                     :still still?)
                :ref ring-ref
                :hidden (not target?)
                :aria-hidden "true"}
          [:span {:class (stl/css :here)} (tr "ds.mcp-tour.here")]]

         [:div {:class (stl/css-case :global/ds-coach-tip true
                                     :tip true
                                     :tip-center (or done? (not target?))
                                     :still still?)
                :ref tip-ref
                :role "dialog"
                :aria-live "polite"
                :aria-label (tr "ds.mcp-tip.label")
                :data-step (name step)}
          (when-not done?
            [:p {:class (stl/css :step)}
             (tr "ds.mcp-tour.step" (cadence/step-number step))])
          (when done?
            [:p {:class (stl/css :title)} (tr "ds.mcp-tour.done-title")])
          [:p {:class (stl/css :text)} (step-text step modal?)]
          [:div {:class (stl/css :actions)}
           (if done?
             [:*
              [:a {:class (stl/css :primary)
                   :href studio-uri
                   :target "_blank"
                   :rel "noopener"
                   :on-click on-end}
               (tr "ds.mcp-tour.open-ai-access")]
              [:a {:class (stl/css :link)
                   :href guide-uri
                   :target "_blank"
                   :rel "noopener"}
               (tr "ds.mcp-tour.read-guide")]
              [:button {:type "button"
                        :class (stl/css :global/ds-coach-skip :skip)
                        :on-click on-end}
               (tr "ds.mcp-tour.finish")]]
             [:button {:type "button"
                       :class (stl/css :global/ds-coach-skip :skip)
                       :on-click on-end}
              (tr "ds.mcp-tour.skip")])]]]))))
