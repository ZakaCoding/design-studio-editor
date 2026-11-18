;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.auth
  "Design Studio auth screens (F1 · 08 Masuk, F1 · 09 states).

  Presentation only. The upstream auth namespaces keep their forms, schemas,
  events and redirects, and render with the components defined here."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.common.data.macros :as dm]
   [app.config :as cf]
   [app.main.router :as rt]
   [app.main.store :as st]
   [app.main.ui.components.forms :as fm]
   [app.main.ui.studio.auth-viewer.scope :as scope]
   [app.main.ui.studio.ds :as sds]
   [app.util.dom :as dom]
   [app.util.forms :as uf]
   [app.util.globals :as globals]
   [app.util.i18n :as i18n :refer [tr]]
   [rumext.v2 :as mf]))

;; --- Helpers

(defn registration-open?
  "Open sign-up (ADR 0017), by the overlay's rule (penpot-login
  ds-login.js.tpl `open()`): closed without `penpotFlags` (config.js missing),
  closed with `disable-registration`, open otherwise. Penpot's defaults
  include `:registration`, so the flag set alone would say open when
  config.js is missing."
  []
  (and (some? (unchecked-get js/globalThis "penpotFlags"))
       (contains? cf/flags :registration)))

(defn signup-href
  "Our own sign-up form. Penpot's register form is refused by the backend
  guard, because a password chosen before the email is proved could be
  someone else's (ADR 0017)."
  []
  (if (= "id" (deref i18n/locale))
    "/start/signup?lang=id"
    "/start/signup"))

(defn show-used-link?
  "A sign-up or reset link works once. With open sign-up a used or expired
  one gets our own explanation; an invitation sent to another email keeps
  Penpot's message, and so does everything while sign-up is closed."
  [reason]
  (and (some? reason)
       (not= :email-mismatch reason)
       (registration-open?)))

(defn- lang-param
  "`?lang=id|en` before the hash, as the Studio pages link here."
  []
  (let [search (.-search globals/location)
        value  (.get (js/URLSearchParams. search) "lang")]
    (when (or (= value "id") (= value "en"))
      value)))

(defn- error-message
  [error]
  (let [message (:message error)
        code    (:code error)]
    (cond
      (some? message) message
      (vector? code)  (tr (nth code 0) (i18n/c (nth code 1)))
      (some? code)    (tr code)
      :else           nil)))

;; Classes for upstream components that take a `:class` (form, link).
(def form-class (stl/css :form))
(def link-class (stl/css :link))
(def links-class (stl/css :links))

;; --- Brand

(mf/defc logo*
  "The official Logo component (brand rule: nobody redraws the mark)."
  []
  [:> sds/logo* {}])

;; --- Building blocks

;; Carries the DS tokens. Inside the shell or our modal it is invisible;
;; anywhere else (the viewer's own login modal) it is a white card, so the
;; form stays readable on Penpot's theme.
(mf/defc scope*
  [{:keys [children]}]
  [:div {:class (stl/css :scope)} children])

(mf/defc page*
  [{:keys [title subtitle title-testid children]}]
  [:div {:class (stl/css :page)}
   [:h1 {:class (stl/css :title)
         :data-testid title-testid}
    title]
   (when (some? subtitle)
     [:p {:class (stl/css :subtitle)} subtitle])
   children])

(mf/defc alert*
  [{:keys [children]}]
  [:div {:class (stl/css :alert)
         :role "alert"
         :data-testid "ds-auth-error"}
   children])

(mf/defc note*
  [{:keys [level data-testid children]}]
  [:p {:class (stl/css-case :note true
                            :note-warning (= level "warning"))
       :data-testid data-testid}
   children])

(mf/defc divider*
  []
  [:div {:class (stl/css :divider)
         :role "separator"}
   [:span {:class (stl/css :divider-text)} (tr "ds.auth.or")]])

;; A text input bound to the surrounding `fm/form`, like `fm/input`:
;; same form state, same validation, same ids (`#email`, `#password`).
(mf/defc field*
  [{:keys [field label type placeholder aside auto-focus auto-complete]}]
  (let [form      (mf/use-ctx fm/form-ctx)
        id        (d/name field)
        type      (d/nilv type "text")
        password? (= type "password")

        reveal*   (mf/use-state false)
        reveal?   (deref reveal*)
        blurred*  (mf/use-state false)
        blurred?  (deref blurred*)

        state     (deref form)
        value     (get-in state [:data field] "")
        touched?  (get-in state [:touched field])
        extra     (get-in state [:extra-errors field])
        error     (or (get-in state [:errors field]) extra)
        message   (when (or (some? extra)
                            (and touched? blurred?))
                    (some-> error error-message))
        options   (when (some? message) (:options error))
        error-id  (dm/str "error-" id)

        on-change
        (mf/use-fn
         (mf/deps form field)
         (fn [event]
           (let [value (-> event dom/get-target dom/get-input-value)]
             (uf/on-input-change form field value false))))

        on-blur
        (mf/use-fn #(reset! blurred* true))

        on-toggle
        (mf/use-fn #(swap! reveal* not))]

    [:div {:class (stl/css :field)}
     [:div {:class (stl/css :field-head)}
      [:label {:class (stl/css :field-label)
               :for id}
       label]
      aside]
     [:div {:class (stl/css-case :control true
                                 :control-invalid (some? message))}
      [:input {:class (stl/css :input)
               :id id
               :name id
               :type (if (and password? reveal?) "text" type)
               :value value
               :placeholder placeholder
               :auto-focus (true? auto-focus)
               :auto-complete auto-complete
               :aria-invalid (when (some? message) "true")
               :aria-describedby (when (some? message) error-id)
               :on-change on-change
               :on-blur on-blur}]
      (when password?
        [:button {:class (stl/css :reveal)
                  :type "button"
                  :aria-pressed (if reveal? "true" "false")
                  :aria-label (if reveal?
                                (tr "ds.signin.hide-password")
                                (tr "ds.signin.show-password"))
                  :on-click on-toggle}
         [:svg {:viewBox "0 0 20 20"
                :aria-hidden "true"
                :class (stl/css :reveal-icon)}
          [:path {:d "M2.5 10s2.7-5 7.5-5 7.5 5 7.5 5-2.7 5-7.5 5-7.5-5-7.5-5z"}]
          [:circle {:cx "10" :cy "10" :r "2.2"}]
          (when reveal?
            [:path {:d "M4 16 16 4"}])]])]
     (when (some? message)
       [:div {:class (stl/css :field-error)
              :id error-id
              :data-testid (dm/str id "-error")}
        message
        (when (seq options)
          [:ul {:class (stl/css :field-error-options)}
           (for [option options]
             [:li {:key option} option])])])]))

;; Primary button. Disabled while the form is invalid, as `fm/submit-button*`.
;; `busy` only draws the spinner while the request is on its way (P3-B): the
;; button itself behaves as before.
(mf/defc submit*
  [{:keys [label data-testid on-click busy]}]
  [:> fm/submit-button* {:label label
                         :data-testid data-testid
                         :on-click on-click
                         :aria-busy (when (true? busy) "true")
                         :class (stl/css-case :button true
                                              :button-primary true
                                              :button-busy (true? busy))}])

(mf/defc sso-button*
  [{:keys [label on-click]}]
  [:button {:class (stl/css :button :button-secondary)
            :type "button"
            :data-testid "ds-sso"
            :on-click on-click}
   label])

;; Open sign-up: a link to our sign-up form, in a row of its own under the
;; form so a newcomer sees it (P3-B). Closed: say so, in the same place.
(mf/defc signup-row*
  []
  (if (registration-open?)
    [:p {:class (stl/css :signup)}
     [:span {:class (stl/css :signup-text)}
      (tr "ds.signin.no-account")]
     [:a {:class (stl/css :signup-link)
          :href (signup-href)
          :data-testid "register-submit"}
      (tr "ds.signin.signup")
      [:svg {:viewBox "0 0 16 16"
             :aria-hidden "true"}
       [:path {:d "M3.5 8h9m-3.5-3.5L12.5 8 9 11.5"}]]]]
    [:> note* {:data-testid "ds-signup-closed"}
     (tr "ds.signin.closed")]))

;; --- Mail sent (F1 · 09 "Cek email kamu")

(mf/defc sent*
  [{:keys [title text children]}]
  [:div {:class (stl/css :page)
         :data-testid "ds-auth-sent"}
   [:span {:class (stl/css :state-icon :state-icon-accent)
           :aria-hidden "true"}
    [:svg {:viewBox "0 0 24 24"}
     [:rect {:x "3" :y "5" :width "18" :height "14" :rx "3"}]
     [:path {:d "m4 7.5 8 6 8-6"}]]]
   [:h1 {:class (stl/css :title)} title]
   [:p {:class (stl/css :body)} text]
   children])

;; --- Used link (F1 · 09 "Link sudah dipakai")

(mf/defc used-link*
  []
  (let [on-request (mf/use-fn #(st/emit! (rt/nav :auth-recovery-request)))
        on-login   (mf/use-fn #(st/emit! (rt/nav :auth-login)))]
    [:div {:class (stl/css :page)
           :data-testid "ds-used-link"}
     [:span {:class (stl/css :state-icon :state-icon-danger)
             :aria-hidden "true"}
      [:svg {:viewBox "0 0 24 24"}
       [:circle {:cx "12" :cy "12" :r "9"}]
       [:path {:d "m5.6 5.6 12.8 12.8"}]]]
     [:h1 {:class (stl/css :title)} (tr "ds.auth.link-used.title")]
     [:p {:class (stl/css :body)} (tr "ds.used-link.description")]
     [:div {:class (stl/css :actions)}
      [:button {:class (stl/css :button :button-primary)
                :type "button"
                :data-testid "ds-used-link-request"
                :on-click on-request}
       (tr "ds.used-link.request")]
      [:button {:class (stl/css :button :button-secondary)
                :type "button"
                :data-testid "ds-used-link-login"
                :on-click on-login}
       (tr "ds.used-link.login")]]]))

;; --- Shell (F1 · 08)

(mf/defc lang-switch*
  {::mf/private true}
  [{:keys [locale]}]
  (let [on-click
        (mf/use-fn
         (fn [event]
           (let [value (-> (dom/get-current-target event)
                           (dom/get-data "lang"))]
             (i18n/set-locale value))))]
    [:div {:class (stl/css :lang)
           :role "group"
           :aria-label (tr "ds.shell.language")}
     (for [value ["id" "en"]]
       [:button {:key value
                 :type "button"
                 :class (stl/css-case :lang-option true
                                      :lang-current (= value locale))
                 :data-lang value
                 :aria-pressed (if (= value locale) "true" "false")
                 :on-click on-click}
        value])]))

(mf/defc stickers*
  {::mf/private true}
  []
  [:aside {:class (stl/css :panel)
           :aria-hidden "true"}
   [:span {:class (stl/css :sticker :sticker-landing)} (tr "ds.shell.sticker.landing")]
   [:span {:class (stl/css :sticker :sticker-brand)} (tr "ds.shell.sticker.brand")]
   [:span {:class (stl/css :sticker :sticker-poster)} (tr "ds.shell.sticker.poster")]
   [:span {:class (stl/css :selection)}
    [:span {:class (stl/css :sticker :sticker-card)} (tr "ds.shell.sticker.card")]
    [:i {:class (stl/css :handle :handle-nw)}]
    [:i {:class (stl/css :handle :handle-ne)}]
    [:i {:class (stl/css :handle :handle-sw)}]
    [:i {:class (stl/css :handle :handle-se)}]
    [:i {:class (stl/css :measure)}]
    [:span {:class (stl/css :measure-tag)} "86"]]
   [:span {:class (stl/css :sticker :sticker-app)} (tr "ds.shell.sticker.app")]
   [:span {:class (stl/css :sticker :sticker-feed)} (tr "ds.shell.sticker.feed")]
   [:span {:class (stl/css :cursor :cursor-you)}
    [:svg {:viewBox "0 0 22 22"}
     [:path {:d "M3 2.5 17 8l-6.2 2.3L8.5 17z"}]]
    [:span {:class (stl/css :cursor-label)} (tr "ds.shell.cursor.you")]]
   [:span {:class (stl/css :cursor :cursor-craft)}
    [:svg {:viewBox "0 0 22 22"}
     [:path {:d "M3 2.5 17 8l-6.2 2.3L8.5 17z"}]]
    [:span {:class (stl/css :cursor-label)} "Craft"]]
   [:div {:class (stl/css :panel-copy)}
    [:p {:class (stl/css :panel-title)} (tr "ds.shell.panel.title")]
    [:p {:class (stl/css :panel-text)} (tr "ds.shell.panel.text")]]])

;; The page around every auth screen: lockup, ID/EN, the screen itself,
;; legal line and the sticker panel.
;;
;; `leave-to-signup` is true on Penpot's register routes. With open sign-up
;; those go to our own form, as the overlay did before F2.
(mf/defc shell*
  [{:keys [section leave-to-signup children]}]
  (let [locale (mf/deref i18n/locale)
        leave? (and (true? leave-to-signup)
                    (registration-open?))]

    ;; Light theme and the `ds-auth` scope for what Penpot draws here
    ;; (toasts, the register form while sign-up is closed).
    (scope/use-scope "ds-auth")

    (mf/with-effect []
      (when-let [value (lang-param)]
        (when (not= value (deref i18n/locale))
          (i18n/set-locale value))))

    (mf/with-effect [leave?]
      (when leave?
        (.replace globals/location (signup-href))))

    [:main {:class (stl/css :shell)
            :data-ds-auth section
            :lang locale}
     [:div {:class (stl/css :pane)}
      [:header {:class (stl/css :topbar)}
       [:a {:class (stl/css :lockup-link)
            :href "#/"
            :aria-label "Design Studio"}
        [:> logo* {}]]
       [:> lang-switch* {:locale locale}]]

      [:section {:class (stl/css :content)
                 :key locale}
       (when-not leave?
         children)]

      [:footer {:class (stl/css :legal)}
       [:p {:class (stl/css :legal-text)}
        (tr "ds.shell.legal.prefix")
        " "
        [:a {:class (stl/css :legal-link)
             :href "/start/legal/terms"}
         (tr "ds.shell.legal.terms")]
        " "
        (tr "ds.shell.legal.and")
        " "
        [:a {:class (stl/css :legal-link)
             :href "/start/legal/privacy"}
         (tr "ds.shell.legal.privacy")]
        "."]
       [:p {:class (stl/css :legal-text)}
        (tr "ds.auth.attribution")]]]

     [:> stickers* {}]]))

(mf/defc used-link-page*
  []
  [:> shell* {:section "used-link"}
   [:> used-link* {}]])

;; --- Modal (signed-out visit to a dashboard, workspace or viewer URL)

(mf/defc modal*
  [{:keys [on-close close-label children]}]
  [:div {:class (stl/css :modal)
         :role "dialog"
         :aria-modal "true"
         :aria-label "Design Studio"
         :data-ds-auth "modal"}
   [:button {:class (stl/css :modal-close)
             :type "button"
             :aria-label close-label
             :on-click on-close}
    [:svg {:viewBox "0 0 16 16"
           :aria-hidden "true"}
     [:path {:d "m3.5 3.5 9 9m0-9-9 9"}]]]
   [:div {:class (stl/css :modal-logo)}
    [:> logo* {}]]
   children])
