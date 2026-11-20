;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard-shell.scope
  "Marks <body> while a dashboard or settings page is mounted: class
  `ds-dash`, the scope of every rule in `chrome.scss`, and the light theme
  (the boards F1 · 03, 10, 12 and 14 are light only)."
  (:require
   [app.main.refs :as refs]
   [app.util.i18n :refer [tr]]
   [app.util.theme :as theme]
   [rumext.v2 :as mf]))

(defn- apply-scope!
  []
  (let [body    (.-body js/document)
        classes (.-classList body)]
    (.remove classes "default")
    (.add classes "light")
    (.add classes "ds-dash")
    ;; Read by `chrome.scss`: the file cards say "Edited 2 hours ago".
    (.setProperty (.-style body) "--ds-dash-edited" (js/JSON.stringify (str (tr "ds.shell.files.edited") " ")))
    ;; In Trash the date is when the file goes for good ("in 7 days").
    (.setProperty (.-style body) "--ds-dash-removed" (js/JSON.stringify (str (tr "ds.shell.files.removed") " ")))))

(defn- remove-scope!
  "Gives <body> back to the theme of the profile."
  []
  (let [body    (.-body js/document)
        classes (.-classList body)
        theme   (theme/resolve-theme (:theme (deref refs/profile))
                                     (theme/get-system-theme))]
    (.remove classes "ds-dash")
    (.removeProperty (.-style body) "--ds-dash-edited")
    (.removeProperty (.-style body) "--ds-dash-removed")
    (when (= theme "dark")
      (.remove classes "light")
      (.add classes "default"))))

(defn use-scope
  "Hook: the scope is on <body> while the calling component is mounted."
  []
  (mf/with-effect []
    (apply-scope!)
    ;; The app resets the class of <body> when the theme of the profile or
    ;; of the system changes (also once at start, after this effect).
    (let [dispose (theme/add-color-scheme-listener! apply-scope!)]
      (fn []
        (dispose)
        (remove-scope!)))))

(defn use-board-scope
  "Hook: <body> also has the class `ds-board` while the calling component
  is mounted: the dashboard pages and the dialogs opened from them, not
  Settings (P2 `polish.scss` lives under it)."
  []
  (mf/with-effect []
    (let [classes (.-classList (.-body js/document))]
      (.add classes "ds-board")
      (fn []
        (.remove classes "ds-board")))))
