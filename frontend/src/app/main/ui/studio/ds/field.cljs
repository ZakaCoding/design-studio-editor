;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.ds.field
  "Form field of the Design Studio DS: label, control, hint and error.

  `field*` is the frame (label row, hint, error) around any control.
  `input*` is the common case: a text input inside that frame."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [cuerdas.core :as str]
   [rumext.v2 :as mf]))

(mf/defc field*
  "Props: `id` (id of the control the label points to), `label`,
  `label-end` (element at the end of the label row, e.g. a \"Forgot
  password?\" link), `hint`, `error` (replaces the hint, announced with
  role=alert), `class`. Children: the control.

  The hint has the id `<id>-hint`, the error `<id>-error`; point the
  control's `aria-describedby` at whichever is shown (`input*` does)."
  [{:keys [id label label-end hint error class children]}]
  (let [error? (not (str/blank? error))
        hint?  (and (not error?) (not (str/blank? hint)))]
    [:div {:class [class (stl/css :field)]}
     (when (or (some? label) (some? label-end))
       [:div {:class (stl/css :label-row)}
        (when (some? label)
          [:label {:class (stl/css :label) :html-for id} label])
        (when (some? label-end)
          [:span {:class (stl/css :label-end)} label-end])])
     children
     (when hint?
       [:p {:class (stl/css :hint) :id (str id "-hint")} hint])
     (when error?
       [:p {:class (stl/css :error) :id (str id "-error") :role "alert"} error])]))

(def ^:private schema:input
  [:map
   [:id {:optional true} [:maybe :string]]
   [:class {:optional true} [:maybe :string]]
   [:label {:optional true} [:maybe :string]]
   [:hint {:optional true} [:maybe :string]]
   [:error {:optional true} [:maybe :string]]
   [:size {:optional true} [:maybe [:enum "md" "lg"]]]
   [:type {:optional true} [:maybe :string]]])

(mf/defc input*
  "Text input with label, hint and error.

  Props: `label`, `hint`, `error`, `label-end`, `size` md (32, default) |
  lg (44, auth and dashboard forms), `leading` / `trailing` (elements inside
  the box, e.g. an icon or a show-password button), `id` (generated when
  absent), `class` (on the field frame). Everything else (`value`,
  `default-value`, `on-change`, `type`, `name`, `placeholder`, `disabled`,
  `auto-complete`, ...) goes to the <input>. Forwards its ref to the <input>.

  Give the input a `label`; when the design has none, pass `aria-label`."
  {::mf/forward-ref true
   ::mf/schema schema:input}
  [{:keys [id label label-end hint error size type leading trailing disabled class] :rest props} ref]
  (let [gen-id   (mf/use-id)
        id       (d/nilv id gen-id)
        size     (d/nilv size "md")
        error?   (not (str/blank? error))
        hint?    (and (not error?) (not (str/blank? hint)))
        disabled (true? disabled)
        props    (mf/spread-props props
                                  {:ref ref
                                   :id id
                                   :class (stl/css :input)
                                   :type (d/nilv type "text")
                                   :disabled disabled
                                   :aria-invalid (when error? true)
                                   :aria-describedby (cond
                                                       error? (str id "-error")
                                                       hint?  (str id "-hint"))})]
    [:> field* {:id id
                :label label
                :label-end label-end
                :hint hint
                :error error
                :class class}
     [:div {:class (stl/css-case :control true
                                 :size-lg (= size "lg")
                                 :has-error error?
                                 :is-disabled disabled)}
      (when (some? leading)
        [:span {:class (stl/css :slot)} leading])
      [:> "input" props]
      (when (some? trailing)
        [:span {:class (stl/css :slot)} trailing])]]))
