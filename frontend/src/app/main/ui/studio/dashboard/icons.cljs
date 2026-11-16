;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard.icons
  "The line icons of the dashboard home (F1 · 03, 12), drawn on a 24 px
  grid for `app.main.ui.studio.ds.icon/icon*`. Decorative: the text next
  to each one says what it means."
  (:require
   [app.main.ui.studio.ds.icon :refer [icon*]]
   [rumext.v2 :as mf]))

(mf/defc check*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M5 12.5l4.5 4.5L19 7.5"}]])

(mf/defc plus*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M12 5v14M5 12h14"}]])

(mf/defc link*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M10 13a5 5 0 0 0 7.07 0l2.83-2.83a5 5 0 0 0-7.07-7.07L11.4 4.5"}]
   [:path {:d "M14 11a5 5 0 0 0-7.07 0L4.1 13.83a5 5 0 0 0 7.07 7.07l1.42-1.4"}]])

(mf/defc frame*
  "The board sign (#) of \"Dari template\"."
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M8 3v18M16 3v18M3 8h18M3 16h18"}]])

(mf/defc download*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M12 4v11M7 10.5l5 5 5-5M5 20h14"}]])

(mf/defc sparkle*
  "Craft's four-point star."
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M12 3l2.2 6.8L21 12l-6.8 2.2L12 21l-2.2-6.8L3 12l6.8-2.2z"}]])
