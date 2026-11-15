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

(t/deftest rail-is-asked-for-while-craft-is-open
  ;; "1280–1599 px Layers jadi rail 48 px selama Craft terbuka" (research, 9 Nov).
  (t/is (true? (state/rail? :medium true)))
  (t/is (true? (state/rail? :narrow true)))
  (t/is (false? (state/rail? :wide true)))
  (t/is (false? (state/rail? :medium false)))
  (t/is (false? (state/rail? :narrow nil)))
  (t/is (false? (state/rail? :wide false))))

(t/deftest layers-rule-collapses-and-reopens
  ;; Craft opens at 1440 px, or the window shrinks while Craft is open.
  (t/is (= :collapse (state/layers-action {:was-rail? false :rail? true :collapsed? false :auto? false})))
  ;; Craft closes, or the window grows to 1600 px: the rule reopens what it closed.
  (t/is (= :expand (state/layers-action {:was-rail? true :rail? false :collapsed? true :auto? true}))))

(t/deftest layers-rule-keeps-the-persons-choice
  ;; Collapsed by hand before Craft opened: stays collapsed after Craft closes.
  (t/is (nil? (state/layers-action {:was-rail? false :rail? true :collapsed? true :auto? false})))
  (t/is (nil? (state/layers-action {:was-rail? true :rail? false :collapsed? true :auto? false})))
  ;; Reopened by hand while Craft is open: nothing to reopen when Craft closes.
  (t/is (nil? (state/layers-action {:was-rail? true :rail? false :collapsed? false :auto? false})))
  ;; No change in the need for the rail: never acts.
  (t/is (nil? (state/layers-action {:was-rail? true :rail? true :collapsed? false :auto? true})))
  (t/is (nil? (state/layers-action {:was-rail? false :rail? false :collapsed? true :auto? true})))
  (t/is (nil? (state/layers-action {:was-rail? nil :rail? false :collapsed? false :auto? false}))))
