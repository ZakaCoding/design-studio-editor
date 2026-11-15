;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard-shell.views
  "What the dashboard mounts around the upstream pages: the top bar
  actions, the page tabs of Library / Font, and the three pages that
  share the route `/dashboard/recent`:

  - no `view`          the home (`app.main.ui.studio.dashboard.home/home*`)
  - `view=projects`    upstream's Projects page (\"Proyek\")
  - `view=templates`   upstream's templates, as a page (\"Template\")"
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.data.macros :as dm]
   [app.config :as cf]
   [app.main.data.common :as dcm]
   [app.main.data.dashboard :as dd]
   [app.main.refs :as refs]
   [app.main.store :as st]
   [app.main.ui.dashboard.projects :refer [projects-section*]]
   [app.main.ui.dashboard.templates :refer [templates-section*]]
   [app.main.ui.studio.dashboard-shell.home :as home]
   [app.main.ui.studio.dashboard-shell.page :as page]
   [app.main.ui.studio.dashboard-shell.rules :as rules]
   [app.main.ui.studio.dashboard.home :refer [home*]]
   [app.main.ui.studio.guard :refer [guard*]]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

;; --- Templates page

(mf/defc templates-page*
  {::mf/private true}
  [{:keys [team profile projects]}]
  (let [team-id    (:id team)
        project-id (:id (rules/drafts projects))
        can-edit   (dm/get-in team [:permissions :can-edit])]

    (mf/with-effect [team]
      (dom/set-html-title (tr "ds.shell.templates.page-title")))

    ;; The upstream section only loads the templates while its drawer is
    ;; open; here it is a page, always open.
    (mf/with-effect []
      (st/emit! (dd/fetch-builtin-templates)))

    [:> page/page* {:title (tr "ds.shell.nav.templates")
                    :data-testid "ds-shell-templates"}
     (if (and ^boolean can-edit (contains? cf/flags :dashboard-templates-section))
       [:div {:class (stl/css :templates)
              :data-ds-dash "templates"}
        [:> templates-section* {:profile profile
                                :project-id project-id
                                :team-id team-id
                                :default-project-id project-id}]]
       [:p {:class (stl/css :templates-empty)} (tr "ds.shell.templates.none")])]))

;; --- /dashboard/recent

(mf/defc recent*
  "Takes the props of `projects-section*` (`team`, `projects`, `profile`,
  `layout`, `on-layout-change`) and renders the page the URL names."
  [{:keys [team projects profile] :as props}]
  (let [route (mf/deref refs/route)
        view  (rules/view (dm/get-in route [:params :query]))]
    (cond
      (= view :projects)
      [:> projects-section* props]

      (= view :templates)
      [:> templates-page* {:team team
                           :profile profile
                           :projects projects}]

      :else
      [:> home* props])))

;; --- Around the upstream pages

(mf/defc section-tabs*
  "Library and Font are one item of the sidebar (\"Library\"); the tabs
  switch between the two upstream pages."
  {::mf/private true}
  [{:keys [section team-id]}]
  (let [go-libraries
        (mf/use-fn
         (mf/deps team-id)
         #(st/emit! (dcm/go-to-dashboard-libraries :team-id team-id)))

        go-fonts
        (mf/use-fn
         (mf/deps team-id)
         #(st/emit! (dcm/go-to-dashboard-fonts :team-id team-id)))]

    [:> page/tabs* {:aria-label (tr "ds.shell.nav.libraries")}
     [:> page/tab* {:is-current (= section :dashboard-libraries)
                    :on-click go-libraries
                    :data-testid "ds-shell-tab-libraries"}
      (tr "ds.shell.tabs.libraries")]
     [:> page/tab* {:is-current (not= section :dashboard-libraries)
                    :on-click go-fonts
                    :data-testid "fonts"}
      (tr "ds.shell.tabs.fonts")]]))

(mf/defc top*
  "Mounted once by `app.main.ui.dashboard`, before the page: the search
  box and \"+ File baru\" of the pages that have them, and the Library /
  Font tabs."
  [{:keys [section team default-project search-term]}]
  [:> guard* {}
   [:> home/top-actions* {:section section
                          :team team
                          :default-project default-project
                          :search-term search-term}]
   (when (rules/library-section? section)
     [:> section-tabs* {:section section
                        :team-id (:id team)}])])
