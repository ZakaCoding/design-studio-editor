(ns frontend-tests.studio.ai-access-test
  (:require
   [app.main.ui.studio.ai-access :as aa]
   [cljs.test :as t :include-macros true]))

(t/deftest access-status
  (t/is (= :loading (aa/access-status nil)))
  ;; The server could not answer ({}), or the account has no AI access.
  (t/is (= :unavailable (aa/access-status {})))
  (t/is (= :unavailable (aa/access-status {:aiAccess false :connected true})))
  (t/is (= :not-connected (aa/access-status {:aiAccess true :connected false})))
  (t/is (= :connected (aa/access-status {:aiAccess true :connected true}))))

(t/deftest manage-link
  (t/is (= "/studio/" aa/manage-uri)))

(t/deftest connect-flow
  ;; Not connected starts the in-editor flow; the others link to Studio.
  (t/is (true? (aa/starts-connect-flow? :not-connected)))
  (t/is (false? (aa/starts-connect-flow? :connected)))
  (t/is (false? (aa/starts-connect-flow? :unavailable)))
  (t/is (false? (aa/starts-connect-flow? :loading))))
