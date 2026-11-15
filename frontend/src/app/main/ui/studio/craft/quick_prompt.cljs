;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.craft.quick-prompt
  "Quick prompt at the selection (F1 · 06): Ctrl/Cmd+Enter opens a small
  prompt under the selected layers and sends what is typed to Craft, in
  the same thread as the dock (\"two doors, one history\"). Ctrl/Cmd+K
  stays Penpot's create-component shortcut. It reads the selection only to
  place itself and to say what is selected; Craft reads the selection on
  its own side."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.geom.shapes :as gsh]
   [app.main.data.helpers :as dsh]
   [app.main.store :as st]
   [app.main.ui.studio.craft.rules :as rules]
   [app.main.ui.studio.craft.state :as state]
   [app.main.ui.studio.ds.button :refer [button*]]
   [app.main.ui.workspace.viewport.viewport-ref :as vref]
   [app.util.i18n :refer [tr]]
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
  "What is selected now: how many layers, the name of a single one and
  their box on screen (client px). Nil without a selection, or while the
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
       :rect  {:x      (+ (:x origin) (* zoom (- (:x rect) (:x vbox))))
               :y      (+ (:y origin) (* zoom (- (:y rect) (:y vbox))))
               :width  (* zoom (:width rect))
               :height (* zoom (:height rect))}})))

(mf/defc quick-prompt*
  []
  (let [box*      (mf/use-state nil)
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

        stop
        (mf/use-fn
         (fn [event]
           (.stopPropagation event)))]

    ;; Ctrl/Cmd+Enter anywhere in the workspace, except while writing text.
    (mf/with-effect []
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
                (if-let [selected (selection)]
                  (let [view {:width (.-innerWidth js/window) :height (.-innerHeight js/window)}]
                    (reset! box* (merge (rules/anchor (:rect selected) view)
                                        (select-keys selected [:count :name]))))
                  ;; Nothing selected: the other door, the dock itself.
                  (state/open!))))]
        (.addEventListener js/document "keydown" on-key true)
        (fn []
          (.removeEventListener js/document "keydown" on-key true))))

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

    (when (some? box)
      [:form {:class (stl/css :prompt)
              :ref form-ref
              :style {:left (str (:left box) "px")
                      :top (str (:top box) "px")}
              :role "dialog"
              :aria-label (tr "ds.craft.quick.label")
              :data-testid "ds-craft-quick"
              :on-submit on-submit
              :on-pointer-down stop
              :on-wheel stop
              :on-context-menu stop}
       [:p {:class (stl/css :context)}
        [:span {:class (stl/css :mark) :aria-hidden true}]
        [:span {:class (stl/css :context-text)}
         (if (some? (:name box))
           (:name box)
           (tr "ds.craft.quick.layers" (:count box)))]]
       [:textarea {:class (stl/css :input)
                   :ref input-ref
                   :rows 2
                   :max-length rules/prompt-max
                   :placeholder (tr "ds.craft.quick.placeholder")
                   :aria-label (tr "ds.craft.quick.placeholder")
                   :value text
                   :on-change on-change
                   :on-key-down on-key-down}]
       [:div {:class (stl/css :foot)}
        [:span {:class (stl/css :hint)} (tr "ds.craft.quick.hint")]
        [:> button* {:type "submit"
                     :size "sm"
                     :disabled (nil? (rules/clean-prompt text))}
         (tr "ds.craft.quick.send")]]])))
