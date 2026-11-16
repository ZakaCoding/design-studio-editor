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

;; --- Dashboard navigation and empty states (P2)

(mf/defc home*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M3.5 10.5L12 3.5l8.5 7"}]
   [:path {:d "M5.5 9v11h13V9"}]
   [:path {:d "M10 20v-5.5h4V20"}]])

(mf/defc folder*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M3.5 7A1.5 1.5 0 0 1 5 5.5h4.2l2 2.2H19A1.5 1.5 0 0 1 20.5 9.2V18a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 18z"}]])

(mf/defc library*
  "Stacked sheets: shared libraries."
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M12 3.5l8.5 4.25L12 12 3.5 7.75z"}]
   [:path {:d "M3.5 12L12 16.25 20.5 12"}]
   [:path {:d "M3.5 16.25L12 20.5l8.5-4.25"}]])

(mf/defc layout*
  "Templates: a page layout."
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:rect {:x 3.5 :y 3.5 :width 17 :height 7 :rx 1.5}]
   [:rect {:x 3.5 :y 13.5 :width 7 :height 7 :rx 1.5}]
   [:rect {:x 13.5 :y 13.5 :width 7 :height 7 :rx 1.5}]])

(mf/defc trash*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M4 6.5h16"}]
   [:path {:d "M9 6.5V4.5h6v2"}]
   [:path {:d "M6 6.5l1 13.5h10l1-13.5"}]
   [:path {:d "M10 10.5v6M14 10.5v6"}]])

(mf/defc file*
  "An empty sheet: a file without a preview."
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M13.5 3.5H7A1.5 1.5 0 0 0 5.5 5v14A1.5 1.5 0 0 0 7 20.5h10a1.5 1.5 0 0 0 1.5-1.5V8.5z"}]
   [:path {:d "M13.5 3.5v5h5"}]])

(mf/defc type*
  "Fonts."
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M5 7V4.5h14V7"}]
   [:path {:d "M12 4.5v15"}]
   [:path {:d "M9 19.5h6"}]])

(mf/defc chevrons*
  "Up and down: a switcher that opens a list."
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M8 9.5l4-4 4 4"}]
   [:path {:d "M8 14.5l4 4 4-4"}]])

(mf/defc search*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:circle {:cx 11 :cy 11 :r 6.5}]
   [:path {:d "M16 16l4.5 4.5"}]])

(mf/defc users*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:circle {:cx 9.5 :cy 8 :r 3.5}]
   [:path {:d "M3.5 19.5c.6-3.2 3-5 6-5s5.4 1.8 6 5"}]
   [:path {:d "M15.5 4.8a3.5 3.5 0 0 1 0 6.4"}]
   [:path {:d "M18 14.8c1.4.8 2.3 2.4 2.6 4.7"}]])
