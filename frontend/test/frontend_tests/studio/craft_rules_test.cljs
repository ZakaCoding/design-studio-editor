;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns frontend-tests.studio.craft-rules-test
  "The Craft dock's width rules (F1 · 05, F1 · 06) and the quick prompt's
  shortcut and placement."
  (:require
   [app.main.ui.studio.craft.rules :as rules]
   [cljs.test :as t :include-macros true]))

(t/deftest dock-mode-by-window-width
  (t/is (= :dock (rules/mode 1920)))
  (t/is (= :dock (rules/mode 1600)))
  (t/is (= :dock (rules/mode 1280)) "1280–1599 px: still beside the design panel")
  (t/is (= :overlay (rules/mode 1279)) "below 1280 px Craft overlays")
  (t/is (= :overlay (rules/mode 720)))
  (t/is (= :sheet (rules/mode 719)) "a phone gets the bottom sheet"))

(t/deftest dock-inset
  (t/is (= 330 (rules/inset :dock 318)) "beside the design panel, one edge gap away")
  (t/is (= 12 (rules/inset :dock 0)) "design panel hidden")
  (t/is (= 12 (rules/inset :overlay 318)) "over the design panel")
  (t/is (= 12 (rules/inset :sheet 318))))

(t/deftest dock-width
  (t/testing "the default is the 440 px panel of F1 · 05"
    (t/is (= 440 (:base rules/width)))
    (t/is (= 440 (rules/clamp-width nil 1600 330)))
    (t/is (= 440 (rules/clamp-width js/NaN 1600 330)))
    (t/is (= 440 (rules/clamp-width 440 1600 330))))
  (t/testing "the chosen width stays between 360 and 560 px"
    (t/is (= 360 (rules/clamp-width 100 1600 330)))
    (t/is (= 560 (rules/clamp-width 960 1600 330)) "a width remembered by the old panel")
    (t/is (= 500 (rules/clamp-width 499.6 1600 330))))
  (t/testing "and inside the window, never under the minimum"
    (t/is (= {:min 360 :max 560} (rules/bounds 1600 330)))
    (t/is (= {:min 360 :max 376} (rules/bounds 400 12)))
    (t/is (= {:min 360 :max 360} (rules/bounds 320 12)))
    (t/is (= 376 (rules/clamp-width 440 400 12)))))

(t/deftest dock-resize-keys
  (let [limits {:min 360 :max 560}]
    (t/is (= 456 (rules/key-width "ArrowLeft" false 440 limits)) "the handle is the left edge: Left widens")
    (t/is (= 456 (rules/key-width "ArrowUp" false 440 limits)))
    (t/is (= 424 (rules/key-width "ArrowRight" false 440 limits)))
    (t/is (= 424 (rules/key-width "ArrowDown" false 440 limits)))
    (t/is (= 504 (rules/key-width "ArrowLeft" true 440 limits)) "Shift takes bigger steps")
    (t/is (= 560 (rules/key-width "ArrowLeft" true 540 limits)))
    (t/is (= 360 (rules/key-width "ArrowRight" false 364 limits)))
    (t/is (= 360 (rules/key-width "Home" false 440 limits)))
    (t/is (= 560 (rules/key-width "End" false 440 limits)))
    (t/is (nil? (rules/key-width "Enter" false 440 limits)))
    (t/is (nil? (rules/key-width "a" false 440 limits)))))

(t/deftest touch-buttons-move-clear
  (t/is (= 0 (rules/touch-shift 1282 1400)) "dock right of the canvas edge")
  (t/is (= 460 (rules/touch-shift 1282 830)) "dock over the canvas: clear of it by 8 px")
  (t/is (= 172 (rules/touch-shift 300 100)) "but 128 px of the buttons stay on screen")
  (t/is (= 0 (rules/touch-shift 100 0))))

(t/deftest quick-prompt-shortcut
  (let [key (fn [m] (rules/quick-key? (merge {:key "Enter" :ctrl? false :meta? false :shift? false :alt? false :repeat? false} m)))]
    (t/is (true? (key {:ctrl? true})))
    (t/is (true? (key {:meta? true})))
    (t/is (false? (key {})) "Enter alone is Penpot's start-editing")
    (t/is (false? (key {:ctrl? true :shift? true})))
    (t/is (false? (key {:ctrl? true :alt? true})))
    (t/is (false? (key {:ctrl? true :repeat? true})) "a held key opens it once")
    (t/is (false? (key {:ctrl? true :key "k"})) "Ctrl/Cmd+K stays create-component")))

(t/deftest quick-prompt-placement
  (let [view {:width 1440 :height 900}]
    (t/testing "under the selection, centred on it"
      (t/is (= {:left 420 :top 412}
               (rules/anchor {:x 500 :y 200 :width 200 :height 200} view))))
    (t/testing "above it when there is no room below"
      (t/is (= {:left 420 :top 456}
               (rules/anchor {:x 500 :y 600 :width 200 :height 280} view))))
    (t/testing "inside the window when the selection fills it"
      (t/is (= {:left 540 :top 756}
               (rules/anchor {:x 0 :y 0 :width 1440 :height 900} view))))
    (t/testing "never past the window's edges"
      (t/is (= {:left 12 :top 112}
               (rules/anchor {:x -400 :y 0 :width 100 :height 100} view)))
      (t/is (= {:left 1068 :top 112}
               (rules/anchor {:x 1500 :y 0 :width 100 :height 100} view)))
      (t/is (= {:left 12 :top 12}
               (rules/anchor {:x 0 :y -500 :width 100 :height 100} view))))
    (t/testing "in a window smaller than the prompt it keeps the top-left margin"
      (t/is (= {:left 12 :top 12}
               (rules/anchor {:x 0 :y 0 :width 10 :height 10} {:width 200 :height 100}))))))

(t/deftest quick-prompt-text
  (t/is (nil? (rules/clean-prompt "")))
  (t/is (nil? (rules/clean-prompt "  \n ")))
  (t/is (nil? (rules/clean-prompt nil)))
  (t/is (= "Make it bolder" (rules/clean-prompt "  Make it bolder \n")))
  (t/is (= rules/prompt-max (count (rules/clean-prompt (apply str (repeat 2500 "a")))))))
