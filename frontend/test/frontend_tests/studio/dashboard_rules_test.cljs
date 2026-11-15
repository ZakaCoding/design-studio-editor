;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns frontend-tests.studio.dashboard-rules-test
  "The dashboard \"Connect AI\" card (F1 · 03) and the empty home (F1 · 12):
  which MCP key is reused, what the card shows, and when the home is the
  first-design page."
  (:require
   [app.common.time :as ct]
   [app.main.ui.studio.dashboard.rules :as rules]
   [cljs.test :as t :include-macros true]))

(def ^:private now (ct/now))
(def ^:private past (ct/inst (- (inst-ms now) 3600000)))
(def ^:private later (ct/inst (+ (inst-ms now) 3600000)))

(t/deftest reuses-a-valid-mcp-key
  (t/testing "a key without an expiry, or one that has not expired, is reused"
    (t/is (= "key-a" (rules/usable-key [{:type "mcp" :token "key-a" :expires-at nil}] now)))
    (t/is (= "key-b" (rules/usable-key [{:type "mcp" :token "key-b" :expires-at later}] now))))
  (t/testing "a personal access token is never taken for the MCP key"
    (t/is (nil? (rules/usable-key [{:type nil :token "pat" :expires-at nil}
                                   {:type "access-token" :token "pat-2"}] now)))
    (t/is (= "key-a" (rules/usable-key [{:type nil :token "pat"}
                                        {:type "mcp" :token "key-a"}] now))))
  (t/testing "an expired key, or a row without its key, means a new key is needed"
    (t/is (nil? (rules/usable-key [{:type "mcp" :token "old" :expires-at past}] now)))
    (t/is (nil? (rules/usable-key [{:type "mcp" :token "" :expires-at nil}] now)))
    (t/is (nil? (rules/usable-key [{:type "mcp" :expires-at nil}] now)))
    (t/is (nil? (rules/usable-key [] now)))
    (t/is (nil? (rules/usable-key nil now)))))

(t/deftest assistant-address-has-no-key
  (t/is (= "https://design.zakacoding.dev/studio/mcp"
           (rules/assistant-uri "https://design.zakacoding.dev")))
  (t/is (= "https://design.zakacoding.dev/studio/mcp"
           (rules/assistant-uri "https://design.zakacoding.dev/")))
  (t/is (not (re-find #"[?#]" (rules/assistant-uri "https://design.zakacoding.dev")))))

(t/deftest card-modes
  (let [member    {:ai-access true :connected false}
        connected {:ai-access true :connected true}]
    (t/testing "nothing until Studio answers, without AI access, or with MCP off"
      (t/is (= :hidden (rules/card-mode {:enabled true :status nil})))
      (t/is (= :hidden (rules/card-mode {:enabled true :status {:ai-access false :connected false}})))
      (t/is (= :hidden (rules/card-mode {:enabled false :status member})))
      (t/is (= :hidden (rules/card-mode {:enabled false :status connected}))))
    (t/testing "the two-step card for a member who is not connected"
      (t/is (= :card (rules/card-mode {:enabled true :status member}))))
    (t/testing "after \"Not now\" it is the reminder"
      (t/is (= :later (rules/card-mode {:enabled true :status member :dismissed true}))))
    (t/testing "step 2 stays open in the visit that finished step 1"
      (t/is (= :pairing (rules/card-mode {:enabled true :status connected :activated true})))
      (t/is (= :pairing (rules/card-mode {:enabled true :status connected :activated true :dismissed true}))))
    (t/testing "once connected it is the status bar, whatever was dismissed"
      (t/is (= :connected (rules/card-mode {:enabled true :status connected})))
      (t/is (= :connected (rules/card-mode {:enabled true :status connected :dismissed true}))))))

(t/deftest failures
  (t/is (= :session (rules/failure 401 nil)))
  (t/is (= :session (rules/failure 409 "account-changed")))
  (t/is (= :no-access (rules/failure 403 "editor-account-not-invited")))
  ;; 403 without a reason is the origin check or a key that is not this
  ;; account's: not something the member can act on.
  (t/is (= :failed (rules/failure 403 nil)))
  (t/is (= :busy (rules/failure 409 nil)))
  (t/is (= :busy (rules/failure 429 nil)))
  (t/is (= :failed (rules/failure 400 nil)))
  (t/is (= :failed (rules/failure 503 nil)))
  (t/is (= :failed (rules/failure 0 nil))))

(t/deftest empty-home
  (let [drafts  {:id 1 :is-default true :count 0}
        project {:id 2 :is-default false :count 0}]
    (t/testing "only Drafts, with no file"
      (t/is (true? (rules/empty-home? [drafts] true)))
      (t/is (true? (rules/empty-home? [(dissoc drafts :count)] true))))
    (t/testing "a file or a project means Penpot's projects page"
      (t/is (false? (rules/empty-home? [(assoc drafts :count 1)] true)))
      (t/is (false? (rules/empty-home? [drafts project] true))))
    (t/testing "a deleted project does not count"
      (t/is (true? (rules/empty-home? [drafts (assoc project :deleted-at now)] true))))
    (t/testing "a viewer, or a team whose projects are not loaded, keeps Penpot's page"
      (t/is (false? (rules/empty-home? [drafts] false)))
      (t/is (false? (rules/empty-home? [drafts] nil)))
      (t/is (false? (rules/empty-home? [] true)))
      (t/is (false? (rules/empty-home? [project] true))))
    (t/is (= drafts (rules/drafts [project drafts])))))

(t/deftest greeting-name
  (t/is (= "Zaka" (rules/first-name {:fullname "Zaka Coding"})))
  (t/is (= "Zaka" (rules/first-name {:fullname "  Zaka  "})))
  (t/is (nil? (rules/first-name {:fullname "  "})))
  (t/is (nil? (rules/first-name {})))
  (t/is (nil? (rules/first-name nil))))
