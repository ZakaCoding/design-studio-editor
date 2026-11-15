;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.workspace.components
  "Editor components of F1 · 13 that Design Studio adds to upstream ones:
  the \"Change with Craft\" item of the right-click menu, the Craft section
  of the shortcut panel and the Color / Token switch of the colour picker.
  Each is mounted from one line in its upstream file (STUDIO-SEAMS.md)."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.config :as cf]
   [app.main.data.workspace :as dw]
   [app.main.store :as st]
   [app.main.ui.studio.craft :as craft]
   [app.main.ui.studio.craft.state :as state]
   [app.main.ui.studio.guard :refer [guard*]]
   [app.util.i18n :refer [tr]]
   [app.util.strings :refer [matches-search]]
   [rumext.v2 :as mf]))

(defn- enter-label
  []
  (if (cf/check-platform? :macos) "⌘ Enter" "Ctrl Enter"))

;; ---- Right-click menu ----------------------------------------------------

(mf/defc craft-item*
  {::mf/private true}
  []
  (let [on-click
        (mf/use-fn
         (fn []
           (st/emit! dw/hide-context-menu)
           ;; After the menu is gone, so the prompt opens under the selection.
           (js/setTimeout state/request-quick-prompt! 0)))]
    [:li {:class (stl/css :craft-item)
          :role "menuitem"
          :tabindex 0
          :data-testid "context-menu-craft"
          :on-click on-click
          :on-key-down (fn [event]
                         (when (= "Enter" (.-key event))
                           (on-click)))}
     [:svg {:class (stl/css :craft-icon)
            :width 16 :height 16 :view-box "0 0 24 24"
            :fill "currentColor" :aria-hidden true}
      [:path {:d "M12 2l2.2 6.3L20.5 10.5l-6.3 2.2L12 19l-2.2-6.3L3.5 10.5l6.3-2.2z"}]
      [:path {:d "M19 15l.9 2.1L22 18l-2.1.9L19 21l-.9-2.1L16 18l2.1-.9z"}]]
     [:span {:class (stl/css :craft-title)} (tr "ds.menu.craft-change")]
     [:span {:class (stl/css :craft-shortcut)} (enter-label)]]))

(mf/defc context-menu-craft*
  "Separator and the Craft item, only for a member who has Craft."
  []
  (let [craft? (craft/use-access)]
    (when craft?
      [:> guard* {}
       [:li {:class (stl/css :craft-separator) :aria-hidden true}]
       [:> craft-item* {}]])))

;; ---- Shortcut panel ------------------------------------------------------

(mf/defc craft-shortcuts*
  "The CRAFT section of the shortcut panel. Hidden while a search term
  matches none of its words."
  [{:keys [filter-term]}]
  (let [craft?    (craft/use-access)
        title     (tr "ds.shortcuts.craft")
        label     (tr "ds.shortcuts.craft-quick")
        term      (or filter-term "")]
    (when (and craft?
               (or (= term "")
                   (matches-search title term)
                   (matches-search label term)
                   (matches-search "ctrl enter" term)))
      [:> guard* {}
       [:section {:class (stl/css :craft-section)
                  :data-testid "shortcuts-craft"}
        [:h3 {:class (stl/css :craft-section-title)} title]
        [:div {:class (stl/css :craft-row)}
         [:span label]
         [:kbd {:class (stl/css :craft-key)} (enter-label)]]]])))

;; ---- Colour picker -------------------------------------------------------

(mf/defc picker-tabs*
  "The Color / Token switch at the top of the colour picker."
  [{:keys [selected on-change]}]
  [:div {:class (stl/css :tabs) :role "tablist"}
   [:button {:type "button"
             :role "tab"
             :class (stl/css-case :tab true :tab-active (= selected :direct-color))
             :aria-selected (= selected :direct-color)
             :data-testid "opt-color"
             :on-click #(on-change :direct-color %)}
    (tr "ds.colorpicker.tab-color")]
   [:button {:type "button"
             :role "tab"
             :class (stl/css-case :tab true :tab-active (= selected :token-color))
             :aria-selected (= selected :token-color)
             :data-testid "opt-token-color"
             :on-click #(on-change :token-color %)}
    (tr "ds.colorpicker.tab-token")]])
