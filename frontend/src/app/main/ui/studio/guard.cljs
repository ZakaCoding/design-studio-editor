;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.guard
  "An error boundary for Design Studio parts mounted inside an upstream page
  (a card, a note, a menu item, the workspace shell). A fault in one of them
  hides that part only; the page around it keeps working. Without it, one
  bad render in the Connect AI card turned the whole dashboard and Settings
  into Penpot's \"Internal Error\" page."
  (:require
   [app.main.ui.error-boundary :refer [error-boundary*]]
   [rumext.v2 :as mf]))

(mf/defc silent*
  {::mf/private true}
  []
  nil)

(mf/defc guard*
  [{:keys [children]}]
  [:> error-boundary* {:fallback silent*}
   children])
