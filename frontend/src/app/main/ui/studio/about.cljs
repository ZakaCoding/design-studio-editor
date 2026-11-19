;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.about
  "\"About Design Studio\" (owner, 19 Nov 2026): our name with the official
  lockup, our version, one line on what it is, the engine line with the
  MPL 2.0 link, and Terms, Privacy and Contact. Opened from the Help
  submenu of the workspace main menu and the profile menu of the
  dashboard as the modal `:ds-about`."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.config :as cf]
   [app.main.data.modal :as modal]
   [app.main.store :as st]
   [app.main.ui.studio.brand :as brand]
   [app.main.ui.studio.ds :as sds]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(defn show!
  "Opens the About dialog (replaces any open modal). Takes and ignores the
  event of the menu row that calls it."
  [& _]
  (st/emit! (modal/show {:type :ds-about})))

(mf/defc about-dialog*
  {::mf/register modal/components
   ::mf/register-as :ds-about}
  []
  (let [on-close (mf/use-fn #(st/emit! (modal/hide)))

        on-release-notes
        (mf/use-fn
         #(st/emit! (modal/show {:type :release-notes
                                 :version (:main cf/version)})))

        build (brand/build-label)]

    [:> sds/dialog* {:title (tr "ds.about.title")
                     :class (stl/css :dialog)
                     :on-close on-close
                     :footer (mf/html
                              [:*
                               [:button {:type "button"
                                         :class (stl/css :pill-soft)
                                         :on-click on-release-notes
                                         :data-testid "ds-about-release-notes"}
                                (tr "labels.release-notes")]
                               [:button {:type "button"
                                         :class (stl/css :pill-black)
                                         :on-click on-close}
                                (tr "ds.release-notes.done")]])}
     [:div {:class (stl/css :about) :data-testid "ds-about"}
      [:div {:class (stl/css :identity)}
       [:> sds/logo* {:size "lg"}]
       [:p {:class (stl/css :version)}
        (if build
          (tr "ds.about.version-build" (brand/version-label) build)
          (tr "ds.about.version" (brand/version-label)))]]

      [:p {:class (stl/css :tagline)} (tr "ds.about.tagline")]

      [:p {:class (stl/css :engine)}
       (tr "ds.about.engine" (brand/engine-version))
       " · "
       [:a {:href brand/mpl-uri
            :target "_blank"
            :rel "noopener noreferrer"
            :class (stl/css :link)}
        "MPL 2.0"]]

      [:nav {:class (stl/css :links) :aria-label (tr "ds.about.links")}
       [:a {:href (brand/terms-of-service-uri) :target "_blank" :rel "noopener noreferrer" :class (stl/css :link)}
        (tr "ds.about.terms")]
       [:span {:class (stl/css :dot) :aria-hidden true} "·"]
       [:a {:href brand/privacy-uri :target "_blank" :rel "noopener noreferrer" :class (stl/css :link)}
        (tr "ds.about.privacy")]
       [:span {:class (stl/css :dot) :aria-hidden true} "·"]
       [:a {:href brand/contact-uri :class (stl/css :link)}
        (tr "ds.about.contact")]]]]))
