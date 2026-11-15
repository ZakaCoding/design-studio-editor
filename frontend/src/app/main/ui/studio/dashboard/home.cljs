;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard.home
  "The content of the dashboard home (F1 · 03, 03c, 12), inside the
  dashboard shell (`app.main.ui.studio.dashboard-shell`, which draws the
  sidebar, the greeting, search and \"+ File baru\").

  With files (F1 · 03, 03c), top to bottom:
    1. the Craft hero \"Mulai dari satu kalimat\" (members with Craft), or
       the start cards (members without);
    2. `recent`: the shell's \"File terbaru\" grid, passed in as an element;
    3. the \"Connect AI\" card, or its status bar once connected;
    4. the \"Template Design Studio\" row.

  With nothing yet (F1 · 12): the four start cards, the empty \"File
  kamu\" box and the \"Connect AI\" card.

  Mount (the one contract with the shell):

    [:> home* {:team team :projects projects :profile profile
               :recent [:> shell/recent-files* {...}]}]

  `home*` adds no page padding, background or scrolling of its own: the
  shell's page does. Every section is also exported on its own
  (`craft-hero*`, `start-cards*`, `empty-files*`, `connect-card*`,
  `templates-row*`) for a shell that lays the page out itself."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.data.common :as dcm]
   [app.main.data.dashboard :as dd]
   [app.main.data.event :as-alias ev]
   [app.main.data.project :as dpj]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.dashboard.import :as udi]
   [app.main.ui.studio.dashboard.connect-card :as connect-card]
   [app.main.ui.studio.dashboard.hero :as hero]
   [app.main.ui.studio.dashboard.icons :as icons]
   [app.main.ui.studio.dashboard.rules :as rules]
   [app.main.ui.studio.dashboard.templates :as templates]
   [app.main.ui.studio.guard :refer [guard*]]
   [app.main.ui.studio.shell-state :as ss]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def craft-hero* hero/craft-hero*)
(def connect-card* connect-card/connect-card*)
(def templates-row* templates/templates-row*)

(defn- create-file!
  "A blank file in `project-id`, opened in the editor."
  [team-id project-id origin]
  (let [on-success
        (fn [{:keys [id data]}]
          (st/emit! (dcm/go-to-workspace :team-id team-id
                                         :file-id id
                                         :page-id (get-in data [:pages 0]))))]
    (st/emit! (-> (dd/create-file (with-meta {:project-id project-id}
                                    {:on-success on-success}))
                  (with-meta {::ev/origin origin})))))

;; --- Start cards (F1 · 12 "Mulai desain pertamamu")

(mf/defc start-cards*
  "The four ways to start: blank file, from a template, import, Craft.
  Props: `team-id`, `project-id` (the team's Drafts), `has-craft` (the
  member has Craft: the fourth card opens it; without, it shows the
  plans), `price` (the Monthly price as text, optional)."
  [{:keys [team-id project-id has-craft price]}]
  (let [file-input (mf/use-ref nil)

        on-create
        (mf/use-fn
         (mf/deps team-id project-id)
         #(create-file! team-id project-id "studio:home-start"))

        on-template
        (mf/use-fn
         (mf/deps team-id)
         #(templates/go-to-templates! team-id))

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

        on-craft
        (mf/use-fn
         (mf/deps team-id project-id has-craft)
         (fn [_]
           (if has-craft
             (hero/start-with-craft! {:team-id team-id
                                      :project-id project-id
                                      :origin "studio:home-start"})
             (st/emit! (rt/nav :settings-subscription)))))]

    [:div {:class (stl/css :starts)
           :data-testid "ds-home-starts"}
     [:button {:class (stl/css :start)
               :type "button"
               :on-click on-create
               :data-testid "ds-home-create"}
      [:span {:class (stl/css :start-icon) :aria-hidden true}
       [:> icons/plus* {:size "l"}]]
      [:span {:class (stl/css :start-title)} (tr "ds.home.start.blank")]
      [:span {:class (stl/css :start-text)} (tr "ds.home.start.blank-text")]]

     [:button {:class (stl/css :start)
               :type "button"
               :on-click on-template
               :data-testid "ds-home-template-start"}
      [:span {:class (stl/css :start-icon) :aria-hidden true}
       [:> icons/frame* {:size "l"}]]
      [:span {:class (stl/css :start-title)} (tr "ds.home.start.template")]
      [:span {:class (stl/css :start-text)} (tr "ds.home.start.template-text")]]

     [:button {:class (stl/css :start)
               :type "button"
               :on-click on-import
               :data-testid "ds-home-import"}
      [:span {:class (stl/css :start-icon) :aria-hidden true}
       [:> icons/download* {:size "l"}]]
      [:span {:class (stl/css :start-title)} (tr "ds.home.start.import")]
      [:span {:class (stl/css :start-text)} (tr "ds.home.start.import-text")]]

     [:div {:class (stl/css :promo)
            :data-testid "ds-home-craft"}
      [:span {:class (stl/css :promo-title)}
       [:> icons/sparkle* {:size "l"}]
       (tr "ds.home.start.craft")]
      [:span {:class (stl/css :promo-text)}
       (cond
         has-craft     (tr "ds.home.start.craft-text-member")
         (some? price) (tr "ds.home.start.craft-text-price" price)
         :else         (tr "ds.home.start.craft-text"))]
      [:button {:class (stl/css :promo-cta)
                :type "button"
                :on-click on-craft
                :data-testid "ds-home-plans"}
       (if has-craft
         (tr "ds.home.start.craft-open")
         (tr "ds.home.start.craft-plans"))]]

     ;; Penpot's own file input; it has nothing to show.
     [:div {:hidden true}
      [:> udi/import-form* {:ref file-input
                            :project-id project-id
                            :on-finish-import on-finish-import}]]]))

;; --- Empty "File kamu" (F1 · 12)

(mf/defc empty-files*
  "Props: `team-id`, `project-id` (the team's Drafts)."
  [{:keys [team-id project-id]}]
  (let [on-create
        (mf/use-fn
         (mf/deps team-id project-id)
         #(create-file! team-id project-id "studio:empty-home"))]
    [:div {:class (stl/css :empty)
           :data-testid "ds-home-empty-files"}
     [:span {:class (stl/css :empty-art) :aria-hidden true}
      [:span {:class (stl/css :empty-sheet :empty-sheet-sky)}]
      [:span {:class (stl/css :empty-sheet :empty-sheet-yellow)}]
      [:span {:class (stl/css :empty-sheet :empty-sheet-mint)}]]
     [:div {:class (stl/css :empty-body)}
      [:h3 {:class (stl/css :empty-title)} (tr "ds.home.files.empty")]
      [:p {:class (stl/css :empty-text)} (tr "ds.home.files.empty-text")]
      [:button {:class (stl/css :empty-cta)
                :type "button"
                :on-click on-create
                :data-testid "ds-home-empty-create"}
       (tr "ds.home.files.create")]]]))

;; --- Home

(mf/defc home*
  "Props: `team`, `projects`, `profile` (as Penpot's `projects-section*`
  takes them) and `recent`, the element of the shell's \"File terbaru\"
  section (optional)."
  [{:keys [team projects recent]}]
  (let [team-id    (:id team)
        can-edit   (-> team :permissions :can-edit)
        project-id (:id (rules/drafts projects))
        is-empty   (rules/empty-home? projects can-edit)
        state      (ss/use-state)
        craft?     (true? (:craft state))
        price      (rules/price-label (:plans state))]

    (mf/with-effect [team]
      (let [tname (if (:is-default team)
                    (tr "dashboard.personal-projects")
                    (:name team))]
        (dom/set-html-title (tr "title.dashboard.projects" tname))))

    [:div {:class (stl/css :home)
           :data-testid (if is-empty "ds-empty-home" "ds-home")}
     (cond
       is-empty
       [:*
        [:section {:class (stl/css :section)
                   :aria-labelledby "ds-home-start-title"}
         [:h2 {:id "ds-home-start-title" :class (stl/css :section-title)}
          (tr "ds.home.start.title-first")]
         [:> guard* {}
          [:> start-cards* {:team-id team-id
                            :project-id project-id
                            :has-craft craft?
                            :price price}]]]
        [:section {:class (stl/css :section)
                   :aria-labelledby "ds-home-files-title"}
         [:h2 {:id "ds-home-files-title" :class (stl/css :section-title)}
          (tr "ds.home.files.title")]
         [:> empty-files* {:team-id team-id
                           :project-id project-id}]]]

       (and craft? can-edit (some? project-id))
       [:*
        [:> guard* {}
         [:> craft-hero* {:team-id team-id
                          :project-id project-id}]]
        recent]

       (and can-edit (some? project-id) (some? state))
       [:*
        [:section {:class (stl/css :section)
                   :aria-labelledby "ds-home-start-title"}
         [:h2 {:id "ds-home-start-title" :class (stl/css :section-title)}
          (tr "ds.home.start.title")]
         [:> guard* {}
          [:> start-cards* {:team-id team-id
                            :project-id project-id
                            :has-craft false
                            :price price}]]]
        recent]

       :else
       recent)

     [:div {:class (stl/css-case :connect true :connect-first is-empty)}
      [:> guard* {}
       [:> connect-card*]]]

     (when (and (not is-empty) can-edit (some? project-id))
       [:div {:class (stl/css :after-connect)}
        [:> guard* {}
         [:> templates-row* {:team-id team-id
                             :project-id project-id}]]])]))
