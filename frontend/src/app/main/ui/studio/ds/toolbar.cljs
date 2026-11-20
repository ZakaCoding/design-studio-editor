;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.ds.toolbar
  "Toolbar of the Design Studio DS.

  `toolbar*`           role=toolbar; arrow keys move between its buttons.
  `toolbar-group*`     a set of related buttons inside a toolbar.
  `toolbar-separator*` hairline between groups.

  Buttons are `icon-button*` (or `button*`) from `app.main.ui.studio.ds.button`."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.util.dom :as dom]
   [app.util.keyboard :as kbd]
   [rumext.v2 :as mf]))

(def ^:private focusable-selector
  "button:not([disabled]), a[href], input:not([disabled])")

(defn- move-focus
  "Moves keyboard focus to the next or previous control of the toolbar,
  wrapping around. Does nothing when focus is not on one of them."
  [^js container ^js event step]
  (let [nodes (.from js/Array (.querySelectorAll container focusable-selector))
        total (.-length nodes)
        index (.indexOf nodes (dom/get-active))]
    (when (and (pos? total) (>= index 0))
      (dom/prevent-default event)
      (dom/focus! (aget nodes (mod (+ index step) total))))))

(def ^:private schema:toolbar
  [:map
   [:class {:optional true} [:maybe :string]]
   [:aria-label :string]
   [:orientation {:optional true} [:maybe [:enum "horizontal" "vertical"]]]
   [:variant {:optional true} [:maybe [:enum "floating" "plain"]]]])

(mf/defc toolbar*
  "Props: `aria-label` (required), `orientation` horizontal (default) |
  vertical, `variant` floating (default: its own island) | plain (no
  surface, for use inside a panel header). The rest goes to the <div>."
  {::mf/schema schema:toolbar}
  [{:keys [orientation variant class children] :rest props}]
  (let [orientation (d/nilv orientation "horizontal")
        vertical?   (= orientation "vertical")
        on-key-down
        (mf/use-fn
         (mf/deps vertical?)
         (fn [event]
           (let [container (dom/get-current-target event)]
             (cond
               (or (and vertical? (kbd/down-arrow? event))
                   (and (not vertical?) (kbd/right-arrow? event)))
               (move-focus container event 1)

               (or (and vertical? (kbd/up-arrow? event))
                   (and (not vertical?) (kbd/left-arrow? event)))
               (move-focus container event -1)))))
        props       (mf/spread-props props
                                     {:class [class (stl/css-case :toolbar true
                                                                  :toolbar-vertical vertical?
                                                                  :toolbar-plain (= variant "plain"))]
                                      :role "toolbar"
                                      :aria-orientation orientation
                                      :on-key-down on-key-down})]
    [:> "div" props children]))

(mf/defc toolbar-group*
  "Props: `aria-label` (optional, names the group), `class`."
  [{:keys [class children] :rest props}]
  (let [props (mf/spread-props props
                               {:class [class (stl/css :group)]
                                :role "group"})]
    [:> "div" props children]))

(mf/defc toolbar-separator*
  []
  [:span {:class (stl/css :separator)
          :role "separator"}])
