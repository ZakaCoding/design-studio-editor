;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns frontend-tests.studio.workspace-state-test
  (:require
   [app.main.ui.studio.workspace.state :as state]
   [cljs.test :as t :include-macros true]))

(t/deftest width-classes
  ;; F1 · 06: 1600 px and wider shows every panel, 1280 to 1599 px collapses
  ;; Layers to a rail, below that Craft overlays.
  (t/is (= :wide (state/width-class 1920)))
  (t/is (= :wide (state/width-class 1600)))
  (t/is (= :medium (state/width-class 1599)))
  (t/is (= :medium (state/width-class 1440)))
  (t/is (= :medium (state/width-class 1280)))
  (t/is (= :narrow (state/width-class 1279)))
  (t/is (= :narrow (state/width-class 1024))))

(t/deftest layers-rule-on-first-load
  (t/is (nil? (state/layers-action {:from nil :to :wide :collapsed? false :auto? false})))
  (t/is (= :collapse (state/layers-action {:from nil :to :medium :collapsed? false :auto? false})))
  (t/is (= :collapse (state/layers-action {:from nil :to :narrow :collapsed? false :auto? false})))
  ;; Already collapsed (the flag survives a file change): nothing to do.
  (t/is (nil? (state/layers-action {:from nil :to :medium :collapsed? true :auto? false}))))

(t/deftest layers-rule-on-resize
  (t/is (= :collapse (state/layers-action {:from :wide :to :medium :collapsed? false :auto? false})))
  (t/is (= :collapse (state/layers-action {:from :wide :to :narrow :collapsed? false :auto? false})))
  ;; The rule reopens what it closed.
  (t/is (= :expand (state/layers-action {:from :medium :to :wide :collapsed? true :auto? true})))
  (t/is (= :expand (state/layers-action {:from :narrow :to :wide :collapsed? true :auto? true}))))

(t/deftest layers-rule-keeps-the-persons-choice
  ;; Collapsed by hand on a wide window, then resized: stays collapsed.
  (t/is (nil? (state/layers-action {:from :medium :to :wide :collapsed? true :auto? false})))
  ;; Reopened by hand at 1440 px: a resize inside the rail widths leaves it open.
  (t/is (nil? (state/layers-action {:from :medium :to :narrow :collapsed? false :auto? false})))
  (t/is (nil? (state/layers-action {:from :narrow :to :medium :collapsed? false :auto? false})))
  ;; Same width class: never acts.
  (t/is (nil? (state/layers-action {:from :medium :to :medium :collapsed? false :auto? true})))
  (t/is (nil? (state/layers-action {:from :wide :to :wide :collapsed? true :auto? true}))))

(t/deftest craft-makes-room-at-the-rail-widths
  (t/is (= :collapse (state/craft-action {:width-class :medium :craft-open? true :collapsed? false})))
  (t/is (= :collapse (state/craft-action {:width-class :narrow :craft-open? true :collapsed? false})))
  ;; 1600 px and wider shows every panel.
  (t/is (nil? (state/craft-action {:width-class :wide :craft-open? true :collapsed? false})))
  (t/is (nil? (state/craft-action {:width-class :medium :craft-open? true :collapsed? true})))
  (t/is (nil? (state/craft-action {:width-class :medium :craft-open? false :collapsed? false}))))
