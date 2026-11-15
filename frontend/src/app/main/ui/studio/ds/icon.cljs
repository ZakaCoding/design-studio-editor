;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.ds.icon
  "Icon wrapper of the Design Studio DS.

  Two sources, one box:
  - `icon-id`: an id of the Penpot icon sprite (`app.main.ui.ds.foundations.assets.icon`,
    e.g. `i/add`), drawn with `currentColor`;
  - children: your own SVG content (paths) in the `view-box` you pass
    (default \"0 0 24 24\", the lucide grid).

  Decorative by default (`aria-hidden`). Pass `label` when the icon is the
  only content that carries the meaning."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [rumext.v2 :as mf]))

(def ^:private sizes
  {"s" 12 "m" 16 "l" 20 "xl" 24})

(mf/defc icon*
  [{:keys [icon-id size label view-box class children]}]
  (let [size-px (get sizes size 16)
        sprite? (some? icon-id)]
    [:svg {:class [class (stl/css :icon)]
           :width size-px
           :height size-px
           :view-box (if sprite?
                       (str "0 0 " size-px " " size-px)
                       (or view-box "0 0 24 24"))
           :stroke-width (when-not sprite? 2)
           :stroke-linecap (when-not sprite? "round")
           :stroke-linejoin (when-not sprite? "round")
           :role (when (some? label) "img")
           :aria-label label
           :aria-hidden (when (nil? label) true)
           :focusable "false"}
     (if sprite?
       [:use {:href (str "#icon-" icon-id)
              :width size-px
              :height size-px}]
       children)]))
