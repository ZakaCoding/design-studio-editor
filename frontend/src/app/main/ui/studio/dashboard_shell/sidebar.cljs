;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard-shell.sidebar
  "The sidebar of the dashboard (F1 · 03, 12, 14): logo, team switcher,
  the six pages, the project list, the plan card and the member.

  It takes the place of `app.main.ui.dashboard.sidebar/sidebar*` and the
  same props. The team switcher, the project rows (rename, menu, drop a
  file on a project) and the member menu are the upstream components,
  restyled in `chrome.scss`; nothing of their behaviour changes."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.data.macros :as dm]
   [app.main.data.common :as dcm]
   [app.main.refs :as refs]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.dashboard.sidebar :as upstream]
   [app.main.ui.studio.dashboard-shell.rules :as rules]
   [app.main.ui.studio.dashboard-shell.scope :as scope]
   [app.main.ui.studio.dashboard-shell.team-switch :refer [team-switch*]]
   [app.main.ui.studio.dashboard.icons :as icons]
   [app.main.ui.studio.ds :as sds]
   [app.main.ui.studio.guard :refer [guard*]]
   [app.main.ui.studio.shell-state :as shell-state]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [app.util.timers :as ts]
   [rumext.v2 :as mf]))

;; --- Craft

(defn- focus-craft-prompt!
  "Puts the caret in the Craft prompt of the home page (the element with
  `data-ds-craft-prompt`). The home may still be loading, so it looks a
  few times."
  ([] (focus-craft-prompt! 12))
  ([tries]
   (if-let [node (dom/query "[data-ds-craft-prompt]")]
     (do (dom/scroll-into-view-if-needed! node)
         (dom/focus! node))
     (when (pos? tries)
       (ts/schedule 150 #(focus-craft-prompt! (dec tries)))))))

;; --- Navigation

(mf/defc nav-item*
  {::mf/private true}
  [{:keys [label icon is-current on-click test-id]}]
  [:li {:class (stl/css :nav-row)}
   [:button {:type "button"
             :class (stl/css-case :nav-item true
                                  :nav-current is-current)
             :aria-current (when ^boolean is-current "page")
             :on-click on-click
             :data-testid test-id}
    [:> icon {:size "m" :class (stl/css :nav-icon)}]
    [:span {:class (stl/css :nav-label)} label]]])

;; --- Plan card (F1 · 03: "Monthly, 812 / 1000 kredit Craft"; F1 · 12: "Free, 12 MB dari 1 GB")

(mf/defc plan-card*
  {::mf/private true}
  []
  (let [state (shell-state/use-state)
        card  (rules/plan-card (:member state))
        meter (:meter card)

        on-plans
        (mf/use-fn #(st/emit! (rt/nav :settings-subscription)))]

    (when (some? card)
      [:section {:class (stl/css :plan)
                 :aria-label (tr "ds.shell.plan.label")
                 :data-testid "ds-shell-plan"}
       [:div {:class (stl/css :plan-head)}
        [:span {:class (stl/css :plan-name)} (:name card)]
        (when ^boolean (:plans? card)
          [:button {:type "button"
                    :class (stl/css :plan-link)
                    :on-click on-plans
                    :data-testid "ds-shell-plans"}
           (tr "ds.shell.plan.see-plans")])]
       (when (some? meter)
         [:*
          [:p {:class (stl/css :plan-text)}
           (if (= :credits (:kind meter))
             (tr "ds.shell.plan.credits" (str (:left meter)) (str (:total meter)))
             (tr "ds.shell.plan.storage" (:used meter) (:total meter)))]
          [:div {:class (stl/css :plan-bar) :aria-hidden true}
           [:div {:class (stl/css :plan-bar-fill)
                  :style {:width (dm/str (* 100 (:ratio meter)) "%")}}]]])])))

;; --- Sidebar

(mf/defc sidebar*
  {::mf/wrap [mf/memo]}
  [{:keys [team profile projects project section]}]
  (let [team-id   (:id team)
        route     (mf/deref refs/route)
        view      (rules/view (dm/get-in route [:params :query]))
        current   (rules/nav-current section view)
        can-edit  (dm/get-in team [:permissions :can-edit])
        shell     (shell-state/use-state)
        free?     (= "free" (dm/get-in shell [:member :plan]))

        listed
        (mf/with-memo [projects]
          (rules/sidebar-projects projects))

        go-home
        (mf/use-fn
         (mf/deps team-id)
         #(st/emit! (rt/nav :dashboard-recent {:team-id team-id})))

        go-craft
        (mf/use-fn
         (mf/deps team-id free?)
         (fn []
           (if ^boolean free?
             (st/emit! (rt/nav :settings-subscription))
             (do
               (st/emit! (rt/nav :dashboard-recent {:team-id team-id}))
               (focus-craft-prompt!)))))

        go-projects
        (mf/use-fn
         (mf/deps team-id)
         #(st/emit! (rt/nav :dashboard-recent {:team-id team-id :view "projects"})))

        go-libraries
        (mf/use-fn
         (mf/deps team-id)
         #(st/emit! (dcm/go-to-dashboard-libraries :team-id team-id)))

        go-templates
        (mf/use-fn
         (mf/deps team-id)
         #(st/emit! (rt/nav :dashboard-recent {:team-id team-id :view "templates"})))

        go-deleted
        (mf/use-fn
         (mf/deps team-id)
         #(st/emit! (dcm/go-to-dashboard-deleted :team-id team-id)))]

    (scope/use-scope)

    [:nav {:class (stl/css :sidebar)
           :data-testid "dashboard-sidebar"
           :data-ds-dash "sidebar"}
     [:div {:class (stl/css :brand)}
      [:> sds/logo* {:size "sm" :class (stl/css :logo)}]]

     ;; One switcher: the team list, "create team" and the team's own
     ;; actions (members, invitations, settings, rename, leave, delete) in
     ;; one menu; the items are upstream's.
     [:div {:class (stl/css :team)}
      [:> team-switch* {:team team :profile profile}]]

     [:ul {:class (stl/css :nav)
           :aria-label (tr "ds.shell.nav.label")}
      [:> nav-item* {:label (tr "ds.shell.nav.home")
                     :icon icons/home*
                     :is-current (= current :home)
                     :on-click go-home
                     :test-id "ds-shell-nav-home"}]
      [:> nav-item* {:label (tr "ds.shell.nav.craft")
                     :icon icons/sparkle*
                     :is-current false
                     :on-click go-craft
                     :test-id "ds-shell-nav-craft"}]
      [:> nav-item* {:label (tr "ds.shell.nav.projects")
                     :icon icons/folder*
                     :is-current (= current :projects)
                     :on-click go-projects
                     :test-id "ds-shell-nav-projects"}]
      [:> nav-item* {:label (tr "ds.shell.nav.libraries")
                     :icon icons/library*
                     :is-current (= current :libraries)
                     :on-click go-libraries
                     :test-id "libs-link-sidebar"}]
      [:> nav-item* {:label (tr "ds.shell.nav.templates")
                     :icon icons/layout*
                     :is-current (= current :templates)
                     :on-click go-templates
                     :test-id "ds-shell-nav-templates"}]
      (when ^boolean can-edit
        [:> nav-item* {:label (tr "ds.shell.nav.deleted")
                       :icon icons/trash*
                       :is-current (= current :deleted)
                       :on-click go-deleted
                       :test-id "ds-shell-nav-deleted"}])]

     [:div {:class (stl/css :projects)
            :data-testid "pinned-projects"}
      [:div {:class (stl/css :projects-title)} (tr "ds.shell.projects.title")]
      (if (seq listed)
        ;; Upstream rows: click opens, double click renames, right click
        ;; opens the project menu, a dragged file can be dropped on one.
        [:ul {:class (stl/css :projects-list)}
         (for [item listed]
           [:> upstream/sidebar-project*
            {:item item
             :key (dm/str (:id item))
             :id (:id item)
             :team-id team-id
             :is-selected (= (:id item) (:id project))}])]
        [:p {:class (stl/css :projects-empty)} (tr "ds.shell.projects.empty")])]

     [:div {:class (stl/css :foot)}
      [:> guard* {}
       [:> plan-card*]]
      ;; Upstream: the member, their menu and the comments button.
      [:> upstream/profile-section* {:profile profile :team team}]]]))
