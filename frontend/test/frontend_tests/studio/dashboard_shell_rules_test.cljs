;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns frontend-tests.studio.dashboard-shell-rules-test
  "The dashboard shell (F1 · 03, 12, 14): the greeting, the plan card of
  the sidebar, which page a dashboard URL means and what the sidebar lists."
  (:require
   [app.main.ui.studio.dashboard-shell.rules :as rules]
   [cljs.test :as t :include-macros true]))

(def ^:private mb (* 1024 1024))
(def ^:private gb (* 1024 mb))

(t/deftest greeting-follows-the-hour
  (t/testing "pagi, siang, sore, malam"
    (t/is (= :morning (rules/day-part 4)))
    (t/is (= :morning (rules/day-part 10)))
    (t/is (= :midday (rules/day-part 11)))
    (t/is (= :midday (rules/day-part 14)))
    (t/is (= :afternoon (rules/day-part 15)))
    (t/is (= :afternoon (rules/day-part 17)))
    (t/is (= :evening (rules/day-part 18)))
    (t/is (= :evening (rules/day-part 23)))
    (t/is (= :evening (rules/day-part 0)))
    (t/is (= :evening (rules/day-part 3))))
  (t/testing "the key says whether the name goes in"
    (t/is (= "ds.shell.greeting.afternoon-name" (rules/greeting-key 16 true)))
    (t/is (= "ds.shell.greeting.morning" (rules/greeting-key 8 false))))
  (t/testing "the first name only"
    (t/is (= "Zaka" (rules/first-name {:fullname "Zaka Coding"})))
    (t/is (= "Zaka" (rules/first-name {:fullname "  Zaka  "})))
    (t/is (nil? (rules/first-name {:fullname "   "})))
    (t/is (nil? (rules/first-name nil)))))

(t/deftest storage-reads-as-on-the-board
  (t/is (= "12 MB" (rules/format-bytes (* 12 mb))))
  (t/is (= "1 GB" (rules/format-bytes gb)))
  (t/is (= "1.5 GB" (rules/format-bytes (* 1.5 gb))))
  (t/is (= "512 KB" (rules/format-bytes (* 512 1024))))
  (t/is (= "0 B" (rules/format-bytes 0)))
  (t/is (= "0 B" (rules/format-bytes -5))))

(t/deftest plan-card-shows-what-member-carries
  (t/testing "F1 · 03: a paid plan shows the Craft credits that are left"
    (let [card (rules/plan-card {:plan "monthly"
                                 :credits {:left 812 :total 1000}
                                 :storage {:usedBytes (* 12 mb) :totalBytes gb}})]
      (t/is (= "Monthly" (:name card)))
      (t/is (= :credits (-> card :meter :kind)))
      (t/is (= 812 (-> card :meter :left)))
      (t/is (= 1000 (-> card :meter :total)))
      (t/is (< 0.81 (-> card :meter :ratio) 0.82))
      (t/is (false? (:plans? card)))))
  (t/testing "F1 · 12: a Free account shows its storage and the link to the plans"
    (let [card (rules/plan-card {:plan "free"
                                 :credits nil
                                 :storage {:usedBytes (* 12 mb) :totalBytes gb}})]
      (t/is (= "Free" (:name card)))
      (t/is (= {:kind :storage :used "12 MB" :total "1 GB"}
               (select-keys (:meter card) [:kind :used :total])))
      (t/is (true? (:plans? card)))))
  (t/testing "absent or null fields are left out"
    (t/is (= {:name "Lifetime" :meter nil :plans? false}
             (rules/plan-card {:plan "lifetime" :credits nil :storage nil})))
    (t/is (nil? (:name (rules/plan-card {:plan nil :credits {:left 3 :total 10}}))))
    (t/is (nil? (rules/plan-card nil)))
    (t/is (nil? (rules/plan-card {})))
    (t/is (nil? (rules/plan-card {:plan nil :credits nil :storage nil}))))
  (t/testing "numbers that make no meter"
    (t/is (nil? (:meter (rules/plan-card {:plan "monthly" :credits {:left 5 :total 0}}))))
    (t/is (nil? (:meter (rules/plan-card {:plan "monthly" :credits {:left "5" :total 10}}))))
    (t/is (= 1 (-> (rules/plan-card {:plan "monthly" :credits {:left 20 :total 10}}) :meter :ratio)))))

(t/deftest one-route-three-pages
  (t/is (= :home (rules/view nil)))
  (t/is (= :home (rules/view {})))
  (t/is (= :home (rules/view {:view "something"})))
  (t/is (= :projects (rules/view {:view "projects"})))
  (t/is (= :templates (rules/view {:view "templates"}))))

(t/deftest sidebar-marks-the-open-page
  (t/is (= :home (rules/nav-current :dashboard-recent :home)))
  (t/is (= :projects (rules/nav-current :dashboard-recent :projects)))
  (t/is (= :templates (rules/nav-current :dashboard-recent :templates)))
  (t/is (= :projects (rules/nav-current :dashboard-files :home)))
  (t/is (= :libraries (rules/nav-current :dashboard-libraries :home)))
  (t/is (= :libraries (rules/nav-current :dashboard-fonts :home)))
  (t/is (= :deleted (rules/nav-current :dashboard-deleted :home)))
  (t/is (nil? (rules/nav-current :dashboard-search :home)))
  (t/is (nil? (rules/nav-current :dashboard-members :home))))

(t/deftest top-bar-actions-by-page
  (t/is (true? (rules/top-actions? :dashboard-recent)))
  (t/is (true? (rules/top-actions? :dashboard-search)))
  (t/is (true? (rules/top-actions? :dashboard-deleted)))
  (t/is (false? (rules/top-actions? :dashboard-files)))
  (t/is (false? (rules/top-actions? :dashboard-members)))
  (t/is (true? (rules/library-section? :dashboard-fonts)))
  (t/is (false? (rules/library-section? :dashboard-recent))))

(t/deftest recent-files-newest-first
  (let [files {1 {:id 1 :modified-at 10}
               2 {:id 2 :modified-at 30}
               3 {:id 3 :modified-at 20}
               4 {:id 4 :modified-at 40 :deleted-at 41}}]
    (t/is (= [2 3] (mapv :id (rules/recent-files files 2))))
    (t/is (= [2 3 1] (mapv :id (rules/recent-files files 4))))
    (t/is (= [] (rules/recent-files nil 4)))))

(t/deftest sidebar-lists-real-projects
  (let [projects [{:id 1 :name "Drafts" :is-default true}
                  {:id 2 :name "website"}
                  {:id 3 :name "App" :is-pinned true}
                  {:id 4 :name "Brand"}
                  {:id 5 :name "Old" :deleted-at 1}]]
    (t/is (= [3 4 2] (mapv :id (rules/sidebar-projects projects))))
    (t/is (= [] (rules/sidebar-projects nil)))
    (t/is (= 1 (:id (rules/drafts projects))))
    (t/is (nil? (rules/drafts [])))))
