;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.settings-shell.page
  "What every Settings page shares (P3-A): the scope of `polish.scss` on
  <body>, the page header (title + one line under it) and the sidebar row
  with its line icon, the look of the dashboard sidebar. P3D: the dense
  parts of a page, a section card, its rows (label left, control right)
  and its foot."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.data.macros :as dm]
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

;; --- Dense page parts (P3D, 19 Nov)

(mf/defc card*
  "A section of a Settings page: the DS card around its rows. `title` and
  `subtitle` (optional) head the card, `action` sits right of them, `foot`
  is the card's foot (Save etc., right aligned), `tone` \"danger\" for the
  delete card."
  [{:keys [title subtitle action foot tone class test-id children]}]
  [:section {:class (dm/str (stl/css-case :card true
                                          :card-danger (= tone "danger"))
                            (when class (dm/str " " class)))
             :data-ds-settings "card"
             :data-testid test-id}
   (when (or (some? title) (some? action))
     [:header {:class (stl/css :card-head)}
      [:div {:class (stl/css :card-head-text)}
       (when (some? title)
         [:h2 {:class (stl/css :card-title)} title])
       (when (some? subtitle)
         [:p {:class (stl/css :card-subtitle)} subtitle])]
      action])
   children
   (when (some? foot)
     [:footer {:class (stl/css :card-foot)} foot])])

(mf/defc row*
  "One row of a card: `label` and its `hint` on the left, the control
  (`children`) right of them, an optional `action` at the end. `for` ties
  the label to an input id; the input keeps its own (hidden) label as its
  accessible name, so this one is not read twice."
  [{:keys [label hint for action class test-id children]}]
  [:div {:class (dm/str (stl/css :srow) (when class (dm/str " " class)))
         :data-ds-settings "row"
         :data-testid test-id}
   [:div {:class (stl/css :srow-text)}
    (if (some? for)
      [:label {:class (stl/css :srow-label) :for for :aria-hidden "true"} label]
      [:p {:class (stl/css :srow-label)} label])
    (when (some? hint)
      [:p {:class (stl/css :srow-hint)} hint])]
   [:div {:class (stl/css :srow-control)} children]
   (when (some? action)
     [:div {:class (stl/css :srow-action)} action])])
