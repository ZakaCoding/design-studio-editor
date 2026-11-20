;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.ds.showcase
  "Developer showcase of the Design Studio DS: every base component, in
  every variant, on one page. Mounted in the existing debug playground
  route (`#/debug/playground`, development builds only); the release build
  still compiles it, and through it every component.
  Copy here is fixed English on purpose: it is not a user-facing screen."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.main.ui.ds.foundations.assets.icon :as i]
   [app.main.ui.studio.ds :as sds]
   [rumext.v2 :as mf]))

(def ^:private swatches
  ["bg-canvas" "bg-panel" "bg-subtle" "bg-selected" "bg-inverse"
   "border-default" "border-strong"
   "text-primary" "text-body" "text-secondary" "text-placeholder" "text-disabled"
   "accent-default" "accent-subtle" "accent-text-strong"
   "status-success" "status-success-text" "status-success-bg"
   "status-warning" "status-warning-text" "status-warning-bg"
   "status-danger-text" "status-danger-bg"
   "guide-measure" "brand-wordmark-ink" "brand-wordmark-muted"])

(mf/defc section*
  {::mf/private true}
  [{:keys [title children]}]
  [:section
   [:h2 {:class (stl/css :section-title)} title]
   children])

(mf/defc showcase*
  []
  (let [dialog*   (mf/use-state false)
        dialog?   (deref dialog*)
        toasts*   (mf/use-state [])
        toasts    (deref toasts*)
        active*   (mf/use-state "move")
        active    (deref active*)

        open-dialog  (mf/use-fn #(reset! dialog* true))
        close-dialog (mf/use-fn #(reset! dialog* false))

        add-toast
        (mf/use-fn
         (fn [tone]
           (swap! toasts* conj {:id (str (random-uuid)) :tone tone})))

        remove-toast
        (mf/use-fn
         (fn [id]
           (swap! toasts* (fn [items] (filterv #(not= (:id %) id) items)))))]

    [:div {:class (stl/css :showcase)}
     [:div {:class (stl/css :page)}
      [:> sds/logo* {:size "lg"}]
      [:h1 {:class (stl/css :heading)} "Design Studio DS"]
      [:p {:class (stl/css :lead)}
       "Base components for F3 and F4. Tokens: ds/core and ds/light, theme Terang."]

      [:> section* {:title "Logo"}
       [:div {:class (stl/css :row)}
        [:> sds/logo* {:size "sm"}]
        [:> sds/logo* {:size "md"}]
        [:> sds/logo* {:size "lg" :to "#/debug/playground"}]
        [:> sds/logo* {:variant "mark"}]
        [:> sds/logo-mark* {:size 20 :label "Design Studio"}]]]

      [:> section* {:title "Buttons"}
       [:div {:class (stl/css :row)}
        [:> sds/button* {:variant "primary"} "Primary"]
        [:> sds/button* {:variant "secondary"} "Secondary"]
        [:> sds/button* {:variant "ghost"} "Ghost"]
        [:> sds/button* {:variant "danger"} "Danger"]
        [:> sds/button* {:variant "primary" :icon-id i/add} "With icon"]
        [:> sds/button* {:variant "primary" :is-loading true} "Loading"]
        [:> sds/button* {:variant "primary" :disabled true} "Disabled"]
        [:> sds/button* {:variant "secondary" :disabled true} "Disabled"]
        [:> sds/button* {:variant "secondary" :to "#/debug/playground"} "Link"]]
       [:div {:class (stl/css :row)}
        [:> sds/button* {:size "sm"} "Small"]
        [:> sds/button* {:size "md"} "Medium"]
        [:> sds/button* {:size "lg"} "Large"]
        [:> sds/icon-button* {:aria-label "Add" :icon-id i/add}]
        [:> sds/icon-button* {:aria-label "Add" :icon-id i/add :variant "secondary"}]
        [:> sds/icon-button* {:aria-label "Add" :icon-id i/add :variant "primary" :size "lg"}]]]

      [:> section* {:title "Fields"}
       [:div {:class (stl/css :grid)}
        [:> sds/input* {:label "Email"
                        :type "email"
                        :size "lg"
                        :placeholder "name@email.com"}]
        [:> sds/input* {:label "Password"
                        :type "password"
                        :size "lg"
                        :default-value "secret"
                        :label-end "Forgot password?"
                        :error "Email or password is not correct."}]
        [:> sds/input* {:label "File name"
                        :hint "Shown on the dashboard."
                        :default-value "Landing page"}]
        [:> sds/input* {:label "Disabled"
                        :disabled true
                        :default-value "Cannot change"}]
        [:> sds/input* {:aria-label "Search"
                        :placeholder "Search files"
                        :leading (mf/html [:> sds/icon* {:icon-id i/search}])}]]]

      [:> section* {:title "Cards"}
       [:div {:class (stl/css :grid)}
        [:> sds/card* {} "Outlined card"]
        [:> sds/card* {:variant "subtle"} "Subtle card"]
        [:> sds/card* {:variant "raised" :padding "lg"} "Raised card, large padding"]
        [:> sds/card* {:on-click open-dialog} "Clickable card (opens the dialog)"]
        [:> sds/card* {:on-click open-dialog :is-selected true} "Selected card"]]]

      [:> section* {:title "Panel and toolbar"}
       [:div {:class (stl/css :grid)}
        [:> sds/panel* {:title "Layers"
                        :actions (mf/html
                                  [:> sds/icon-button* {:aria-label "Add layer"
                                                        :icon-id i/add
                                                        :size "sm"}])
                        :footer (mf/html
                                 [:> sds/button* {:variant "secondary" :size "sm"} "Footer action"])}
         "Panel body. The layout that mounts a panel places it."]
        [:> sds/panel* {:aria-label "Untitled panel" :is-flush true}
         [:div {:class (stl/css :row)}
          [:> sds/toolbar* {:aria-label "Tools" :variant "plain"}
           [:> sds/toolbar-group* {:aria-label "Pointer"}
            [:> sds/tooltip* {:content "Move" :shortcut "V" :placement "bottom"}
             [:> sds/icon-button* {:aria-label "Move"
                                   :icon-id i/move
                                   :is-active (= active "move")
                                   :on-click #(reset! active* "move")}]]
            [:> sds/tooltip* {:content "Board" :shortcut "B" :placement "bottom"}
             [:> sds/icon-button* {:aria-label "Board"
                                   :icon-id i/board
                                   :is-active (= active "board")
                                   :on-click #(reset! active* "board")}]]]
           [:> sds/toolbar-separator*]
           [:> sds/toolbar-group* {:aria-label "Shapes"}
            [:> sds/icon-button* {:aria-label "Rectangle"
                                  :icon-id i/rectangle
                                  :is-active (= active "rect")
                                  :on-click #(reset! active* "rect")}]
            [:> sds/icon-button* {:aria-label "Text"
                                  :icon-id i/text
                                  :is-active (= active "text")
                                  :on-click #(reset! active* "text")}]]]]]
        [:> sds/toolbar* {:aria-label "Floating tools" :orientation "vertical"}
         [:> sds/toolbar-group* {}
          [:> sds/icon-button* {:aria-label "Add" :icon-id i/add}]
          [:> sds/icon-button* {:aria-label "Search" :icon-id i/search}]]]]]

      [:> section* {:title "Badges and tags"}
       [:div {:class (stl/css :row)}
        [:> sds/badge* {} "Neutral"]
        [:> sds/badge* {:tone "accent"} "Accent"]
        [:> sds/badge* {:tone "craft"} "Craft"]
        [:> sds/badge* {:tone "solid"} "Solid"]
        [:> sds/badge* {:tone "success"} "Connected"]
        [:> sds/badge* {:tone "warning"} "Pending"]
        [:> sds/badge* {:tone "danger"} "Failed"]
        [:> sds/badge* {:tone "solid" :is-mono true} "New UI"]
        [:> sds/tag* {} "Poster"]
        [:> sds/tag* {:tone "accent" :on-remove (fn [_] nil) :remove-label "Remove Brand kit"}
         "Brand kit"]]]

      [:> section* {:title "Tooltip and icons"}
       [:div {:class (stl/css :row)}
        [:> sds/tooltip* {:content "Tooltip on top"}
         [:> sds/button* {:variant "secondary"} "Hover or focus me"]]
        [:> sds/tooltip* {:content "Tooltip on the right" :placement "right"}
         [:> sds/button* {:variant "secondary"} "Right"]]
        [:> sds/icon* {:icon-id i/add :size "s"}]
        [:> sds/icon* {:icon-id i/add :size "m"}]
        [:> sds/icon* {:icon-id i/add :size "l"}]
        [:> sds/icon* {:icon-id i/add :size "xl" :label "Add"}]
        [:> sds/icon* {:size "xl" :label "Custom icon"}
         [:path {:d "M12 5v14"}]
         [:path {:d "M5 12h14"}]]]]

      [:> section* {:title "Toast and dialog"}
       [:div {:class (stl/css :row)}
        [:> sds/button* {:variant "secondary" :on-click #(add-toast "neutral")} "Toast"]
        [:> sds/button* {:variant "secondary" :on-click #(add-toast "success")} "Success toast"]
        [:> sds/button* {:variant "secondary" :on-click #(add-toast "warning")} "Warning toast"]
        [:> sds/button* {:variant "secondary" :on-click #(add-toast "danger")} "Danger toast"]
        [:> sds/button* {:on-click open-dialog} "Open dialog"]]
       [:> sds/toast* {:tone "success" :title "Saved"}
        "A toast rendered inline, outside the region."]]

      [:> section* {:title "Semantic colors (ds/light)"}
       [:div {:class (stl/css :swatches)}
        (for [token swatches]
          [:div {:class (stl/css :swatch) :key token}
           [:span {:class (stl/css :swatch-chip)
                   :style {:background (str "var(--ds-" token ")")}}]
           token])]]]

     [:> sds/toast-region* {:aria-label "Notifications"}
      (for [{:keys [id tone]} toasts]
        [:> sds/toast* {:key id
                        :tone tone
                        :title (str "Toast: " tone)
                        :on-close #(remove-toast id)
                        :action (mf/html
                                 [:> sds/button* {:variant "ghost" :size "sm"} "Undo"])}
         "The caller decides when a toast goes away."])]

     (when dialog?
       [:> sds/dialog* {:title "Delete this file?"
                        :description "The file moves to the trash. You can restore it for 30 days."
                        :on-close close-dialog
                        :footer (mf/html
                                 [:*
                                  [:> sds/button* {:variant "secondary" :on-click close-dialog} "Cancel"]
                                  [:> sds/button* {:variant "danger" :on-click close-dialog} "Delete"]])}
        [:> sds/input* {:label "Type the file name to confirm"
                        :placeholder "Landing page"}]])]))
