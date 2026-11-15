;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.workspace.quick-prompt
  "Mount point `viewport-overlay` (F1 · 06), filled by F3-B. The workspace
  shell (`app.main.ui.studio.workspace.shell`, F3-A) mounts `quick-prompt*`
  inside `<div id=\"ds-viewport-overlay\" data-ds-mount=\"viewport-overlay\">`,
  which covers the canvas area and ignores the pointer. The prompt places
  itself there, in px from the top left corner of the canvas area.

  This file has the same path as the stub on the shell's branch: when the
  two branches meet, keep this one.

  Props (unused here): `file-id`, `page-id`, `layout`, `width-class`."
  (:require
   [app.main.ui.studio.craft :as craft]
   [app.main.ui.studio.craft.quick-prompt :as quick]
   [rumext.v2 :as mf]))

(mf/defc quick-prompt*
  [_props]
  (when (craft/use-access)
    [:> quick/quick-prompt* {:placement "overlay"}]))
