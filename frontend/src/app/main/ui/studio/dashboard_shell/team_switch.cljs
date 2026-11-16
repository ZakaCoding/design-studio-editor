;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard-shell.team-switch
  "The team switcher of the dashboard sidebar (P2): one field, one menu.

  Upstream has two buttons side by side: the team name opens the team
  list, a separate \"⋮\" opens the team's actions. Here the field opens
  one menu with both lists: the teams (switch, \"Create new team\") and,
  for a team that is not the personal one, its actions (members,
  invitations, settings, rename, leave, delete). Both lists are the
  upstream components, so every action, check and test hook stays."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.common.data.macros :as dm]
   [app.config :as cf]
   [app.main.refs :as refs]
   [app.main.ui.dashboard.sidebar :as upstream]
   [app.main.ui.studio.dashboard.icons :as icons]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [app.util.keyboard :as kbd]
   [app.util.timers :as ts]
   [rumext.v2 :as mf]))

(defn- menu-items
  [panel]
  (when panel
    (vec (dom/query-all panel "[role=menuitem]"))))

(defn- focus-step!
  "Moves the focus `step` items through every item of both lists."
  [panel step]
  (let [items  (menu-items panel)
        active (dom/get-active)
        index  (d/index-of-pred items #(identical? % active))
        target (cond
                 (empty? items) nil
                 (nil? index)   (if (pos? step) (first items) (peek items))
                 :else          (get items (mod (+ index step) (count items))))]
    (some-> target dom/focus!)))

(mf/defc team-switch*
  [{:keys [team profile]}]
  (let [nitrate?   (contains? cf/flags :admin-console)
        org-id     (when nitrate? (dm/get-in team [:organization :id]))
        teams      (cond->> (mf/deref refs/teams)
                     nitrate? (filter #(= (dm/get-in (val %) [:organization :id]) org-id))
                     nitrate? (into {}))
        default?   (:is-default team)

        open*      (mf/use-state false)
        open?      (deref open*)
        panel-ref  (mf/use-ref nil)

        close
        (mf/use-fn #(reset! open* false))

        on-toggle
        (mf/use-fn
         (fn [event]
           ;; The lists close on any click on the page; this one toggles.
           (dom/stop-propagation event)
           (swap! open* not)))

        ;; Opened with the keyboard: the first item takes the focus.
        on-button-key-down
        (mf/use-fn
         (fn [event]
           (when (kbd/down-arrow? event)
             (dom/prevent-default event)
             (dom/stop-propagation event)
             (reset! open* true)
             (ts/schedule 0 #(focus-step! (mf/ref-val panel-ref) 1)))))

        ;; Arrow keys walk through both lists as one menu. The upstream
        ;; lists listen on the document; the keys stop here.
        on-panel-key-down
        (mf/use-fn
         (fn [event]
           (let [panel (mf/ref-val panel-ref)]
             (cond
               (kbd/down-arrow? event)
               (do (dom/prevent-default event)
                   (dom/stop-propagation event)
                   (focus-step! panel 1))

               (kbd/up-arrow? event)
               (do (dom/prevent-default event)
                   (dom/stop-propagation event)
                   (focus-step! panel -1))

               (kbd/enter? event)
               (do (dom/prevent-default event)
                   (dom/stop-propagation event)
                   (some-> (dom/get-active) dom/click))

               (kbd/tab? event)
               (close)))))]

    [:div {:class (stl/css :switch)
           :data-testid "ds-team-switch"}
     [:button {:type "button"
               :class (stl/css-case :current true
                                    :current-open open?)
               :aria-haspopup "menu"
               :aria-expanded open?
               :on-click on-toggle
               :on-key-down on-button-key-down
               :data-testid "ds-team-switch-button"}
      (if ^boolean default?
        [:span {:class (stl/css :avatar :avatar-personal) :aria-hidden true}]
        [:img {:class (stl/css :avatar)
               :src (cf/resolve-team-photo-url team)
               :alt ""}])
      [:span {:class (stl/css :name)
              :title (if default? (tr "dashboard.personal-projects") (:name team))}
       (if default? (tr "dashboard.personal-projects") (:name team))]
      [:> icons/chevrons* {:size "m" :class (stl/css :chevron)}]]

     (when ^boolean open?
       [:div {:class (stl/css :panel)
              :ref panel-ref
              :on-key-down on-panel-key-down
              :data-testid "ds-team-menu"}
        [:div {:class (stl/css :label)} (tr "ds.shell.team.switch")]
        [:> upstream/teams-selector-dropdown* {:show true
                                               :on-close close
                                               :class (stl/css :menu :menu-teams)
                                               :team team
                                               :profile profile
                                               :teams teams}]
        (when-not ^boolean default?
          [:*
           [:div {:class (stl/css :separator) :role "separator"}]
           [:div {:class (stl/css :label) :title (:name team)}
            (:name team)]
           [:> upstream/team-options-dropdown* {:show true
                                                :on-close close
                                                :class (stl/css :menu :menu-actions)
                                                :team team
                                                :profile profile}]])])]))
