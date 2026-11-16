;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.craft.quick-prompt
  "Quick prompt at the selection (F1 · 06). While layers are selected a
  small chip over the selection says \"Change with Craft · Ctrl Enter\"
  (step 1); the chip or Ctrl/Cmd+Enter opens a one-line prompt under the
  selection (step 2), which sends what is typed to Craft, in the same
  thread as the dock (\"two doors, one history\"). Ctrl/Cmd+K stays Penpot's
  create-component shortcut. It reads the selection only to place itself;
  Craft reads the selection on its own side. The preview and \"Apply\" of
  step 3 are Craft's, in Studio's panel."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.geom.shapes :as gsh]
   [app.config :as cf]
   [app.main.data.helpers :as dsh]
   [app.main.refs :as refs]
   [app.main.store :as st]
   [app.main.ui.studio.craft.rules :as rules]
   [app.main.ui.studio.craft.state :as state]
   [app.main.ui.studio.workspace.glyphs :refer [glyph*]]
   [app.main.ui.workspace.viewport.viewport-ref :as vref]
   [app.util.i18n :refer [tr]]
   [okulary.core :as l]
   [rumext.v2 :as mf]))

(defn- editable?
  "True when typing in `node` is the member writing text."
  [node]
  (and (some? node)
       (or (contains? #{"INPUT" "TEXTAREA" "SELECT"} (.-tagName node))
           (true? (.-isContentEditable node)))))

(defn- read-only?
  []
  (true? (get-in @st/state [:workspace-global :read-only?])))

(defn- selection
  "What is selected now: how many layers, the name of a single one,
  whether that one is a board, and their box on screen (client px). Nil without a selection, or while the
  canvas is not there."
  []
  (let [state   @st/state
        local   (:workspace-local state)
        objects (dsh/lookup-page-objects state)
        shapes  (into [] (keep #(get objects %)) (:selected local))
        origin  @vref/viewport-brect
        vbox    (:vbox local)
        zoom    (or (:zoom local) 1)
        rect    (when (seq shapes) (gsh/shapes->rect shapes))]
    (when (and (some? rect) (some? origin) (some? vbox))
      {:count (count shapes)
       :name  (when (= 1 (count shapes)) (:name (first shapes)))
       ;; A board draws its name over its top left corner.
       :board? (and (= 1 (count shapes)) (= :frame (:type (first shapes))))
       :rect  {:x      (+ (:x origin) (* zoom (- (:x rect) (:x vbox))))
               :y      (+ (:y origin) (* zoom (- (:y rect) (:y vbox))))
               :width  (* zoom (:width rect))
               :height (* zoom (:height rect))}})))

(def ^:private overlay-id "ds-viewport-overlay")

(defn- frame
  "The box the prompt is placed in, in client px: the workspace shell's
  `viewport-overlay` mount point (the canvas area) when the prompt is
  mounted there, else the window."
  [overlay?]
  (let [rect (when overlay?
               (some-> (.getElementById js/document overlay-id) (.getBoundingClientRect)))]
    (if (some? rect)
      {:x (.-left rect) :y (.-top rect) :width (.-width rect) :height (.-height rect)}
      {:x 0 :y 0 :width (.-innerWidth js/window) :height (.-innerHeight js/window)})))

(defn- in-frame
  "`rect` (client px) in px from the top left corner of `view`."
  [rect view]
  (-> rect
      (update :x - (:x view))
      (update :y - (:y view))))

;; What moves the chip, and what hides it: the canvas' position and zoom,
;; a layer being moved or resized, text being edited.
(def ^:private ref:canvas
  (l/derived (fn [local]
               {:vbox (:vbox local)
                :zoom (:zoom local)
                :busy? (or (some? (:transform local))
                           (some? (:edition local)))})
             refs/workspace-local
             =))

(def ^:private keys-label
  (if (cf/check-platform? :macos) "\u2318 Enter" "Ctrl Enter"))

(mf/defc selection-chip*
  "Step 1 of F1 · 06: the chip over the selection. Props: `is-overlay`,
  `on-open`."
  {::mf/private true}
  [{:keys [is-overlay on-open]}]
  (let [selected (mf/deref refs/selected-shapes)
        objects  (mf/deref refs/workspace-page-objects)
        canvas   (mf/deref ref:canvas)
        drawtool (mf/deref refs/selected-drawing-tool)

        ;; Read when the selection, a layer or the canvas changed.
        current  (when (and (seq selected)
                            (some? objects)
                            (some? (:vbox canvas))
                            (not (:busy? canvas))
                            (nil? drawtool)
                            (not (read-only?)))
                   (selection))
        view     (frame is-overlay)
        place    (when (some? current)
                   (rules/chip-anchor (in-frame (:rect current) view)
                                      view
                                      (if (:board? current) 20 0)))

        stop
        (mf/use-fn
         (fn [event]
           (.stopPropagation event)))]

    (when (some? place)
      [:button {:class (stl/css-case :chip true
                                     :chip-overlay is-overlay)
                :type "button"
                :style {:left (str (:left place) "px")
                        :top (str (:top place) "px")}
                :data-testid "ds-craft-chip"
                :on-click on-open
                :on-pointer-down stop
                :on-context-menu stop}
       [:> glyph* {:id "craft"
                   :size 16
                   :class (stl/css :chip-mark)}]
       [:span {:class (stl/css :chip-label)}
        (tr "ds.craft.quick.chip")]
       [:span {:class (stl/css :chip-divider)
               :aria-hidden true}]
       [:kbd {:class (stl/css :chip-keys)}
        keys-label]])))

(mf/defc quick-prompt*
  "Props: `placement` \"floating\" (default: fixed, in window px) |
  \"overlay\" (inside the workspace shell's `viewport-overlay` mount point:
  absolute, in px from the top left corner of the canvas area)."
  [{:keys [placement]}]
  (let [overlay?  (= placement "overlay")
        box*      (mf/use-state nil)
        text*     (mf/use-state "")
        box       (deref box*)
        text      (deref text*)
        form-ref  (mf/use-ref nil)
        input-ref (mf/use-ref nil)

        close
        (mf/use-fn
         (fn []
           (reset! box* nil)
           (reset! text* "")))

        on-submit
        (mf/use-fn
         (mf/deps text)
         (fn [event]
           (.preventDefault event)
           (when (state/send-prompt! text)
             (close))))

        on-key-down
        (mf/use-fn
         (mf/deps text)
         (fn [event]
           (let [key (.-key event)]
             (cond
               (= key "Escape")
               (do (.preventDefault event)
                   (close))

               ;; Enter sends (also with Ctrl or Cmd); Shift+Enter is a new line.
               (and (= key "Enter") (not (.-shiftKey event)) (not (.-isComposing (.-nativeEvent event))))
               (do (.preventDefault event)
                   (when (state/send-prompt! text)
                     (close)))))
           ;; Typing here never reaches the canvas shortcuts.
           (.stopPropagation event)))

        on-change
        (mf/use-fn
         (fn [event]
           (reset! text* (.-value (.-target event)))))

        ;; Opens the prompt at the selection; without one, the dock itself
        ;; (the other door).
        open
        (mf/use-fn
         (mf/deps overlay?)
         (fn []
           (if-let [selected (selection)]
             (let [view (frame overlay?)]
               (reset! box* (merge (rules/anchor (in-frame (:rect selected) view) view)
                                   (select-keys selected [:count :name]))))
             (state/open!))))

        stop
        (mf/use-fn
         (fn [event]
           (.stopPropagation event)))]

    ;; Ctrl/Cmd+Enter anywhere in the workspace, except while writing text.
    ;; The right-click menu asks for the same thing with `ds-craft:quick-open`.
    (mf/with-effect [open]
      (let [on-key
            (fn [event]
              (when (and (rules/quick-key? {:key     (.-key event)
                                            :ctrl?   (.-ctrlKey event)
                                            :meta?   (.-metaKey event)
                                            :shift?  (.-shiftKey event)
                                            :alt?    (.-altKey event)
                                            :repeat? (.-repeat event)})
                         (not (editable? (.-target event)))
                         (not (read-only?)))
                (.preventDefault event)
                (.stopPropagation event)
                (open)))

            on-request
            (fn []
              (when-not (read-only?)
                (open)))]
        (.addEventListener js/document "keydown" on-key true)
        (.addEventListener js/window "ds-craft:quick-open" on-request)
        (fn []
          (.removeEventListener js/document "keydown" on-key true)
          (.removeEventListener js/window "ds-craft:quick-open" on-request))))

    ;; While it is open: focus the box; a press outside or a resized
    ;; window closes it.
    (mf/with-effect [(some? box)]
      (when (some? box)
        (let [on-press
              (fn [event]
                (let [form (mf/ref-val form-ref)]
                  (when-not (and (some? form) (.contains form (.-target event)))
                    (close))))]
          (some-> (mf/ref-val input-ref) (.focus #js {:preventScroll true}))
          (.addEventListener js/document "pointerdown" on-press true)
          (.addEventListener js/window "resize" close)
          (fn []
            (.removeEventListener js/document "pointerdown" on-press true)
            (.removeEventListener js/window "resize" close)))))

    (if (some? box)
      [:form {:class (stl/css-case :prompt true
                                   :prompt-overlay overlay?)
              :ref form-ref
              :style {:left (str (:left box) "px")
                      :top (str (:top box) "px")}
              :role "dialog"
              :aria-label (if (some? (:name box))
                            (str (tr "ds.craft.quick.label") ": " (:name box))
                            (str (tr "ds.craft.quick.label") ": " (tr "ds.craft.quick.layers" (:count box))))
              :data-testid "ds-craft-quick"
              :on-submit on-submit
              :on-pointer-down stop
              :on-wheel stop
              :on-context-menu stop}
       [:div {:class (stl/css :field)}
        [:> glyph* {:id "craft"
                    :size 16
                    :class (stl/css :field-mark)}]
        [:textarea {:class (stl/css :input)
                    :ref input-ref
                    :rows 1
                    :max-length rules/prompt-max
                    :placeholder (tr "ds.craft.quick.placeholder")
                    :aria-label (tr "ds.craft.quick.placeholder")
                    :value text
                    :on-change on-change
                    :on-key-down on-key-down}]
        [:button {:class (stl/css :send)
                  :type "submit"
                  :aria-label (tr "ds.craft.quick.send")
                  :title (tr "ds.craft.quick.send")
                  :disabled (nil? (rules/clean-prompt text))}
         [:svg {:class (stl/css :send-icon)
                :width 14
                :height 14
                :view-box "0 0 14 14"
                :aria-hidden true
                :focusable "false"}
          [:path {:d "M7 11.2V2.8M3.5 6.3L7 2.8l3.5 3.5"}]]]]
       [:p {:class (stl/css :hint)}
        (tr "ds.craft.quick.hint")]]

      [:> selection-chip* {:is-overlay overlay?
                           :on-open open}])))
