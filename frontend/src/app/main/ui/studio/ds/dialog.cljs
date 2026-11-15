;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding Studio

(ns app.main.ui.studio.ds.dialog
  "Dialog shell of the Design Studio DS: scrim, surface, title, body, footer.

  Mounted = open. The caller renders it conditionally and closes it from
  `on-close`. The shell handles: focus moves into the dialog and returns to
  where it was, Tab stays inside, Esc and a click on the scrim call
  `on-close`."
  (:require-macros
   [app.main.style :as stl])
  (:require
   [app.common.data :as d]
   [app.main.ui.hooks :as hooks]
   [app.main.ui.studio.ds.button :refer [icon-button*]]
   [app.main.ui.studio.ds.icon :refer [icon*]]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [app.util.keyboard :as kbd]
   [rumext.v2 :as mf]))

(def ^:private focusable-selector
  (str "a[href], button:not([disabled]), input:not([disabled]), "
       "select:not([disabled]), textarea:not([disabled]), "
       "[tabindex]:not([tabindex='-1'])"))

(defn- trap-tab
  "Keeps Tab and Shift+Tab inside `node`."
  [^js node ^js event]
  (let [nodes  (.from js/Array (.querySelectorAll node focusable-selector))
        total  (.-length nodes)
        active (dom/get-active)]
    (if (zero? total)
      (dom/prevent-default event)
      (let [first-node (aget nodes 0)
            last-node  (aget nodes (dec total))]
        (cond
          (and (kbd/shift? event)
               (or (identical? active first-node)
                   (identical? active node)))
          (do (dom/prevent-default event)
              (dom/focus! last-node))

          (and (not (kbd/shift? event))
               (identical? active last-node))
          (do (dom/prevent-default event)
              (dom/focus! first-node)))))))

(def ^:private schema:dialog
  [:map
   [:class {:optional true} [:maybe :string]]
   [:title :string]
   [:description {:optional true} [:maybe :string]]
   [:size {:optional true} [:maybe [:enum "md" "lg"]]]
   [:on-close fn?]
   [:close-label {:optional true} [:maybe :string]]
   [:is-dismissible {:optional true} [:maybe :boolean]]])

(mf/defc dialog*
  "Props: `title` (required), `description`, children (body), `footer`
  (element: the action buttons, primary last), `on-close` (required),
  `size` md (440, default) | lg (640), `is-dismissible` (default true;
  false removes the close button and ignores Esc and scrim clicks, for a
  step the user must answer), `close-label`, `class`.
  Rendered in a portal on <body>."
  {::mf/schema schema:dialog}
  [{:keys [title description footer on-close size is-dismissible close-label class children]}]
  (let [title-id     (mf/use-id)
        desc-id      (mf/use-id)
        container    (hooks/use-portal-container :modal)
        node-ref     (mf/use-ref nil)
        dismissible? (d/nilv is-dismissible true)

        on-key-down
        (mf/use-fn
         (mf/deps on-close dismissible?)
         (fn [event]
           (cond
             (kbd/esc? event)
             (do
               (dom/stop-propagation event)
               (when dismissible?
                 (on-close event)))

             (kbd/tab? event)
             (trap-tab (mf/ref-val node-ref) event))))

        on-scrim-pointer-down
        (mf/use-fn
         (mf/deps on-close dismissible?)
         (fn [event]
           (when (and dismissible?
                      (identical? (dom/get-target event)
                                  (dom/get-current-target event)))
             (on-close event))))]

    ;; Focus goes into the dialog on open and back to the opener on close.
    (mf/with-effect []
      (let [previous (dom/get-active)]
        (dom/focus! (mf/ref-val node-ref))
        (fn []
          (dom/focus! previous))))

    (mf/portal
     (mf/html
      [:div {:class (stl/css :scrim)
             :on-pointer-down on-scrim-pointer-down}
       [:div {:class [class (stl/css-case :dialog true
                                          :size-lg (= size "lg"))]
              :ref node-ref
              :role "dialog"
              :aria-modal true
              :aria-labelledby title-id
              :aria-describedby (when (some? description) desc-id)
              :tab-index -1
              :on-key-down on-key-down}
        [:header {:class (stl/css :header)}
         [:div {:class (stl/css :heading)}
          [:h2 {:class (stl/css :title) :id title-id} title]
          (when (some? description)
            [:p {:class (stl/css :description) :id desc-id} description])]
         (when dismissible?
           [:> icon-button* {:aria-label (d/nilv close-label (tr "labels.close"))
                             :on-click on-close}
            [:> icon* {:size "m"}
             [:path {:d "M18 6 6 18"}]
             [:path {:d "m6 6 12 12"}]]])]
        [:div {:class (stl/css :body)} children]
        (when (some? footer)
          [:footer {:class (stl/css :footer)} footer])]])
     container)))
