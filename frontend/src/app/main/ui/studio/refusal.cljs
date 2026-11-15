;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.refusal
  "Design Studio guard refusals.

  The proxy in front of the API asks Studio before commands that grow
  storage, send mail or export. When Studio refuses, the proxy answers in
  the backend's place with a small JSON body, `{\"type\": \"restriction\",
  \"code\": \"<one of ours>\"}`, on status 413, 429, 503 or 507. The backend
  never wrote that answer, so `app.main.repo` would read it as a generic
  failure of that status.

  This namespace has no UI and no DOM dependency: `app.main.repo` requires
  it, and that namespace is also loaded in the worker."
  (:require
   [cuerdas.core :as str]))

(def codes
  #{:storage-limit-reached
    :too-many-uploads
    :storage-unavailable
    :invite-limit-reached
    :too-many-recovery-requests
    :guard-unavailable})

(def ^:private statuses
  #{413 429 503 507})

(defn code?
  [code]
  (contains? codes code))

(defn- thumbnail?
  "Thumbnails are refused quietly, because nobody asked for one: their
  answer keeps going through the stock handling."
  [uri]
  (and (string? uri)
       (str/includes? (str/lower uri) "thumbnail")))

(defn response->code
  "The refusal code of a guard answer, nil for any other response."
  [{:keys [status body uri]}]
  (when (and (contains? statuses status)
             (string? body)
             (not (thumbnail? uri)))
    (let [data (try
                 (js/JSON.parse body)
                 (catch :default _ nil))]
      (when (and (object? data)
                 (= "restriction" (unchecked-get data "type")))
        (let [code (unchecked-get data "code")
              code (when (string? code) (keyword code))]
          (when (code? code)
            code))))))

;; The card that explains a refusal lives in the UI
;; (`app.main.ui.studio.refusal-card`), which registers itself here. Without a
;; listener (the worker, tests) announcing is a no-op.

(defonce ^:private listener (atom nil))

(defn listen!
  "Register `f` (code -> any) as the refusal listener; nil removes it."
  [f]
  (reset! listener f))

(defn announce!
  [code]
  (when-let [f @listener]
    (when (code? code)
      (f code)))
  nil)
