;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.ds.button
  "Buttons of the Design Studio DS.

  `button*`      text button, optional leading icon.
  `icon-button*` square icon-only button; `aria-label` is required."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.main.ui.studio.ds.icon :refer [icon*]]
   [app.util.dom :as dom]
   [rumext.v2 :as mf]))

(def ^:private schema:button
  [:map
   [:class {:optional true} [:maybe :string]]
   [:variant {:optional true} [:maybe [:enum "primary" "secondary" "ghost" "danger"]]]
   [:size {:optional true} [:maybe [:enum "sm" "md" "lg"]]]
   [:type {:optional true} [:maybe [:enum "button" "submit" "reset"]]]
   [:icon-id {:optional true} [:maybe :string]]
   [:to {:optional true} [:maybe :string]]
   [:is-loading {:optional true} [:maybe :boolean]]
   [:is-full-width {:optional true} [:maybe :boolean]]])

(mf/defc button*
  "Props: `variant` primary (default) | secondary | ghost | danger;
  `size` sm (28) | md (32, default) | lg (44, auth and dashboard forms);
  `icon-id` Penpot sprite id for a leading icon; `to` renders an anchor;
  `is-loading` shows a spinner, sets `aria-busy` and ignores clicks
  (it also stops a submit);
  `is-full-width`; everything else (`on-click`, `disabled`, `aria-*`,
  `data-*`) goes to the element."
  {::mf/schema schema:button}
  [{:keys [variant size type icon-id to is-loading is-full-width class children] :rest props}]
  (let [variant  (d/nilv variant "primary")
        size     (d/nilv size "md")
        loading? (true? is-loading)
        link?    (some? to)
        element  (if link? "a" "button")
        class'   (stl/css-case :button true
                               :variant-primary (= variant "primary")
                               :variant-secondary (= variant "secondary")
                               :variant-ghost (= variant "ghost")
                               :variant-danger (= variant "danger")
                               :size-sm (= size "sm")
                               :size-lg (= size "lg")
                               :full-width (true? is-full-width)
                               :is-loading loading?)
        props    (mf/spread-props props
                                  {:class [class class']
                                   :href to
                                   :type (when-not link? (d/nilv type "button"))
                                   :aria-busy (when loading? true)})
        ;; A busy button keeps its look and its place in the tab order, but
        ;; neither runs its handler nor submits its form.
        props    (if loading?
                   (mf/spread-props props {:on-click dom/prevent-default})
                   props)]
    [:> element props
     (cond
       loading?        [:span {:class (stl/css :spinner) :aria-hidden true}]
       (some? icon-id) [:> icon* {:icon-id icon-id :size "m"}])
     [:span {:class (stl/css :label)} children]]))

(def ^:private schema:icon-button
  [:map
   [:class {:optional true} [:maybe :string]]
   [:aria-label :string]
   [:icon-id {:optional true} [:maybe :string]]
   [:variant {:optional true} [:maybe [:enum "primary" "secondary" "ghost" "danger"]]]
   [:size {:optional true} [:maybe [:enum "sm" "md" "lg"]]]
   [:is-active {:optional true} [:maybe :boolean]]])

(mf/defc icon-button*
  "Square icon-only button. Props: `aria-label` (required), `icon-id` (Penpot
  sprite id) or one child (an `icon*`), `variant` (default ghost), `size`,
  `is-active` (toggle buttons: sets `aria-pressed` and the selected look).
  The rest goes to the <button>."
  {::mf/schema schema:icon-button}
  [{:keys [variant size icon-id is-active class children] :rest props}]
  (let [variant (d/nilv variant "ghost")
        size    (d/nilv size "md")
        class'  (stl/css-case :button true
                              :icon-only true
                              :variant-primary (= variant "primary")
                              :variant-secondary (= variant "secondary")
                              :variant-ghost (= variant "ghost")
                              :variant-danger (= variant "danger")
                              :size-sm (= size "sm")
                              :size-lg (= size "lg")
                              :is-active (true? is-active))
        props   (mf/spread-props props
                                 {:class [class class']
                                  :type "button"
                                  :aria-pressed (when (some? is-active) (true? is-active))})]
    [:> "button" props
     (if (some? icon-id)
       [:> icon* {:icon-id icon-id :size "m"}]
       children)]))
