;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.ds.toast
  "Toast of the Design Studio DS.

  `toast*`        one message. Presentational: the caller owns when it
                  appears and disappears.
  `toast-region*` the fixed, live-announced stack toasts are rendered in.
                  Mount one per screen and keep it mounted while empty, so
                  screen readers announce what is added later."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.main.ui.hooks :as hooks]
   [app.main.ui.studio.ds.button :refer [icon-button*]]
   [app.main.ui.studio.ds.icon :refer [icon*]]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def ^:private schema:toast
  [:map
   [:class {:optional true} [:maybe :string]]
   [:tone {:optional true} [:maybe [:enum "neutral" "success" "warning" "danger"]]]
   [:title {:optional true} [:maybe :string]]
   [:on-close {:optional true} [:maybe fn?]]
   [:close-label {:optional true} [:maybe :string]]])

(mf/defc toast*
  "Props: `tone` neutral (default) | success | warning | danger; `title`
  (optional bold first line); children (the message); `action` (element,
  e.g. a ghost `button*`); `on-close` (shows the close button);
  `close-label`; `class`. The rest goes to the element.

  Do not auto-dismiss a toast that carries an action or an error."
  {::mf/schema schema:toast}
  [{:keys [tone title action on-close close-label class children] :rest props}]
  (let [tone  (d/nilv tone "neutral")
        props (mf/spread-props props
                               {:class [class (stl/css-case :toast true
                                                            :tone-success (= tone "success")
                                                            :tone-warning (= tone "warning")
                                                            :tone-danger (= tone "danger"))]
                                :role (if (= tone "danger") "alert" "status")})]
    [:> "div" props
     [:div {:class (stl/css :content)}
      (when (some? title)
        [:p {:class (stl/css :title)} title])
      children]
     (when (some? action)
       [:div {:class (stl/css :action)} action])
     (when (some? on-close)
       [:> icon-button* {:size "sm"
                         :aria-label (d/nilv close-label (tr "labels.close"))
                         :on-click on-close}
        [:> icon* {:size "m"}
         [:path {:d "M18 6 6 18"}]
         [:path {:d "m6 6 12 12"}]]])]))

(def ^:private schema:toast-region
  [:map
   [:class {:optional true} [:maybe :string]]
   [:aria-label {:optional true} [:maybe :string]]
   [:position {:optional true} [:maybe [:enum "bottom-center" "bottom-right" "top-right"]]]])

(mf/defc toast-region*
  "Props: `position` bottom-center (default) | bottom-right | top-right;
  `aria-label`; `class`. Children: `toast*` elements, each with a `:key`.
  Rendered in a portal on <body>."
  {::mf/schema schema:toast-region}
  [{:keys [position class children] :rest props}]
  (let [position  (d/nilv position "bottom-center")
        container (hooks/use-portal-container :default)
        props     (mf/spread-props props
                                   {:class [class (stl/css-case :region true
                                                                :region-bottom-center (= position "bottom-center")
                                                                :region-bottom-right (= position "bottom-right")
                                                                :region-top-right (= position "top-right"))]
                                    :role "region"
                                    :aria-live "polite"})]
    (mf/portal
     (mf/html [:> "div" props children])
     container)))
