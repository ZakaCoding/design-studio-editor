;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard-shell.page
  "The page mount of the dashboard shell (F1 · 03, 12, 14).

  A dashboard page is `page*`: a header (title, optional eyebrow above it,
  optional actions on the right), optional tabs and the scrolling body.

    [:> page* {:title \"Selamat sore, Zaka\"}
     [:> hero* …]
     [:> recent-files* …]]

  The shell adds the search box and \"+ File baru\" itself, to the right of
  the header, on the pages that have them (see `top-actions*`). `actions`
  is for what only this page has; it sits to the left of those.

  The parts are grid items of the dashboard content area (`chrome.scss`
  lays it out), so `page-header*` and `tabs*` also work alone, next to an
  upstream section."
  (:require-macros [app.main.style :as stl])
  (:require
   [rumext.v2 :as mf]))

(mf/defc black-button*
  "The black pill button of the boards (\"+ File baru\", \"Bagikan\").
  `plus` puts a + before the label. Other props go to the <button>."
  [{:keys [plus class children] :rest props}]
  (let [props (mf/spread-props props {:type "button"
                                      :class [class (stl/css :black-button)]})]
    [:> "button" props
     (when ^boolean plus
       [:span {:class (stl/css :black-button-plus) :aria-hidden true} "+"])
     [:span {:class (stl/css :black-button-label)} children]]))

(mf/defc soft-button*
  "The gray pill button next to the black one (\"Undang\")."
  [{:keys [class children] :rest props}]
  (let [props (mf/spread-props props {:type "button"
                                      :class [class (stl/css :soft-button)]})]
    [:> "button" props children]))

(mf/defc tab*
  "One tab of `tabs*`. `is-current` marks the open page."
  [{:keys [is-current on-click children] :rest props}]
  (let [props (mf/spread-props props {:type "button"
                                      :role "tab"
                                      :aria-selected (boolean is-current)
                                      :class (stl/css-case :tab true
                                                           :tab-current is-current)
                                      :on-click on-click})]
    [:> "button" props children]))

(mf/defc tabs*
  "The tab row under the header (F1 · 14). Children are `tab*`."
  [{:keys [aria-label children]}]
  [:div {:class (stl/css :tabs)
         :role "tablist"
         :aria-label aria-label
         :data-ds-dash "tabs"}
   children])

(mf/defc page-header*
  "`title` is a string or an element (it becomes the <h1>), `eyebrow` the
  small line above it, `actions` an element on the right. `title-id` is
  the id of the <h1> (the upstream keyboard shortcuts focus it)."
  [{:keys [title eyebrow actions title-id]}]
  [:header {:class (stl/css-case :header true
                                 :header-eyebrow (some? eyebrow))
            :data-testid "dashboard-header"
            :data-ds-dash "header"}
   [:div {:class (stl/css :heading)}
    (when (some? eyebrow)
      [:div {:class (stl/css :eyebrow)} eyebrow])
    [:h1 {:class (stl/css :title)
          :id title-id
          :tab-index (when (some? title-id) -1)}
     title]]
   (when (some? actions)
     [:div {:class (stl/css :actions)} actions])])

(mf/defc page*
  "Props: `title`, `eyebrow`, `actions`, `tabs` (an element, `tabs*`),
  `title-id`, `children` (the body). Other props (`data-testid`, …) go to
  the body element."
  [{:keys [title eyebrow actions tabs title-id class children] :rest props}]
  (let [props (mf/spread-props props {:class [class (stl/css :body)]
                                      :data-ds-dash "body"})]
    [:*
     [:> page-header* {:title title
                       :eyebrow eyebrow
                       :actions actions
                       :title-id title-id}]
     tabs
     [:> "div" props children]]))

(mf/defc section-title*
  "The heading of a block inside a page (\"File terbaru\", \"File 6\"):
  `title`, an optional `count` after it and an optional `action` element
  on the right (\"Lihat semua →\")."
  [{:keys [title count action]}]
  [:div {:class (stl/css :section-head)}
   [:h2 {:class (stl/css :section-title)}
    title
    (when (some? count)
      [:span {:class (stl/css :section-count)} count])]
   (when (some? action)
     [:div {:class (stl/css :section-action)} action])])

(mf/defc text-link*
  "The small indigo link of the boards (\"Lihat semua →\")."
  [{:keys [class children] :rest props}]
  (let [props (mf/spread-props props {:type "button"
                                      :class [class (stl/css :text-link)]})]
    [:> "button" props children]))
