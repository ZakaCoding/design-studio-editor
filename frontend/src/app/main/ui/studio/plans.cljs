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
   [app.main.ui.ds.buttons.button :refer [button*]]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [app.util.keyboard :as kbd]
   [rumext.v2 :as mf]))

(def plans-uri "/studio/craft/plans")

(def plans
  "Order and translation keys of the plan summary. Lifetime is paused for now."
  [{:id :free     :title "ds.plans.free.title"     :desc "ds.plans.free.desc"}
   {:id :monthly  :title "ds.plans.monthly.title"  :desc "ds.plans.monthly.desc"
    :price "ds.plans.monthly.price"}
   {:id :lifetime :title "ds.plans.lifetime.title" :desc "ds.plans.lifetime.desc"
    :soon true}])

(mf/defc plan-card*
  [{:keys [plan]}]
  [:li {:class (stl/css-case :card true :card-soon (:soon plan))
        :data-testid (str "ds-plan-" (name (:id plan)))}
   [:h3 {:class (stl/css :card-title)} (tr (:title plan))]
   (when-let [price (:price plan)]
     [:p {:class (stl/css :card-price)} (tr price)])
   [:p {:class (stl/css :card-desc)} (tr (:desc plan))]])

(mf/defc plans-page*
  []
  (mf/with-effect []
    (dom/set-html-title (tr "ds.plans.title")))

  (let [on-see-plans
        (mf/use-fn
         #(set! (.-href js/location) plans-uri))]
    [:section {:class (stl/css :plans)
               :aria-labelledby "ds-plans-title"}
     [:h2 {:id "ds-plans-title" :class (stl/css :title)} (tr "ds.plans.title")]
     [:p {:class (stl/css :lead)} (tr "ds.plans.lead")]
     [:ul {:class (stl/css :cards)}
      (for [plan plans]
        [:> plan-card* {:key (name (:id plan)) :plan plan}])]
     [:> button* {:variant "primary"
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
