;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.craft
  "Craft in the editor (F3): the native dock that hosts Studio's Craft
  panel (F1 · 05) and the quick prompt at the selection (F1 · 06). Both
  show only in the workspace and only for a member who has Craft, which
  Studio says in the editor-shell state. Mounted once, from
  `app.main.ui.studio.widgets/global-widgets*`.

  For the workspace layout (F3-A): `craft-dock*` places itself (fixed,
  beside the design panel) and needs no slot. `craft-toggle*` can be
  mounted in a top bar; then pass `dock-toggle \"none\"` here. Whether
  Craft is open is `app.main.ui.studio.craft.state/open?`, and on
  `<html>` the attribute `data-ds-craft-open` (\"dock\" | \"overlay\")."
  (:require
   [app.main.ui.studio.craft.dock :as dock]
   [app.main.ui.studio.craft.quick-prompt :refer [quick-prompt*]]
   [app.main.ui.studio.shell-state :as ss]
   [rumext.v2 :as mf]))

(def craft-dock* dock/craft-dock*)
(def craft-toggle* dock/craft-toggle*)

(mf/defc craft*
  "Props: `section` (the route's name), `dock-toggle` (see `craft-dock*`)."
  [{:keys [section dock-toggle]}]
  (let [shell  (ss/use-state)
        craft? (true? (:craft shell))]

    ;; Says to Studio's ds-craft.js that this editor has a dock of its own,
    ;; before the dock is on screen, so it never adds its launcher.
    (mf/with-effect []
      (.setAttribute (.-documentElement js/document) "data-ds-craft-dock" "1"))

    (when (and craft? (= section :workspace))
      [:*
       [:> dock/craft-dock* {:toggle dock-toggle}]
       [:> quick-prompt*]])))
