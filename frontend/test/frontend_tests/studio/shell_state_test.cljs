(ns frontend-tests.studio.shell-state-test
  (:require
   [app.main.ui.studio.plans :as plans]
   [app.main.ui.studio.shell-state :as ss]
   [cljs.test :as t :include-macros true]))

(t/deftest open-signup-detection
  (t/is (true? (ss/open? {:open true})))
  (t/is (false? (ss/open? {:open false})))
  (t/is (false? (ss/open? {})))
  (t/is (false? (ss/open? nil))))

(t/deftest password-wording
  ;; Open sign-up account without ZakaCoding ID: the plain "this is your password" note.
  (t/is (false? (boolean (ss/backup? {:open true :sso false}))))
  ;; Invited member who also has ZakaCoding ID: backup wording.
  (t/is (true? (boolean (ss/backup? {:open true :sso true}))))
  (t/is (true? (boolean (ss/backup? {:open false :sso false}))))
  ;; The server could not answer ({}): backup wording, as the overlay did.
  (t/is (true? (boolean (ss/backup? {})))))

(t/deftest plan-summary
  (t/is (= [:free :monthly :lifetime] (mapv :id plans/plans)))
  (t/is (= "/studio/craft/plans" plans/plans-uri)))

(t/deftest plan-prices
  (let [usd {:currency "USD"
             :monthly {:priceUsdCents 549 :credits 1000}
             :lifetime {:priceUsdCents 8249 :paused false :credits 1000}}
        idr {:currency "IDR"
             :monthly {:priceIdr 99000 :credits 1000}
             :lifetime {:priceIdr 1490000 :paused true :credits 1000}}]
    (t/is (= "US$5.49" (plans/format-price "USD" {:priceUsdCents 549})))
    (t/is (= "US$82.49" (plans/format-price "USD" {:priceUsdCents 8249})))
    (t/is (= "US$5.00" (plans/format-price "USD" {:priceUsdCents 500})))
    (t/is (= "Rp99.000" (plans/format-price "IDR" {:priceIdr 99000})))
    (t/is (= "Rp1.490.000" (plans/format-price "IDR" {:priceIdr 1490000})))
    (t/is (= "Rp500" (plans/format-price "IDR" {:priceIdr 500})))
    (t/is (nil? (plans/format-price "USD" {})))
    (t/is (= {:amount "US$5.49"} (plans/plan-price usd :monthly)))
    (t/is (= {:amount "US$82.49"} (plans/plan-price usd :lifetime)))
    (t/is (= {:amount "Rp99.000"} (plans/plan-price idr :monthly)))
    ;; Lifetime paused: "Opening soon" instead of a price.
    (t/is (= {:soon true} (plans/plan-price idr :lifetime)))
    (t/is (nil? (plans/plan-price usd :free)))))

(t/deftest plan-prices-unavailable
  ;; Sales off (plans null) or state unavailable ({} / nil): no prices at all.
  (doseq [st [nil {} {:plans nil}]]
    (doseq [id [:free :monthly :lifetime]]
      (t/is (nil? (plans/plan-price (:plans st) id))))))
