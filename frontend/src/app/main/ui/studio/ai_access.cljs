;; Design Studio (F4-B, F1 · 10): AI access card on Settings > Integrations.
;; Shows whether this member's AI is connected to the editor and links to the
;; page that manages the connection. State comes from
;; `GET /studio/api/editor-shell/state` (shell-state); only booleans are read.
;; No token, key or link is ever shown or fetched here.

(ns app.main.ui.studio.ai-access
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.ui.studio.ds :as sds]
   [app.main.ui.studio.shell-state :as ss]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def manage-uri "/studio/")

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

(def ^:private status-copy
  {:connected     {:tone "success" :badge "ds.ai-access.connected"
                   :text "ds.ai-access.connected-text" :cta "ds.ai-access.manage"}
   :not-connected {:tone "warning" :badge "ds.ai-access.not-connected"
                   :text "ds.ai-access.not-connected-text" :cta "ds.ai-access.connect"}
   :unavailable   {:tone "neutral" :badge "ds.ai-access.unavailable"
                   :text "ds.ai-access.unavailable-text" :cta "ds.ai-access.manage"}})

(mf/defc ai-access-card*
  []
  (let [state  (ss/use-state)
        status (access-status state)
        copy   (get status-copy status)]
    (when copy
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
        [:> sds/button* {:variant (if (= status :not-connected) "primary" "secondary")
                         :to manage-uri
                         :data-testid "ds-ai-access-manage"}
         (tr (:cta copy))]]])))
