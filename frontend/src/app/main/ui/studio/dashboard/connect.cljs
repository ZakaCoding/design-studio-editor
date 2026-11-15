;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard.connect
  "The two-step \"Connect AI\" flow of the dashboard card (F1 · 03).

  Step 1 is one click. It does what a member used to do by hand in
  Settings > Integrations and in /studio:

  1. reuse the account's valid MCP key, or create one, with the same RPC
     commands the Integrations page uses (a valid key is never replaced:
     creating one deletes the old key and unpairs every client using it);
  2. hand the key to Studio once, in the body of a same-origin POST, so it
     stores the editor connection;
  3. turn the MCP switch of the profile on, so an open file connects.

  The key is a local value of that one call. It is never put in a URL, the
  app state, browser storage, the DOM or a log line."
  (:require
   [app.common.time :as ct]
   [app.config :as cf]
   [app.main.broadcast :as mbc]
   [app.main.data.profile :as du]
   [app.main.repo :as rp]
   [app.main.store :as st]
   [app.main.ui.studio.dashboard.rules :as rules]
   [app.main.ui.studio.shell-client :as shell]
   [beicon.v2.core :as rx]
   [cuerdas.core :as str]))

(def ^:private api-uri "/studio/api/editor-shell/")
(def ^:private shell-header "X-Design-Studio-Shell")
(def guide-uri cf/mcp-help-center-uri)

;; --- Studio

(defn- json->status
  [data]
  (when (object? data)
    {:ai-access (true? (unchecked-get data "aiAccess"))
     :connected (true? (unchecked-get data "connected"))}))

(defn fetch-status
  "A promise of `{:ai-access bool :connected bool}`, or nil when Studio
  can't say (signed out, Studio away). Never rejects. Asked fresh, because
  the card must not claim a connection that was removed."
  []
  (-> (js/fetch (str api-uri "state")
                #js {:credentials "same-origin"
                     :headers (js-obj shell-header "1")})
      (.then (fn [response]
               (when (.-ok response)
                 (.json response))))
      (.then (fn [data]
               ;; The other widgets read this answer from the tab's cache.
               (when (object? data)
                 (shell/store-set! :session "state" #js {:at (js/Date.now) :value data}))
               (json->status data)))
      (.catch (constantly nil))))

(defn- store-connection
  "A promise of `:connected` or a `rules/failure` keyword. `mcp-key` goes in the
  request body, nowhere else."
  [mcp-key]
  (-> (js/fetch (str api-uri "connection")
                #js {:method "POST"
                     :credentials "same-origin"
                     :headers (js-obj "Content-Type" "application/json"
                                      shell-header "1")
                     :body (js/JSON.stringify #js {:mcpKey mcp-key})})
      (.then (fn [response]
               (if (.-ok response)
                 :connected
                 (-> (.json response)
                     (.catch (constantly nil))
                     (.then (fn [data]
                              (rules/failure (.-status response)
                                             (when (object? data)
                                               (unchecked-get data "reason")))))))))
      (.catch (constantly :failed))))

;; --- Editor

(defn- ensure-key
  "A promise of the account's MCP key: the valid one it has, else a new
  one without an expiry (Penpot's own default)."
  []
  (js/Promise.
   (fn [resolve reject]
     (->> (rp/cmd! :get-access-tokens)
          (rx/mapcat
           (fn [tokens]
             (if-let [mcp-key (rules/usable-key tokens (ct/now))]
               (rx/of mcp-key)
               (->> (rp/cmd! :create-access-token {:name "MCP key" :type "mcp"})
                    (rx/map :token)))))
          (rx/take 1)
          (rx/subs! resolve reject)))))

(defn- forget-cached-state!
  "The tab's cached Studio answer still says \"not connected\"."
  []
  (shell/store-remove! :session "state"))

(defn activate!
  "Step 1. A promise of `:connected` or of `:session`, `:no-access`,
  `:busy` or `:failed`. Never rejects, and never says why in terms of the
  key."
  []
  (-> (ensure-key)
      (.then (fn [mcp-key]
               (if (and (string? mcp-key) (not (str/blank? mcp-key)))
                 (store-connection mcp-key)
                 :failed)))
      (.then (fn [outcome]
               (when (= :connected outcome)
                 (forget-cached-state!)
                 ;; As Settings > Integrations does after it creates a key:
                 ;; the profile switch, then the open editor tabs.
                 (st/emit! (du/update-profile-props {:mcp-enabled true})
                           (mbc/event :mcp/enable {})))
               outcome))
      ;; The RPC error may describe the request; nothing of it is shown.
      (.catch (constantly :failed))))

;; --- What this browser remembers

(defn dismissed?
  []
  (true? (some-> (shell/store-get :local "connect-card")
                 (unchecked-get "dismissed"))))

(defn set-dismissed!
  [value]
  (if value
    (shell/store-set! :local "connect-card" #js {:dismissed true})
    (shell/store-remove! :local "connect-card")))
