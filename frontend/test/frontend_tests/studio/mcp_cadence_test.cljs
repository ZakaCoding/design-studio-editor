;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns frontend-tests.studio.mcp-cadence-test
  "The MCP tip cadence and the guide steps. The `decide` cases are the ones
  the injected shell script was tested with
  (zakacoding-cloud studio/tests/editor-shell-script.test.mjs)."
  (:require
   [app.main.ui.studio.mcp-cadence :as cadence]
   [cljs.test :as t :include-macros true]))

(def ^:private settle (:settle cadence/cadence))
(def ^:private reappear (:reappear cadence/cadence))

(def ^:private day "2026-10-17")
(def ^:private next-day "2026-10-18")
(def ^:private t0 1792200000000)

(def ^:private member
  {:ai-access true :connected false})

(t/deftest cadence-values
  (t/is (= {:settle 6000 :visible-for 30000 :reappear 1500000} cadence/cadence)))

(t/deftest shows-on-first-open
  (t/testing "the first open of a work session shows it after the settle delay"
    (t/is (= {:show true :delay settle}
             (cadence/decide member t0 day)))))

(t/deftest shows-again-after-reappear-interval
  (let [state (assoc member :last-day day :last-shown t0)]
    (t/testing "within the interval it waits for the rest of it"
      (t/is (= {:show false :retry-in (- reappear 60000)}
               (cadence/decide state (+ t0 60000) day))))
    (t/testing "at the interval it shows again"
      (t/is (= {:show true :delay settle}
               (cadence/decide state (+ t0 reappear) day)))
      (t/is (= {:show true :delay settle}
               (cadence/decide state (+ t0 reappear 1) day))))
    (t/testing "one millisecond early it still waits"
      (t/is (= {:show false :retry-in 1}
               (cadence/decide state (+ t0 reappear -1) day))))))

(t/deftest next-day-shows-first
  (t/testing "the next day shows first, even if it was shown minutes ago"
    (t/is (true? (:show (cadence/decide (assoc member :last-day day :last-shown t0)
                                        (+ t0 (* 5 60000))
                                        next-day))))))

(t/deftest manual-close-hides-for-session-and-day
  (let [state (assoc member :last-day day :last-shown t0 :dismissed day)]
    (t/is (= {:show false}
             (cadence/decide state (+ t0 (* 10 reappear)) day)))
    (t/testing "a tab left open overnight shows it again the next day"
      (t/is (true? (:show (cadence/decide state (+ t0 (* 24 3600000)) next-day)))))))

(t/deftest comes-back-until-connected
  (t/testing "connected: never"
    (t/is (= {:show false} (cadence/decide (assoc member :connected true) t0 day))))
  (t/testing "\"remind me tomorrow\" only snoozes the day"
    (t/is (= {:show false} (cadence/decide (assoc member :snoozed day) t0 day)))
    (t/is (true? (:show (cadence/decide (assoc member :snoozed day :last-day day :last-shown t0)
                                        (+ t0 3600000)
                                        next-day)))))
  (t/testing "there is no permanent opt-out before connecting"
    (t/is (true? (:show (cadence/decide (assoc member :never true) t0 day)))))
  (t/testing "without AI access it is never advertised"
    (t/is (= {:show false} (cadence/decide {:ai-access false :connected false} t0 day)))
    (t/is (= {:show false} (cadence/decide {:connected false} t0 day)))))

(t/deftest missing-session-memory-shows
  (t/testing "shown today in this browser, but a new work session"
    (t/is (= {:show true :delay settle}
             (cadence/decide (assoc member :last-day day) t0 day)))
    (t/is (= {:show true :delay settle}
             (cadence/decide (assoc member :last-day day :last-shown 0) t0 day)))))

(t/deftest guide-steps
  (t/is (= [:connect :paste :done] cadence/steps))
  (t/is (= 1 (cadence/step-number :connect)))
  (t/is (= 2 (cadence/step-number :paste)))
  (t/is (true? (cadence/step? :paste)))
  (t/is (false? (cadence/step? :enable)))
  (t/is (false? (cadence/step? nil))))

(t/deftest guide-advance-on-integrations
  (let [screen {:integrations? true}]
    (t/testing "Connect on screen: step 1"
      (t/is (= :connect (cadence/advance :connect (assoc screen :connect? true))))
      (t/is (= :connect (cadence/advance :paste (assoc screen :connect? true)))))
    (t/testing "address ready: step 2, forwards"
      (t/is (= :paste (cadence/advance :connect (assoc screen :address? true)))))
    (t/testing "nothing to point at yet: the step stays"
      (t/is (= :connect (cadence/advance :connect screen)))
      (t/is (= :paste (cadence/advance :paste screen))))))

(t/deftest guide-advance-elsewhere
  (t/testing "no guide, or finished: unchanged"
    (t/is (nil? (cadence/advance nil {:integrations? true :connect? true})))
    (t/is (= :done (cadence/advance :done {:integrations? true :address? true}))))
  (t/testing "another page keeps the step"
    (t/is (= :connect (cadence/advance :connect {:connect? true})))))
