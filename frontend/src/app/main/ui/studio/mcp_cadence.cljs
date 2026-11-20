;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.mcp-cadence
  "Pure rules of the Design Studio MCP onboarding: when the workspace tip
  shows, and which step of the interactive guide is current. No DOM, no
  storage and no clock in here, so the rules are unit tested as they are.")

;; --- Workspace tip
;;
;; Show on open; again after `:reappear` within the same work session; first
;; thing on a new day. A manual close hides it for the rest of that work
;; session and day; "remind me tomorrow" snoozes it to the next day. It keeps
;; coming back while MCP is not connected and stops only once AI access is
;; connected. There is no permanent opt-out before that.

(def cadence
  {:settle      6000
   :visible-for 30000
   :reappear    (* 25 60 1000)})

(defn decide
  "Whether the tip shows now.

  `state` holds what the browser remembers and what Studio answered:
    :connected  an editor link is already stored in AI access
    :ai-access  the member can open AI access at all
    :dismissed  day (YYYY-MM-DD) of the last manual close, this work session
    :last-shown epoch ms of the last time it showed, this work session
    :snoozed    day of the last \"remind me tomorrow\", this browser
    :last-day   day it last showed, this browser

  Returns `{:show true :delay ms}`, `{:show false}`, or
  `{:show false :retry-in ms}` when it is only waiting for the reappear
  interval."
  [{:keys [connected ai-access dismissed snoozed last-day last-shown]} now today]
  (cond
    (or (true? connected)
        (not (true? ai-access))
        (= dismissed today)
        (= snoozed today))
    {:show false}

    (or (not= last-day today)
        (not (number? last-shown))
        (zero? last-shown))
    {:show true :delay (:settle cadence)}

    :else
    (let [wait (- (+ last-shown (:reappear cadence)) now)]
      (if (<= wait 0)
        {:show true :delay (:settle cadence)}
        {:show false :retry-in wait}))))

;; --- Interactive guide
;;
;; Two steps, the same two as the dashboard card and the AI access card: press
;; Connect in the editor, then paste the address in the AI assistant. The
;; guide opens Settings > Integrations, where the Connect card lives, and
;; points at its controls. The member performs every click; the guide only
;; points, so the current step is read from what is on screen.

(def steps
  [:connect :paste :done])

(def ^:private step-set (set steps))

(defn step?
  [step]
  (contains? step-set step))

(defn step-number
  "1-based position of `step`, as shown in \"Step n of 2\"."
  [step]
  (inc (.indexOf steps step)))

(defn advance
  "The step the guide is on, given the stored `step` and the screen:
    :integrations? the route is Settings > Integrations
    :connect?      the Connect button is on the page
    :address?      the assistant address is ready to copy (Connect is done)"
  [step {:keys [integrations? connect? address?]}]
  (cond
    (or (nil? step) (= step :done)) step
    (not integrations?)             step
    address?                        :paste
    connect?                        :connect
    :else                           step))
