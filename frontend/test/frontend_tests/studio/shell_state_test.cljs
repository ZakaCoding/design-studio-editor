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
  (t/is (= [:lifetime] (mapv :id (filter :soon plans/plans))))
  (t/is (= "/studio/craft/plans" plans/plans-uri)))
