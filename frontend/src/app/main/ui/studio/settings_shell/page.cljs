;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.settings-shell.page
  "What every Settings page shares (P3-A): the scope of `polish.scss` on
  <body>, the page header (title + one line under it) and the sidebar row
  with its line icon, the look of the dashboard sidebar."
  (:require-macros [app.main.style :as stl])
  (:require
   [rumext.v2 :as mf]))

(defn use-scope
  "Hook: <body> has the class `ds-settings` while the calling component is
  mounted: the Settings pages and the dialogs opened from them
  (`polish.scss` lives under it)."
  []
  (mf/with-effect []
    (let [classes (.-classList (.-body js/document))]
      (.add classes "ds-settings")
      (fn []
        (.remove classes "ds-settings")))))

(mf/defc page-head*
  "The header of a Settings page: `title`, `subtitle` (one line), `id` of
  the title (the page's `aria-labelledby`)."
  [{:keys [title subtitle id]}]
  [:header {:class (stl/css :head)
            :data-ds-settings "head"}
   [:h1 {:class (stl/css :title) :id id} title]
   (when (some? subtitle)
     [:p {:class (stl/css :subtitle)} subtitle])])

(mf/defc nav-row*
  "One row of the Settings sidebar, the dashboard's row: a 16 px line icon
  and the label; the current row is raised and its icon takes the accent.
  `icon` is a component of `settings-shell.icons`."
  [{:keys [label icon is-current on-click test-id]}]
  [:li {:class (stl/css-case :row true
                             :row-current is-current)
        :aria-current (when ^boolean is-current "page")
        :role "link"
        :tab-index 0
        :on-click on-click
        :on-key-down (fn [event]
                       (when (= "Enter" (.-key event))
                         (on-click event)))
        :data-testid test-id}
   [:> icon {:size "m" :class (stl/css :row-icon)}]
   [:span {:class (stl/css :row-label)} label]])
