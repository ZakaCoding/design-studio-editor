;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.craft.rules
  "Pure rules of the Craft dock (F1 · 05) and of the quick prompt at the
  selection (F1 · 06): where the dock sits for a window width, how wide it
  may be, and where the prompt goes for a selection. No DOM and no state,
  so the unit tests run them as they are.")

;; --- Dock

(def width
  "Dock width in px. The designer may change it between `:min` and `:max`
  (Craft placement research, rule 6); `:base` is the F1 · 05 width."
  {:min 360 :base 440 :max 560 :step 16 :big 64})

(def edge
  "Gap between the dock and what is next to it (`space.panel-edge`)."
  12)

(def storage-key
  "Same key the injected panel used, so a remembered width carries over."
  "ds-craft:width")

(defn mode
  "How the dock sits in a window `view` px wide (F1 · 06 width rules):
  `:dock` beside the design panel from 1280 px, `:overlay` on top of the
  design panel below that, `:sheet` (bottom sheet) on a phone."
  [view]
  (cond
    (< view 720)  :sheet
    (< view 1280) :overlay
    :else         :dock))

(defn inset
  "Distance in px from the window's right edge to the dock's, where `side`
  is the width of the design panel."
  [mode side]
  (if (= mode :dock)
    (+ side edge)
    edge))

(defn bounds
  "Narrowest and widest the dock may be in a window `view` px wide: never
  wider than the room left of `inset` minus one more edge gap, and never
  under the minimum."
  [view inset]
  (let [lo (:min width)
        hi (min (:max width) (- view inset edge))]
    {:min lo :max (max lo hi)}))

(defn clamp-width
  "The width shown for the width the designer chose. Anything that is not
  a number is the base width."
  [chosen view inset]
  (let [{lo :min hi :max} (bounds view inset)
        chosen (if (and (number? chosen) (js/isFinite chosen)) chosen (:base width))]
    (js/Math.round (min hi (max lo chosen)))))

(defn key-width
  "The width after a key on the resize handle, or nil for other keys. The
  handle is the dock's left edge, so Left (and Up) widens and Right (and
  Down) narrows; Shift takes bigger steps; Home and End go to the limits."
  [key shift? shown {lo :min hi :max}]
  (let [step (if shift? (:big width) (:step width))]
    (case key
      ("ArrowLeft" "ArrowUp")    (min hi (+ shown step))
      ("ArrowRight" "ArrowDown") (max lo (- shown step))
      "Home"                     lo
      "End"                      hi
      nil)))

(defn touch-shift
  "How far (px) the touch buttons at the canvas' top right move left so
  the open dock does not cover them, keeping 128 px of them on screen.
  `canvas-right` is the x of the canvas' right edge, `dock-left` the x of
  the dock's left edge."
  [canvas-right dock-left]
  (min (max 0 (+ (- canvas-right dock-left) 8))
       (max 0 (- canvas-right 128))))

;; --- Quick prompt

(def prompt-size
  "Box of the quick prompt in px (board F1 · 06): the 40 px prompt and the
  key hint under it."
  {:width 336 :height 64 :gap 6 :margin 12})

(def prompt-max
  "Longest quick prompt, in characters."
  2000)

(defn quick-key?
  "True for Ctrl+Enter or Cmd+Enter alone. Ctrl/Cmd+K stays Penpot's
  create-component shortcut."
  [{:keys [key ctrl? meta? shift? alt? repeat?]}]
  (and (= key "Enter")
       (or (true? ctrl?) (true? meta?))
       (not shift?)
       (not alt?)
       (not repeat?)))

(defn anchor
  "Top-left corner (client px) of the quick prompt for a selection whose
  box on screen is `rect` ({:x :y :width :height}, client px) in a window
  of `view` ({:width :height}). It goes under the selection, from its left
  edge (board F1 · 06); above it when there is no room below; and always
  fully inside the window, `:margin` px from its edges."
  [rect view]
  (let [{w :width h :height gap :gap margin :margin} prompt-size
        clamp  (fn [v lo hi] (max lo (min v (max lo hi))))
        left   (:x rect)
        below  (+ (:y rect) (:height rect) gap)
        above  (- (:y rect) gap h)
        bottom (- (:height view) margin h)
        top    (cond
                 (<= below bottom) below
                 (>= above margin) above
                 :else             bottom)]
    {:left (js/Math.round (clamp left margin (- (:width view) margin w)))
     :top  (js/Math.round (clamp top margin bottom))}))

(def chip-size
  "Box of the \"Change with Craft\" chip at the selection in px (board
  F1 · 06, step 1). The width is the Indonesian chip's; it only keeps the
  chip inside the canvas."
  {:width 236 :height 32 :gap 8 :margin 12})

(defn chip-anchor
  "Top-left corner (px) of the chip for a selection whose box is `rect` in
  a canvas area of `view` ({:width :height}), both in the same px. Over
  the selection, from its left edge, `lift` px higher when something is
  drawn there (a board's name); under the selection when there is no room
  above. Nil when the selection is not in view at all."
  [rect view lift]
  (let [{w :width h :height gap :gap margin :margin} chip-size
        clamp (fn [v lo hi] (max lo (min v (max lo hi))))
        above (- (:y rect) gap lift h)
        below (+ (:y rect) (:height rect) gap)
        top   (if (>= above margin) above below)]
    (when (and (< (:x rect) (:width view))
               (> (+ (:x rect) (:width rect)) 0)
               (< (:y rect) (:height view))
               (> (+ (:y rect) (:height rect)) 0))
      {:left (js/Math.round (clamp (:x rect) margin (- (:width view) margin w)))
       :top  (js/Math.round (clamp top margin (- (:height view) margin h)))})))

(defn clean-prompt
  "The text that is sent: trimmed and cut to `prompt-max`; nil when empty."
  [text]
  (let [text (.trim (str text))]
    (when (pos? (.-length text))
      (.slice text 0 prompt-max))))
