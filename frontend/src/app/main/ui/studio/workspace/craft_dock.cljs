;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.workspace.craft-dock
  "Mount point `craft-dock` (F1 · 05). Owned by F3-B: replace the body of
  `craft-dock*`; the shell (`app.main.ui.studio.workspace.shell`, F3-A)
  already mounts it.

  Where it renders: inside `<aside id=\"ds-craft-dock\"
  data-ds-mount=\"craft-dock\">`, a grid column of the workspace between the
  canvas and the right sidebar. While this component renders nothing the
  column has no width. The aside is laid out by the shell (floating panel
  edge, full height); the dock sets its own inline size (440 px, remembered
  360 to 560 px) and surface (`ws.island` from `studio/workspace`).

  Props: `file-id`, `page-id`, `layout` (workspace layout flags) and
  `width-class` (:wide | :medium | :narrow, see
  `app.main.ui.studio.workspace.state`).

  The shell does not need to be told that Craft is open: it reads
  `<html data-ds-craft-open>`, which F3-B's dock sets."
  (:require
   [rumext.v2 :as mf]))

(mf/defc craft-dock*
  [_props]
  nil)
