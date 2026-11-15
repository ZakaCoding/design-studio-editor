;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns app.plugins.persistence-guard
  "Save confirmation for the integrated MCP plugin.

  A plugin write only changes the local file; the server learns about it
  when the persistence queue sends it. Without this guard an MCP task
  reports success as soon as its script returns, even when the save then
  fails or never happens, and later tasks keep stacking edits on a queue
  that is not being saved.

  The guard does three things and nothing else:

  - `note-write!`, called from the plugin permission check, refuses an
    MCP write while saving has failed, is being retried or has stalled.
    Reads are never refused.
  - `checkpoint` and `settle!` bracket one MCP task: a task that wrote
    something reports success only after the existing persistence queue
    has acknowledged the save.
  - A save that fails, times out or cannot be attributed to the same
    file is reported as unconfirmed, and that uncertainty latches: later
    writes are refused until the editor is reopened.

  It never sends, repeats or discards a save itself, and it is not an
  authorization boundary: the plugin permission check still decides
  what a plugin may do.

  This namespace must stay free of `app.main.data.*` requires:
  `app.plugins.register` requires it, and the persistence namespace
  reaches `app.plugins.register` through `app.main.errors`. The caller
  (`app.main.data.workspace.mcp`) passes the persistence operations in."
  (:require
   [app.main.store :as st]))

;; Kept in sync with `app.plugins.register/mcp-plugin-id`, which cannot be
;; required from here (it requires this namespace).
(def mcp-plugin-id "96dfa740-005d-8020-8007-55ede24a2bae")

(def blocked
  "DS_SAVE_BLOCKED: Autosave failed or stopped making progress. Keep this tab open. Recover unsaved changes before reloading; inspect saved data before repeating an edit.")

(def unconfirmed
  "DS_SAVE_UNCONFIRMED: The edit may have completed, but saving was not confirmed. Keep this tab open. Inspect the canvas and saved data before repeating; do not reload or retry automatically.")

(def unavailable
  "DS_SAVE_UNAVAILABLE: The editor assets do not support save confirmation together. Keep existing changes open; reconnect only after they are saved or recovered.")

(def write-permissions
  #{"content:write" "library:write" "comment:write"})

(def runtime-ready-permission
  "Pseudo permission the plugins runtime reports once per plugin API it
  creates. It proves the runtime in use calls the write hook, so a stale
  cached runtime that would skip the hook cannot start an MCP task."
  "runtime-ready")

(def stall-timeout-ms
  "A queue that is pending or saving without progress for this long is
  treated as stalled (same threshold as the persistence stall report)."
  (* 5 60 1000))

(defn write-blocked?
  "True when the persistence state `pstate` does not allow a new MCP
  write at `now` (epoch ms).

  `:retrying` is a failed save that is resent with backoff; it refreshes
  `:last-progress-at` on every failure, so only the status shows it. After
  the backoff the queue re-enters `:saving` with a fresh
  `:last-progress-at` on every slow cycle; `:failing-since` is set on the
  first failure and only cleared when a save lands or the failed file is
  released, so it covers those attempts too."
  [pstate now]
  (let [{:keys [status last-progress-at stall-reported failing-since]} pstate]
    (boolean
     (or (= status :error)
         (= status :retrying)
         (some? failing-since)
         stall-reported
         (and (or (= status :pending) (= status :saving))
              (number? last-progress-at)
              (js/isFinite last-progress-at)
              (> (- now last-progress-at) stall-timeout-ms))))))

(defn create
  "Creates a guard. `state-fn` returns the current app state and `now-fn`
  the current time in epoch ms; tests replace them."
  ([] (create {}))
  ([{:keys [state-fn now-fn]}]
   {:state-fn      (or state-fn (fn [] @st/state))
    :now-fn        (or now-fn (fn [] (.now js/Date)))
    ;; Number of MCP writes let through; a task that did not move it only read.
    :epoch         (atom 0)
    :runtime-ready (atom false)
    :unconfirmed   (atom false)}))

(defonce ^{:doc "The guard of the running app."} instance
  (create))

(defn- file-id
  [{:keys [state-fn]}]
  (str (:current-file-id (state-fn))))

(defn- persistence-state
  [{:keys [state-fn]}]
  (:persistence (state-fn)))

(defn- assert-can-write!
  [{:keys [now-fn unconfirmed] :as guard}]
  (when (or @unconfirmed
            (write-blocked? (persistence-state guard) (now-fn)))
    (throw (js/Error. blocked))))

(defn note-write!
  "Called at the plugin permission boundary (`register/check-permission`
  and the plugins runtime `checkPermission`), which every plugin API
  write goes through, including shape setters and objects returned to
  callbacks. Throws `blocked` when the MCP plugin asks for a write
  permission while saving cannot be trusted. Any other plugin and any
  read permission pass untouched."
  ([plugin-id permission]
   (note-write! instance plugin-id permission))
  ([{:keys [epoch runtime-ready] :as guard} plugin-id permission]
   (when (= plugin-id mcp-plugin-id)
     (cond
       (= permission runtime-ready-permission)
       (reset! runtime-ready true)

       (contains? write-permissions permission)
       (do
         (assert-can-write! guard)
         (swap! epoch inc))))
   nil))

(defn checkpoint
  "Starts an MCP task: returns the JS object `settle!` needs to tell
  whether the task wrote anything and whether the file is still the same."
  ([] (checkpoint instance))
  ([{:keys [epoch runtime-ready] :as guard}]
   (when-not @runtime-ready
     (throw (js/Error. unavailable)))
   #js {:epoch @epoch :fileId (file-id guard)}))

(defn- same-file?
  [guard checkpoint]
  (and (some? checkpoint)
       (= (unchecked-get checkpoint "fileId") (file-id guard))))

(defn- wait-acknowledged
  "Promise that resolves when `wait-persisted!` reports a save and the file
  is still the one of `checkpoint`; rejects otherwise.

  `wait-persisted!` is `(fn [on-saved on-failed]) => dispose-fn`. It must
  call `on-saved` when the queue settles as saved and `on-failed` when
  saving fails, times out or the wait ends without an answer. It may call
  them before it returns."
  [guard checkpoint wait-persisted!]
  (js/Promise.
   (fn [resolve reject]
     (let [finished (volatile! false)
           dispose  (volatile! nil)
           finish   (fn [ok?]
                      (when-not @finished
                        (vreset! finished true)
                        (if (and ok? (same-file? guard checkpoint))
                          (resolve nil)
                          (reject (js/Error. unconfirmed)))
                        (when-let [f @dispose] (f))))]
       (vreset! dispose (wait-persisted! #(finish true) #(finish false)))
       ;; The wait answered before returning its disposer.
       (when @finished
         (when-let [f @dispose] (f)))))))

(defn settle!
  "Ends an MCP task started with `checkpoint`. Returns a promise that
  resolves when the task did not write, or when its writes were saved,
  and rejects with `unconfirmed` otherwise.

  `ops` holds the persistence operations of the caller:

  - `:force-persist!` `(fn [])` asks the existing queue to send what it
    holds now. It is called once and never again: a lost response has an
    unknown outcome, so a save is never replayed from here.
  - `:wait-persisted!` see `wait-acknowledged`.
  - `:warn!` `(fn [js-error])` shows the user that saving was not
    confirmed. It is skipped when the queue is already in `:error`,
    because that failure has been reported."
  ([checkpoint ops] (settle! instance checkpoint ops))
  ([{:keys [epoch] :as guard} checkpoint {:keys [force-persist! wait-persisted! warn!]}]
   (-> (js/Promise.
        (fn [resolve reject]
          (cond
            (not (same-file? guard checkpoint))
            (reject (js/Error. unconfirmed))

            ;; Nothing was written: reads stay available after a failed save.
            (= (unchecked-get checkpoint "epoch") @epoch)
            (resolve nil)

            :else
            ;; Let buffered commits reach the queue first, including the
            ;; writes of a script that then threw.
            (js/setTimeout
             (fn []
               (try
                 (if (same-file? guard checkpoint)
                   (do
                     (force-persist!)
                     (resolve (wait-acknowledged guard checkpoint wait-persisted!)))
                   (reject (js/Error. unconfirmed)))
                 (catch :default cause
                   (reject cause))))
             0))))
       (.catch
        (fn [_]
          ;; A timed out wait does not necessarily put the queue in `:error`.
          ;; Latch the uncertainty, so the next task cannot add edits to a
          ;; queue that is still saving just because the stall threshold
          ;; comes later.
          (reset! (:unconfirmed guard) true)
          (when (not= :error (:status (persistence-state guard)))
            ;; The queue may still be saving. Tell the user without changing
            ;; or discarding it; the tool error alone is not a UI warning.
            (try
              (warn! (js/Error. unconfirmed))
              (catch :default _
                ;; Reporting must never turn an uncertain save into success.
                nil)))
          (throw (js/Error. unconfirmed)))))))
