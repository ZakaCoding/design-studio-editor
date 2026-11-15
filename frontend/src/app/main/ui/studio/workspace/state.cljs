;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.workspace.state
  "Width rules of the Design Studio workspace chrome (F3).

  From the craft placement research (9 Nov) and board F1 · 06:

    1600 px and wider   :wide    every panel is open
    1280 to 1599 px     :medium  Layers is a 48 px rail while Craft is open
    below 1280 px       :narrow  the same, and Craft overlays the canvas

  The Layers rail is upstream's `:collapse-left-sidebar` layout flag, so every
  upstream function that reads or toggles that flag keeps working. The rule
  sets the flag only when the need for the rail starts or ends (Craft opens
  or closes, the window crosses 1600 px); in between, the person's own
  choice stands, and the rule reopens only what it closed itself.

  The Craft dock (F3-B) says that it is open on `<html data-ds-craft-open>`
  (its \"Craft dock v1\" contract). This namespace only reads that
  attribute, so neither package requires the other."
  (:require
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

(def ^:const rail-needs-craft?
  "The research says \"Layers becomes a rail while Craft is open\"; the day 2
  brief shortens it to \"1280 to 1599 px: Layers collapses\". True follows
  the research, so a laptop without Craft keeps today's layout. Set it to
  false to make the rail the default below 1600 px."
  true)

(defn rail?
  "True when the width rules ask for the Layers rail."
  [width-class craft-open?]
  (and (not= width-class :wide)
       (or (not rail-needs-craft?)
           (true? craft-open?))))

(defn layers-action
  "What the width rule does to the Layers panel when the need for the rail
  changes from `was-rail?` to `rail?`.

  `collapsed?` is the current `:collapse-left-sidebar` flag and `auto?` says
  whether the rule itself collapsed the panel last. Returns `:collapse`,
  `:expand` or nil (leave it as the person set it)."
  [{:keys [was-rail? rail? collapsed? auto?]}]
  (cond
    (= (boolean was-rail?) (boolean rail?))
    nil

    (and rail? (not collapsed?))
    :collapse

    ;; The need is over: reopen only what the rule closed.
    (and (not rail?) collapsed? auto?)
    :expand

    :else
    nil))

;; --- Hooks

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

(def ^:private craft-open-attr "data-ds-craft-open")

(defn- craft-open-now?
  []
  (.hasAttribute (.-documentElement js/document) craft-open-attr))

(defn use-craft-open
  "True while the Craft dock is open (`<html data-ds-craft-open>`, set by
  F3-B's dock). Always false while no dock is mounted."
  []
  (let [open* (mf/use-state craft-open-now?)]
    (mf/with-effect []
      (let [on-change #(reset! open* (craft-open-now?))
            observer  (js/MutationObserver. on-change)]
        (.observe observer
                  (.-documentElement js/document)
                  #js {:attributes true
                       :attributeFilter #js [craft-open-attr]})
        (on-change)
        (fn []
          (.disconnect observer))))
    (deref open*)))
