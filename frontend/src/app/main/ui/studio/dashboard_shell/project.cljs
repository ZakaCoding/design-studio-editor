;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard-shell.project
  "The project page chrome (F1 · 14): team / project name, the members,
  \"Undang\", \"+ File\", the tabs, the \"File n\" heading and the dashed
  \"+ File baru\" tile at the end of the grid.

  `project-header*` takes the place of the private `header*` of
  `app.main.ui.dashboard.files` and the same props (plus `file-count`).
  Rename, pin, the project menu and import are upstream's. The board's
  brand block and activity column need data Penpot does not have (owner,
  16 Nov: leave them out)."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.data.macros :as dm]
   [app.config :as cf]
   [app.main.data.common :as dcm]
   [app.main.data.dashboard :as dd]
   [app.main.data.event :as-alias ev]
   [app.main.data.nitrate :as dnt]
   [app.main.data.project :as dpj]
   [app.main.data.team :as dtm]
   [app.main.refs :as refs]
   [app.main.store :as st]
   [app.main.ui.dashboard.inline-edition :refer [inline-edition]]
   [app.main.ui.dashboard.layout-toggle :refer [layout-toggle*]]
   [app.main.ui.dashboard.pin-button :refer [pin-button*]]
   [app.main.ui.dashboard.project-menu :refer [project-menu*]]
   [app.main.ui.ds.foundations.assets.icon :as i]
   [app.main.ui.studio.dashboard-shell.page :as page]
   [app.main.ui.studio.ds :as sds]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [cuerdas.core :as str]
   [rumext.v2 :as mf]))

(def ^:private max-avatars 3)

(mf/defc members*
  "Up to three member avatars, overlapping (F1 · 14)."
  {::mf/private true}
  [{:keys [members]}]
  (when (seq members)
    [:ul {:class (stl/css :members)
          :aria-label (tr "labels.members")}
     (for [member (take max-avatars members)]
       [:li {:key (dm/str (:id member))
             :class (stl/css :member)}
        [:img {:class (stl/css :member-photo)
               :src (cf/resolve-profile-photo-url member)
               :alt (:name member)
               :title (:name member)}]])]))

(mf/defc project-header*
  [{:keys [team project can-edit create-fn]}]
  (let [team-id     (:id team)
        project-id  (:id project)
        profile     (mf/deref refs/profile)
        drafts?     (:is-default project)
        default?    (:is-default team)

        local*      (mf/use-state {:menu-open false :edition false :menu-pos nil})
        local       (deref local*)

        can-invite
        (and (not default?)
             (dnt/can-send-invitations?
              {:organization (:organization team)
               :profile-id (:id profile)
               :team-permissions (:permissions team)}))

        team-name
        (if ^boolean default?
          (tr "dashboard.personal-projects")
          (:name team))

        project-name
        (if ^boolean drafts?
          (tr "labels.drafts")
          (:name project))

        on-create
        (mf/use-fn
         (mf/deps create-fn)
         (fn [event]
           (dom/prevent-default event)
           (create-fn "dashboard:header")))

        on-invite
        (mf/use-fn
         (mf/deps team-id)
         #(st/emit! (dtm/check-and-invite-members {:team-id team-id
                                                   :origin :team})))

        on-menu-click
        (mf/use-fn
         (fn [event]
           (let [position (dom/get-client-position event)]
             (dom/prevent-default event)
             (swap! local* assoc :menu-open true :menu-pos position))))

        on-menu-close
        (mf/use-fn #(swap! local* assoc :menu-open false))

        on-edit
        (mf/use-fn #(swap! local* assoc :edition true :menu-open false))

        on-edit-end
        (mf/use-fn
         (mf/deps project)
         (fn [name]
           (let [name (str/trim name)]
             (when-not (str/empty? name)
               (st/emit! (-> (dd/rename-project (assoc project :name name))
                             (with-meta {::ev/origin "project"}))))
             (swap! local* assoc :edition false))))

        toggle-pin
        (mf/use-fn
         (mf/deps project)
         #(st/emit! (dd/toggle-project-pin project)))

        on-import
        (mf/use-fn
         (mf/deps project-id)
         (fn []
           (st/emit! (dpj/fetch-files project-id)
                     (dd/clear-selected-files))))

        go-libraries
        (mf/use-fn
         (mf/deps team-id)
         #(st/emit! (dcm/go-to-dashboard-libraries :team-id team-id)))

        go-members
        (mf/use-fn #(st/emit! (dcm/go-to-dashboard-members)))

        go-settings
        (mf/use-fn #(st/emit! (dcm/go-to-dashboard-settings)))

        title
        (if (and (:edition local) can-edit (not drafts?))
          (mf/html
           [:& inline-edition {:content (:name project)
                               :on-end on-edit-end
                               :max-length 250}])
          (mf/html
           [:span {:on-double-click (when (and can-edit (not drafts?)) on-edit)
                   :data-testid "project-title"}
            project-name]))

        actions
        (mf/html
         [:*
          (when-not ^boolean drafts?
            [:> pin-button* {:class (stl/css :pin)
                             :tab-index 0
                             :is-pinned (:is-pinned project)
                             :on-click toggle-pin}])

          (when ^boolean can-edit
            [:> sds/icon-button* {:aria-label (tr "dashboard.options")
                                  :icon-id i/menu
                                  :on-click on-menu-click
                                  :data-testid "ds-shell-project-menu"}])

          (when-not ^boolean default?
            [:> members* {:members (:members team)}])

          (when ^boolean can-invite
            [:> page/soft-button* {:on-click on-invite
                                   :data-testid "ds-shell-invite"}
             (tr "ds.shell.project.invite")])

          (when ^boolean can-edit
            [:> page/black-button* {:plus true
                                    :on-click on-create
                                    :data-testid "new-file"}
             (tr "ds.shell.project.new-file")])

          (when ^boolean can-edit
            [:> project-menu* {:project project
                               :show (:menu-open local)
                               :left (- (:x (:menu-pos local)) 180)
                               :top (:y (:menu-pos local))
                               :on-edit on-edit
                               :on-close on-menu-close
                               :on-import on-import}])])]

    (mf/with-effect [team-id default?]
      (when-not ^boolean default?
        (st/emit! (dtm/fetch-members team-id))))

    [:*
     [:> page/page-header* {:eyebrow (dm/str team-name "  /")
                            :title title
                            :title-id (if ^boolean drafts?
                                        "dashboard-drafts-title"
                                        (dm/str project-id))
                            :actions actions}]
     [:> page/tabs* {:aria-label project-name}
      [:> page/tab* {:is-current true
                     :data-testid "ds-shell-tab-files"}
       (tr "ds.shell.project.tab.files")]
      [:> page/tab* {:is-current false
                     :on-click go-libraries
                     :data-testid "ds-shell-tab-libraries"}
       (tr "ds.shell.project.tab.libraries")]
      (when-not ^boolean default?
        [:> page/tab* {:is-current false
                       :on-click go-members
                       :data-testid "ds-shell-tab-members"}
         (tr "ds.shell.project.tab.members")])
      (when-not ^boolean default?
        [:> page/tab* {:is-current false
                       :on-click go-settings
                       :data-testid "ds-shell-tab-settings"}
         (tr "ds.shell.project.tab.settings")])]]))

(mf/defc files-heading*
  "\"File 6\" above the grid, with upstream's grid / list switch."
  [{:keys [file-count layout on-change]}]
  [:> page/section-title*
   {:title (tr "ds.shell.project.files")
    :count (when (pos? file-count) file-count)
    :action (mf/html
             [:> layout-toggle* {:layout layout
                                 :on-change on-change}])}])

(mf/defc new-file-tile*
  "The dashed \"+ File baru\" tile at the end of the file grid."
  [{:keys [create-fn]}]
  (let [on-click
        (mf/use-fn
         (mf/deps create-fn)
         (fn [event]
           (dom/stop-propagation event)
           (create-fn "dashboard:grid-new-file-tile")))]
    [:button {:type "button"
              :class (stl/css :new-file-tile)
              :on-click on-click
              :data-ds-dash "new-file-tile"
              :data-testid "ds-shell-new-file-tile"}
     (tr "ds.shell.project.new-file-tile")]))
