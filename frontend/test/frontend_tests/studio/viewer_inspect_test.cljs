;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns frontend-tests.studio.viewer-inspect-test
  "The pure helpers of the viewer Inspect panel (F1 · 11) and of the AI
  access page (F1 · 10)."
  (:require
   [app.main.ui.studio.ai-access :as ai]
   [app.main.ui.studio.viewer.inspect :as vi]
   [cljs.test :as t :include-macros true]))

(t/deftest padding-text
  (t/is (nil? (vi/padding-text {})))
  (t/is (= "10 22" (vi/padding-text {:p1 10 :p2 22 :p3 10 :p4 22})))
  (t/is (= "8" (vi/padding-text {:p1 8 :p2 8 :p3 8 :p4 8})))
  (t/is (= "1 2 3 4" (vi/padding-text {:p1 1 :p2 2 :p3 3 :p4 4}))))

(t/deftest font-text
  (t/is (nil? (vi/font-text nil)))
  (t/is (= "Geist 600 · 14 / 18.2"
           (vi/font-text {:font-family "Geist" :font-weight "600"
                          :font-size "14" :line-height "1.3"}))))

(t/deftest fill-info
  (t/is (= {:hex "#C8743A" :token "brand.cta"}
           (vi/fill-info {:fills [{:fill-color "#c8743a"}]
                          :applied-tokens {:fill "brand.cta"}})))
  (t/is (nil? (vi/fill-info {:fills []}))))

(t/deftest geometry-is-relative-to-the-frame
  (t/is (= {:w 150 :h 38 :x 20 :y 236}
           (vi/geometry {:id 2 :width 150 :height 38 :x 120 :y 336}
                        {:id 1 :x 100 :y 100})))
  (t/is (= {:w 512 :h 660 :x 0 :y 0}
           (vi/geometry {:id 1 :width 512 :height 660 :x 100 :y 100}
                        {:id 1 :x 100 :y 100}))))

(t/deftest parse-export
  (t/is (= {:type :png :scale 2 :suffix ""} (vi/parse-export "png-2")))
  (t/is (= {:type :svg :scale 1 :suffix ""} (vi/parse-export "svg-1"))))

(t/deftest ai-access-helpers
  (t/is (= "C" (ai/app-initial "claude")))
  (t/is (= "?" (ai/app-initial nil)))
  (t/is (= "claude" (ai/app-tone "Claude")))
  (t/is (= "other" (ai/app-tone "Cursor")))
  (t/is (= 0.812 (ai/credits-ratio {:left 812 :total 1000})))
  (t/is (nil? (ai/credits-ratio nil)))
  (t/is (= 1 (ai/credits-ratio {:left 2000 :total 1000}))))
