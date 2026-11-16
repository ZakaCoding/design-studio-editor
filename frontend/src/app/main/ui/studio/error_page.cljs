;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.error-page
  "Layout of the full-page errors (404, 500, 502/503, invalid link, lost
  WebGL context) in the Design Studio light style: official Logo, one short
  message, the way back, and the credit upstream is owed. Penpot's own pages
  were a black screen with its illustration.

  `static.cljs` keeps the page logic (which error, retry, report); it renders
  its message and its buttons as `children` of `error-shell*`. The classes it
  gives those children are restyled here by their CSS-module name."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.ui.studio.ds.button :refer [button*]]
   [app.main.ui.studio.ds.logo :refer [logo*]]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(mf/defc error-shell*
  "Props: `code` (\"404\", \"500\"... shown as a small label), `on-home`
  (go to the home page), `primary-home?` (the home button is the main
  action, when the page has no other), `children` (message and actions)."
  [{:keys [code on-home primary-home? children]}]
  [:section {:class (stl/css :page)
             :data-testid "ds-error-page"}
   [:header {:class (stl/css :top)}
    [:button {:type "button"
              :class (stl/css :brand)
              :aria-label (tr "ds.error.home")
              :on-click on-home}
     [:> logo* {}]]]

   [:main {:class (stl/css :center)}
    [:div {:class (stl/css :card)}
     (when (some? code)
       [:span {:class (stl/css :code)} code])
     [:div {:class (stl/css :content)} children]
     [:div {:class (stl/css :home)}
      [:> button* {:variant (if primary-home? "primary" "ghost")
                   :size "lg"
                   :on-click on-home}
       (tr "ds.error.home")]]]]

   [:footer {:class (stl/css :credit)}
    [:span (tr "ds.error.built-on") " "
     [:a {:href "https://penpot.app"
          :target "_blank"
          :rel "noopener noreferrer"}
      "penpot.app"]]
    [:span {:aria-hidden true} "·"]
    [:a {:href "https://www.mozilla.org/MPL/2.0/"
         :target "_blank"
         :rel "noopener noreferrer"}
     (tr "ds.error.license")]]])
