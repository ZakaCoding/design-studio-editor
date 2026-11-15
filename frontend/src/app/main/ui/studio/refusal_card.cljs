;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.refusal-card
  "What a guard refusal means, in the member's words: one card at the
  bottom of the page until they close it. See `app.main.ui.studio.refusal`
  for where refusals come from."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.ui.studio.refusal :as refusal]
   [app.util.i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def ^:private plans-uri "/studio/craft/plans")

(defn- title
  [code]
  (case code
    :storage-limit-reached      (tr "ds.refusal.storage-limit-reached.title")
    :too-many-uploads           (tr "ds.refusal.too-many-uploads.title")
    :storage-unavailable        (tr "ds.refusal.storage-unavailable.title")
    :invite-limit-reached       (tr "ds.refusal.invite-limit-reached.title")
    :too-many-recovery-requests (tr "ds.refusal.too-many-recovery-requests.title")
    :guard-unavailable          (tr "ds.refusal.guard-unavailable.title")))

(defn- text
  [code]
  (case code
    :storage-limit-reached      (tr "ds.refusal.storage-limit-reached.text")
    :too-many-uploads           (tr "ds.refusal.too-many-uploads.text")
    :storage-unavailable        (tr "ds.refusal.storage-unavailable.text")
    :invite-limit-reached       (tr "ds.refusal.invite-limit-reached.text")
    :too-many-recovery-requests (tr "ds.refusal.too-many-recovery-requests.text")
    :guard-unavailable          (tr "ds.refusal.guard-unavailable.text")))

(mf/defc refusal-card*
  []
  (let [code*    (mf/use-state nil)
        code     (deref code*)
        ;; Mirrors `code` for the listener, which outlives a render.
        current  (mf/use-ref nil)

        on-close
        (mf/use-fn
         (fn []
           (mf/set-ref-val! current nil)
           (reset! code* nil)))]

    (mf/with-effect []
      ;; The first refusal stays until it is closed; later ones don't
      ;; replace it.
      (refusal/listen! (fn [code]
                         (when (nil? (mf/ref-val current))
                           (mf/set-ref-val! current code)
                           (reset! code* code))))
      (fn []
        (refusal/listen! nil)))

    (when (some? code)
      [:aside {:class (stl/css :global/ds-refused :card)
               :role "alert"
               :data-code (name code)}
       [:h2 (title code)]
       [:p (text code)]
       [:div {:class (stl/css :actions)}
        (when (= code :storage-limit-reached)
          [:a {:href plans-uri} (tr "ds.refusal.storage-limit-reached.link")])
        [:button {:type "button"
                  :class (stl/css :global/ds-refused-close :close)
                  :on-click on-close}
         (tr "ds.refusal.close")]]])))
