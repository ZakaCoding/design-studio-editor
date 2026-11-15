;; Design Studio (F4-B, F1 · 10): AI access card on Settings > Integrations.
;; Shows whether this member's AI is connected to the editor. Not connected:
;; the card hosts the two-step Connect flow of the dashboard (F4-A). Connected
;; or unavailable: a link to Studio, where the connection is managed. State comes from
;; `GET /studio/api/editor-shell/state` (shell-state); only booleans are read.
;; No token, key or link is ever shown or fetched here.

(ns app.main.ui.studio.ai-access
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.ui.studio.dashboard.connect-card :as cc]
   [app.main.ui.studio.ds :as sds]
   [app.main.ui.studio.shell-state :as ss]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def manage-uri
  "Studio: pause or change an existing connection, see why AI access is
  unavailable. Starting a connection does not go there."
  "/studio/")

(defn access-status
  "Which of the four states the card shows, from the shell state map:
  :loading (nil), :unavailable (the server could not answer, or this account has
  no AI access), :connected or :not-connected."
  [state]
  (cond
    (nil? state)                :loading
    (not (true? (:aiAccess state))) :unavailable
    (true? (:connected state))  :connected
    :else                       :not-connected))

(defn starts-connect-flow?
  "True when the card hosts the Connect flow instead of linking to Studio."
  [status]
  (= :not-connected status))

(def ^:private status-copy
  {:connected     {:tone "success" :badge "ds.ai-access.connected"
                   :text "ds.ai-access.connected-text" :cta "ds.ai-access.manage"}
   :unavailable   {:tone "neutral" :badge "ds.ai-access.unavailable"
                   :text "ds.ai-access.unavailable-text" :cta "ds.ai-access.manage"}})

(mf/defc ai-access-card*
  []
  (let [state  (ss/use-state)
        status (access-status state)
        copy   (get status-copy status)]
    (cond
      (starts-connect-flow? status)
      ;; The same card as the dashboard: step 1 is one click here, in the
      ;; editor; step 2 is the address to paste in the assistant.
      [:div {:class (stl/css :connect) :data-testid "ds-ai-access" :data-status "not-connected"}
       [:> cc/connect-card* {}]]

      (some? copy)
      [:> sds/card* {:class (stl/css :card)
                     :padding "lg"
                     :data-testid "ds-ai-access"
                     :data-status (name status)}
       [:div {:class (stl/css :head)}
        [:h3 {:class (stl/css :title)} (tr "ds.ai-access.title")]
        [:> sds/badge* {:tone (:tone copy)}
         (tr (:badge copy))]]
       [:p {:class (stl/css :text)} (tr (:text copy))]
       [:div
        [:> sds/button* {:variant "secondary"
                         :to manage-uri
                         :data-testid "ds-ai-access-manage"}
         (tr (:cta copy))]]])))
