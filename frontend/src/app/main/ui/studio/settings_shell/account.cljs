;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.settings-shell.account
  "What the Settings pages say about the member's account (P3D): the
  facts of the Account panel and the Plans page, from the shell state
  (`member`, `plans`, `apps`) and the profile, and how they are written.
  Pure but for the meter. Tested in
  `frontend-tests.studio.settings-account-test`."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.ui.studio.dashboard-shell.rules :as rules]
   [app.util.i18n :as i18n :refer [tr]]
   [cuerdas.core :as str]
   [rumext.v2 :as mf]))

(defn- group-thousands
  [n]
  (-> (str (js/Math.round n))
      (.replace (js/RegExp. "\\B(?=(\\d{3})+(?!\\d))" "g") ".")))

(defn format-price
  "Formats a plan entry of the shell state: US$ with 2 decimals, Rp with
  thousands separators. nil when the entry carries no price."
  [currency {:keys [priceUsdCents priceIdr]}]
  (cond
    (and (= "IDR" currency) (number? priceIdr))
    (str "Rp" (group-thousands priceIdr))

    (and (not= "IDR" currency) (number? priceUsdCents))
    (str "US$" (.toFixed (/ priceUsdCents 100) 2))))

(def contact-email "hello@zakacoding.dev")

(def ^:private plan-ids #{"free" "monthly" "lifetime"})

(def ^:private max-apps 3)

(defn- amount?
  [v]
  (and (number? v) (>= v 0)))

(defn- positive?
  [v]
  (and (number? v) (pos? v)))

(defn- ratio
  [part total]
  (-> (/ part total) (max 0) (min 1)))

(defn parse-date
  "A js/Date from a Date, an ISO string or epoch ms; nil when invalid."
  [value]
  (let [date (cond
               (instance? js/Date value) value
               (or (string? value) (number? value)) (js/Date. value))]
    (when (and (some? date) (not (js/isNaN (.getTime date))))
      date)))

(defn account-facts
  "What the Account panel shows, from the shell `state` and the `profile`.
  Each key is nil when its data is absent:

  - `:plan`    `{:id \"monthly\" :price \"US$5.49\" :per-month? true}`
               (`:price` nil for Free or without a price list)
  - `:renews`  js/Date of the next renewal (`member.renewsAt`)
  - `:credits` `{:left :total :ratio}` (Craft credits left)
  - `:storage` `{:used \"212 MB\" :total \"5 GB\" :ratio}`
  - `:apps`    `{:shown [app…] :more n}` (at most three shown)
  - `:since`   js/Date the account was made (profile `created-at`)"
  [state profile]
  (let [member  (:member state)
        plan    (some-> (:plan member) str str/lower)
        plan    (when (contains? plan-ids plan) plan)
        prices  (:plans state)
        entry   (when (and (some? plan) (not= "free" plan) (map? prices))
                  (get prices (keyword plan)))
        price   (when (map? entry)
                  (format-price (:currency prices) entry))
        credits (:credits member)
        storage (:storage member)
        apps    (let [apps (:apps state)]
                  (when (sequential? apps)
                    (filterv #(and (map? %) (some? (:name %))) apps)))]
    {:plan    (when (some? plan)
                {:id plan
                 :price price
                 :per-month? (= "monthly" plan)})
     :renews  (parse-date (:renewsAt member))
     :credits (when (and (amount? (:left credits)) (positive? (:total credits)))
                {:left  (:left credits)
                 :total (:total credits)
                 :ratio (ratio (:left credits) (:total credits))})
     :storage (when (and (amount? (:usedBytes storage)) (positive? (:totalBytes storage)))
                {:used  (rules/format-bytes (:usedBytes storage))
                 :total (rules/format-bytes (:totalBytes storage))
                 :ratio (ratio (:usedBytes storage) (:totalBytes storage))})
     :apps    (when (seq apps)
                {:shown (vec (take max-apps apps))
                 :more  (max 0 (- (count apps) max-apps))})
     :since   (parse-date (:created-at profile))}))

;; --- Formatting (Intl: inside the editor Date.prototype.toLocale* is
;; replaced upstream)

(defn- locale-tag [] (if (= "id" (deref i18n/locale)) "id-ID" "en-US"))

(defn format-number
  [n]
  (.format (js/Intl.NumberFormat. (locale-tag)) n))

(defn format-date
  "\"Dec 7, 2026\" / \"7 Des 2026\"."
  [date]
  (.format (js/Intl.DateTimeFormat. (locale-tag) #js {:day "numeric" :month "short" :year "numeric"}) date))

(defn format-month
  [date]
  (.format (js/Intl.DateTimeFormat. (locale-tag) #js {:month "short" :year "numeric"}) date))

(defn plan-title
  [id]
  (cond
    (= id "monthly")  (tr "ds.plans.monthly.title")
    (= id "lifetime") (tr "ds.plans.lifetime.title")
    :else             (tr "ds.plans.free.title")))

(mf/defc meter*
  [{:keys [ratio label tone]}]
  [:span {:class (stl/css-case :meter true
                               :meter-storage (= tone "storage"))
          :role "img"
          :aria-label label}
   [:span {:class (stl/css :meter-fill)
           :style {:inline-size (str (js/Math.round (* 100 ratio)) "%")}}]])

