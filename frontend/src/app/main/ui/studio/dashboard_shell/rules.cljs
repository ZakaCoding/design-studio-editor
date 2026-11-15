;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard-shell.rules
  "The decisions of the dashboard shell, as pure functions: the greeting,
  what the plan card shows, which page a dashboard URL means.
  Tested in `frontend-tests.studio.dashboard-shell-rules-test`."
  (:require
   [cuerdas.core :as str]))

;; --- Greeting (F1 · 03: "Selamat sore, Zaka")

(defn day-part
  "The part of the day of a local `hour` (0 to 23)."
  [hour]
  (cond
    (and (>= hour 4) (< hour 11))  :morning
    (and (>= hour 11) (< hour 15)) :midday
    (and (>= hour 15) (< hour 18)) :afternoon
    :else                          :evening))

(defn first-name
  "The first word of the member's name, or nil."
  [profile]
  (let [fullname (str/trim (str (:fullname profile)))]
    (when-not (str/blank? fullname)
      (first (str/split fullname #"\s+")))))

(defn greeting-key
  "The translation key of the greeting. The `-name` keys take the name."
  [hour has-name]
  (str "ds.shell.greeting." (name (day-part hour)) (when has-name "-name")))

;; --- Plan card (contract: `member` of the editor-shell state)

(def ^:private plan-names
  {"free" "Free" "monthly" "Monthly" "lifetime" "Lifetime"})

(defn- positive?
  [v]
  (and (number? v) (pos? v)))

(defn- amount?
  [v]
  (and (number? v) (>= v 0)))

(defn format-bytes
  "A storage size as the boards write it: \"12 MB\", \"1 GB\", \"1.5 GB\"."
  [bytes]
  (let [units ["B" "KB" "MB" "GB" "TB"]
        [value unit]
        (loop [value (max 0 bytes) units units]
          (if (and (>= value 1024) (some? (next units)))
            (recur (/ value 1024) (next units))
            [value (first units)]))
        rounded (if (or (>= value 10) (= value (js/Math.floor value)))
                  (js/Math.round value)
                  (/ (js/Math.round (* value 10)) 10))]
    (str rounded " " unit)))

(defn- ratio
  [part total]
  (-> (/ part total) (max 0) (min 1)))

(defn plan-card
  "What the plan card of the sidebar shows, from `member`
  (`{:plan :credits {:left :total} :storage {:usedBytes :totalBytes}}`),
  or nil when there is nothing to show. Absent or null fields are left
  out. Credits win over storage: a paid plan shows what is left of Craft,
  a Free account its storage.

  - `:name`   \"Free\", \"Monthly\", \"Lifetime\"
  - `:meter`  `{:kind :credits :left :total :ratio}` or
              `{:kind :storage :used :total :ratio}` (sizes as text)
  - `:plans?` show the link to the plans (Free only)"
  [member]
  (let [plan    (some-> (:plan member) str str/lower)
        name    (get plan-names plan)
        credits (:credits member)
        storage (:storage member)
        meter   (cond
                  (and (amount? (:left credits)) (positive? (:total credits)))
                  {:kind  :credits
                   :left  (:left credits)
                   :total (:total credits)
                   :ratio (ratio (:left credits) (:total credits))}

                  (and (amount? (:usedBytes storage)) (positive? (:totalBytes storage)))
                  {:kind  :storage
                   :used  (format-bytes (:usedBytes storage))
                   :total (format-bytes (:totalBytes storage))
                   :ratio (ratio (:usedBytes storage) (:totalBytes storage))})]
    (when (or (some? name) (some? meter))
      {:name   name
       :meter  meter
       :plans? (= plan "free")})))

;; --- Pages

(defn view
  "The page of `/dashboard/recent`: `:home` (default), `:projects` or
  `:templates`, from the `view` query parameter."
  [query]
  (case (some-> (:view query) str)
    "projects"  :projects
    "templates" :templates
    :home))

(defn nav-current
  "The item of the sidebar that is current.

  `section` is the route name, `view` the result of `view`. A project and
  the drafts belong to \"Proyek\" (F1 · 14); fonts are a tab of the
  libraries."
  [section view]
  (case section
    :dashboard-recent    (case view
                           :projects  :projects
                           :templates :templates
                           :home)
    :dashboard-files     :projects
    :dashboard-libraries :libraries
    :dashboard-fonts     :libraries
    :dashboard-font-providers :libraries
    :dashboard-deleted   :deleted
    nil))

(defn recent-files
  "The `limit` most recently changed files of a `recent-files` map."
  [recent-map limit]
  (->> (vals recent-map)
       (remove :deleted-at)
       (sort-by :modified-at #(compare %2 %1))
       (take limit)
       (vec)))

(defn sidebar-projects
  "The projects of the sidebar list: no drafts, nothing deleted, pinned
  first, then by name."
  [projects]
  (->> projects
       (remove :deleted-at)
       (remove :is-default)
       (sort-by (juxt (comp not boolean :is-pinned)
                      (comp str/lower str :name)))
       (vec)))

(defn top-actions?
  "Whether a dashboard page has the search box and \"+ File baru\" in its
  top bar. A project has its own actions (F1 · 14); the team pages have
  their tabs there."
  [section]
  (contains? #{:dashboard-recent
               :dashboard-search
               :dashboard-libraries
               :dashboard-fonts
               :dashboard-font-providers
               :dashboard-deleted}
             section))

(defn library-section?
  "Library and Font share one item of the sidebar and a pair of tabs."
  [section]
  (contains? #{:dashboard-libraries
               :dashboard-fonts
               :dashboard-font-providers}
             section))

(defn drafts
  "The default project (\"Drafts\") of `projects`, or nil."
  [projects]
  (some #(when (:is-default %) %) projects))
