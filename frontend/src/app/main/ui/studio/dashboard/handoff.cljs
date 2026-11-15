;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard.handoff
  "The \"Craft prompt hand-off\" between the dashboard and the workspace
  (F1 · 03 \"Mulai dari satu kalimat\"). Both sides use this tab's
  session storage, so nothing travels in a URL:

  - `ds-craft:prompt` = JSON `{prompt, fileId, at}`: the dashboard writes
    it before it opens the new file. Studio's Craft panel (`ds-craft.js`)
    reads it in the workspace of that file within five minutes, fills its
    box, does not send, and removes it.
  - `ds-craft:open` = \"1\": the native Craft dock opens when it is set,
    and removes it (`take-open-request!`, called by the dock)."
  (:require
   [app.main.ui.studio.dashboard.rules :as rules]))

(def ^:private prompt-key "ds-craft:prompt")
(def ^:private open-key "ds-craft:open")

(defn- session
  []
  (try
    (.-sessionStorage js/window)
    (catch :default _ nil)))

(defn leave!
  "Called by the dashboard right before it opens `file-id`: asks for the
  Craft dock to be open there and, when there is a prompt, leaves it for
  Craft's box."
  [prompt file-id]
  (try
    (when-let [storage (session)]
      (if-let [entry (rules/handoff prompt file-id (js/Date.now))]
        (.setItem storage prompt-key (js/JSON.stringify (clj->js entry)))
        (.removeItem storage prompt-key))
      (.setItem storage open-key "1"))
    (catch :default _
      ;; Storage blocked: the file still opens, with Craft closed.
      nil))
  nil)

(defn take-open-request!
  "True when the dashboard asked for Craft to be open; the request is
  used up."
  []
  (try
    (if-let [storage (session)]
      (let [asked? (some? (.getItem storage open-key))]
        (when asked?
          (.removeItem storage open-key))
        asked?)
      false)
    (catch :default _ false)))
