;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.ds
  "Design Studio design system: one place to import every base component.

    (:require [app.main.ui.studio.ds :as sds])

    [:> sds/button* {:variant \"primary\" :on-click on-save} (tr \"labels.save\")]

  Not to be confused with `app.main.ui.ds.*`, which is upstream Penpot's own
  design system and stays untouched. Tokens, component API and usage rules:
  STUDIO-DS.md at the repository root."
  (:require
   [app.main.ui.studio.ds.badge :as badge]
   [app.main.ui.studio.ds.button :as button]
   [app.main.ui.studio.ds.card :as card]
   [app.main.ui.studio.ds.dialog :as dialog]
   [app.main.ui.studio.ds.field :as field]
   [app.main.ui.studio.ds.icon :as icon]
   [app.main.ui.studio.ds.logo :as logo]
   [app.main.ui.studio.ds.panel :as panel]
   [app.main.ui.studio.ds.toast :as toast]
   [app.main.ui.studio.ds.toolbar :as toolbar]
   [app.main.ui.studio.ds.tooltip :as tooltip]))

(def badge* badge/badge*)
(def tag* badge/tag*)
(def button* button/button*)
(def icon-button* button/icon-button*)
(def card* card/card*)
(def dialog* dialog/dialog*)
(def field* field/field*)
(def input* field/input*)
(def icon* icon/icon*)
(def logo* logo/logo*)
(def logo-mark* logo/logo-mark*)
(def panel* panel/panel*)
(def toast* toast/toast*)
(def toast-region* toast/toast-region*)
(def toolbar* toolbar/toolbar*)
(def toolbar-group* toolbar/toolbar-group*)
(def toolbar-separator* toolbar/toolbar-separator*)
(def tooltip* tooltip/tooltip*)
