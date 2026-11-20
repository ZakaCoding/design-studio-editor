;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.craft.state
  "State of the native Craft dock, shared by the dock, its toggle, the
  quick prompt and any layout that needs to know whether Craft is open
  (F3-A: the Layers rail collapses at 1280–1599 px while it is).

  The Craft panel itself is Studio's (`/studio/shell/loader.js` loads
  `ds-craft.js`). The two sides talk through the DOM only; the contract
  (\"Craft dock v1\") is:

  - `<html data-ds-craft-dock=\"1\">`: this editor has a native dock, so
    `ds-craft.js` adds no launcher of its own.
  - `[data-ds-craft-mount]`: the element `ds-craft.js` puts its frame in.
    `data-open` (\"true\" | \"false\") opens and collapses the panel,
    `data-ds-page-name` is the current page's name.
  - `window` event `ds-craft-dock:change`: the mount appeared or went.
  - Events `ds-craft.js` sends on the mount: `ds-craft:attached` (it took
    the mount), `ds-craft:collapse` and `ds-craft:open` (the panel asks to
    be collapsed or opened), `ds-craft:busy` (`detail.busy`).
  - Cancelable event `ds-craft:prompt` (`detail.text`) on the mount: a
    quick prompt for Craft. `ds-craft.js` cancels it when it took it."
  (:require
   [app.main.ui.studio.craft.rules :as rules]))

(def mount-selector "[data-ds-craft-mount]")
(def toggle-selector "[data-ds-craft-toggle]")

(defonce ^{:doc "True while the dock is open."} open? (atom false))
(defonce ^{:doc "True while Craft is working."} busy? (atom false))

;; A quick prompt Craft has not taken yet (its script may still be loading).
(defonce ^:private pending (atom nil))

(defn open!
  []
  (reset! open? true))

(defn close!
  []
  (reset! pending nil)
  (reset! open? false))

(defn toggle!
  []
  (if @open? (close!) (open!)))

(defn flush-prompt!
  "Hand the waiting quick prompt to Craft, if it is there to take it."
  []
  (when-let [text @pending]
    (when-let [mount (.querySelector js/document mount-selector)]
      (let [event (js/CustomEvent. "ds-craft:prompt"
                                   #js {:cancelable true
                                        :detail #js {:text text}})]
        ;; `dispatchEvent` answers false when a listener cancelled the
        ;; event, which is how ds-craft.js says it took the prompt.
        (when-not (.dispatchEvent mount event)
          (reset! pending nil))))))

(defn request-quick-prompt!
  "Ask the quick prompt to open under the selection, as Ctrl/Cmd+Enter
  does (used by the right-click menu)."
  []
  (.dispatchEvent js/window (js/CustomEvent. "ds-craft:quick-open")))

(defn send-prompt!
  "Open the dock and send `text` to Craft. Returns true when there was
  something to send."
  [text]
  (if-let [text (rules/clean-prompt text)]
    (do
      (reset! pending text)
      (open!)
      (flush-prompt!)
      true)
    false))
