;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard.home
  "The dashboard home (the Projects page). For an account with nothing in
  it yet (F1 · 12) it shows a first-design page instead of Penpot's empty
  Drafts row; otherwise it is Penpot's own projects section."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.data.common :as dcm]
   [app.main.data.dashboard :as dd]
   [app.main.data.event :as-alias ev]
   [app.main.data.project :as dpj]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.dashboard.import :as udi]
   [app.main.ui.dashboard.projects :refer [projects-section*]]
   [app.main.ui.ds.foundations.assets.icon :as i]
   [app.main.ui.studio.dashboard.connect-card :refer [connect-card*]]
   [app.main.ui.studio.dashboard.rules :as rules]
   [app.main.ui.studio.ds :as sds]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

;; --- Empty home (F1 · 12)

(mf/defc empty-home*
  {::mf/private true}
  [{:keys [team profile project]}]
  (let [team-id    (:id team)
        project-id (:id project)
        file-input (mf/use-ref nil)
        name       (rules/first-name profile)

        on-create
        (mf/use-fn
         (mf/deps team-id project-id)
         (fn [_]
           (let [on-success
                 (fn [{:keys [id data]}]
                   (st/emit! (dcm/go-to-workspace :team-id team-id
                                                  :file-id id
                                                  :page-id (get-in data [:pages 0]))))]
             (st/emit! (-> (dd/create-file (with-meta {:project-id project-id}
                                             {:on-success on-success}))
                           (with-meta {::ev/origin "studio:empty-home"}))))))

        on-import
        (mf/use-fn #(dom/click (mf/ref-val file-input)))

        on-finish-import
        (mf/use-fn
         (mf/deps team-id project-id)
         (fn []
           (st/emit! (dpj/fetch-files project-id)
                     (dd/fetch-recent-files team-id)
                     (dd/fetch-projects team-id)
                     (dd/clear-selected-files))))

        on-plans
        (mf/use-fn #(st/emit! (rt/nav :settings-subscription)))

        on-deleted
        (mf/use-fn
         (mf/deps team-id)
         #(st/emit! (dcm/go-to-dashboard-deleted :team-id team-id)))]

    (mf/with-effect [team]
      (let [tname (if (:is-default team)
                    (tr "dashboard.personal-projects")
                    (:name team))]
        (dom/set-html-title (tr "title.dashboard.projects" tname))))

    [:div {:class (stl/css :home)
           :data-testid "ds-empty-home"}
     [:header {:class (stl/css :greeting)}
      [:h1 {:class (stl/css :greeting-title)}
       (if (some? name)
         (tr "ds.home.greeting-name" name)
         (tr "ds.home.greeting"))]
      [:p {:class (stl/css :greeting-lead)} (tr "ds.home.empty.lead")]]

     [:div {:class (stl/css :starts)}
      [:> sds/card* {:class (stl/css :start :start-primary)
                     :variant "raised"
                     :padding "lg"
                     :on-click on-create
                     :data-testid "ds-home-create"}
       [:span {:class (stl/css :start-icon) :aria-hidden true}
        [:> sds/icon* {:icon-id i/add :size "l"}]]
       [:span {:class (stl/css :start-title)} (tr "ds.home.empty.create")]
       [:span {:class (stl/css :start-text)} (tr "ds.home.empty.create-text")]]

      [:> sds/card* {:class (stl/css :start)
                     :padding "lg"
                     :on-click on-import
                     :data-testid "ds-home-import"}
       [:span {:class (stl/css :start-icon) :aria-hidden true}
        [:> sds/icon* {:icon-id i/import-export :size "l"}]]
       [:span {:class (stl/css :start-title)} (tr "ds.home.empty.import")]
       [:span {:class (stl/css :start-text)} (tr "ds.home.empty.import-text")]]]

     [:> udi/import-form* {:ref file-input
                           :project-id project-id
                           :on-finish-import on-finish-import}]

     [:> connect-card*]

     [:section {:class (stl/css :plan)
                :aria-label (tr "ds.home.plan.label")
                :data-testid "ds-home-plan"}
      [:> sds/badge* {:tone "craft" :is-mono true} (tr "ds.home.plan.badge")]
      [:p {:class (stl/css :plan-text)} (tr "ds.home.plan.text")]
      [:> sds/button* {:variant "secondary"
                       :on-click on-plans
                       :data-testid "ds-home-plans"}
       (tr "ds.home.plan.cta")]]

     [:> sds/button* {:class (stl/css :deleted)
                      :variant "ghost"
                      :size "sm"
                      :on-click on-deleted
                      :data-testid "ds-home-deleted"}
      (tr "ds.home.empty.deleted")]]))

;; --- Home

(mf/defc home*
  "Takes the props of `projects-section*` and passes them on. With
  projects or files it is Penpot's own section, which mounts the
  \"Connect AI\" card itself (`home-card*`), inside its scroll area."
  [{:keys [team projects profile] :as props}]
  (let [can-edit (-> team :permissions :can-edit)]
    (if (rules/empty-home? projects can-edit)
      [:> empty-home* {:team team
                       :profile profile
                       :project (rules/drafts projects)}]
      [:> projects-section* props])))
