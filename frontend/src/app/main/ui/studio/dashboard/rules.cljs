;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard.rules
  "The decisions of the Design Studio dashboard, as pure functions: which
  MCP key the \"Connect AI\" flow may reuse, what the card shows, how a
  refusal reads, and when the home is the empty first-design page.
  Tested in `frontend-tests.studio.dashboard-rules-test`."
  (:require
   [app.common.data :as d]
   [app.common.time :as ct]
   [cuerdas.core :as str]))

;; --- Connect AI

(defn usable-key
  "The key of the first MCP access token of `tokens` that has not expired
  at `now`, or nil."
  [tokens now]
  (some (fn [{:keys [type token expires-at]}]
          (when (and (= "mcp" type)
                     (string? token)
                     (not (str/blank? token))
                     (or (nil? expires-at)
                         (ct/is-after? expires-at now)))
            token))
        tokens))

(defn assistant-uri
  "The address a member adds to their AI assistant. It carries no secret:
  the assistant signs in through Studio."
  [origin]
  (str (str/rtrim (str origin) "/") "/studio/mcp"))

(defn card-mode
  "What the dashboard shows.

  - `:hidden`    Studio can't say yet, MCP is off here, or no AI access
  - `:card`      the two-step card, step 1 open
  - `:pairing`   the two-step card, step 1 done, step 2 open
  - `:connected` the slim status bar
  - `:later`     the slim reminder, after \"Not now\""
  [{:keys [enabled status activated dismissed]}]
  (cond
    (not enabled)                 :hidden
    (not (:ai-access status))     :hidden
    activated                     :pairing
    (:connected status)           :connected
    dismissed                     :later
    :else                         :card))

(defn failure
  "Why step 1 did not finish, from Studio's answer."
  [status reason]
  (cond
    (= 401 status)                        :session
    (= "account-changed" reason)          :session
    (and (= 403 status) (some? reason))   :no-access
    (= 409 status)                        :busy
    (= 429 status)                        :busy
    :else                                 :failed))

;; --- Home

(defn drafts
  "The Drafts project of the team on screen."
  [projects]
  (d/seek :is-default (remove :deleted-at projects)))

(defn empty-home?
  "True when the team has nothing yet: no project besides Drafts and no
  file in it, and the member may create one. A viewer keeps Penpot's own
  page, which has nothing to offer them to click."
  [projects can-edit]
  (let [projects (remove :deleted-at projects)
        drafts   (d/seek :is-default projects)]
    (boolean
     (and can-edit
          (some? drafts)
          (every? :is-default projects)
          (zero? (or (:count drafts) 0))))))

(defn first-name
  "What the greeting calls the member, or nil."
  [profile]
  (let [fullname (some-> (:fullname profile) str/trim)]
    (when-not (str/blank? fullname)
      (first (str/split fullname #"\s+")))))
