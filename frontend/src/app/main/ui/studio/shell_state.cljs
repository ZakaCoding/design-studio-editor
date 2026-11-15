;; Design Studio (WP4): read-only editor shell state.
;; Source: studio/editor-shell.mjs `GET /studio/api/editor-shell/state`, called with the
;; same `X-Design-Studio-Shell: 1` header the overlay used. Never writes anything.

(ns app.main.ui.studio.shell-state
  (:require
   [app.util.http :as http]
   [beicon.v2.core :as rx]
   [rumext.v2 :as mf]))

(def ^:private state-uri "/studio/api/editor-shell/state")

;; nil until the server answered once. A failed call settles on {} so that the
;; wording falls back to the "backup" text, as the overlay did.
(defonce ^:private cache (atom nil))
(defonce ^:private requested? (atom false))

(defn open?
  "Open sign-up: the account only has this one password."
  [state]
  (true? (:open state)))

(defn backup?
  "True when the password is a backup of ZakaCoding ID. Also the fallback when
  the server could not answer."
  [state]
  (or (not (open? state))
      (not (false? (:sso state)))))

(defn- fetch-state!
  []
  (when (compare-and-set! requested? false true)
    (->> (http/send! {:method :get
                      :uri state-uri
                      :headers {"X-Design-Studio-Shell" "1"}
                      :response-type :json})
         (rx/map (fn [{:keys [status body]}]
                   (if (= 200 status)
                     (js->clj body :keywordize-keys true)
                     {})))
         (rx/catch (fn [_] (rx/of {})))
         (rx/subs! #(reset! cache %)))))

(defn use-state
  "Hook: the shell state map, or nil while it is loading."
  []
  (let [state* (mf/use-state @cache)]
    (mf/with-effect []
      (if-let [v @cache]
        (reset! state* v)
        (let [key (gensym "ds-shell-state")]
          (add-watch cache key (fn [_ _ _ v] (when v (reset! state* v))))
          (fetch-state!)
          #(remove-watch cache key))))
    @state*))
