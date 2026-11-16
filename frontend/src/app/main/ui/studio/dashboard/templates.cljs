;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard.templates
  "The \"Template Design Studio\" row of the dashboard home (F1 · 03c).
  Until Design Studio has templates of its own, the tiles are the
  editor's built-in templates and a tile adds one to Drafts with Penpot's
  own import dialog; \"See all\" goes to the templates page."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.data.dashboard :as dd]
   [app.main.data.event :as ev]
   [app.main.data.modal :as modal]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.studio.dashboard.rules :as rules]
   [app.util.i18n :refer [tr]]
   [okulary.core :as l]
   [rumext.v2 :as mf]))

(def ^:private ref:builtin-templates
  (l/derived :builtin-templates st/state))

(defn go-to-templates!
  "The templates page of the dashboard shell (`view=templates`)."
  [team-id]
  (st/emit! (rt/nav :dashboard-recent {:team-id team-id :view "templates"})))

(defn- import-template!
  [template team-id project-id]
  (st/emit!
   (ev/event {::ev/name "import-template-launch"
              ::ev/origin "studio:home"
              :template (:name template)})
   (modal/show
    {:type :import
     :project-id project-id
     :files []
     :template template
     :on-finish-import
     (fn []
       (st/emit! (dd/fetch-recent-files team-id)
                 (dd/fetch-projects team-id)))})))

(mf/defc templates-row*
  "Props: `team-id`, `project-id` (where a template is added: the team's
  Drafts). Renders nothing while there is no template to show."
  [{:keys [team-id project-id]}]
  (let [templates (mf/deref ref:builtin-templates)
        tiles     (mf/with-memo [templates]
                    (rules/template-tiles templates))

        on-all
        (mf/use-fn
         (mf/deps team-id)
         #(go-to-templates! team-id))

        on-tile
        (mf/use-fn
         (mf/deps tiles team-id project-id)
         (fn [event]
           (let [index (js/parseInt (.getAttribute (.-currentTarget event) "data-index") 10)]
             (when-let [template (:template (get tiles index))]
               (import-template! template team-id project-id)))))]

    (mf/with-effect []
      (st/emit! (dd/fetch-builtin-templates)))

    (when (seq tiles)
      [:section {:class (stl/css :templates)
                 :aria-labelledby "ds-templates-title"
                 :data-testid "ds-home-templates"}
       [:header {:class (stl/css :head)}
        [:h2 {:id "ds-templates-title" :class (stl/css :title)}
         (tr "ds.home.templates.title")]
        [:button {:class (stl/css :all)
                  :type "button"
                  :on-click on-all
                  :data-testid "ds-home-templates-all"}
         (tr "ds.home.see-all")]]
       [:ul {:class (stl/css :tiles)}
        (for [[index {:keys [template tone]}] (map-indexed vector tiles)]
          [:li {:key (str (:id template))}
           [:button {:class (stl/css :tile)
                     :type "button"
                     :data-tone tone
                     :data-index index
                     :data-testid "ds-home-template"
                     :title (tr "ds.home.templates.add" (:name template))
                     :on-click on-tile}
            [:span {:class (stl/css :tile-name)} (:name template)]]])]])))
