;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard-shell.home
  "The shell parts of the home page (F1 · 03, 03c, 12): the greeting, the
  search box and \"+ File baru\" of the top bar, and \"File terbaru\".

  The home content (Craft hero, Connect AI, start cards, templates) is
  `app.main.ui.studio.dashboard.home`; it renders inside `home-page*` and
  places `recent-files*` where the board has it."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.common.data.macros :as dm]
   [app.main.data.common :as dcm]
   [app.main.data.dashboard :as dd]
   [app.main.data.event :as-alias ev]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.dashboard.grid :refer [line-grid*]]
   [app.main.ui.studio.dashboard-shell.page :as page]
   [app.main.ui.studio.dashboard-shell.rules :as rules]
   [app.util.dom :as dom]
   [app.util.functions :as f]
   [app.util.i18n :refer [tr]]
   [app.util.keyboard :as kbd]
   [okulary.core :as l]
   [rumext.v2 :as mf]))

;; --- Search ("Cari file, proyek, komponen…")

(mf/defc search*
  "The search box of the top bar. Typing opens the upstream search page
  (`go-to-dashboard-search`), as the old sidebar field did."
  [{:keys [search-term]}]
  (let [term  (d/nilv search-term "")
        node  (mf/use-ref nil)
        emit! (mf/use-memo #(f/debounce st/emit! 500))

        on-change
        (mf/use-fn
         (fn [event]
           (emit! (dcm/go-to-dashboard-search :term (dom/get-target-val event)))))

        on-key-down
        (mf/use-fn
         (fn [event]
           (when (kbd/enter? event)
             (dom/prevent-default event)
             (st/emit! (dcm/go-to-dashboard-search :term (dom/get-target-val event))))))]

    ;; Leaving the search page empties the box; typing is never overwritten.
    (mf/with-effect [term]
      (when-let [input (mf/ref-val node)]
        (when (not= input (dom/get-active))
          (set! (.-value input) term))))

    [:div {:class (stl/css :search) :role "search"}
     [:input {:class (stl/css :search-input)
              :id "search-input"
              :type "text"
              :ref node
              :aria-label (tr "ds.shell.search.placeholder")
              :placeholder (tr "ds.shell.search.placeholder")
              :default-value term
              :auto-complete "off"
              :on-change on-change
              :on-key-down on-key-down
              :data-testid "ds-shell-search"}]]))

;; --- "+ File baru"

(defn use-create-file
  "Hook: a function `(fn [origin])` that creates a file in `project-id`
  with Penpot's own `create-file` and opens it in the workspace."
  [team-id project-id]
  (mf/use-fn
   (mf/deps team-id project-id)
   (fn [origin]
     (when (some? project-id)
       (let [on-success
             (fn [{:keys [id data]}]
               (st/emit! (dcm/go-to-workspace :team-id team-id
                                              :file-id id
                                              :page-id (dm/get-in data [:pages 0]))))]
         (st/emit! (-> (dd/create-file (with-meta {:project-id project-id}
                                         {:on-success on-success}))
                       (with-meta {::ev/origin origin}))))))))

(mf/defc new-file-button*
  "The black \"+ File baru\": a new file in `project-id` (the drafts on the
  home page), opened in the workspace."
  [{:keys [team-id project-id origin label test-id]}]
  (let [create   (use-create-file team-id project-id)
        on-click (mf/use-fn
                  (mf/deps create origin)
                  (fn [event]
                    (dom/stop-propagation event)
                    (create (d/nilv origin "studio:shell-new-file"))))]
    [:> page/black-button* {:plus true
                            :class (stl/css :new-file)
                            :on-click on-click
                            :disabled (nil? project-id)
                            :data-testid (d/nilv test-id "project-new-file")}
     (d/nilv label (tr "ds.shell.new-file"))]))

(mf/defc top-actions*
  "Search and \"+ File baru\", to the right of the page header. Mounted
  once by the dashboard for the pages that have them, so the search box
  keeps the focus while typing moves from one page to the search page."
  [{:keys [section team default-project search-term]}]
  (let [can-edit (dm/get-in team [:permissions :can-edit])]
    (when (rules/top-actions? section)
      [:div {:class (stl/css :top-actions)
             :data-ds-dash "top-actions"}
       [:> search* {:search-term search-term}]
       (when ^boolean can-edit
         [:> new-file-button* {:team-id (:id team)
                               :project-id (:id default-project)}])])))

;; --- Home page

(mf/defc home-page*
  "The home page: `page*` with the greeting as its title (\"Selamat sore,
  Zaka\"). `title` replaces the greeting (F1 · 12: \"Selamat datang,
  Zaka\"). Other props are those of `page*`."
  [{:keys [profile title children] :rest props}]
  (let [name  (rules/first-name profile)
        hour  (.getHours (js/Date.))
        title (cond
                (some? title) title
                (some? name)  (tr (rules/greeting-key hour true) name)
                :else         (tr (rules/greeting-key hour false)))
        props (mf/spread-props props {:title title
                                      :title-id "dashboard-projects-title"})]
    [:> page/page* props children]))

;; --- "File terbaru"

(def ^:private ref:recent-files
  (l/derived :recent-files st/state))

(mf/defc recent-files*
  "The newest files of the team as one row of cards (F1 · 03 \"File
  terbaru\", four at 1440 px), with \"Lihat semua →\" to the Projects page.
  The cards are the upstream grid items (thumbnail, rename, menu, drag).
  Renders nothing while the team has no file.

  `project` is where a file dropped on the row is imported (the drafts)."
  [{:keys [team project limit title]}]
  (let [team-id    (:id team)
        limit      (d/nilv limit 4)
        can-edit   (dm/get-in team [:permissions :can-edit])
        recent-map (mf/deref ref:recent-files)

        files
        (mf/with-memo [recent-map limit]
          (rules/recent-files recent-map limit))

        create     (use-create-file team-id (:id project))

        go-all
        (mf/use-fn
         (mf/deps team-id)
         #(st/emit! (rt/nav :dashboard-recent {:team-id team-id :view "projects"})))]

    (mf/with-effect [team-id]
      (st/emit! (dd/fetch-recent-files team-id)
                (dd/clear-selected-files)))

    (when (seq files)
      [:section {:class (stl/css :recent)
                 :data-ds-dash "files"
                 :data-testid "ds-shell-recent"}
       [:> page/section-title*
        {:title (d/nilv title (tr "ds.shell.recent.title"))
         :action (mf/html
                  [:> page/text-link* {:on-click go-all
                                       :data-testid "ds-shell-recent-all"}
                   (tr "ds.shell.recent.all")])}]
       [:> line-grid* {:project project
                       :team team
                       :files files
                       :create-fn create
                       :can-edit can-edit
                       :limit limit
                       :layout :grid}]])))
