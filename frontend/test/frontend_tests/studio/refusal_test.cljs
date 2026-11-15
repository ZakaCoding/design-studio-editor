;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns frontend-tests.studio.refusal-test
  "Guard refusals: which answers are ours, and how the repository layer
  hands them on as `:restriction` errors."
  (:require
   [app.main.repo :as rp]
   [app.main.ui.studio.refusal :as refusal]
   [beicon.v2.core :as rx]
   [cljs.test :as t :include-macros true]))

(def ^:private api "https://design.example/api/main/methods/")

(defn- answer
  [status code & {:keys [command] :or {command "create-file"}}]
  {:status status
   :uri (str api command)
   :headers {"content-type" "application/json"}
   :body (js/JSON.stringify #js {:type "restriction" :code code})})

(t/deftest reads-only-our-codes
  (t/is (= :storage-limit-reached (refusal/response->code (answer 413 "storage-limit-reached"))))
  (t/is (= :too-many-uploads (refusal/response->code (answer 429 "too-many-uploads"))))
  (t/is (= :invite-limit-reached (refusal/response->code (answer 429 "invite-limit-reached"))))
  (t/is (= :too-many-recovery-requests (refusal/response->code (answer 429 "too-many-recovery-requests"))))
  (t/is (= :guard-unavailable (refusal/response->code (answer 503 "guard-unavailable"))))
  (t/is (= :storage-unavailable (refusal/response->code (answer 507 "storage-unavailable"))))

  (t/testing "another code, type or status is not ours"
    (t/is (nil? (refusal/response->code (answer 413 "close"))))
    (t/is (nil? (refusal/response->code (answer 413 "max-quote-reached"))))
    (t/is (nil? (refusal/response->code (answer 403 "not-authenticated"))))
    (t/is (nil? (refusal/response->code (answer 200 "storage-limit-reached"))))
    (t/is (nil? (refusal/response->code {:status 413
                                         :uri (str api "create-file")
                                         :body "{\"type\":\"validation\",\"code\":\"storage-limit-reached\"}"}))))

  (t/testing "a body that is not our JSON is left alone"
    (t/is (nil? (refusal/response->code {:status 413 :uri (str api "create-file") :body "<html>too large</html>"})))
    (t/is (nil? (refusal/response->code {:status 503 :uri (str api "create-file") :body ""})))
    (t/is (nil? (refusal/response->code {:status 503 :uri (str api "create-file") :body nil})))
    (t/is (nil? (refusal/response->code {:status 429 :uri (str api "create-file") :body "null"})))
    (t/is (nil? (refusal/response->code {:status 429 :uri (str api "create-file") :body "[1]"})))
    (t/is (nil? (refusal/response->code {:status 413
                                         :uri (str api "create-file")
                                         :body {:type :restriction :code :storage-limit-reached}}))))

  (t/testing "thumbnails are refused quietly"
    (t/is (nil? (refusal/response->code (answer 413 "storage-limit-reached" :command "create-file-thumbnail"))))
    (t/is (nil? (refusal/response->code (answer 413 "storage-limit-reached" :command "create-file-object-THUMBNAIL"))))))

(t/deftest announces-to-the-listener
  (let [seen (atom [])]
    (refusal/listen! #(swap! seen conj %))
    (refusal/announce! :too-many-uploads)
    (refusal/announce! :max-quote-reached)
    (refusal/announce! nil)
    (t/is (= [:too-many-uploads] @seen))
    (refusal/listen! nil)
    (refusal/announce! :too-many-uploads)
    (t/is (= [:too-many-uploads] @seen))))

(t/deftest repository-raises-a-restriction
  (t/async done
    (let [seen (atom [])]
      (refusal/listen! #(swap! seen conj %))
      (->> (rp/handle-response (answer 413 "storage-limit-reached"))
           (rx/subs!
            (fn [_]
              (refusal/listen! nil)
              (t/is false "a refusal must not be a value")
              (done))
            (fn [cause]
              (refusal/listen! nil)
              (let [data (ex-data cause)]
                (t/is (= :restriction (:type data)))
                (t/is (= :storage-limit-reached (:code data)))
                (t/is (= 413 (:status data)))
                (t/is (= [:storage-limit-reached] @seen)))
              (done)))))))

(t/deftest repository-keeps-stock-handling-for-the-rest
  (t/async done
    (let [seen (atom [])]
      (refusal/listen! #(swap! seen conj %))
      (->> (rp/handle-response (answer 413 "storage-limit-reached" :command "create-file-thumbnail"))
           (rx/subs!
            (fn [_]
              (refusal/listen! nil)
              (t/is false "a 413 must not be a value")
              (done))
            (fn [cause]
              (refusal/listen! nil)
              (let [data (ex-data cause)]
                (t/is (= :validation (:type data)))
                (t/is (= :request-body-too-large (:code data)))
                (t/is (= [] @seen)))
              (done)))))))
