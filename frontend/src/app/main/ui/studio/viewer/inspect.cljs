;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.viewer.inspect
  "The Inspect panel of the viewer (F1 · 11): the layer's name, size and
  position, style, code, export and the hand-off to an AI coding assistant.
  The values come from the same shape data and the same code generator the
  upstream Inspect uses ('KODE = ENGINE PENPOT'); only the layout is ours.
  The rest of the upstream panels stay one click away under \"All
  properties\"."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.common.files.helpers :as cfh]
   [app.common.geom.shapes :as gsh]
   [app.common.types.shape-tree :as ctst]
   [app.main.data.exports.assets :as de]
   [app.main.store :as st]
   [app.main.ui.ds.foundations.assets.icon :as i]
   [app.main.ui.inspect.styles :refer [styles-tab*]]
   [app.main.ui.studio.ds :as sds]
   [app.util.clipboard :as clipboard]
   [app.util.code-beautify :as cb]
   [app.util.code-gen :as cg]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

;; --- Pure helpers (tested in frontend-tests.studio.viewer-inspect-test)

(defn- round2
  "At most two decimals, as a number."
  [value]
  (when (number? value)
    (/ (js/Math.round (* value 100)) 100)))

(defn- num->text
  "12, not 12.0; at most two decimals."
  [value]
  (when-let [rounded (round2 value)]
    (if (== rounded (js/Math.round rounded))
      (str (js/Math.round rounded))
      (str rounded))))

(defn padding-text
  "CSS-like shorthand of the layout padding, or nil without padding."
  [{:keys [p1 p2 p3 p4]}]
  (let [values (map #(or % 0) [p1 p2 p3 p4])]
    (when (some pos? values)
      (let [[t r b l] (map num->text values)]
        (cond
          (= t r b l) t
          (and (= t b) (= r l)) (str t " " r)
          :else (str t " " r " " b " " l))))))

(defn radius-text
  [shape]
  (let [rx (or (:rx shape) (:r1 shape))]
    (when (and (number? rx) (pos? rx))
      (num->text rx))))

(defn text-node
  "The first text node of a text shape that carries a font."
  [shape]
  (when (= :text (:type shape))
    (d/seek :font-family (tree-seq map? :children (:content shape)))))

(defn font-text
  "Geist 600 · 14 / 18"
  [node]
  (when (some? node)
    (let [size   (d/parse-double (:font-size node))
          height (d/parse-double (:line-height node))
          family (:font-family node)
          weight (:font-weight node)]
      (str family
           (when weight (str " " weight))
           (when size (str " · " (num->text size)
                           (when (and height (pos? height))
                             (str " / " (num->text (* size height))))))))))

(defn fill-info
  "`{:hex :token}` of the first fill (or the text's first fill), or nil."
  [shape]
  (let [node (or (text-node shape) shape)
        fill (first (or (seq (:fills node)) (:fills shape)))
        hex  (:fill-color fill)]
    (when (string? hex)
      {:hex (.toUpperCase hex)
       :token (or (get-in shape [:applied-tokens :fill]))})))

(defn stroke-text
  [shape]
  (when-let [stroke (first (:strokes shape))]
    (when-let [hex (:stroke-color stroke)]
      (str "stroke " (.toUpperCase hex) " " (num->text (or (:stroke-width stroke) 1))))))

(defn geometry
  "Size and position of `shape` inside `frame`, as numbers."
  [shape frame]
  {:w (:width shape)
   :h (:height shape)
   :x (- (or (:x shape) 0) (if (= (:id shape) (:id frame)) (or (:x shape) 0) (or (:x frame) 0)))
   :y (- (or (:y shape) 0) (if (= (:id shape) (:id frame)) (or (:y shape) 0) (or (:y frame) 0)))})

(defn style-lines
  "The mono lines under the colour row."
  [shape]
  (let [font   (font-text (text-node shape))
        radius (radius-text shape)
        pad    (padding-text (:layout-padding shape))
        stroke (stroke-text shape)]
    (cond-> []
      (some? font)   (conj font)
      (or radius pad) (conj (str (when radius (str "radius " radius))
                                 (when (and radius pad) " · ")
                                 (when pad (str "padding " pad))))
      (some? stroke) (conj stroke))))

(defn handoff
  "The package copied by \"Copy handoff package\": layer, geometry, style,
  design tokens applied to the layer, and its CSS. Plain JSON text."
  [{:keys [shape frame css]}]
  (let [{:keys [w h x y]} (geometry shape frame)
        fill (fill-info shape)]
    (js/JSON.stringify
     (clj->js
      (d/without-nils
       {:layer (:name shape)
        :type (name (or (:type shape) :unknown))
        :frame (:name frame)
        :size {:width (round2 w)
               :height (round2 h)}
        :position {:x (round2 x)
                   :y (round2 y)}
        :fill (:hex fill)
        :radius (round2 (or (:rx shape) (:r1 shape)))
        :padding (padding-text (:layout-padding shape))
        :font (font-text (text-node shape))
        :tokens (not-empty (:applied-tokens shape))
        :css css}))
     nil 2)))

(def ^:private export-options
  [["png-1" "PNG · 1x"] ["png-2" "PNG · 2x"] ["png-4" "PNG · 4x"]
   ["jpeg-2" "JPG · 2x"] ["webp-2" "WEBP · 2x"] ["svg-1" "SVG"] ["pdf-1" "PDF"]])

(defn parse-export
  "\"png-2\" -> {:type :png :scale 2}"
  [value]
  (let [[type scale] (.split (str value) "-")]
    {:type (keyword type)
     :scale (or (d/parse-double scale) 1)
     :suffix ""}))

(defn- type-label
  [shape]
  (let [shape-type (:type shape)
        suffix     (cond
                     (= shape-type :rect) "rect"
                     (= shape-type :circle) "circle"
                     (= shape-type :frame) "frame"
                     (= shape-type :group) "group"
                     (= shape-type :text) "text"
                     (= shape-type :image) "image"
                     (= shape-type :svg-raw) "svg-raw"
                     :else "path")]
    (tr (str "inspect.tabs.code.selected." suffix))))

;; --- Parts

(mf/defc section*
  {::mf/private true}
  [{:keys [label class children]}]
  [:section {:class [class (stl/css :section)]}
   [:h3 {:class (stl/css :label)} label]
   children])

(mf/defc copied-button*
  {::mf/private true}
  [{:keys [on-copy class label testid icon?]}]
  (let [copied* (mf/use-state false)
        on-click
        (mf/use-fn
         (mf/deps on-copy)
         (fn [_]
           (-> (clipboard/to-clipboard (on-copy))
               (.then (fn []
                        (reset! copied* true)
                        (js/setTimeout #(reset! copied* false) 2000)))
               (.catch (constantly nil)))))]
    [:button {:class class
              :type "button"
              :aria-label (when icon? label)
              :data-testid testid
              :on-click on-click}
     (if icon?
       [:> sds/icon* {:icon-id (if @copied* i/tick i/clipboard) :size "m"}]
       (if @copied* (tr "ds.connect.copied") label))]))

(mf/defc geometry-row*
  {::mf/private true}
  [{:keys [shape frame]}]
  (let [{:keys [w h x y]} (geometry shape frame)]
    [:dl {:class (stl/css :geometry)}
     (for [[k v] [["W" w] ["H" h] ["X" x] ["Y" y]]]
       [:div {:key k :class (stl/css :geometry-cell)}
        [:dt {:class (stl/css :geometry-key)} k]
        [:dd {:class (stl/css :geometry-value)} (or (num->text v) "-")]])]))

(mf/defc style-rows*
  {::mf/private true}
  [{:keys [shape]}]
  (let [fill  (fill-info shape)
        lines (style-lines shape)]
    [:div {:class (stl/css :style)}
     (when (some? fill)
       [:div {:class (stl/css :fill)}
        [:span {:class (stl/css :swatch)
                :style {:background-color (:hex fill)}}]
        [:span {:class (stl/css :fill-name)}
         (or (:token fill) (:hex fill))]
        (when (some? (:token fill))
          [:span {:class (stl/css :fill-hex)} (:hex fill)])])
     (for [line lines]
       [:p {:key line :class (stl/css :mono)} line])
     (when (and (nil? fill) (empty? lines))
       [:p {:class (stl/css :muted)} (tr "ds.viewer.inspect.no-style")])]))

(mf/defc code-block*
  {::mf/private true}
  [{:keys [css svg]}]
  (let [tab*  (mf/use-state "css")
        tab   (deref tab*)
        code  (if (= tab "svg") svg css)
        on-css (mf/use-fn #(reset! tab* "css"))
        on-svg (mf/use-fn #(reset! tab* "svg"))
        on-copy (mf/use-fn (mf/deps code) (fn [] code))]
    [:section {:class (stl/css :section)}
     [:div {:class (stl/css :code-head)}
      [:h3 {:class (stl/css :label)} (tr "ds.viewer.inspect.code")]
      [:div {:class (stl/css :segmented) :role "tablist"}
       [:button {:class (stl/css-case :segment true :segment-on (= tab "css"))
                 :type "button" :role "tab"
                 :aria-selected (= tab "css")
                 :on-click on-css}
        "CSS"]
       [:button {:class (stl/css-case :segment true :segment-on (= tab "svg"))
                 :type "button" :role "tab"
                 :aria-selected (= tab "svg")
                 :on-click on-svg}
        "SVG"]]]
     [:div {:class (stl/css :code-box)}
      [:> copied-button* {:class (stl/css :code-copy)
                          :on-copy on-copy
                          :icon? true
                          :testid "ds-inspect-copy-code"
                          :label (tr "ds.viewer.inspect.copy-code")}]
      [:pre {:class (stl/css :code-pre)}
       [:code {:data-testid "ds-inspect-code"} code]]]]))

(mf/defc export-row*
  {::mf/private true}
  [{:keys [shape page-id file-id share-id]}]
  (let [choice* (mf/use-state "png-2")
        on-change
        (mf/use-fn #(reset! choice* (.. % -target -value)))
        on-export
        (mf/use-fn
         (mf/deps shape page-id file-id share-id)
         (fn [_]
           (let [spec     (parse-export @choice*)
                 defaults (cond-> {:page-id page-id
                                   :file-id file-id
                                   :name (:name shape)
                                   :object-id (:id shape)}
                            share-id (assoc :share-id share-id))
                 exports  [(merge spec defaults)]]
             (st/emit! (de/request-export {:exports exports})
                       (de/export-shapes-event exports "viewer")))))]
    [:div {:class (stl/css :export)}
     [:select {:class (stl/css :export-select)
               :value @choice*
               :aria-label (tr "ds.viewer.inspect.export")
               :on-change on-change}
      (for [[value label] export-options]
        [:option {:key value :value value} label])]
     [:button {:class (stl/css :export-button)
               :type "button"
               :data-testid "ds-inspect-export"
               :on-click on-export}
      (tr "ds.viewer.inspect.export-button")]]))

(mf/defc handoff-card*
  {::mf/private true}
  [{:keys [get-package]}]
  [:div {:class (stl/css :handoff)}
   [:p {:class (stl/css :handoff-title)}
    [:> sds/icon* {:size "m" :view-box "0 0 24 24"}
     [:path {:d "M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z"}]]
    (tr "ds.viewer.inspect.handoff-title")]
   [:p {:class (stl/css :handoff-text)} (tr "ds.viewer.inspect.handoff-text")]
   [:> copied-button* {:class (stl/css :handoff-button)
                       :on-copy get-package
                       :testid "ds-inspect-handoff"
                       :label (tr "ds.viewer.inspect.handoff-button")}]])

(mf/defc viewer-inspect*
  [{:keys [shapes frame objects page-id file-id share-id color-space libraries from]}]
  (let [shape     (first shapes)
        shapes*   (mf/with-memo [shapes frame]
                    (mapv #(gsh/translate-to-frame % frame) shapes))

        all-children
        (mf/with-memo [shapes* objects]
          (->> shapes*
               (map :id)
               (cfh/selected-with-children objects)
               (ctst/sort-z-index objects)
               (map (d/getf objects))))

        css (mf/with-memo [shapes* all-children objects]
              (-> (cg/generate-style-code objects "css" shapes* all-children)
                  (cb/format-code "css")))
        svg (mf/with-memo [shapes* objects]
              (cg/generate-formatted-markup-code objects "svg" shapes*))

        more*   (mf/use-state false)
        on-more (mf/use-fn #(swap! more* not))

        get-package
        (mf/use-fn
         (mf/deps shape frame css)
         (fn [] (handoff {:shape shape :frame frame :css css})))]
    [:div {:class (stl/css :panel)
           :data-testid "ds-viewer-inspect"}
     [:header {:class (stl/css :head)}
      [:h2 {:class (stl/css :name)} (:name shape)]
      [:p {:class (stl/css :sub)}
       (if (> (count shapes) 1)
         (tr "inspect.multiple-selected" (count shapes))
         (str (type-label shape) " · " (:name frame)))]]
     [:div {:class (stl/css :scroll)}
      [:> section* {:label (tr "ds.viewer.inspect.size")}
       [:> geometry-row* {:shape shape :frame frame}]]
      [:> section* {:label (tr "ds.viewer.inspect.style")}
       [:> style-rows* {:shape shape}]]
      [:> code-block* {:css css :svg svg}]
      [:> section* {:label (tr "ds.viewer.inspect.export")}
       [:> export-row* {:shape shape :page-id page-id :file-id file-id :share-id share-id}]]
      [:> section* {:label (tr "ds.viewer.inspect.handoff")}
       [:> handoff-card* {:get-package get-package}]]
      [:section {:class (stl/css :section)}
       [:button {:class (stl/css :more)
                 :type "button"
                 :aria-expanded (if @more* "true" "false")
                 :on-click on-more}
        (tr "ds.viewer.inspect.all-properties")]
       (when @more*
         [:div {:class (stl/css :more-body)}
          [:> styles-tab* {:color-space (or color-space "hex")
                           :objects objects
                           :shapes shapes
                           :from from
                           :libraries libraries
                           :page-id page-id
                           :file-id file-id
                           :share-id share-id}]])]]]))
