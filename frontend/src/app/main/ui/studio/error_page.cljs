;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.error-page
  "Layout of the full-page errors (404, 500, 502/503, invalid link, lost
  WebGL context) in the Design Studio light style: official Logo, the
  error and its cause, what the member can do about it, the way on, and
  the credit upstream is owed. Penpot's own pages were a black screen with
  its illustration.

  P3D (owner 19 Nov, \"dense, functional and informative\"): a card of at
  most 560 px placed high and left, a status pill, two or three things to
  try, and for server errors a line of error details to copy.

  `static.cljs` keeps the page logic (which error, retry, report); it renders
  its message as `children` and its buttons as `actions` / `more`. The
  classes it gives the message are restyled here by their CSS-module name."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.ui.studio.ds.button :refer [button*]]
   [app.main.ui.studio.ds.logo :refer [logo*]]
   [app.util.clipboard :as clipboard]
   [app.util.i18n :as i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def contact-email "hello@zakacoding.dev")

(defn- locale-tag [] (if (= "id" (deref i18n/locale)) "id-ID" "en-US"))

(defn- format-time
  "Through Intl: inside the editor Date.prototype.toLocale* is replaced."
  [date]
  (.format (js/Intl.DateTimeFormat. (locale-tag)
                                    #js {:day "numeric" :month "short" :year "numeric"
                                         :hour "2-digit" :minute "2-digit" :second "2-digit"})
           date))

(defn- current-path
  []
  (str (.-pathname js/location) (.-hash js/location)))

(mf/defc details*
  "The error details of a server error: status, time, page (and what the
  page adds, e.g. the report link), with Copy."
  {::mf/private true}
  [{:keys [code extra]}]
  (let [at      (mf/with-memo [] (js/Date.))
        path    (mf/with-memo [] (current-path))
        copied* (mf/use-state false)
        text    (str "Status: " code "\nTime: " (.toISOString at) "\nPage: " path)
        on-copy (mf/use-fn
                 (mf/deps text)
                 (fn [_]
                   (-> (clipboard/to-clipboard text)
                       (.then (fn []
                                (reset! copied* true)
                                (js/setTimeout #(reset! copied* false) 2000)))
                       (.catch (constantly nil)))))]
    [:div {:class (stl/css :details) :data-testid "ds-error-details"}
     [:p {:class (stl/css :details-line)}
      [:span {:class (stl/css :details-label)} (tr "ds.error.details")]
      [:span {:class (stl/css :details-value)} code]
      [:span {:class (stl/css :details-value)} (format-time at)]
      [:span {:class (stl/css :details-path) :title path} path]]
     [:div {:class (stl/css :details-actions)}
      extra
      [:button {:class (stl/css :copy) :type "button" :on-click on-copy}
       (if @copied* (tr "ds.error.copied") (tr "ds.error.copy"))]]]))

(mf/defc error-shell*
  "Props: `code` (\"404\", \"500\"…), `label` (what kind of error, next to
  the code in the pill), `on-home` (go to the home page), `primary-home?`
  (the home button is the main action, when the page has no other),
  `actions` (the page's own main buttons, before the home button), `more`
  (secondary buttons after it), `tips` (two or three things to try),
  `details?` (a server error: show the error details line), `extra` (in
  that line, e.g. the report download), `children` (title and cause)."
  [{:keys [code label on-home primary-home? actions more tips details? extra children]}]
  [:section {:class (stl/css :page)
             :data-testid "ds-error-page"}
   [:div {:class (stl/css :frame)}
    [:header {:class (stl/css :top)}
     [:button {:type "button"
               :class (stl/css :brand)
               :aria-label (tr "ds.error.home")
               :on-click on-home}
      [:> logo* {}]]]

    [:main {:class (stl/css :main)}
     [:div {:class (stl/css :card)}
      (when (or (some? code) (some? label))
        [:p {:class (stl/css :status)}
         (when (some? code)
           [:span {:class (stl/css-case :code true
                                        :code-server (= "5" (subs code 0 1)))}
            code])
         (when (some? label) [:span {:class (stl/css :label)} label])])

      [:div {:class (stl/css :content)} children]

      (when (seq tips)
        [:div {:class (stl/css :tips)}
         [:p {:class (stl/css :tips-title)} (tr "ds.error.what-to-do")]
         [:ul {:class (stl/css :tips-list)}
          (for [[idx tip] (map-indexed vector tips)]
            [:li {:key idx} tip])]])

      [:div {:class (stl/css :actions)}
       (when ^boolean primary-home?
         [:> button* {:variant "primary" :on-click on-home}
          (tr "ds.error.home")])
       actions
       (when-not ^boolean primary-home?
         [:> button* {:variant "secondary" :on-click on-home}
          (tr "ds.error.home")])
       more]

      [:footer {:class (stl/css :card-foot)}
       (when ^boolean details?
         [:> details* {:code code :extra extra}])
       [:p {:class (stl/css :contact)}
        (tr "ds.error.still-stuck") " "
        [:a {:href (str "mailto:" contact-email)} contact-email]]]]]

    [:footer {:class (stl/css :credit)}
     [:span (tr "ds.error.built-on") " "
      [:a {:href "https://penpot.app"
           :target "_blank"
           :rel "noopener noreferrer"}
       "penpot.app"]]
     [:span {:aria-hidden true} "·"]
     [:a {:href "https://www.mozilla.org/MPL/2.0/"
          :target "_blank"
          :rel "noopener noreferrer"}
      (tr "ds.error.license")]]]])
