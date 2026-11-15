;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.craft
  "Craft in the editor (F3-B): the native dock that hosts Studio's Craft
  panel (F1 · 05) and the quick prompt at the selection (F1 · 06). Both
  show only in the workspace and only for a member who has Craft, which
  Studio says in the editor-shell state.

  Two ways in, and never both:

  - With the workspace shell (F3-A, `app.main.ui.studio.workspace.shell`):
    the shell mounts `app.main.ui.studio.workspace.craft-dock/craft-dock*`
    in its `craft-dock` mount point (a column beside the canvas) and
    `app.main.ui.studio.workspace.quick-prompt/quick-prompt*` in its
    `viewport-overlay` mount point.
  - Without it: `craft*`, mounted once from
    `app.main.ui.studio.widgets/global-widgets*`, shows both itself,
    floating. It steps back as soon as the shell is there, which it knows
    by the shell's scope class on <body> (`ds-chrome`).

  `craft*` also sets `<html data-ds-craft-dock>` for Studio's script. For
  other layouts: whether Craft is open is
  `app.main.ui.studio.craft.state/open?` and, on <html>, the attribute
  `data-ds-craft-open` (\"dock\" | \"overlay\"). `craft-toggle*` can be
  mounted in a top bar; then pass `toggle \"none\"` to the dock."
  (:require
   [app.main.refs :as refs]
   [app.main.ui.studio.craft.dock :as dock]
   [app.main.ui.studio.craft.quick-prompt :refer [quick-prompt*]]
   [app.main.ui.studio.shell-state :as ss]
   [rumext.v2 :as mf]))

(def craft-dock* dock/craft-dock*)
(def craft-toggle* dock/craft-toggle*)

(defn use-access
  "Hook: true when Studio says this member has Craft."
  []
  (true? (:craft (ss/use-state))))

(defn- shell?
  "True when the workspace shell is mounted: it has the mount points."
  []
  (.contains (.-classList (.-body js/document)) "ds-chrome"))

(mf/defc craft*
  "Props: `section` (the route's name)."
  [{:keys [section]}]
  (let [craft?     (use-access)
        workspace? (= section :workspace)
        ;; The page is there only after the workspace, and with it the
        ;; shell when this build has one, has been mounted.
        ready?     (some? (mf/deref refs/workspace-page))
        ;; True once it is known that this component shows the dock itself.
        ;; It stays so while the page changes, so the panel is not reloaded.
        own*       (mf/use-state false)]

    ;; Says to Studio's ds-craft.js that this editor has a dock of its own,
    ;; before the dock is on screen, so it never adds its launcher.
    (mf/with-effect []
      (.setAttribute (.-documentElement js/document) "data-ds-craft-dock" "1"))

    (mf/with-effect [workspace? ready?]
      (cond
        (not workspace?) (reset! own* false)
        ready?           (reset! own* (not (shell?)))))

    (when (and craft? workspace? (deref own*))
      [:*
       [:> dock/craft-dock* {}]
       [:> quick-prompt* {}]])))
