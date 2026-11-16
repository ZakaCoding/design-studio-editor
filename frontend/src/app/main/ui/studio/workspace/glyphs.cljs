;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.workspace.glyphs
  "Icons of the workspace chrome as drawn on boards F1 · 01 and F1 · 02:
  a 20 px grid and a 1.6 px stroke. The Penpot sprite has other shapes for
  the same tools, so these are paths of our own (read from the boards)."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [rumext.v2 :as mf]))

(def ^:private rounded-box
  "M5.5 4h9A2.5 2.5 0 0 1 17 6.5v7a2.5 2.5 0 0 1-2.5 2.5h-9A2.5 2.5 0 0 1 3 13.5v-7A2.5 2.5 0 0 1 5.5 4z")

(def ^:private paths
  {"select"  "M4 3l11.5 5.2-5 1.6-1.6 5z"
   "frame"   "M6.5 3v14M13.5 3v14M3 6.5h14M3 13.5h14"
   "rect"    "M6 3.5h8A2.5 2.5 0 0 1 16.5 6v8a2.5 2.5 0 0 1-2.5 2.5H6A2.5 2.5 0 0 1 3.5 14V6A2.5 2.5 0 0 1 6 3.5z"
   "ellipse" "M3.5 10a6.5 6.5 0 1 0 13 0a6.5 6.5 0 1 0-13 0z"
   "line"    "M4 16L16 4"
   "arrow"   "M4 16L16 4M9 4h7v7"
   "text"    "M4.5 5V4h11v1M10 4v12M7.5 16h5"
   "pen"     "M10 3l5 6-3.2 7H8.2L5 9zM10 3v6.5M9 10.5a1 1 0 1 0 2 0a1 1 0 1 0-2 0z"
   "curve"   "M3.5 15.5C6 6 9 15 11.5 9S15 5 16.5 4.5"
   "image"   (str rounded-box "M6.1 8.5a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0-2.8 0zM17 13l-4-4-7 7")
   "comment" "M4 15.5V6a2.5 2.5 0 0 1 2.5-2.5h7A2.5 2.5 0 0 1 16 6v5a2.5 2.5 0 0 1-2.5 2.5H7z"
   "chevron" "M6 8l4 4 4-4"
   "play"    "M6.5 4.5l9 5.5-9 5.5z"
   "panel"   (str rounded-box "M8 4v12")
   "craft"   "M10 2.5l1.7 5.8 5.8 1.7-5.8 1.7-1.7 5.8-1.7-5.8L2.5 10l5.8-1.7z"})

(mf/defc glyph*
  "Props: `id` (a key of `paths`), `size` in px (default 20), `class`."
  [{:keys [id size class]}]
  (let [size (or size 20)]
    [:svg {:class [class (stl/css :glyph)]
           :width size
           :height size
           :view-box "0 0 20 20"
           :aria-hidden true
           :focusable "false"}
     [:path {:d (get paths id)}]]))
