;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.shell-client
  "What the Design Studio widgets remember in the browser, and their calls
  to the Studio service on the same origin (`/studio/api/editor-shell/*`).

  The storage keys and their JSON shapes are the ones the former injected
  shell script used (`ds-shell:*`), so what a member already dismissed or
  snoozed stays so across the move to this code."
  (:require
   [app.common.data :as d]))

;; --- Browser storage (plain JSON under `ds-shell:<key>`)

(defn- storage
  [area]
  (try
    (case area
      :local   (.-localStorage js/window)
      :session (.-sessionStorage js/window))
    (catch :default _ nil)))

(defn store-get
  "The parsed JSON value, or nil when absent, unreadable or blocked."
  [area k]
  (try
    (some-> (storage area)
            (.getItem (str "ds-shell:" k))
            (js/JSON.parse))
    (catch :default _ nil)))

(defn store-set!
  [area k v]
  (try
    (some-> (storage area)
            (.setItem (str "ds-shell:" k) (js/JSON.stringify v)))
    (catch :default _ nil))
  nil)

(defn store-merge!
  "Merge the keys of the JS object `v` into the stored object."
  [area k v]
  (let [current (store-get area k)
        current (if (object? current) current #js {})]
    (store-set! area k (js/Object.assign #js {} current v))))

(defn store-remove!
  [area k]
  (try
    (some-> (storage area)
            (.removeItem (str "ds-shell:" k)))
    (catch :default _ nil))
  nil)

(defn today
  "The local calendar day, YYYY-MM-DD."
  []
  (let [date (js/Date.)
        pad  (fn [n] (.padStart (str n) 2 "0"))]
    (str (.getFullYear date) "-" (pad (inc (.getMonth date))) "-" (pad (.getDate date)))))

;; --- Motion

(defn reduced-motion?
  "True when the member asked the system for less motion."
  []
  (try
    (boolean (.-matches (.matchMedia js/window "(prefers-reduced-motion: reduce)")))
    (catch :default _ false)))

;; --- Studio API

(def ^:private api-uri "/studio/api/editor-shell/")
(def ^:private state-max-age (* 10 60 1000))

(defn- request
  "A same-origin request with the header the editor-shell API requires."
  [path]
  (js/fetch (str api-uri path)
            #js {:credentials "same-origin"
                 :headers #js {"X-Design-Studio-Shell" "1"}}))

(defn- json->state
  [data]
  (when (object? data)
    {:ai-access (true? (unchecked-get data "aiAccess"))
     :connected (true? (unchecked-get data "connected"))
     :open      (true? (unchecked-get data "open"))
     ;; Left unknown (nil) when Studio did not say.
     :sso       (let [sso (unchecked-get data "sso")]
                  (when (boolean? sso) sso))}))

(defonce ^:private state-request (volatile! nil))

(defn fetch-state
  "A promise of what Studio knows about this member, or nil when it can't
  say (signed out, Studio away). Never rejects. The answer is kept for ten
  minutes per tab, as before."
  []
  (let [cached (store-get :session "state")
        at     (some-> cached (unchecked-get "at"))]
    (cond
      (and (number? at) (< (- (js/Date.now) at) state-max-age))
      (js/Promise.resolve (json->state (unchecked-get cached "value")))

      (some? @state-request)
      @state-request

      :else
      (vreset! state-request
               (-> (request "state")
                   (.then (fn [response]
                            (when (.-ok response)
                              (.json response))))
                   (.catch (constantly nil))
                   (.then (fn [data]
                            (vreset! state-request nil)
                            (when (object? data)
                              (store-set! :session "state" #js {:at (js/Date.now) :value data}))
                            (json->state data))))))))

(defn fetch-password-needed
  "A promise of `{:status n :needed bool}` for the email-first sign-up
  password step; `:status 0` when the request itself failed."
  []
  (-> (request "password")
      (.then (fn [response]
               (if (.-ok response)
                 (-> (.json response)
                     (.then (fn [data]
                              {:status (.-status response)
                               :needed (true? (d/nilv (some-> data (unchecked-get "needed")) false))})))
                 {:status (.-status response) :needed false})))
      (.catch (constantly {:status 0 :needed false}))))

(defn save-password
  "A promise of the HTTP status of saving the first password (0 when the
  request itself failed)."
  [password]
  (-> (js/fetch "/studio/api/signup/password"
                #js {:method "POST"
                     :credentials "same-origin"
                     :headers #js {"Content-Type" "application/json"
                                   "X-Studio-Bridge" "1"}
                     :body (js/JSON.stringify #js {:password password})})
      (.then (fn [response] (.-status response)))
      (.catch (constantly 0))))
