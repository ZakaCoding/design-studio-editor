;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.craft.dock
  "Native Craft dock (F1 · 05): a 440 px panel of its own beside the design
  panel. It is never a tab and never replaces Layers or the Inspector. The
  dock is only the container and the open/close control; the Craft panel
  inside it is Studio's, framed by `ds-craft.js` (see
  `app.main.ui.studio.craft.state` for the contract between the two)."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.refs :as refs]
   [app.main.ui.studio.craft.rules :as rules]
   [app.main.ui.studio.craft.state :as state]
   [app.main.ui.studio.ds.button :refer [button*]]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def ^:private dock-id "ds-craft-dock")
(def ^:private right-selector "[data-testid=\"right-sidebar\"]")

(defn- stored-width
  []
  (try
    (let [value (js/Number (.getItem js/localStorage rules/storage-key))]
      (if (and (js/isFinite value) (>= value (:min rules/width)))
        value
        (:base rules/width)))
    (catch :default _
      (:base rules/width))))

(defn- store-width!
  [value]
  (try
    (.setItem js/localStorage rules/storage-key (str (js/Math.round value)))
    (catch :default _
      ;; Storage blocked: this tab still keeps the width.
      nil)))

(defn- side-width
  "Width of the design panel, or 0 when it is hidden."
  []
  (let [panel (.querySelector js/document right-selector)
        width (some-> panel (.getBoundingClientRect) (.-width))]
    (if (and (number? width) (< width (/ (.-innerWidth js/window) 2)))
      width
      0)))

(defn- focus-toggle!
  []
  (some-> (.querySelector js/document state/toggle-selector)
          (.focus #js {:preventScroll true})))

(mf/defc craft-toggle*
  "The control that opens and collapses the Craft dock. The dock shows one
  of its own at the bottom of the canvas; a layout that has a place for
  it (the F3 top bar) mounts this one and passes `toggle \"none\"` to
  `craft-dock*`. Props: `class` (placement only)."
  [{:keys [class]}]
  (let [open? (mf/deref state/open?)
        busy? (mf/deref state/busy?)]
    [:> button* {:variant "secondary"
                 :class class
                 :aria-expanded open?
                 :aria-controls dock-id
                 :data-ds-craft-toggle "1"
                 :data-testid "ds-craft-toggle"
                 :on-click state/toggle!}
     [:span {:class (stl/css-case :mark true :mark-busy busy?)
             :aria-hidden true}]
     (if busy?
       (tr "ds.craft.dock.busy")
       (tr "ds.craft.dock.open"))]))

(mf/defc craft-dock*
  "Props: `toggle` \"floating\" (default: the dock shows its own control
  while it is collapsed) | \"none\" (the layout mounts `craft-toggle*`)."
  [{:keys [toggle]}]
  (let [open?     (mf/deref state/open?)
        page      (mf/deref refs/workspace-page)
        layout    (mf/deref refs/workspace-layout)
        page-name (:name page)

        view*     (mf/use-state #(.-innerWidth js/window))
        side*     (mf/use-state 0)
        chosen*   (mf/use-state stored-width)
        sizing*   (mf/use-state false)
        attached* (mf/use-state false)
        late*     (mf/use-state false)

        view      (deref view*)
        side      (deref side*)
        mode      (rules/mode view)
        inset     (rules/inset mode side)
        limits    (rules/bounds view inset)
        shown     (rules/clamp-width (deref chosen*) view inset)

        mount-ref (mf/use-ref nil)
        drag-ref  (mf/use-ref nil)

        on-pointer-down
        (mf/use-fn
         (mf/deps shown)
         (fn [event]
           (when (or (not= "mouse" (.-pointerType event)) (zero? (.-button event)))
             (.setPointerCapture (.-currentTarget event) (.-pointerId event))
             (mf/set-ref-val! drag-ref #js {:x (.-clientX event) :w shown :moved false})
             (reset! sizing* true))))

        on-pointer-move
        (mf/use-fn
         (fn [event]
           (when-let [drag (mf/ref-val drag-ref)]
             (let [dx (- (unchecked-get drag "x") (.-clientX event))]
               (when (> (js/Math.abs dx) 2)
                 (unchecked-set drag "moved" true))
               (reset! chosen* (+ (unchecked-get drag "w") dx))))))

        on-pointer-up
        (mf/use-fn
         (mf/deps shown)
         (fn [_]
           (when-let [drag (mf/ref-val drag-ref)]
             (mf/set-ref-val! drag-ref nil)
             (reset! sizing* false)
             ;; What fits is what is remembered, as before.
             (reset! chosen* shown)
             (when (unchecked-get drag "moved")
               (store-width! shown)))))

        on-resize-key
        (mf/use-fn
         (mf/deps shown limits)
         (fn [event]
           (when-not (or (.-altKey event) (.-ctrlKey event) (.-metaKey event))
             (when-let [next (rules/key-width (.-key event) (.-shiftKey event) shown limits)]
               (.preventDefault event)
               (reset! chosen* next)
               (store-width! next)))
           ;; The arrow keys never reach the canvas shortcuts from here.
           (.stopPropagation event)))]

    ;; The window's width decides how the dock sits.
    (mf/with-effect []
      (let [on-resize #(reset! view* (.-innerWidth js/window))
            on-storage (fn [event]
                         (when (= (.-key event) rules/storage-key)
                           (reset! chosen* (stored-width))))]
        (.addEventListener js/window "resize" on-resize)
        (.addEventListener js/window "storage" on-storage)
        (fn []
          (.removeEventListener js/window "resize" on-resize)
          (.removeEventListener js/window "storage" on-storage))))

    ;; Follow the design panel: it is resizable, wider in Inspect, and
    ;; hidden with the rest of the interface.
    (mf/with-effect [page-name layout]
      (let [measure  #(reset! side* (side-width))
            panel    (.querySelector js/document right-selector)
            observer (when (some? panel) (js/ResizeObserver. measure))
            frame    (js/requestAnimationFrame measure)]
        (when (some? observer)
          (.observe observer panel))
        (fn []
          (js/cancelAnimationFrame frame)
          (when (some? observer)
            (.disconnect observer)))))

    ;; Tell ds-craft.js that the mount is here, and listen to what the
    ;; panel asks for.
    (mf/with-effect []
      (let [mount       (mf/ref-val mount-ref)
            announce    #(.dispatchEvent js/window (js/CustomEvent. "ds-craft-dock:change"))
            on-attached (fn [_]
                          (reset! attached* true)
                          (state/flush-prompt!))
            on-collapse (fn [_]
                          (state/close!)
                          ;; After the render that shows the toggle again.
                          (js/setTimeout focus-toggle! 0))
            on-open     (fn [_] (state/open!))
            on-busy     (fn [event]
                          (reset! state/busy? (true? (some-> (.-detail event) (unchecked-get "busy")))))]
        (.addEventListener mount "ds-craft:attached" on-attached)
        (.addEventListener mount "ds-craft:collapse" on-collapse)
        (.addEventListener mount "ds-craft:open" on-open)
        (.addEventListener mount "ds-craft:busy" on-busy)
        (announce)
        (fn []
          (.removeEventListener mount "ds-craft:attached" on-attached)
          (.removeEventListener mount "ds-craft:collapse" on-collapse)
          (.removeEventListener mount "ds-craft:open" on-open)
          (.removeEventListener mount "ds-craft:busy" on-busy)
          (reset! state/busy? false)
          (state/close!)
          ;; Once the mount has left the document.
          (js/setTimeout announce 0))))

    ;; Studio's script may be slow, or away: say so instead of an empty dock.
    (mf/with-effect [open? (deref attached*)]
      (when (and open? (not (deref attached*)))
        (let [timer (js/setTimeout #(reset! late* true) 8000)]
          #(js/clearTimeout timer))))

    ;; What other layouts and the touch buttons read: whether Craft is
    ;; open, how it sits, and how far the canvas' top-right buttons move.
    (mf/with-effect [open? mode shown inset view side]
      (let [root (.-documentElement js/document)]
        (if (and open? (not= mode :sheet))
          (let [canvas-right (- view side)
                dock-left    (- view inset shown)]
            (.setAttribute root "data-ds-craft-open" (name mode))
            (.setProperty (.-style root) "--ds-craft-shift"
                          (str (rules/touch-shift canvas-right dock-left) "px")))
          (do
            (.removeAttribute root "data-ds-craft-open")
            (.removeProperty (.-style root) "--ds-craft-shift")))
        (fn []
          (.removeAttribute root "data-ds-craft-open")
          (.removeProperty (.-style root) "--ds-craft-shift"))))

    [:*
     [:aside {:id dock-id
              :class (stl/css-case :dock true
                                   :dock-overlay (= mode :overlay)
                                   :dock-sheet (= mode :sheet)
                                   :dock-sizing (deref sizing*))
              :style {"--ds-craft-dock-w" (str shown "px")
                      "--ds-craft-dock-right" (str inset "px")}
              :hidden (not open?)
              :aria-label (tr "ds.craft.dock.label")
              :data-testid "ds-craft-dock"}
      [:div {:class (stl/css :resize)
             :role "separator"
             :tab-index 0
             :aria-orientation "vertical"
             :aria-controls dock-id
             :aria-label (tr "ds.craft.dock.resize")
             :aria-valuemin (:min limits)
             :aria-valuemax (:max limits)
             :aria-valuenow shown
             :aria-valuetext (tr "ds.craft.dock.width" shown)
             :on-pointer-down on-pointer-down
             :on-pointer-move on-pointer-move
             :on-pointer-up on-pointer-up
             :on-pointer-cancel on-pointer-up
             :on-key-down on-resize-key}]
      (when (deref sizing*)
        [:span {:class (stl/css :size) :aria-hidden true}
         (tr "ds.craft.dock.width" shown)])
      ;; Shown until ds-craft.js has put the panel in the mount.
      (when-not (deref attached*)
        [:p {:class (stl/css :waiting) :role "status"}
         (if (deref late*)
           (tr "ds.craft.dock.unavailable")
           (tr "ds.craft.dock.waiting"))])
      [:div {:class (stl/css :mount)
             :ref mount-ref
             :data-ds-craft-mount "1"
             :data-open (if open? "true" "false")
             :data-ds-page-name page-name}]]
     (when (and (not open?) (not= toggle "none"))
       [:div {:class (stl/css :floating)
              :style {"--ds-craft-dock-right" (str (+ (rules/inset :dock side) 4) "px")}}
        [:> craft-toggle*]])]))
