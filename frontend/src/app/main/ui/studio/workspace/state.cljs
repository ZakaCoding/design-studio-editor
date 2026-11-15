;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.workspace.state
  "Width rules and shared state of the Design Studio workspace chrome (F3).

  Width rules (craft placement research, 9 Nov; board F1 · 06):

    1600 px and wider   :wide    every panel is open
    1280 to 1599 px     :medium  Layers collapses to a rail
    below 1280 px       :narrow  Layers stays a rail; Craft overlays the canvas

  The Layers rail is upstream's `:collapse-left-sidebar` layout flag, so every
  upstream function that reads or toggles that flag keeps working. The width
  rule only sets the flag when the window crosses a breakpoint; in between,
  the person's own choice stands.

  `craft-open*` is written by the Craft dock (F3-B) through `set-craft-open!`
  and read by the shell, which mirrors it on <body> as `data-ds-craft`."
  (:require
   [okulary.core :as l]
   [rumext.v2 :as mf]))

(def ^:const wide-min-width 1600)
(def ^:const medium-min-width 1280)

(defn width-class
  "Width class of a window that is `width` CSS px wide."
  [width]
  (cond
    (>= width wide-min-width)   :wide
    (>= width medium-min-width) :medium
    :else                       :narrow))

(defn layers-action
  "What the width rule does to the Layers panel when the width class changes
  `from` (nil on the first run) `to` the new one.

  `collapsed?` is the current `:collapse-left-sidebar` flag and `auto?` says
  whether the rule itself collapsed the panel last. Returns `:collapse`,
  `:expand` or nil (leave it as the person set it)."
  [{:keys [from to collapsed? auto?]}]
  (cond
    (= from to)
    nil

    ;; Entering the rail widths, from a wide window or on first load.
    (and (not= to :wide)
         (or (nil? from) (= from :wide))
         (not collapsed?))
    :collapse

    ;; Back to a wide window: reopen only what the rule closed.
    (and (= to :wide) (some? from) collapsed? auto?)
    :expand

    :else
    nil))

;; --- Craft dock state (owned by F3-B, mirrored by the shell)

(defonce craft-open* (l/atom false))

(defn set-craft-open!
  "Called by the Craft dock when it opens or closes."
  [open?]
  (reset! craft-open* (boolean open?)))

;; --- Hook

(defn- window-width
  []
  (.-innerWidth js/window))

(defn use-width-class
  "Current width class of the window; re-renders when it changes."
  []
  (let [class* (mf/use-state #(width-class (window-width)))]
    (mf/with-effect []
      (let [on-resize #(reset! class* (width-class (window-width)))]
        (.addEventListener js/window "resize" on-resize)
        (on-resize)
        (fn []
          (.removeEventListener js/window "resize" on-resize))))
    (deref class*)))
