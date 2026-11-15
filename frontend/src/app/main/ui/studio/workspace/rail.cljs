;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.workspace.rail
  "The Layers rail (F3-A; width rules of F1 · 06): what the left sidebar
  becomes while upstream's `:collapse-left-sidebar` flag is set.

  Upstream shows one small expand button in that state and nothing else, so
  the file menu and the way back to the dashboard are gone. The rail is the
  default at 1280 to 1599 px, so it keeps both, and adds one button per tab
  of the left sidebar. Every control emits the events upstream's left header
  and tab switcher emit; nothing here has state of its own.

  The element keeps the id `left-sidebar-aside` and a `data-width`, which
  the colour palette and the token colour picker read to place themselves."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.main.data.common :as dcm]
   [app.main.data.event :as ev]
   [app.main.data.modal :as modal]
   [app.main.data.workspace :as dw]
   [app.main.data.workspace.colors :as dc]
   [app.main.features :as features]
   [app.main.refs :as refs]
   [app.main.store :as st]
   [app.main.ui.ds.foundations.assets.icon :as i]
   [app.main.ui.studio.ds :as sds]
   [app.main.ui.workspace.main-menu :as main-menu]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

;; Inline size of the rail in CSS px. `chrome.scss` uses the same value
;; (`ws.$rail-size`).
(def ^:const rail-width 48)

(defn- expand
  []
  (st/emit! (dw/remove-layout-flag :collapse-left-sidebar)))

(defn- go-back
  "Same events as the logo of upstream's left header."
  []
  (st/emit! (dc/stop-picker)
            (modal/hide)
            :interrupt
            (dw/set-options-mode :design)
            (dcm/go-to-dashboard-recent)))

(defn- open-tab
  "Expands the sidebar on the given tab, as the tab switcher does."
  [event]
  (let [id (-> (dom/get-current-target event)
               (dom/get-data "tab"))]
    (st/emit! (dcm/go-to-workspace :layout (keyword id))
              (dw/remove-layout-flag :collapse-left-sidebar))
    (when (= id "tokens")
      (st/emit! (ev/event {::ev/name "open-tokens-tab"})))))

(mf/defc rail-tab*
  {::mf/private true}
  [{:keys [id label icon-id is-active]}]
  [:> sds/tooltip* {:content label :placement "right"}
   [:> sds/icon-button* {:aria-label label
                         :icon-id icon-id
                         :is-active is-active
                         :data-tab id
                         :data-testid (str "ds-rail-tab-" id)
                         :on-click open-tab}]])

(mf/defc layers-rail*
  {::mf/memo true}
  [{:keys [layout file]}]
  (let [options-mode   (mf/deref refs/options-mode-global)
        design-tokens? (features/use-feature "design-tokens/v1")
        inspect?       (= options-mode :inspect)

        section        (cond
                         (or inspect? (contains? layout :layers)) "layers"
                         (contains? layout :assets) "assets"
                         (contains? layout :tokens) "tokens")]

    [:aside {:id "left-sidebar-aside"
             :class (stl/css :rail)
             :data-testid "left-sidebar-rail"
             :data-width (str rail-width)
             :aria-label (tr "ds.workspace.rail.label")
             :on-context-menu dom/prevent-default-context-menu}

     [:> sds/tooltip* {:content (tr "ds.workspace.rail.back") :placement "right"}
      [:button {:class (stl/css :rail-logo)
                :type "button"
                :aria-label (tr "ds.workspace.rail.back")
                :data-testid "ds-rail-back"
                :on-click go-back}
       [:> sds/logo-mark* {:size 24}]]]

     ;; Upstream's main menu, unchanged. Its dropdown is placed from
     ;; `--left-sidebar-width`, which the stylesheet sets for the rail.
     [:div {:class (stl/css :rail-menu)}
      [:> main-menu/menu* {:layout layout
                           :file file}]]

     [:div {:class (stl/css :rail-separator)
            :role "separator"}]

     [:> rail-tab* {:id "layers"
                    :label (tr "workspace.sidebar.layers")
                    :icon-id i/layers
                    :is-active (= section "layers")}]

     (when-not ^boolean inspect?
       [:> rail-tab* {:id "assets"
                      :label (tr "workspace.toolbar.assets")
                      :icon-id i/library
                      :is-active (= section "assets")}])

     (when (and ^boolean design-tokens? (not ^boolean inspect?))
       [:> rail-tab* {:id "tokens"
                      :label (tr "ds.workspace.tab.tokens")
                      :icon-id i/tokens
                      :is-active (= section "tokens")}])

     [:div {:class (stl/css :rail-spacer)}]

     [:> sds/tooltip* {:content (tr "workspace.sidebar.expand") :placement "right"}
      [:> sds/icon-button* {:aria-label (tr "workspace.sidebar.expand")
                            :icon-id i/arrow
                            :data-testid "ds-rail-expand"
                            :on-click expand}]]]))
