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
   [app.common.data.macros :as dm]
   [app.common.uri :as u]
   [app.config :as cf]
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

(defn- thumbnail-uri
  [template]
  (dm/str (u/join cf/public-uri (dm/str "images/thumbnails/template-" (:id template) ".jpg"))))

(mf/defc template-card*
  "One template: the preview in the frame of a file card and the name
  under it (P2). A click adds it to Drafts with Penpot's import dialog."
  [{:keys [template tone on-click test-id index]}]
  [:button {:class (stl/css :card)
            :type "button"
            :data-index index
            :data-testid test-id
            :title (tr "ds.home.templates.add" (:name template))
            :on-click on-click}
   [:span {:class (stl/css :preview) :data-tone tone}
    [:img {:class (stl/css :image)
           :src (thumbnail-uri template)
           :alt ""
           :loading "lazy"
           :decoding "async"}]]
   [:span {:class (stl/css :info)}
    [:span {:class (stl/css :name)} (rules/template-name (:name template))]
    [:span {:class (stl/css :meta)} (tr "ds.home.templates.add-short")]]])

(defn- use-import
  "Hook: the click handler of a card (`data-index` into `templates`)."
  [templates team-id project-id]
  (mf/use-fn
   (mf/deps templates team-id project-id)
   (fn [event]
     (let [index (js/parseInt (.getAttribute (.-currentTarget event) "data-index") 10)]
       (when-let [template (get templates index)]
         (import-template! template team-id project-id))))))

(mf/defc templates-row*
  "Props: `team-id`, `project-id` (where a template is added: the team's
  Drafts). Renders nothing while there is no template to show."
  [{:keys [team-id project-id]}]
  (let [templates (mf/deref ref:builtin-templates)
        tiles     (mf/with-memo [templates]
                    (rules/template-tiles templates))
        items     (mf/with-memo [tiles]
                    (mapv :template tiles))

        on-all
        (mf/use-fn
         (mf/deps team-id)
         #(go-to-templates! team-id))

        on-tile   (use-import items team-id project-id)]

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
           [:> template-card* {:template template
                               :tone tone
                               :index index
                               :test-id "ds-home-template"
                               :on-click on-tile}]])]])))

(mf/defc templates-grid*
  "Every built-in template as a grid of cards: the Templates page."
  [{:keys [team-id project-id]}]
  (let [templates (mf/deref ref:builtin-templates)
        items     (mf/with-memo [templates]
                    (rules/visible-templates templates))
        on-card   (use-import items team-id project-id)]

    (mf/with-effect []
      (st/emit! (dd/fetch-builtin-templates)))

    [:ul {:class (stl/css :grid)
          :data-testid "ds-templates-grid"}
     (for [[index template] (map-indexed vector items)]
       [:li {:key (str (:id template))}
        [:> template-card* {:template template
                            :tone (nth rules/template-tones (mod index (count rules/template-tones)))
                            :index index
                            :test-id "ds-template-card"
                            :on-click on-card}]])]))
