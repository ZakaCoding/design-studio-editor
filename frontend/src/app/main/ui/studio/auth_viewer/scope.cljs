;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.auth-viewer.scope
  "Marks <body> while an auth screen or the viewer is mounted: the class
  `ds-auth` or `ds-viewer` (the scope of the rules in `auth_viewer/*.scss`)
  and Penpot's light theme, so upstream parts drawn there (toasts, menus,
  dialogs, comments) start light and the Design Studio rules finish them.
  Same idea as `dashboard-shell.scope`."
  (:require
   [app.main.refs :as refs]
   [app.util.theme :as theme]
   [rumext.v2 :as mf]))

(defn- apply-scope!
  [scope]
  (let [classes (.-classList (.-body js/document))]
    (.remove classes "default")
    (.add classes "light")
    (.add classes scope)))

(defn- remove-scope!
  "Gives <body> back to the theme of the profile."
  [scope]
  (let [classes (.-classList (.-body js/document))
        theme   (theme/resolve-theme (:theme (deref refs/profile))
                                     (theme/get-system-theme))]
    (.remove classes scope)
    (when (= theme "dark")
      (.remove classes "light")
      (.add classes "default"))))

(defn use-scope
  "Hook: `scope` (\"ds-auth\" or \"ds-viewer\") is on <body> while the
  calling component is mounted."
  [scope]
  (mf/with-effect [scope]
    (apply-scope! scope)
    ;; The app resets the class of <body> when the theme of the profile or
    ;; of the system changes (also once at start, after this effect).
    (let [apply!  (fn [] (apply-scope! scope))
          dispose (theme/add-color-scheme-listener! apply!)]
      (fn []
        (dispose)
        (remove-scope! scope)))))
