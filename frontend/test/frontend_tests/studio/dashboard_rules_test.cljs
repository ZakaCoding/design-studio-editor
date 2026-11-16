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
  (let [member    {:ai-access true :connected false :assistant-known true}
        linked    {:ai-access true :connected true :assistant-known true}
        connected (assoc linked :assistant {:name "Claude" :access "read"})
        legacy    {:ai-access true :connected true}]
    (t/testing "nothing until Studio answers, without AI access, or with MCP off"
      (t/is (= :hidden (rules/card-mode {:enabled true :status nil})))
      (t/is (= :hidden (rules/card-mode {:enabled true :status {:ai-access false :connected false}})))
      (t/is (= :hidden (rules/card-mode {:enabled false :status member})))
      (t/is (= :hidden (rules/card-mode {:enabled false :status connected}))))
    (t/testing "0 of 2: the card with step 1 open"
      (t/is (= :card (rules/card-mode {:enabled true :status member})))
      (t/is (= :card (rules/card-mode {:enabled true :status member :adding true}))))
    (t/testing "1 of 2: the editor is linked and no AI app has connected yet"
      (t/is (= :pairing (rules/card-mode {:enabled true :status linked})))
      (t/is (= :pairing (rules/card-mode {:enabled true :status linked :activated true}))))
    (t/testing "an AI app connected: the status bar"
      (t/is (= :connected (rules/card-mode {:enabled true :status connected})))
      (t/is (= :connected (rules/card-mode {:enabled true :status connected :activated true}))))
    (t/testing "connecting one more app from the status bar opens step 2 again"
      (t/is (= :pairing (rules/card-mode {:enabled true :status connected :adding true}))))
    (t/testing "a Studio that does not say which app is connected: as before"
      (t/is (= :connected (rules/card-mode {:enabled true :status legacy})))
      (t/is (= :pairing (rules/card-mode {:enabled true :status legacy :activated true}))))
    (t/is (= [0 1 2 0] (mapv rules/steps-done [:card :pairing :connected :hidden])))))

(t/deftest step-2
  (t/is (= "design.zakacoding.dev/studio/mcp"
           (rules/display-uri "https://design.zakacoding.dev/studio/mcp")))
  (t/is (= "perf.localhost:19443/studio/mcp"
           (rules/display-uri "https://perf.localhost:19443/studio/mcp")))
  (t/is (= [:claude :codex :other] rules/assistants))
  (t/is (= :claude (rules/assistant-choice nil)))
  (t/is (= :claude (rules/assistant-choice "Claude")))
  (t/is (= :codex (rules/assistant-choice "Codex CLI")))
  (t/is (= :other (rules/assistant-choice "Cursor"))))

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

(t/deftest craft-hero-prompt
  (t/testing "what the member typed, trimmed"
    (t/is (= "landing page kedai kopi" (rules/hero-prompt "  landing page kedai kopi \n" nil)))
    (t/is (nil? (rules/hero-prompt "   " nil)))
    (t/is (nil? (rules/hero-prompt nil nil))))
  (t/testing "a chip alone starts the sentence; with text it names the kind first"
    (t/is (= "Poster acara" (rules/hero-prompt "" "Poster acara")))
    (t/is (= "Poster acara: workshop batik, 12 Desember" (rules/hero-prompt "workshop batik, 12 Desember" "Poster acara"))))
  (t/testing "never longer than Craft's box takes"
    (t/is (= rules/prompt-max (count (rules/hero-prompt (apply str (repeat 3000 "a")) nil))))
    (t/is (= rules/prompt-max (count (rules/hero-prompt (apply str (repeat 3000 "a")) "Brand kit"))))))

(t/deftest craft-hero-file-name
  (t/is (= "Landing page kedai kopi, nuansa hangat" (rules/file-name "landing page kedai kopi, nuansa hangat")))
  (t/is (= "Poster acara" (rules/file-name "Poster acara: ")))
  (t/is (= "Landing page untuk kedai kopi di Bandung"
           (rules/file-name "Landing page untuk kedai kopi di Bandung dengan nuansa hangat dan foto besar")))
  (t/is (= "First line" (rules/file-name "first line\nsecond line")))
  (t/is (= 40 (count (rules/file-name (apply str (repeat 90 "x"))))))
  (t/is (nil? (rules/file-name "  ")))
  (t/is (nil? (rules/file-name "...")))
  (t/is (nil? (rules/file-name nil))))

(t/deftest craft-handoff
  (let [file-id #uuid "11111111-2222-4333-8444-555555555555"]
    (t/is (= {:prompt "Landing page" :fileId "11111111-2222-4333-8444-555555555555" :at 1700}
             (rules/handoff " Landing page " file-id 1700)))
    (t/is (nil? (rules/handoff "  " file-id 1700)))
    (t/is (nil? (rules/handoff nil file-id 1700)))))

(t/deftest templates-row
  (t/is (= [] (rules/template-tiles nil)))
  (t/is (= [{:template :a :tone "pink"} {:template :b :tone "sky"}]
           (rules/template-tiles [:a :b])))
  (t/is (= ["pink" "sky" "mint" "yellow" "lilac" "peach"]
           (mapv :tone (rules/template-tiles (range 9))))))

(t/deftest craft-card-price
  (t/is (= "US$5.49" (rules/price-label {:currency "USD" :monthly {:priceUsdCents 549 :credits 1000}})))
  (t/is (= "US$5.00" (rules/price-label {:currency "USD" :monthly {:priceUsdCents 500}})))
  (t/is (= "Rp99.000" (rules/price-label {:currency "IDR" :monthly {:priceIdr 99000}})))
  (t/is (= "Rp1.490.000" (rules/price-label {:currency "IDR" :monthly {:priceIdr 1490000}})))
  (t/is (nil? (rules/price-label {:currency "USD" :monthly {:priceIdr 99000}})))
  (t/is (nil? (rules/price-label {:currency "EUR" :monthly {:priceUsdCents 549}})))
  (t/is (nil? (rules/price-label nil))))
