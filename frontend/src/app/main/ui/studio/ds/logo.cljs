;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.ds.logo
  "The official Design Studio logo. Brand rule: every Design Studio surface
  uses this component; nobody redraws the mark or restyles the wordmark.

  - Mark: `studio/public/frontdoor/mark.svg` of zakacoding-cloud, path for
    path (pen glyph on the ink tile, 64x64, radius 15).
  - Wordmark: always dual tone. \"Design\" in ink (#182421, semibold) and
    \"Studio\" in muted (#627169, regular), set in Inter."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [rumext.v2 :as mf]))

(def ^:private mark-sizes
  {"sm" 24 "md" 32 "lg" 40})

(mf/defc logo-mark*
  "The mark alone. `size` is in px. Give `label` when no wordmark or other
  text names the product next to it."
  [{:keys [size label class]}]
  (let [size (d/nilv size 32)]
    [:svg {:class [class (stl/css :mark)]
           :width size
           :height size
           :view-box "0 0 64 64"
           :role (when (some? label) "img")
           :aria-label label
           :aria-hidden (when (nil? label) true)
           :focusable "false"}
     [:rect {:width 64 :height 64 :rx 15 :fill "#182421"}]
     [:g {:transform "translate(14 14) scale(1.5)"
          :fill "none"
          :stroke "#fff"
          :stroke-width 2.1
          :stroke-linecap "round"
          :stroke-linejoin "round"}
      [:path {:d "M15.707 21.293a1 1 0 0 1-1.414 0l-1.586-1.586a1 1 0 0 1 0-1.414l5.586-5.586a1 1 0 0 1 1.414 0l1.586 1.586a1 1 0 0 1 0 1.414z"}]
      [:path {:d "m18 13-1.375-6.874a1 1 0 0 0-.746-.776L3.235 2.028a1 1 0 0 0-1.207 1.207L5.35 15.879a1 1 0 0 0 .776.746L13 18"}]
      [:path {:d "m2.3 2.3 7.286 7.286"}]
      [:circle {:cx 11 :cy 11 :r 2}]]]))

(mf/defc logo*
  "Lockup (mark + wordmark) or, with `variant` \"mark\", the mark alone.
  `size`: \"sm\" (24 px mark), \"md\" (32, default), \"lg\" (40).
  `to` renders a link."
  [{:keys [variant size to class] :rest props}]
  (let [variant (d/nilv variant "lockup")
        size    (d/nilv size "md")
        mark?   (= variant "mark")
        element (if (some? to) "a" "span")
        props   (mf/spread-props props
                                 {:class [class (stl/css-case :logo true
                                                              :logo-link (some? to)
                                                              :size-sm (= size "sm")
                                                              :size-md (= size "md")
                                                              :size-lg (= size "lg"))]
                                  :href to})]
    [:> element props
     [:> logo-mark* {:size (get mark-sizes size 32)
                     :label (when mark? "Design Studio")}]
     (when-not mark?
       [:span {:class (stl/css :wordmark)}
        "Design"
        [:span {:class (stl/css :wordmark-muted)} " Studio"]])]))
