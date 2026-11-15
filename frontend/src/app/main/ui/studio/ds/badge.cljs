;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.ds.badge
  "Badge and tag of the Design Studio DS.

  `badge*` a short status or count next to something (not interactive).
  `tag*`   a labelled chip; removable when `on-remove` is given."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.main.ui.studio.ds.icon :refer [icon*]]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def ^:private tones
  ["neutral" "accent" "craft" "solid" "success" "warning" "danger"])

(def ^:private schema:badge
  [:map
   [:class {:optional true} [:maybe :string]]
   [:tone {:optional true} [:maybe (into [:enum] tones)]]
   [:is-mono {:optional true} [:maybe :boolean]]])

(mf/defc badge*
  "Props: `tone` neutral (default) | accent | craft | solid | success |
  warning | danger; `is-mono` (the small mono caps label of the F1 boards);
  `class`. The rest goes to the <span>."
  {::mf/schema schema:badge}
  [{:keys [tone is-mono class children] :rest props}]
  (let [tone  (d/nilv tone "neutral")
        props (mf/spread-props props
                               {:class [class (stl/css-case :badge true
                                                            :mono (true? is-mono)
                                                            :tone-neutral (= tone "neutral")
                                                            :tone-accent (= tone "accent")
                                                            :tone-craft (= tone "craft")
                                                            :tone-solid (= tone "solid")
                                                            :tone-success (= tone "success")
                                                            :tone-warning (= tone "warning")
                                                            :tone-danger (= tone "danger"))]})]
    [:> "span" props children]))

(def ^:private schema:tag
  [:map
   [:class {:optional true} [:maybe :string]]
   [:tone {:optional true} [:maybe (into [:enum] tones)]]
   [:on-remove {:optional true} [:maybe fn?]]
   [:remove-label {:optional true} [:maybe :string]]])

(mf/defc tag*
  "Props: `tone` (as `badge*`), `on-remove` (shows a remove button),
  `remove-label` (accessible name of that button; default: the translated
  \"Close\"; pass one that names the tag, e.g. \"Remove Poster\"), `class`."
  {::mf/schema schema:tag}
  [{:keys [tone on-remove remove-label class children] :rest props}]
  (let [tone       (d/nilv tone "neutral")
        removable? (some? on-remove)
        props      (mf/spread-props props
                                    {:class [class (stl/css-case :badge true
                                                                 :tag true
                                                                 :tag-removable removable?
                                                                 :tone-neutral (= tone "neutral")
                                                                 :tone-accent (= tone "accent")
                                                                 :tone-craft (= tone "craft")
                                                                 :tone-solid (= tone "solid")
                                                                 :tone-success (= tone "success")
                                                                 :tone-warning (= tone "warning")
                                                                 :tone-danger (= tone "danger"))]})]
    [:> "span" props
     [:span {:class (stl/css :label)} children]
     (when removable?
       [:button {:class (stl/css :remove)
                 :type "button"
                 :aria-label (d/nilv remove-label (tr "labels.close"))
                 :on-click on-remove}
        [:> icon* {:size "s"}
         [:path {:d "M18 6 6 18"}]
         [:path {:d "m6 6 12 12"}]]])]))
