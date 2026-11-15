;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns frontend-tests.studio.widgets-test
  (:require
   [app.main.ui.studio.widgets :as widgets]
   [cljs.test :as t :include-macros true]))

(t/deftest members-only-hash
  (t/testing "dashboard and settings addresses need an account, with or without a team"
    (t/is (widgets/members-only-hash? "#/dashboard/recent"))
    (t/is (widgets/members-only-hash? "#/dashboard/recent?team-id=4947ad8d-2927-8038-8008-f34a7fc54c49"))
    (t/is (widgets/members-only-hash? "#/settings/profile")))
  (t/testing "shared views, the workspace, the sign-in pages and an empty address do not move"
    (t/is (not (widgets/members-only-hash? "#/view?file-id=1&page-id=2")))
    (t/is (not (widgets/members-only-hash? "#/workspace?team-id=1&file-id=2")))
    (t/is (not (widgets/members-only-hash? "#/auth/login")))
    (t/is (not (widgets/members-only-hash? "")))
    (t/is (not (widgets/members-only-hash? nil)))))
