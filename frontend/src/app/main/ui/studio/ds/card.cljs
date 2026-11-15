;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.ds.card
  "Card of the Design Studio DS: a bordered surface for one unit of content
  (a file, a plan, a setting group)."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [rumext.v2 :as mf]))

(def ^:private schema:card
  [:map
   [:class {:optional true} [:maybe :string]]
   [:variant {:optional true} [:maybe [:enum "outlined" "subtle" "raised"]]]
   [:padding {:optional true} [:maybe [:enum "none" "md" "lg"]]]
   [:to {:optional true} [:maybe :string]]
   [:is-selected {:optional true} [:maybe :boolean]]])

(mf/defc card*
  "Props: `variant` outlined (default) | subtle | raised; `padding` none |
  md (16, default) | lg (24); `is-selected`. A card with `on-click` renders
  a <button>, one with `to` an <a> (both keyboard-focusable); otherwise a
  <div>. The rest goes to the element."
  {::mf/schema schema:card}
  [{:keys [variant padding to on-click is-selected class children] :rest props}]
  (let [variant      (d/nilv variant "outlined")
        padding      (d/nilv padding "md")
        link?        (some? to)
        button?      (and (not link?) (some? on-click))
        element      (cond link? "a" button? "button" :else "div")
        class'       (stl/css-case :card true
                                   :variant-subtle (= variant "subtle")
                                   :variant-raised (= variant "raised")
                                   :padding-none (= padding "none")
                                   :padding-lg (= padding "lg")
                                   :interactive (or link? button?)
                                   :is-selected (true? is-selected))
        props        (mf/spread-props props
                                      {:class [class class']
                                       :href to
                                       :on-click on-click
                                       :type (when button? "button")
                                       :aria-pressed (when (and button? (some? is-selected))
                                                       (true? is-selected))})]
    [:> element props children]))
