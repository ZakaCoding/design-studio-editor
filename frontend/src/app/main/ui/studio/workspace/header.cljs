;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.workspace.header
  "Headers of the two workspace panels as on board F1 · 01, and their
  parts, which the canvas focus islands of F1 · 02 reuse.

  Left: logo (back to the dashboard), project, file name with the main
  menu behind a chevron, and the button that collapses the panel.
  Right: who is here, the MCP status pill and the black \"Share\" button.

  `app.main.ui.workspace.sidebar` mounts `left-header*` and
  `right-header*` in place of upstream's headers, which stay in their files
  and unused. What upstream's right header also had moved to the floating
  toolbar (zoom, comments, viewer); the file history is in the main menu.
  Every control emits the events the upstream control emits."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.config :as cf]
   [app.main.data.common :as dcm]
   [app.main.data.event :as ev]
   [app.main.data.modal :as modal]
   [app.main.data.team :as dtm]
   [app.main.data.workspace :as dw]
   [app.main.data.workspace.colors :as dc]
   [app.main.data.workspace.mcp :as mcp]
   [app.main.refs :as refs]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.components.dropdown-menu :refer [dropdown-menu* dropdown-menu-item*]]
   [app.main.ui.ds.foundations.assets.icon :as i]
   [app.main.ui.exports.assets :refer [progress-widget]]
   [app.main.ui.studio.ds :as sds]
   [app.main.ui.studio.guard :refer [guard*]]
   [app.main.ui.studio.workspace.glyphs :refer [glyph*]]
   [app.main.ui.workspace.main-menu :as main-menu]
   [app.main.ui.workspace.presence :refer [active-sessions*]]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [app.util.keyboard :as kbd]
   [cuerdas.core :as str]
   [rumext.v2 :as mf]))

;; --- Parts

(defn- go-back
  "Same events as the logo of upstream's left header."
  []
  (st/emit! (dc/stop-picker)
            (modal/hide)
            :interrupt
            (dw/set-options-mode :design)
            (dcm/go-to-dashboard-recent)))

(mf/defc logo-button*
  "The logo, as the way back to the dashboard. Props: `size` in px."
  [{:keys [size]}]
  [:button {:class (stl/css :logo)
            :type "button"
            :aria-label (tr "ds.workspace.rail.back")
            :title (tr "ds.workspace.rail.back")
            :data-testid "ds-header-back"
            :on-click go-back}
   [:> sds/logo-mark* {:size size}]])

(defn- status-title
  [status]
  (cond
    (= status :saved)    (tr "workspace.header.saved")
    (= status :retrying) (tr "workspace.header.retrying")
    (= status :error)    (tr "workspace.header.save-error")
    :else                (tr "workspace.header.saving")))

(mf/defc file-title*
  "File name (double click renames it, as upstream), the save state as a
  dot, and upstream's main menu behind the chevron. Props: `file`,
  `layout`."
  [{:keys [file layout]}]
  (let [file-id   (:id file)
        file-name (:name file)

        ;; Nothing queued to save means the file is up to date.
        status    (or (:status (mf/deref refs/persistence)) :saved)

        editing*  (mf/use-state false)
        editing?  (deref editing*)
        input-ref (mf/use-ref nil)

        on-blur
        (mf/use-fn
         (mf/deps file-id)
         (fn [_]
           (let [value (str/trim (-> input-ref mf/ref-val dom/get-value))]
             (when (not= value "")
               (st/emit! (dw/rename-file file-id value)))
             (reset! editing* false))))

        on-key-down
        (mf/use-fn
         (mf/deps on-blur)
         (fn [event]
           (when (kbd/enter? event)
             (on-blur event))))

        on-double-click
        (mf/use-fn
         (fn [event]
           (dom/prevent-default event)
           (reset! editing* true)))]

    (mf/with-effect [editing?]
      (when ^boolean editing?
        (dom/select-text! (mf/ref-val input-ref))))

    [:div {:class (stl/css :file)}
     (if ^boolean editing?
       [:input {:class (stl/css :file-input)
                :type "text"
                :ref input-ref
                :auto-focus true
                :default-value (or file-name "")
                :on-blur on-blur
                :on-key-down on-key-down}]
       [:div {:class (stl/css :file-name)
              :title file-name
              :data-testid "ds-file-name"
              :on-double-click on-double-click}
        [:span {:class (stl/css-case :status true
                                     :status-saved (= status :saved)
                                     :status-error (= status :error))
                :title (status-title status)}]
        [:span {:class (stl/css :file-label)}
         file-name]])
     ;; Upstream's main menu, unchanged. The stylesheet draws its button
     ;; as the chevron of the boards.
     [:div {:class (stl/css :file-menu)}
      [:> main-menu/menu* {:layout layout
                           :file file}]]]))

(mf/defc mcp-pill*
  "MCP status of this tab: green while the assistant's connection is live
  here. The menu is upstream's (connect here), or the way to the AI access
  settings when MCP is not set up yet."
  []
  (let [state      (mf/deref refs/mcp)
        connected? (= "connected" (get state :connection-status))
        ready?     (and (contains? cf/flags :mcp)
                        (true? (get state :enabled))
                        (true? (get state :token-valid)))

        open*      (mf/use-state false)
        open?      (deref open*)

        on-toggle
        (mf/use-fn
         (fn [event]
           (dom/stop-propagation event)
           (swap! open* not)))

        on-close
        (mf/use-fn #(reset! open* false))

        on-connect
        (mf/use-fn
         #(st/emit! (mcp/connect-mcp)
                    (ev/event {::ev/name "connect-mcp-plugin"
                               ::ev/origin "workspace:toolbar"})))

        on-setup
        (mf/use-fn #(st/emit! (rt/nav :settings-integrations)))]

    [:div {:class (stl/css :mcp)}
     [:button {:class (stl/css :mcp-pill)
               :type "button"
               :aria-haspopup "menu"
               :aria-expanded open?
               :data-tool "mcp"
               :data-testid "ds-mcp-pill"
               :data-connected (if connected? "true" "false")
               :on-click on-toggle}
      [:span {:class (stl/css-case :mcp-dot true
                                   :mcp-dot-on connected?)
              :aria-hidden true}]
      (tr "workspace.toolbar.mcp")]
     [:> dropdown-menu* {:show open?
                         :on-close on-close
                         :class (stl/css :mcp-menu)}
      (cond
        connected?
        [:li {:class (stl/css :mcp-info)
              :role "presentation"}
         (tr "workspace.toolbar.mcp-connected")]

        ready?
        [:> dropdown-menu-item* {:class (stl/css :mcp-item)
                                 :on-click on-connect}
         (tr "workspace.toolbar.mcp-connect-here")]

        :else
        [:> dropdown-menu-item* {:class (stl/css :mcp-item)
                                 :data-testid "ds-mcp-setup"
                                 :on-click on-setup}
         (tr "ds.workspace.mcp.setup")])]]))

(mf/defc share-button*
  "The black \"Share\" button. A team that can invite gets upstream's
  invite dialog; a personal team has nobody to invite, so the button opens
  the viewer, where upstream makes the share link. Props: `file-id`,
  `page-id`."
  [{:keys [file-id page-id]}]
  (let [team        (mf/deref refs/team)
        team-id     (:id team)
        permissions (get team :permissions)
        invite?     (and (not (:is-default team))
                         (or (:is-admin permissions)
                             (:is-owner permissions)))

        on-click
        (mf/use-fn
         (mf/deps team-id invite? file-id page-id)
         (fn []
           (if ^boolean invite?
             (st/emit! (dtm/check-and-invite-members {:team-id team-id
                                                      :origin :workspace}))
             (st/emit! (dcm/go-to-viewer {:page-id page-id
                                          :file-id file-id
                                          :section "interactions"})))))]
    [:button {:class (stl/css :share)
              :type "button"
              :data-testid "ds-share"
              :on-click on-click}
     (tr "ds.workspace.share")]))

(mf/defc sharing*
  "Who is here, the MCP pill and \"Share\": the content of the right header
  and of the top right island in canvas focus. Props: `file-id`,
  `page-id`."
  [{:keys [file-id page-id]}]
  [:*
   [:div {:class (stl/css :users)}
    [:> active-sessions*]]
   [:& progress-widget]
   [:> mcp-pill*]
   [:div {:class (stl/css :spacer)}]
   [:> share-button* {:file-id file-id
                      :page-id page-id}]])

;; --- Panel headers

(defn- collapse-left-sidebar
  []
  (st/emit! (dw/toggle-layout-flag :collapse-left-sidebar)))

(mf/defc left-header-content*
  {::mf/private true}
  [{:keys [file layout project class]}]
  (let [project-id (:id project)

        nav-to-project
        (mf/use-fn
         (mf/deps project-id)
         #(st/emit! :interrupt
                    (dcm/go-to-dashboard-files ::rt/new-window true :project-id project-id)))]

    [:header {:class [class (stl/css :left-header)]
              :data-testid "ds-left-header"}
     [:> logo-button* {:size 28}]
     [:div {:class (stl/css :titles)}
      [:button {:class (stl/css :project)
                :type "button"
                :on-click nav-to-project}
       (:name project)]
      [:> file-title* {:file file
                       :layout layout}]]
     (when (true? (:is-shared file))
       [:span {:class (stl/css :shared)
               :title (tr "workspace.assets.shared-library")}
        [:> sds/icon* {:icon-id i/library :size "m"}]])
     [:button {:class (stl/css :icon-action)
               :type "button"
               :aria-label (tr "workspace.sidebar.collapse")
               :title (tr "workspace.sidebar.collapse")
               :data-testid "ds-collapse-left"
               :on-click collapse-left-sidebar}
      [:> glyph* {:id "panel" :size 18}]]]))

(mf/defc left-header*
  "Props: those of upstream's `left-header*` (`file`, `layout`, `project`,
  `class`)."
  [{:keys [file layout project class]}]
  [:> guard* {}
   [:> left-header-content* {:file file
                             :layout layout
                             :project project
                             :class class}]])

(mf/defc right-header*
  "Props: those of upstream's `right-header*` (`file-id`, `page-id`,
  `layout`)."
  [{:keys [file-id page-id]}]
  [:> guard* {}
   [:div {:class (stl/css :right-header)
          :data-testid "ds-right-header"}
    [:> sharing* {:file-id file-id
                  :page-id page-id}]]])
