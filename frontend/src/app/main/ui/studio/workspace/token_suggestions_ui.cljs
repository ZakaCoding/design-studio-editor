;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.workspace.token-suggestions-ui
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.data.workspace.tokens.application :as dwta]
   [app.main.ui.studio.workspace.token-suggestions :as ts]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(defn- token->hex [resolved]
  (:color (dwta/value->color resolved)))

(mf/defc token-suggestions*
  "Row of the colour tokens of the file that are closest to the colour being
  edited. Clicking one applies it through the picker's own `on-token-change`."
  [{:keys [combined-tokens hex applied-token on-token-change]}]
  (let [suggestions
        (mf/with-memo [combined-tokens hex applied-token]
          (ts/suggest {:combined-tokens combined-tokens
                       :hex hex
                       :applied-token applied-token
                       :to-hex token->hex}))]
    (when (seq suggestions)
      [:section {:class (stl/css :suggestions)
                 :aria-label (tr "ds.colorpicker.suggested-tokens")}
       [:span {:class (stl/css :title)}
        (tr "ds.colorpicker.suggested-tokens")]
       [:ul {:class (stl/css :list)}
        (for [{:keys [token hex exact?]} suggestions]
          [:li {:key (:name token)}
           [:button {:type "button"
                     :class (stl/css :item)
                     :title (:name token)
                     :on-click #(on-token-change % token)}
            [:span {:class (stl/css :swatch)
                    :style {:background-color hex}
                    :aria-hidden "true"}]
            [:span {:class (stl/css :name)} (:name token)]
            (when exact?
              [:span {:class (stl/css :exact)} (tr "ds.colorpicker.exact-match")])])]])))
