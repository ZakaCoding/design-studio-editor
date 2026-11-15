;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.ds.panel
  "Floating island of the Design Studio DS (F1 · 01): the white, rounded,
  softly shadowed surface the F3 workspace panels sit in."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [rumext.v2 :as mf]))

(def ^:private schema:panel
  [:map
   [:class {:optional true} [:maybe :string]]
   [:title {:optional true} [:maybe :string]]
   [:is-flush {:optional true} [:maybe :boolean]]])

(mf/defc panel*
  "Props: `title` (renders the header; also names the region), `actions`
  (element at the end of the header, e.g. `icon-button*`s), `footer`
  (element), `is-flush` (body without padding, for lists and trees),
  `class`. Without a `title`, pass `aria-label`. The rest goes to the
  <section>. The panel does not position itself: the layout that mounts it
  sets size and place, with `--ds-space-panel-edge` (12 px) to the viewport."
  {::mf/schema schema:panel}
  [{:keys [title actions footer is-flush class children] :rest props}]
  (let [title-id (mf/use-id)
        header?  (or (some? title) (some? actions))
        props    (mf/spread-props props
                                  {:class [class (stl/css :panel)]
                                   :aria-labelledby (when (some? title) title-id)})]
    [:> "section" props
     (when header?
       [:header {:class (stl/css :header)}
        (if (some? title)
          [:h2 {:class (stl/css :title) :id title-id} title]
          [:span {:class (stl/css :title)}])
        (when (some? actions)
          [:div {:class (stl/css :actions)} actions])])
     [:div {:class (stl/css-case :body true
                                 :body-flush (true? is-flush))}
      children]
     (when (some? footer)
       [:footer {:class (stl/css :footer)} footer])]))
