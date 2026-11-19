;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.settings-shell.layout
  "The frame of every Settings page (P3D, owner 19 Nov: rows + an info
  panel): the page column (up to 720 px) and the sticky Account panel on
  its right (280 px). Below 1180 px the panel is a one-line strip over the
  page (plan · credits · renews · storage).

  The panel reads only the shell state (`shell-state/use-state`, the
  dashboard's client) and the profile already in the app state: no
  endpoint of its own. A missing or null field leaves its line out
  (`settings-shell.account/account-facts`)."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.refs :as refs]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.studio.ai-access :as aa]
   [app.main.ui.studio.guard :refer [guard*]]
   [app.main.ui.studio.plans :as plans]
   [app.main.ui.studio.settings-shell.account :as acc]
   [app.main.ui.studio.shell-state :as ss]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

;; --- Parts

(mf/defc account-panel*
  "The Account panel. Renders its frame with what it has; while the shell
  state loads only the profile lines show."
  []
  (let [state   (ss/use-state)
        profile (mf/deref refs/profile)
        facts   (mf/with-memo [state profile]
                  (acc/account-facts state profile))
        {:keys [plan renews credits storage apps since]} facts

        go-ai   (mf/use-fn #(st/emit! (rt/nav :settings-integrations)))]
    [:aside {:class (stl/css :panel)
             :aria-labelledby "ds-account-title"
             :data-testid "ds-account-panel"}
     [:h2 {:class (stl/css :panel-label) :id "ds-account-title"}
      (tr "ds.settings.account.title")]

     [:div {:class (stl/css :facts)}
      (when (some? plan)
        [:div {:class (stl/css :fact :fact-plan) :data-testid "ds-account-plan"}
         [:span {:class (stl/css :plan-name)} (acc/plan-title (:id plan))]
         (when (some? (:price plan))
           [:span {:class (stl/css :plan-price)}
            (:price plan)
            (when (:per-month? plan) (str " " (tr "ds.plans.per-month")))])])

      (when (some? renews)
        [:div {:class (stl/css :fact :fact-renews)}
         [:span {:class (stl/css :fact-key)} (tr "ds.settings.account.renews")]
         [:span {:class (stl/css :fact-value)} (acc/format-date renews)]])

      (when (some? credits)
        (let [value (str (acc/format-number (:left credits)) " / " (acc/format-number (:total credits)))]
          [:div {:class (stl/css :fact :fact-meter) :data-testid "ds-account-credits"}
           [:span {:class (stl/css :fact-key)} (tr "ds.settings.account.credits")]
           [:span {:class (stl/css :fact-value)} value]
           [:> acc/meter* {:ratio (:ratio credits)
                           :label (str (tr "ds.settings.account.credits") ": " value)}]]))

      (when (some? storage)
        (let [value (tr "ds.settings.account.storage-value" (:used storage) (:total storage))]
          [:div {:class (stl/css :fact :fact-meter) :data-testid "ds-account-storage"}
           [:span {:class (stl/css :fact-key)} (tr "ds.settings.account.storage")]
           [:span {:class (stl/css :fact-value)} value]
           [:> acc/meter* {:ratio (:ratio storage)
                           :tone "storage"
                           :label (str (tr "ds.settings.account.storage") ": " value)}]]))]

     ;; The rest stays in the panel; the narrow strip shows the facts only.
     [:div {:class (stl/css :more)}
      (when (some? apps)
        [:div {:class (stl/css :block)}
         [:p {:class (stl/css :block-label)} (tr "ds.settings.account.apps")]
         [:ul {:class (stl/css :apps)}
          (for [[idx app] (map-indexed vector (:shown apps))]
            [:li {:key idx :class (stl/css :app)}
             [:span {:class (stl/css-case :avatar true
                                          :avatar-claude (= "claude" (aa/app-tone (:name app)))
                                          :avatar-codex (= "codex" (aa/app-tone (:name app))))
                     :aria-hidden "true"}
              (aa/app-initial (:name app))]
             [:span {:class (stl/css :app-name)} (:name app)]
             (when (some? (:access app))
               [:span {:class (stl/css :app-access)}
                (if (= "edit" (:access app)) (tr "ds.ai.apps.edit") (tr "ds.ai.apps.view"))])])]
         (when (pos? (:more apps))
           [:button {:class (stl/css :more-apps) :type "button" :on-click go-ai}
            (tr "ds.settings.account.apps-more" (:more apps))])])

      (when (some? since)
        [:div {:class (stl/css :block :fact)}
         [:span {:class (stl/css :fact-key)} (tr "ds.settings.account.since")]
         [:span {:class (stl/css :fact-value)} (acc/format-month since)]])

      [:nav {:class (stl/css :links) :aria-label (tr "ds.settings.account.title")}
       [:a {:class (stl/css :link) :href plans/plans-uri :data-testid "ds-account-see-plans"}
        (tr "ds.plans.see-plans")]
       [:button {:class (stl/css :link) :type "button" :on-click go-ai}
        (tr "ds.settings.ai-access-side")]
       [:a {:class (stl/css :link) :href (str "mailto:" acc/contact-email)}
        (tr "ds.settings.account.contact")]]]]))

(mf/defc layout*
  "Settings page frame: the page (`children`) and the Account panel."
  [{:keys [children]}]
  [:div {:class (stl/css :layout)
         :data-ds-settings "layout"}
   [:div {:class (stl/css :main)} children]
   [:div {:class (stl/css :side)}
    [:> guard* {}
     [:> account-panel* {}]]]])
