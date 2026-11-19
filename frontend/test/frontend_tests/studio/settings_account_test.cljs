(ns frontend-tests.studio.settings-account-test
  (:require
   [app.main.ui.studio.settings-shell.account :as acc]
   [cljs.test :as t :include-macros true]))

(def ^:private plans
  {:currency "USD"
   :monthly {:priceUsdCents 549 :credits 1000}
   :lifetime {:priceUsdCents 8249 :paused true :credits 1000}})

(t/deftest nothing-known
  ;; Shell state still loading or unavailable, no profile: every line is left out.
  (t/is (= {:plan nil :renews nil :credits nil :storage nil :apps nil :since nil}
           (acc/account-facts nil nil)))
  (t/is (= {:plan nil :renews nil :credits nil :storage nil :apps nil :since nil}
           (acc/account-facts {} {}))))

(t/deftest monthly-member
  (let [state {:plans plans
               :member {:plan "monthly"
                        :credits {:left 812 :total 1000}
                        :renewsAt "2026-12-07T00:00:00Z"
                        :storage {:usedBytes (* 212 1024 1024) :totalBytes (* 5 1024 1024 1024)}}
               :apps [{:name "Claude" :access "view"} {:name "Codex" :access "edit"}]}
        facts (acc/account-facts state {:created-at (js/Date. "2026-11-01T00:00:00Z")})]
    (t/is (= {:id "monthly" :price "US$5.49" :per-month? true} (:plan facts)))
    (t/is (= (.getTime (js/Date. "2026-12-07T00:00:00Z")) (.getTime (:renews facts))))
    (t/is (= {:left 812 :total 1000 :ratio 0.812} (:credits facts)))
    (t/is (= {:used "212 MB" :total "5 GB"} (select-keys (:storage facts) [:used :total])))
    (t/is (= 2 (count (get-in facts [:apps :shown]))))
    (t/is (= 0 (get-in facts [:apps :more])))
    (t/is (some? (:since facts)))))

(t/deftest free-member
  (let [facts (acc/account-facts {:plans plans
                                  :member {:plan "free" :credits nil :renewsAt nil
                                           :storage {:usedBytes 0 :totalBytes 1073741824}}
                                  :apps []}
                                 {})]
    ;; Free has no price line, no credits, no renewal; storage still shows.
    (t/is (= {:id "free" :price nil :per-month? false} (:plan facts)))
    (t/is (nil? (:credits facts)))
    (t/is (nil? (:renews facts)))
    (t/is (= "0 B" (get-in facts [:storage :used])))
    (t/is (nil? (:apps facts)))))

(t/deftest bad-values-are-left-out
  (let [facts (acc/account-facts {:member {:plan "enterprise"
                                           :credits {:left 5 :total 0}
                                           :renewsAt "not a date"
                                           :storage {:usedBytes -1 :totalBytes 10}}
                                  :apps [{:access "view"} "x"]}
                                 {:created-at "garbage"})]
    (t/is (nil? (:plan facts)))
    (t/is (nil? (:credits facts)))
    (t/is (nil? (:renews facts)))
    (t/is (nil? (:storage facts)))
    (t/is (nil? (:apps facts)))
    (t/is (nil? (:since facts)))))

(t/deftest at-most-three-apps
  (let [apps  (mapv #(hash-map :name (str "App " %)) (range 5))
        facts (acc/account-facts {:apps apps} nil)]
    (t/is (= 3 (count (get-in facts [:apps :shown]))))
    (t/is (= 2 (get-in facts [:apps :more])))))

(t/deftest lifetime-price
  ;; A Lifetime member sees the price they paid even while new sales are paused.
  (t/is (= {:id "lifetime" :price "US$82.49" :per-month? false}
           (:plan (acc/account-facts {:plans plans :member {:plan "lifetime"}} nil)))))

(t/deftest price-format
  (t/is (= "US$5.49" (acc/format-price "USD" {:priceUsdCents 549})))
  (t/is (= "Rp99.000" (acc/format-price "IDR" {:priceIdr 99000})))
  (t/is (nil? (acc/format-price "USD" {}))))
