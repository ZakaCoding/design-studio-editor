;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.workspace.craft-dock
  "Mount point `craft-dock` (F1 · 05), filled by F3-B. The workspace shell
  (`app.main.ui.studio.workspace.shell`, F3-A) mounts `craft-dock*` inside
  `<aside id=\"ds-craft-dock\" data-ds-mount=\"craft-dock\">`, a grid column
  between the canvas and the right sidebar, and lays that aside out. The
  dock fills its height and sets its own width (440 px, remembered between
  360 and 560 px).

  This file has the same path as the stub on the shell's branch: when the
  two branches meet, keep this one.

  Props (unused here): `file-id`, `page-id`, `layout`, `width-class`."
  (:require
   [app.main.ui.studio.craft :as craft]
   [rumext.v2 :as mf]))

(mf/defc craft-dock*
  [_props]
  (when (craft/use-access)
    [:> craft/craft-dock* {:placement "column"}]))
