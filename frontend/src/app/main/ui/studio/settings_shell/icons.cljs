;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.settings-shell.icons
  "The line icons of the Settings sidebar, drawn on the 24 px grid of
  `app.main.ui.studio.ds.icon/icon*` like the dashboard's
  (`app.main.ui.studio.dashboard.icons`). Decorative: the row's text says
  what it means."
  (:require
   [app.main.ui.studio.ds.icon :refer [icon*]]
   [rumext.v2 :as mf]))

(mf/defc back*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M14.5 6l-6 6 6 6"}]])

(mf/defc user*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:circle {:cx 12 :cy 8 :r 4}]
   [:path {:d "M4.5 20.5c.8-4 3.8-6 7.5-6s6.7 2 7.5 6"}]])

(mf/defc lock*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:rect {:x 4.5 :y 10.5 :width 15 :height 10 :rx 2}]
   [:path {:d "M8 10.5V8a4 4 0 0 1 8 0v2.5"}]])

(mf/defc bell*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 2h-15z"}]
   [:path {:d "M10 21h4"}]])

(mf/defc keyboard*
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:rect {:x 2.5 :y 6 :width 19 :height 12 :rx 2}]
   [:path {:d "M6.5 10h.01M10 10h.01M13.5 10h.01M17 10h.01M8 14h8"}]])

(mf/defc globe*
  "Language and theme."
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:circle {:cx 12 :cy 12 :r 8.5}]
   [:path {:d "M3.5 12h17"}]
   [:path {:d "M12 3.5c2.4 2.3 3.6 5.1 3.6 8.5s-1.2 6.2-3.6 8.5c-2.4-2.3-3.6-5.1-3.6-8.5s1.2-6.2 3.6-8.5z"}]])

(mf/defc card*
  "Plans and credits."
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:rect {:x 2.5 :y 5.5 :width 19 :height 13 :rx 2}]
   [:path {:d "M2.5 10h19M6.5 14.5h4"}]])

(mf/defc plug*
  "AI access: an app plugged into the account."
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M9 3.5V8M15 3.5V8"}]
   [:path {:d "M6.5 8h11v3.5a5.5 5.5 0 0 1-11 0z"}]
   [:path {:d "M12 17v3.5"}]])

(mf/defc notes*
  "Release notes."
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M13.5 3.5H7A1.5 1.5 0 0 0 5.5 5v14A1.5 1.5 0 0 0 7 20.5h10a1.5 1.5 0 0 0 1.5-1.5V8.5z"}]
   [:path {:d "M13.5 3.5v5h5"}]
   [:path {:d "M9 13h6M9 16.5h4"}]])

(mf/defc message*
  "Contact us."
  [{:keys [size class]}]
  [:> icon* {:size size :class class}
   [:path {:d "M4.5 5.5h15v11h-8l-4 3.5v-3.5h-3z"}]])
