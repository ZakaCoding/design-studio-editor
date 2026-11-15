;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.workspace.quick-prompt
  "Mount point `viewport-overlay` (F1 · 06). Owned by F3-B: replace the body
  of `quick-prompt*`; the shell (`app.main.ui.studio.workspace.shell`, F3-A)
  already mounts it.

  Where it renders: inside `<div id=\"ds-viewport-overlay\"
  data-ds-mount=\"viewport-overlay\">`, which covers exactly the canvas area
  (the `viewport` grid cell, rulers included) above the viewport and below
  menus and dialogs. The layer ignores pointer events; give your own root
  `pointer-events: auto`. Position with `position: absolute` in CSS px from
  the top left corner of the canvas area.

  Props: `file-id`, `page-id`, `layout` and `width-class`. Read the selection
  and the viewport through the existing refs (`refs/selected-shapes`,
  `refs/workspace-local`); do not emit from `main/data/**` that is not there
  already."
  (:require
   [rumext.v2 :as mf]))

(mf/defc quick-prompt*
  [_props]
  nil)
