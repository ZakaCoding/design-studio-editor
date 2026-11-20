;; Design Studio (WP4): notes on Settings > Password and Settings > Profile.
;; Replaces the overlay's passwordPage/profilePage in ds-shell.js (rows S5, S6).

(ns app.main.ui.studio.settings-notes
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.ui.studio.shell-state :as ss]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(mf/defc password-note*
  "Explains what the password is. Renders nothing until the server answered."
  [{:keys [state]}]
  (when state
    [:p {:class (stl/css :note)
         :data-testid "ds-password-note"}
     (if (ss/backup? state)
       (tr "ds.settings.password-note-backup")
       (tr "ds.settings.password-note-open"))]))

(mf/defc email-note*
  "Open sign-up: how to change an email, in place of the change-email link."
  []
  [:p {:class (stl/css :email-note)
       :data-testid "ds-email-note"}
   [:span (tr "ds.settings.email-note")]
   [:a {:href "mailto:hello@zakacoding.dev"} "hello@zakacoding.dev"]
   [:span "."]])
