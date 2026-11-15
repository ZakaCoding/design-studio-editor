;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.ds.tooltip
  "Tooltip of the Design Studio DS.

  Wraps one trigger. Shows on hover and on keyboard focus, hides on leave,
  blur and Esc, and stays open while the pointer is over the bubble
  (WCAG 1.4.13). Positioned with CSS next to the trigger, so an ancestor
  with `overflow: hidden` clips it: choose a `placement` that points into
  free space.

  A tooltip is a hint, never the only name of a control: icon-only buttons
  still need `aria-label`."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.util.keyboard :as kbd]
   [rumext.v2 :as mf]))

(def ^:private schema:tooltip
  [:map
   [:class {:optional true} [:maybe :string]]
   [:content :string]
   [:shortcut {:optional true} [:maybe :string]]
   [:placement {:optional true} [:maybe [:enum "top" "bottom" "left" "right"]]]])

(mf/defc tooltip*
  "Props: `content` (text, required), `shortcut` (optional key hint shown
  after the text, e.g. \"V\"), `placement` top (default) | bottom | left |
  right, `class` (on the wrapper). Children: the trigger."
  {::mf/schema schema:tooltip}
  [{:keys [content shortcut placement class children]}]
  (let [placement (d/nilv placement "top")
        visible*  (mf/use-state false)
        visible?  (deref visible*)
        show      (mf/use-fn #(reset! visible* true))
        hide      (mf/use-fn #(reset! visible* false))
        on-key-down
        (mf/use-fn
         (fn [event]
           (when (kbd/esc? event)
             (reset! visible* false))))]
    [:span {:class [class (stl/css :anchor)]
            :on-mouse-enter show
            :on-mouse-leave hide
            :on-focus show
            :on-blur hide
            :on-key-down on-key-down}
     children
     [:span {:class (stl/css-case :tooltip true
                                  :visible visible?
                                  :placement-top (= placement "top")
                                  :placement-bottom (= placement "bottom")
                                  :placement-left (= placement "left")
                                  :placement-right (= placement "right"))
             :role "tooltip"}
      content
      (when (some? shortcut)
        [:span {:class (stl/css :shortcut)} shortcut])]]))
