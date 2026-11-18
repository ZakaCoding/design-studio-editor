;; Design Studio (WP4): the Plans page and the "Power up" menu entry.
;; Replaces plansPage()/the power-up click hook in ds-shell.js (rows S7, S8).
;; The expectation survey is gone from the UI; `POST /studio/api/editor-shell/plans`
;; and the stored answers stay in Studio untouched.

(ns app.main.ui.studio.plans
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.components.dropdown-menu :refer [dropdown-menu-item*]]
   [app.main.ui.studio.settings-shell.page :refer [page-head*]]
   [app.main.ui.studio.shell-state :as ss]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [app.util.keyboard :as kbd]
   [rumext.v2 :as mf]))

(def plans-uri "/studio/craft/plans")

(def plans
  "Order and translation keys of the plan summary. Prices come from the shell
  state (`plans`), never from translations."
  [{:id :free     :title "ds.plans.free.title"     :desc "ds.plans.free.desc"}
   {:id :monthly  :title "ds.plans.monthly.title"  :desc "ds.plans.monthly.desc"
    :per "ds.plans.per-month"}
   {:id :lifetime :title "ds.plans.lifetime.title" :desc "ds.plans.lifetime.desc"}])

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

(defn plan-price
  "What the price slot of a plan shows: {:amount \"US$5.49\"}, {:soon true} for a
  paused Lifetime, or nil (free plan, sales off, state unavailable)."
  [plans-state id]
  (when-let [entry (get plans-state id)]
    (let [amount (format-price (:currency plans-state) entry)]
      (cond
        (and (= :lifetime id) (true? (:paused entry))) {:soon true}
        amount {:amount amount}))))

(mf/defc plan-card*
  [{:keys [plan price is-current]}]
  [:li {:class (stl/css-case :card true
                             :card-soon (:soon price)
                             :card-current is-current)
        :data-testid (str "ds-plan-" (name (:id plan)))}
   [:div {:class (stl/css :card-head)}
    [:h3 {:class (stl/css :card-title)} (tr (:title plan))]
    (when ^boolean is-current
      [:span {:class (stl/css :card-badge)
              :data-testid "ds-plan-current"}
       (tr "ds.plans.current")])]
   (cond
     (:soon price)
     [:p {:class (stl/css :card-price)} (tr "ds.plans.opening-soon")]

     (:amount price)
     [:p {:class (stl/css :card-price)}
      (:amount price)
      (when (:per plan) (str " " (tr (:per plan))))])
   [:p {:class (stl/css :card-desc)} (tr (:desc plan))]])

(mf/defc plans-page*
  []
  (mf/with-effect []
    (dom/set-html-title (tr "ds.plans.title")))

  (let [state (ss/use-state)
        plans-state (:plans state)
        ;; "free" | "monthly" | "lifetime", or nil when the state has no member
        current (get-in state [:member :plan])
        on-see-plans
        (mf/use-fn
         #(set! (.-href js/location) plans-uri))]
    [:section {:class (stl/css :plans)
               :aria-labelledby "ds-plans-title"}
     [:> page-head* {:id "ds-plans-title"
                     :title (tr "ds.plans.side")
                     :subtitle (tr "ds.plans.lead")}]
     [:ul {:class (stl/css :cards)}
      (for [plan plans]
        [:> plan-card* {:key (name (:id plan))
                        :plan plan
                        :is-current (= current (name (:id plan)))
                        :price (plan-price plans-state (:id plan))}])]
     [:button {:class (stl/css :see)
               :type "button"
               :data-testid "ds-see-plans"
               :on-click on-see-plans}
      (tr "ds.plans.see-plans")]]))

(mf/defc plans-menu-item*
  "Main-menu entry that opens Settings > Plans (was Penpot's nitrate power-up).
  The caller passes its own menu classes so the entry looks like its neighbours."
  [{:keys [class name-class on-pointer-enter]}]
  (let [go (mf/use-fn #(st/emit! (rt/nav :settings-subscription)))]
    [:> dropdown-menu-item* {:class class
                             :on-click go
                             :on-key-down (fn [event]
                                            (when (kbd/enter? event)
                                              (go)))
                             :on-pointer-enter on-pointer-enter
                             :data-testid "ds-plans-menu"
                             :id "file-menu-power-up"}
     [:span {:class name-class} (tr "ds.plans.menu")]]))
