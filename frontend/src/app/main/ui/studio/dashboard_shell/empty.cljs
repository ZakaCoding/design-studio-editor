;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard-shell.empty
  "The empty state of a dashboard page (P2): an icon on a tile, a title,
  one line and an optional action. Library, Fonts and Trash use it in
  the place of upstream's placeholders."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.refs :as refs]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.studio.dashboard.icons :as icons]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(mf/defc empty-state*
  "`icon` is a line icon component (`app.main.ui.studio.dashboard.icons`),
  `title` and `text` strings, `action` the label of the button and
  `on-action` its handler (both optional). Other props go to the root."
  [{:keys [icon title text action on-action class children] :rest props}]
  (let [props (mf/spread-props props {:class [class (stl/css :empty)]})]
    [:> "div" props
     (when (some? icon)
       [:span {:class (stl/css :icon) :aria-hidden true}
        [:> icon {:size "l"}]])
     [:h2 {:class (stl/css :title)} title]
     (when (some? text)
       [:p {:class (stl/css :text)} text])
     (when (some? action)
       [:button {:type "button"
                 :class (stl/css :action)
                 :on-click on-action}
        action])
     children]))

(defn go-to-templates!
  "The Templates page of the current team."
  []
  (when-let [team-id (:id (deref refs/team))]
    (st/emit! (rt/nav :dashboard-recent {:team-id team-id :view "templates"}))))

(mf/defc libraries-empty*
  "Library page of a team without shared libraries. A member who can edit
  is pointed to the templates (each one can be added as a library)."
  [{:keys [can-edit]}]
  (let [on-templates (mf/use-fn go-to-templates!)]
    [:> empty-state* {:icon icons/library*
                      :title (tr "ds.shell.libraries.empty")
                      :text (if ^boolean can-edit
                              (tr "ds.shell.libraries.empty-text")
                              (tr "ds.shell.libraries.empty-text-viewer"))
                      :action (when ^boolean can-edit
                                (tr "ds.shell.libraries.open-templates"))
                      :on-action on-templates
                      :data-testid "ds-libraries-empty"}]))

(mf/defc fonts-empty*
  "Fonts page of a team without custom fonts (the upload is above it)."
  []
  [:> empty-state* {:icon icons/type*
                    :title (tr "ds.shell.fonts.empty")
                    :text (tr "ds.shell.fonts.empty-text")
                    :data-testid "ds-fonts-empty"}])

(mf/defc trash-empty*
  "Trash page with nothing in it. `days` is how long a deleted file stays."
  [{:keys [days]}]
  [:> empty-state* {:icon icons/trash*
                    :title (tr "ds.shell.trash.empty")
                    :text (tr "ds.shell.trash.empty-text" (str days))
                    :data-testid "ds-trash-empty"}])
