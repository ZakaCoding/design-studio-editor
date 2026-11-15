;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC Sucursal en España SL

(ns frontend-tests.plugins.persistence-guard-test
  (:require
   [app.plugins.persistence-guard :as pguard]
   [app.plugins.register :as r]
   [cljs.test :as t :include-macros true]))

(def ^:private mcp-id pguard/mcp-plugin-id)

(def ^:private now 1800000000000)

(defn- runtime
  "A guard over a fake app state and fake persistence operations. The
  fakes record what the guard asked for; `:listeners` holds the
  `[on-saved on-failed]` pair of every wait."
  ([] (runtime true))
  ([ready?]
   (let [state     (atom {:current-file-id "one"
                          :persistence {:status :saved}})
         flushes   (atom 0)
         disposed  (atom 0)
         warnings  (atom [])
         listeners (atom [])
         guard     (pguard/create {:state-fn (fn [] @state)
                                   :now-fn   (constantly now)})]
     (when ready?
       (pguard/note-write! guard mcp-id pguard/runtime-ready-permission))
     {:guard     guard
      :state     state
      :flushes   flushes
      :disposed  disposed
      :warnings  warnings
      :listeners listeners
      :ops       {:force-persist!  (fn [] (swap! flushes inc))
                  :wait-persisted! (fn [on-saved on-failed]
                                     (swap! listeners conj [on-saved on-failed])
                                     (fn [] (swap! disposed inc)))
                  :warn!           (fn [cause] (swap! warnings conj cause))}})))

(defn- persistence!
  [{:keys [state]} pstate]
  (swap! state assoc :persistence pstate))

(defn- write!
  ([rt] (write! rt "content:write"))
  ([{:keys [guard]} permission]
   (pguard/note-write! guard mcp-id permission)))

(defn- thrown-message
  [f]
  (try
    (f)
    nil
    (catch :default cause
      (ex-message cause))))

(defn- settle
  [{:keys [guard ops]} checkpoint]
  (pguard/settle! guard checkpoint ops))

(defn- after-tick
  "Runs `f` after the guard's own zero timeout and promise jobs."
  [f]
  (js/setTimeout f 5))

(defn- outcome
  "Calls `f` with `:resolved` or the rejection message of `promise`."
  [promise f]
  (.then promise
         (fn [_] (f :resolved))
         (fn [cause] (f (ex-message cause)))))

;; --- write-blocked? : one case per persistence state

(t/deftest write-blocked-per-state
  (t/testing "settled and normally progressing queues accept writes"
    (t/is (false? (pguard/write-blocked? nil now)))
    (t/is (false? (pguard/write-blocked? {} now)))
    (t/is (false? (pguard/write-blocked? {:status :saved} now)))
    (t/is (false? (pguard/write-blocked? {:status :pending :last-progress-at now} now)))
    (t/is (false? (pguard/write-blocked? {:status :saving :last-progress-at now} now)))
    (t/is (false? (pguard/write-blocked? {:status :saving} now))
          "no progress timestamp yet is not a stall"))

  (t/testing ":error blocks"
    (t/is (true? (pguard/write-blocked? {:status :error} now))))

  (t/testing ":retrying blocks despite fresh progress"
    (t/is (true? (pguard/write-blocked? {:status :retrying :last-progress-at now} now))))

  (t/testing "an open failure blocks the slow-cycle attempts too"
    (t/is (true? (pguard/write-blocked? {:status :saving
                                         :last-progress-at now
                                         :failing-since (- now 60000)}
                                        now)))
    (t/is (true? (pguard/write-blocked? {:status :saving
                                         :last-progress-at now
                                         :failing-since 0}
                                        now))
          "0 is a timestamp, not absent"))

  (t/testing "a reported stall blocks"
    (t/is (true? (pguard/write-blocked? {:status :saving
                                         :last-progress-at now
                                         :stall-reported true}
                                        now))))

  (t/testing "a pending or saving queue without progress for five minutes blocks"
    (let [limit pguard/stall-timeout-ms]
      (t/is (false? (pguard/write-blocked? {:status :saving :last-progress-at (- now limit)} now)))
      (t/is (true? (pguard/write-blocked? {:status :saving :last-progress-at (- now limit 1)} now)))
      (t/is (true? (pguard/write-blocked? {:status :pending :last-progress-at (- now limit 1)} now)))
      (t/is (false? (pguard/write-blocked? {:status :saved :last-progress-at (- now limit 1)} now))
            "old progress on a saved queue is not a stall"))))

;; --- port of frontend-reliability/persistence-guard.test.mjs

(t/deftest runtime-without-write-hook-cannot-begin-a-task
  (let [rt (runtime false)]
    (t/is (= pguard/unavailable (thrown-message #(pguard/checkpoint (:guard rt)))))
    (pguard/note-write! (:guard rt) "another-plugin" pguard/runtime-ready-permission)
    (t/is (= pguard/unavailable (thrown-message #(pguard/checkpoint (:guard rt))))
          "only the MCP plugin's runtime counts")
    (pguard/note-write! (:guard rt) mcp-id pguard/runtime-ready-permission)
    (t/is (nil? (thrown-message #(pguard/checkpoint (:guard rt)))))))

(t/deftest failed-persistence-blocks-writes-without-disabling-inspection
  (t/async done
    (let [rt (runtime)
          cp (pguard/checkpoint (:guard rt))]
      (persistence! rt {:status :error})
      (t/is (= pguard/blocked (thrown-message #(write! rt))))
      (t/is (nil? (thrown-message #(write! rt "content:read"))))
      (outcome (settle rt cp)
               (fn [result]
                 (t/is (= :resolved result) "a task that only read still succeeds")
                 (t/is (= 0 @(:flushes rt)))
                 (t/is (empty? @(:listeners rt)))
                 (done))))))

(t/deftest library-and-comment-writes-fail-closed-other-plugins-unchanged
  (let [rt (runtime)]
    (persistence! rt {:status :error})
    (doseq [permission ["library:write" "comment:write"]]
      (t/is (= pguard/blocked (thrown-message #(write! rt permission)))))
    (t/is (nil? (thrown-message
                 #(pguard/note-write! (:guard rt) "another-plugin" "content:write"))))))

(t/deftest write-completion-waits-for-the-save-acknowledgement
  (t/async done
    (let [rt      (runtime)
          cp      (pguard/checkpoint (:guard rt))
          _       (write! rt)
          settled (atom nil)]
      (outcome (settle rt cp) #(reset! settled %))
      (t/is (= 0 @(:flushes rt)) "buffered commits get one tick before the flush")
      (after-tick
       (fn []
         (t/is (nil? @settled) "no success before the queue answers")
         (t/is (= 1 @(:flushes rt)))
         (t/is (= 1 (count @(:listeners rt))))
         (let [[on-saved on-failed] (first @(:listeners rt))]
           (on-saved)
           ;; A stream that completes after its value must not flip the result.
           (on-failed))
         (after-tick
          (fn []
            (t/is (= :resolved @settled))
            (t/is (= 1 @(:disposed rt)) "the wait is disposed once")
            (t/is (= 1 @(:flushes rt)))
            (t/is (empty? @(:warnings rt)))
            (t/is (nil? (thrown-message #(write! rt))) "a confirmed save does not latch")
            (done))))))))

(t/deftest save-failure-rejects-with-uncertainty-and-never-retries
  (t/async done
    (let [rt      (runtime)
          cp      (pguard/checkpoint (:guard rt))
          _       (write! rt)
          settled (atom nil)]
      (outcome (settle rt cp) #(reset! settled %))
      (after-tick
       (fn []
         (let [[_ on-failed] (first @(:listeners rt))]
           (on-failed))
         (after-tick
          (fn []
            (t/is (= pguard/unconfirmed @settled))
            (t/is (= 1 @(:flushes rt)) "the save is not sent again")
            (t/is (= 1 (count @(:listeners rt))) "and not waited for again")
            (t/is (= pguard/blocked (thrown-message #(write! rt)))
                  "uncertainty latches even though the status is not :error")
            (t/is (nil? (thrown-message #(write! rt "content:read"))))
            (t/is (= 1 (count @(:warnings rt)))
                  "the user sees a warning when the queue itself did not fail")
            (t/is (= pguard/unconfirmed (ex-message (first @(:warnings rt)))))
            (done))))))))

(t/deftest failed-queue-is-not-reported-twice
  (t/async done
    (let [rt      (runtime)
          cp      (pguard/checkpoint (:guard rt))
          _       (write! rt)
          settled (atom nil)]
      (outcome (settle rt cp) #(reset! settled %))
      (after-tick
       (fn []
         ;; The queue failed terminally and reported it itself.
         (persistence! rt {:status :error})
         (let [[_ on-failed] (first @(:listeners rt))]
           (on-failed))
         (after-tick
          (fn []
            (t/is (= pguard/unconfirmed @settled))
            (t/is (empty? @(:warnings rt)))
            (done))))))))

(t/deftest a-wait-that-answers-synchronously-is-handled
  (t/async done
    (let [rt       (runtime)
          disposed (atom 0)
          ops      (assoc (:ops rt)
                          :wait-persisted!
                          (fn [_ on-failed]
                            (on-failed)
                            (fn [] (swap! disposed inc))))
          cp       (pguard/checkpoint (:guard rt))]
      (write! rt)
      (outcome (pguard/settle! (:guard rt) cp ops)
               (fn [result]
                 (t/is (= pguard/unconfirmed result))
                 (t/is (= 1 @disposed))
                 (done))))))

(t/deftest a-failing-warning-does-not-become-success
  (t/async done
    (let [rt  (runtime)
          ops (assoc (:ops rt)
                     :wait-persisted! (fn [_ on-failed] (on-failed) (fn []))
                     :warn! (fn [_] (throw (js/Error. "toast failed"))))
          cp  (pguard/checkpoint (:guard rt))]
      (write! rt)
      (outcome (pguard/settle! (:guard rt) cp ops)
               (fn [result]
                 (t/is (= pguard/unconfirmed result))
                 (done))))))

(t/deftest a-failing-flush-is-uncertain
  (t/async done
    (let [rt  (runtime)
          ops (assoc (:ops rt) :force-persist! (fn [] (throw (js/Error. "private detail"))))
          cp  (pguard/checkpoint (:guard rt))]
      (write! rt)
      (outcome (pguard/settle! (:guard rt) cp ops)
               (fn [result]
                 (t/is (= pguard/unconfirmed result) "the cause is not leaked")
                 (t/is (empty? @(:listeners rt)))
                 (t/is (= pguard/blocked (thrown-message #(write! rt))))
                 (done))))))

(t/deftest navigation-before-the-save-cannot-produce-success
  (t/async done
    (let [rt (runtime)
          cp (pguard/checkpoint (:guard rt))]
      (swap! (:state rt) assoc :current-file-id "other")
      (outcome (settle rt cp)
               (fn [result]
                 (t/is (= pguard/unconfirmed result))
                 (t/is (= 0 @(:flushes rt)))
                 (done))))))

(t/deftest navigation-during-the-save-cannot-produce-success
  (t/async done
    (let [rt      (runtime)
          cp      (pguard/checkpoint (:guard rt))
          _       (write! rt)
          settled (atom nil)]
      (outcome (settle rt cp) #(reset! settled %))
      (after-tick
       (fn []
         (swap! (:state rt) assoc :current-file-id "other")
         (let [[on-saved _] (first @(:listeners rt))]
           (on-saved))
         (after-tick
          (fn []
            (t/is (= pguard/unconfirmed @settled))
            (done))))))))

(t/deftest navigation-before-the-flush-cannot-produce-success
  (t/async done
    (let [rt (runtime)
          cp (pguard/checkpoint (:guard rt))]
      (write! rt)
      (let [promise (settle rt cp)]
        ;; Between settle! and its deferred flush.
        (swap! (:state rt) assoc :current-file-id "other")
        (outcome promise
                 (fn [result]
                   (t/is (= pguard/unconfirmed result))
                   (t/is (= 0 @(:flushes rt)) "another file's queue is not flushed for this task")
                   (done)))))))

(t/deftest a-missing-checkpoint-is-uncertain
  (t/async done
    (let [rt (runtime)]
      (outcome (settle rt nil)
               (fn [result]
                 (t/is (= pguard/unconfirmed result))
                 (done))))))

(t/deftest stalled-queue-blocks-writes-normal-pending-queue-is-valid
  (let [rt (runtime)]
    (persistence! rt {:status :saving :last-progress-at now})
    (t/is (nil? (thrown-message #(write! rt))))
    (persistence! rt {:status :saving :last-progress-at (- now 300001)})
    (t/is (= pguard/blocked (thrown-message #(write! rt))))))

(t/deftest stall-report-blocks-writes
  (let [rt (runtime)]
    (persistence! rt {:status :saving :last-progress-at now :stall-reported true})
    (t/is (= pguard/blocked (thrown-message #(write! rt))))))

(t/deftest retrying-save-blocks-writes-reads-stay-a-landed-save-lifts-it
  (let [rt (runtime)]
    (persistence! rt {:status :retrying :last-progress-at now})
    (t/is (= pguard/blocked (thrown-message #(write! rt))))
    (t/is (nil? (thrown-message #(write! rt "content:read"))))
    (persistence! rt {:status :saved})
    (t/is (nil? (thrown-message #(write! rt))))))

(t/deftest slow-cycle-attempt-blocks-while-a-failure-is-open
  (let [rt (runtime)]
    (persistence! rt {:status :saving :last-progress-at now :failing-since (- now 60000)})
    (t/is (= pguard/blocked (thrown-message #(write! rt))))
    (t/is (nil? (thrown-message #(write! rt "content:read"))))
    (persistence! rt {:status :saving :last-progress-at now :failing-since 0})
    (t/is (= pguard/blocked (thrown-message #(write! rt))) "0 is a timestamp, not absent")
    (persistence! rt {:status :saved})
    (t/is (nil? (thrown-message #(write! rt))))))

(t/deftest refused-writes-do-not-count-as-writes
  (t/async done
    (let [rt (runtime)
          cp (pguard/checkpoint (:guard rt))]
      (persistence! rt {:status :retrying :last-progress-at now})
      (t/is (= pguard/blocked (thrown-message #(write! rt))))
      (outcome (settle rt cp)
               (fn [result]
                 (t/is (= :resolved result) "nothing was written, so nothing has to be saved")
                 (t/is (= 0 @(:flushes rt)))
                 (done))))))

;; --- the seam in app.plugins.register

(t/deftest guard-and-register-agree-on-the-mcp-plugin
  (t/is (= r/mcp-plugin-id pguard/mcp-plugin-id)))

(t/deftest check-permission-refuses-mcp-writes-through-the-app-guard
  (let [guard (pguard/create {:state-fn (fn [] {:current-file-id "one"
                                                :persistence {:status :retrying}})
                              :now-fn   (constantly now)})]
    (with-redefs [pguard/instance guard]
      (t/is (= pguard/blocked
               (thrown-message #(r/check-permission r/mcp-plugin-id "content:write"))))
      (t/is (true? (r/check-permission r/mcp-plugin-id "content:read")))
      (t/is (false? (boolean (r/check-permission "another-plugin" "content:write")))
            "other plugins keep the plain permission answer"))
    (let [healthy (pguard/create {:state-fn (fn [] {:current-file-id "one"
                                                    :persistence {:status :saved}})
                                  :now-fn   (constantly now)})]
      (with-redefs [pguard/instance healthy]
        (t/is (true? (r/check-permission r/mcp-plugin-id "content:write")))
        (t/is (= 1 @(:epoch healthy)))))))
