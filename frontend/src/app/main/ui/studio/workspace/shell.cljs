;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.workspace.shell
  "The Design Studio workspace shell (F3-A; boards F1 · 01 and F1 · 02).

  Mounted once by `app.main.ui.workspace/workspace*`. It does four things
  and renders no upstream component:

  1. Marks <body> while the workspace is mounted: class `ds-chrome` (the
     scope of every rule in `chrome.scss`), the light theme, and
     `data-ds-width` (wide | medium | narrow) and `data-ds-craft` (open),
     for CSS and for Studio's own scripts.
  2. Applies the width rules (see `app.main.ui.studio.workspace.state`).
  3. Renders the mount points of the other F3 packages: `craft-dock` and
     `viewport-overlay` (listed in STUDIO-SEAMS.md).
  4. In canvas focus (upstream's hide-ui flag, key `\\`) renders two small
     islands over the canvas: the file, and the way back to the panels."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.main.data.workspace :as dw]
   [app.main.refs :as refs]
   [app.main.store :as st]
   [app.main.ui.studio.ds :as sds]
   [app.main.ui.studio.workspace.craft-dock :refer [craft-dock*]]
   [app.main.ui.studio.workspace.quick-prompt :refer [quick-prompt*]]
   [app.main.ui.studio.workspace.state :as state]
   [app.util.i18n :refer [tr]]
   [app.util.theme :as theme]
   [rumext.v2 :as mf]))

;; --- <body> marks

(defn- body-node
  []
  (.-body js/document))

(defn- apply-chrome!
  "The chrome is light only (owner, 15 Nov), so the Penpot theme class is
  forced to `light` while the workspace is mounted."
  []
  (let [classes (.-classList (body-node))]
    (.remove classes "default")
    (.add classes "light")
    (.add classes "ds-chrome")))

(defn- remove-chrome!
  "Gives <body> back to the theme of the profile (dashboard, settings)."
  []
  (let [body    (body-node)
        classes (.-classList body)
        theme   (theme/resolve-theme (:theme (deref refs/profile))
                                     (theme/get-system-theme))]
    (.remove classes "ds-chrome")
    (.removeAttribute body "data-ds-width")
    (.removeAttribute body "data-ds-craft")
    (when (= theme "dark")
      (.remove classes "light")
      (.add classes "default"))))

;; --- Canvas focus (F1 · 02)

(defn- show-panels
  []
  (st/emit! (dw/toggle-layout-flag :hide-ui)))

(mf/defc focus-islands*
  {::mf/private true}
  [{:keys [file-name]}]
  [:*
   [:div {:class (stl/css :island :island-file)
          :data-testid "ds-focus-file"}
    [:> sds/logo-mark* {:size 20}]
    [:span {:class (stl/css :island-file-name)
            :title file-name}
     file-name]]

   [:button {:class (stl/css :island :island-panels)
             :type "button"
             :data-testid "ds-focus-show-panels"
             :on-click show-panels}
    [:span {:class (stl/css :island-label)}
     (tr "ds.workspace.focus.show-panels")]
    ;; The key is upstream's hide-ui shortcut. A backslash is not a word,
    ;; so it stays out of the translations.
    [:kbd {:class (stl/css :island-key)
           :aria-hidden true}
     "\\"]]])

;; --- Shell

(mf/defc shell*
  [{:keys [layout file file-id page-id]}]
  (let [width-class (state/use-width-class)
        craft-open? (mf/deref state/craft-open*)

        collapsed?  (contains? layout :collapse-left-sidebar)
        hide-ui?    (contains? layout :hide-ui)
        ready?      (some? page-id)

        ;; Width class of the last run of the rule, and whether the rule
        ;; itself collapsed Layers.
        prev-ref    (mf/use-ref nil)
        auto-ref    (mf/use-ref false)

        mount-props
        (mf/with-memo [file-id page-id layout width-class]
          #js {:fileId file-id
               :pageId page-id
               :layout layout
               :widthClass width-class})]

    (mf/with-effect []
      (apply-chrome!)
      (let [dispose (theme/add-color-scheme-listener! apply-chrome!)]
        (fn []
          (dispose)
          (remove-chrome!))))

    (mf/with-effect [width-class]
      (.setAttribute (body-node) "data-ds-width" (name width-class)))

    (mf/with-effect [craft-open?]
      (if ^boolean craft-open?
        (.setAttribute (body-node) "data-ds-craft" "open")
        (.removeAttribute (body-node) "data-ds-craft")))

    ;; A panel the person reopened is theirs again.
    (mf/with-effect [collapsed?]
      (when-not ^boolean collapsed?
        (mf/set-ref-val! auto-ref false)))

    (mf/with-effect [width-class ready?]
      (when ^boolean ready?
        (let [action (state/layers-action {:from (mf/ref-val prev-ref)
                                           :to width-class
                                           :collapsed? collapsed?
                                           :auto? (mf/ref-val auto-ref)})]
          (mf/set-ref-val! prev-ref width-class)
          (case action
            :collapse
            (do
              (mf/set-ref-val! auto-ref true)
              (st/emit! (dw/toggle-layout-flag :collapse-left-sidebar :force? true)))

            :expand
            (do
              (mf/set-ref-val! auto-ref false)
              (st/emit! (dw/remove-layout-flag :collapse-left-sidebar)))

            nil))))

    [:*
     ;; Mount point `viewport-overlay`: covers the canvas area, ignores
     ;; pointer events itself.
     [:div {:id "ds-viewport-overlay"
            :class (stl/css :viewport-overlay)
            :data-ds-mount "viewport-overlay"}
      (when (and ^boolean ready? ^boolean hide-ui?)
        [:> focus-islands* {:file-name (:name file)}])
      (when ^boolean ready?
        [:> quick-prompt* mount-props])]

     ;; Mount point `craft-dock`: a grid column between the canvas and the
     ;; right sidebar. No width while the dock renders nothing.
     (when (and ^boolean ready? (not ^boolean hide-ui?))
       [:aside {:id "ds-craft-dock"
                :class (stl/css :craft-dock)
                :data-ds-mount "craft-dock"}
        [:> craft-dock* mount-props]])]))
