;; Design Studio (WP4): the Plans page and the "Power up" menu entry.
;; Replaces plansPage()/the power-up click hook in ds-shell.js (rows S7, S8).
;; The expectation survey is gone from the UI; `POST /studio/api/editor-shell/plans`
;; and the stored answers stay in Studio untouched.

(ns app.main.ui.studio.plans
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.refs :as refs]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.components.dropdown-menu :refer [dropdown-menu-item*]]
   [app.main.ui.studio.settings-shell.account :as acc]
   [app.main.ui.studio.settings-shell.page :refer [page-head* card*]]
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

(def format-price
  "Formats a plan entry of the shell state (see `settings-shell.account`)."
  acc/format-price)

(defn plan-price
  "What the price slot of a plan shows: {:amount \"US$5.49\"}, {:soon true} for a
  paused Lifetime, or nil (free plan, sales off, state unavailable)."
  [plans-state id]
  (when-let [entry (get plans-state id)]
    (let [amount (format-price (:currency plans-state) entry)]
      (cond
        (and (= :lifetime id) (true? (:paused entry))) {:soon true}
        amount {:amount amount}))))

(mf/defc stat*
  {::mf/private true}
  [{:keys [label value sub children test-id]}]
  [:div {:class (stl/css :stat) :data-testid test-id}
   [:span {:class (stl/css :stat-label)} label]
   [:span {:class (stl/css :stat-value)} value]
   (when (some? sub)
     [:span {:class (stl/css :stat-sub)} sub])
   children])

(mf/defc your-plan*
  "P3D: the member's plan in one card: plan and price, renewal, Craft
  credits and storage (lines without data are left out), and the way to
  the plans. No checkout here (owner rule)."
  {::mf/private true}
  [{:keys [facts on-see-plans]}]
  (let [{:keys [plan renews credits storage]} facts]
    [:> card* {:title (tr "ds.plans.current")
               :test-id "ds-plan-summary"
               :action (mf/html
                        [:button {:class (stl/css :see)
                                  :type "button"
                                  :data-testid "ds-see-plans"
                                  :on-click on-see-plans}
                         (tr "ds.plans.see-plans")])}
     [:div {:class (stl/css :stats)}
      [:> stat* {:label (tr "ds.settings.account.plan")
                 :value (if (some? plan) (acc/plan-title (:id plan)) "—")
                 :sub (when (some? (:price plan))
                        (str (:price plan)
                             (when (:per-month? plan) (str " " (tr "ds.plans.per-month")))))
                 :test-id "ds-plan-current"}]
      (when (some? renews)
        [:> stat* {:label (tr "ds.settings.account.renews")
                   :value (acc/format-date renews)}])
      (if (some? credits)
        (let [value (str (acc/format-number (:left credits)) " / " (acc/format-number (:total credits)))]
          [:> stat* {:label (tr "ds.settings.account.credits") :value value}
           [:> acc/meter* {:ratio (:ratio credits)
                           :label (str (tr "ds.settings.account.credits") ": " value)}]])
        [:> stat* {:label (tr "ds.settings.account.credits")
                   :value "—"
                   :sub (tr "ds.plans.credits-none")}])
      (when (some? storage)
        (let [value (tr "ds.settings.account.storage-value" (:used storage) (:total storage))]
          [:> stat* {:label (tr "ds.settings.account.storage") :value value}
           [:> acc/meter* {:ratio (:ratio storage)
                           :tone "storage"
                           :label (str (tr "ds.settings.account.storage") ": " value)}]]))]]))

(defn- credits-text
  [plans-state id]
  (let [n (get-in plans-state [id :credits])]
    (when (and (number? n) (pos? n))
      (if (= :monthly id)
        (tr "ds.plans.credits-cycle" (acc/format-number n))
        (tr "ds.plans.credits-once" (acc/format-number n))))))

(mf/defc yes*
  {::mf/private true}
  [{:keys [on?]}]
  (if ^boolean on?
    [:span {:class (stl/css :yes) :aria-label (tr "ds.plans.included")} "✓"]
    [:span {:class (stl/css :no) :aria-label (tr "ds.plans.not-included")} "—"]))

(mf/defc compare*
  "P3D: Free / Monthly / Lifetime side by side, only the facts the page
  already had (the plan texts and the public price list)."
  {::mf/private true}
  [{:keys [plans-state current]}]
  [:> card* {:title (tr "ds.plans.compare") :test-id "ds-plan-compare"}
   [:div {:class (stl/css :table-wrap)}
    [:table {:class (stl/css :table)}
     [:thead
      [:tr
       [:th {:scope "col" :class (stl/css :th-col)} [:span {:class (stl/css :visually-hidden)} (tr "ds.plans.compare")]]
       (for [plan plans]
         [:th {:key (name (:id plan))
               :scope "col"
               :class (stl/css-case :th-col true
                                    :col-current (= current (name (:id plan))))
               :data-testid (str "ds-plan-" (name (:id plan)))}
          (tr (:title plan))])]]
     [:tbody
      [:tr
       [:th {:scope "row" :class (stl/css :th-row)} (tr "ds.plans.row.editor")]
       (for [plan plans]
         [:td {:key (name (:id plan)) :class (stl/css-case :col-current (= current (name (:id plan))))}
          [:> yes* {:on? true}]])]
      [:tr
       [:th {:scope "row" :class (stl/css :th-row)} (tr "ds.plans.row.craft")]
       (for [plan plans]
         [:td {:key (name (:id plan)) :class (stl/css-case :col-current (= current (name (:id plan))))}
          [:> yes* {:on? (not= :free (:id plan))}]])]
      [:tr
       [:th {:scope "row" :class (stl/css :th-row)} (tr "ds.settings.account.credits")]
       (for [plan plans]
         [:td {:key (name (:id plan)) :class (stl/css-case :col-current (= current (name (:id plan))))}
          (or (credits-text plans-state (:id plan))
              [:> yes* {:on? false}])])]
      [:tr
       [:th {:scope "row" :class (stl/css :th-row)} (tr "ds.plans.row.price")]
       (for [plan plans]
         (let [id     (:id plan)
               amount (some->> (get plans-state id) (format-price (:currency plans-state)))]
           [:td {:key (name id) :class (stl/css-case :col-current (= current (name id)))}
            (cond
              (= :free id)         (tr "ds.plans.free.title")
              (nil? amount)        "—"
              (= :monthly id)      (str amount " " (tr "ds.plans.per-month"))
              :else                (str amount " " (tr "ds.plans.once")))]))]
      [:tr
       [:th {:scope "row" :class (stl/css :th-row)} (tr "ds.plans.row.status")]
       (for [plan plans]
         (let [id    (:id plan)
               price (plan-price plans-state id)]
           [:td {:key (name id) :class (stl/css-case :col-current (= current (name id)))}
            (cond
              (= current (name id))
              [:span {:class (stl/css :badge)} (tr "ds.plans.current")]

              (:soon price)
              [:span {:class (stl/css :soon)} (tr "ds.plans.opening-soon")]

              :else "")]))]]]]])

(mf/defc plans-page*
  []
  (mf/with-effect []
    (dom/set-html-title (tr "ds.plans.title")))

  (let [state (ss/use-state)
        profile (mf/deref refs/profile)
        plans-state (:plans state)
        facts (mf/with-memo [state profile]
                (acc/account-facts state profile))
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
     [:> your-plan* {:facts facts :on-see-plans on-see-plans}]
     [:> compare* {:plans-state plans-state :current current}]]))

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
