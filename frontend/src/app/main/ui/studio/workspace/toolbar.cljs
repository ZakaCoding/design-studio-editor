;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.workspace.toolbar
  "The floating toolbar at the bottom centre of the canvas (boards F1 · 01,
  02 and 05): the drawing tools, comments, zoom and the way to the viewer.

  Rendered by the workspace shell in its `viewport-overlay` mount point, so
  it also stays in canvas focus. It replaces upstream's `top-toolbar*`,
  which `chrome.scss` hides, and the zoom, comments and viewer buttons of
  upstream's right header. Every control emits the events the upstream
  control emits; the zoom menu is upstream's own widget.

  Line, arrow and curve have no button of their own on the boards: they
  are in a flyout over Rectangle and Pen, as upstream groups them. Plugins
  stay in the main menu and MCP is the status pill of the right header."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.common.geom.point :as gpt]
   [app.main.data.common :as dcm]
   [app.main.data.event :as ev]
   [app.main.data.workspace :as dw]
   [app.main.data.workspace.drawing.common :as dwdc]
   [app.main.data.workspace.media :as dwm]
   [app.main.data.workspace.shortcuts :as sc]
   [app.main.refs :as refs]
   [app.main.store :as st]
   [app.main.ui.components.file-uploader :as file-uploader]
   [app.main.ui.context :as ctx]
   [app.main.ui.studio.ds :as sds]
   [app.main.ui.studio.workspace.glyphs :refer [glyph*]]
   [app.main.ui.workspace.right-header :refer [zoom-widget-workspace]]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def ^:private shape-tools ["rect" "line" "arrow"])
(def ^:private path-tools ["path" "curve"])

(def ^:private tool-glyphs
  {"frame" "frame"
   "rect" "rect"
   "circle" "ellipse"
   "line" "line"
   "arrow" "arrow"
   "text" "text"
   "path" "pen"
   "curve" "curve"})

(defn- tool-label
  [tool]
  (cond
    (= tool "frame")  (tr "workspace.toolbar.frame" (sc/get-tooltip :draw-frame))
    (= tool "rect")   (tr "workspace.toolbar.rect" (sc/get-tooltip :draw-rect))
    (= tool "circle") (tr "workspace.toolbar.ellipse" (sc/get-tooltip :draw-ellipse))
    (= tool "line")   (tr "workspace.toolbar.line" (sc/get-tooltip :draw-line))
    (= tool "arrow")  (tr "workspace.toolbar.arrow" (sc/get-tooltip :draw-arrow))
    (= tool "text")   (tr "workspace.toolbar.text" (sc/get-tooltip :draw-text))
    (= tool "path")   (tr "workspace.toolbar.path" (sc/get-tooltip :draw-path))
    (= tool "curve")  (tr "workspace.toolbar.curve" (sc/get-tooltip :draw-curve))
    :else             tool))

(defn- select-tool
  [event]
  (let [tool (-> (dom/get-current-target event)
                 (dom/get-data "tool")
                 (keyword))]
    (st/emit! :interrupt
              (dw/clear-edition-mode)
              (dw/select-for-drawing tool))))

(defn- select-move
  []
  (st/emit! :interrupt
            (dw/clear-edition-mode)
            (dwdc/clear-drawing)))

(mf/defc tool*
  {::mf/private true}
  [{:keys [tool current]}]
  (let [label (tool-label tool)]
    [:> sds/tooltip* {:content label}
     [:button {:class (stl/css :tool)
               :type "button"
               :aria-label label
               :aria-pressed (= current tool)
               :data-tool tool
               :on-click select-tool}
      [:> glyph* {:id (get tool-glyphs tool)}]]]))

(mf/defc tool-group*
  "One button for the tool of the group that is in use (or its first one)
  and, on hover or keyboard focus, a flyout with every tool of the group."
  {::mf/private true}
  [{:keys [tools current label]}]
  (let [shown (if (some #(= % current) tools) current (first tools))]
    [:div {:class (stl/css :group)
           :role "group"
           :aria-label label}
     [:button {:class (stl/css :tool)
               :type "button"
               :aria-label (tool-label shown)
               :aria-pressed (= current shown)
               :aria-haspopup "menu"
               :data-tool shown
               :on-click select-tool}
      [:> glyph* {:id (get tool-glyphs shown)}]]
     [:div {:class (stl/css :flyout)
            :role "menu"
            :aria-label label}
      (for [tool tools]
        [:button {:key tool
                  :class (stl/css :tool)
                  :type "button"
                  :role "menuitemradio"
                  :title (tool-label tool)
                  :aria-label (tool-label tool)
                  :aria-checked (= current tool)
                  :aria-pressed (= current tool)
                  :data-tool tool
                  :on-click select-tool}
         [:> glyph* {:id (get tool-glyphs tool)}]])]]))

(mf/defc image-tool*
  "Upstream's image tool: opens the file dialog and puts the images in the
  middle of what the canvas shows."
  {::mf/private true}
  []
  (let [input-ref (mf/use-ref nil)
        file-id   (mf/use-ctx ctx/current-file-id)
        label     (tr "workspace.toolbar.image" (sc/get-tooltip :insert-image))

        on-click
        (mf/use-fn
         (fn []
           (st/emit! :interrupt (dw/clear-edition-mode))
           (dom/click (mf/ref-val input-ref))))

        on-selected
        (mf/use-fn
         (mf/deps file-id)
         (fn [blobs]
           (let [vbox   (deref refs/vbox)
                 x      (+ (:x vbox) (/ (:width vbox) 2))
                 y      (+ (:y vbox) (/ (:height vbox) 2))
                 params {:file-id file-id
                         :blobs (seq blobs)
                         :position (gpt/point x y)}]
             (st/emit! (dwm/upload-media-workspace params)))))]
    [:*
     [:> sds/tooltip* {:content label}
      [:button {:class (stl/css :tool)
                :type "button"
                :aria-label label
                :on-click on-click}
       [:> glyph* {:id "image"}]]]
     [:& file-uploader/file-uploader {:input-id "ds-image-upload"
                                      :accept dwm/accept-image-types
                                      :multi true
                                      :ref input-ref
                                      :on-selected on-selected}]]))

(mf/defc toolbar*
  "Props: `file-id`, `page-id`."
  [{:keys [file-id page-id]}]
  (let [drawtool   (mf/deref refs/selected-drawing-tool)
        edition    (mf/deref refs/selected-edition)
        zoom       (mf/deref refs/selected-zoom)
        read-only? (mf/use-ctx ctx/workspace-read-only?)

        current    (when (some? drawtool) (name drawtool))
        comments?  (= drawtool :comments)
        move?      (and (nil? drawtool) (not edition))

        move-label     (tr "workspace.toolbar.move" (sc/get-tooltip :move))
        comments-label (tr "workspace.toolbar.comments" (sc/get-tooltip :add-comment))
        viewer-label   (tr "workspace.header.viewer" (sc/get-tooltip :open-viewer))

        on-increase      (mf/use-fn #(st/emit! (dw/increase-zoom nil)))
        on-decrease      (mf/use-fn #(st/emit! (dw/decrease-zoom nil)))
        on-zoom-reset    (mf/use-fn #(st/emit! dw/reset-zoom))
        on-zoom-fit      (mf/use-fn #(st/emit! dw/zoom-to-fit-all))
        on-zoom-selected (mf/use-fn #(st/emit! dw/zoom-to-selected-shape))

        ;; Same events as the comments button of upstream's right header.
        toggle-comments
        (mf/use-fn
         (mf/deps comments?)
         (fn []
           (if ^boolean comments?
             (st/emit! (dwdc/clear-drawing))
             (st/emit! :interrupt
                       (dw/clear-edition-mode)
                       (-> (dw/remove-layout-flag :document-history)
                           (vary-meta assoc ::ev/origin "workspace-header"))
                       (dw/select-for-drawing :comments)))))

        open-viewer
        (mf/use-fn
         (mf/deps file-id page-id)
         (fn []
           (st/emit! (dcm/go-to-viewer {:page-id page-id
                                        :file-id file-id
                                        :section "interactions"}))))]

    [:div {:class (stl/css :toolbar)
           :role "toolbar"
           :aria-label (tr "workspace.toolbar.label")
           :data-testid "ds-toolbar"}

     (when-not ^boolean read-only?
       [:div {:class (stl/css :tools)
              :data-testid "toolbar-options"}
        [:> sds/tooltip* {:content move-label}
         [:button {:class (stl/css :tool)
                   :type "button"
                   :aria-label move-label
                   :aria-pressed move?
                   :on-click select-move}
          [:> glyph* {:id "select"}]]]
        [:> tool* {:tool "frame" :current current}]
        [:> tool-group* {:tools shape-tools
                         :current current
                         :label (tr "ds.workspace.toolbar.shapes")}]
        [:> tool* {:tool "circle" :current current}]
        [:> tool* {:tool "text" :current current}]
        [:> tool-group* {:tools path-tools
                         :current current
                         :label (tr "ds.workspace.toolbar.paths")}]
        [:> image-tool*]])

     [:> sds/tooltip* {:content comments-label}
      [:button {:class (stl/css :tool)
                :type "button"
                :aria-label comments-label
                :aria-pressed comments?
                :data-tool "comments"
                :on-click toggle-comments}
       [:> glyph* {:id "comment"}]]]

     [:div {:class (stl/css :divider)
            :role "separator"}]

     [:div {:class (stl/css :zoom)}
      [:& zoom-widget-workspace {:zoom zoom
                                 :on-increase on-increase
                                 :on-decrease on-decrease
                                 :on-zoom-reset on-zoom-reset
                                 :on-zoom-fit on-zoom-fit
                                 :on-zoom-selected on-zoom-selected}]
      [:> glyph* {:id "chevron"
                  :size 12
                  :class (stl/css :zoom-chevron)}]]

     [:> sds/tooltip* {:content viewer-label}
      [:button {:class (stl/css :tool)
                :type "button"
                :aria-label viewer-label
                :data-testid "ds-toolbar-viewer"
                :on-click open-viewer}
       [:> glyph* {:id "play"}]]]]))
