;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard.hero
  "The Craft hero of the dashboard home (F1 · 03): \"Mulai dari satu
  kalimat\". One sentence, or one of the chips, creates a file with
  Penpot's own create-file, opens it, and leaves the sentence for Craft's
  box (`app.main.ui.studio.dashboard.handoff`): Craft opens with the
  prompt filled in and does not send it."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.data.common :as dcm]
   [app.main.data.dashboard :as dd]
   [app.main.data.event :as-alias ev]
   [app.main.store :as st]
   [app.main.ui.studio.dashboard.handoff :as handoff]
   [app.main.ui.studio.dashboard.rules :as rules]
   [app.util.dom :as dom]
   [app.util.i18n :refer [tr]]
   [beicon.v2.core :as rx]
   [rumext.v2 :as mf]))

(def ^:private chips
  "The chips of F1 · 03, in order."
  [["landing"  "ds.hero.chip.landing"]
   ["poster"   "ds.hero.chip.poster"]
   ["app"      "ds.hero.chip.app"]
   ["card"     "ds.hero.chip.card"]
   ["brand"    "ds.hero.chip.brand"]])

(defn start-with-craft!
  "Creates a file in `project-id`, leaves `prompt` (may be nil) for Craft
  and opens the file with the Craft dock open. `on-settled` is called
  once the file exists or could not be made."
  [{:keys [team-id project-id prompt origin on-settled]}]
  (let [settled    (fn []
                     (when (fn? on-settled)
                       (on-settled)))
        on-success (fn [{:keys [id data]}]
                     (handoff/leave! prompt id)
                     (settled)
                     (st/emit! (dcm/go-to-workspace :team-id team-id
                                                    :file-id id
                                                    :page-id (get-in data [:pages 0]))))
        on-error   (fn [cause]
                     (settled)
                     (rx/throw cause))
        name       (rules/file-name prompt)
        params     (cond-> {:project-id project-id}
                     (some? name) (assoc :name name))]
    (st/emit! (-> (dd/create-file (with-meta params {:on-success on-success
                                                     :on-error on-error}))
                  (with-meta {::ev/origin (or origin "studio:craft-hero")})))))

(mf/defc craft-hero*
  "Props: `team-id`, `project-id` (the project the new file goes in: the
  team's Drafts)."
  [{:keys [team-id project-id]}]
  (let [text*     (mf/use-state "")
        busy*     (mf/use-state false)
        input-ref (mf/use-ref nil)
        text      (deref text*)
        busy?     (deref busy*)

        start
        (mf/use-fn
         (mf/deps team-id project-id busy?)
         (fn [prompt]
           (when (and (not busy?) (some? project-id))
             (reset! busy* true)
             (start-with-craft! {:team-id team-id
                                 :project-id project-id
                                 :prompt prompt
                                 :on-settled #(reset! busy* false)}))))

        on-submit
        (mf/use-fn
         (mf/deps start text)
         (fn [event]
           (dom/prevent-default event)
           (if-let [prompt (rules/hero-prompt text nil)]
             (start prompt)
             ;; Nothing to start from yet: the sentence comes first.
             (some-> (mf/ref-val input-ref) (.focus)))))

        on-change
        (mf/use-fn
         (fn [event]
           (reset! text* (.-value (.-target event)))))

        on-chip
        (mf/use-fn
         (mf/deps start text)
         (fn [event]
           (let [label (.getAttribute (.-currentTarget event) "data-label")]
             (start (rules/hero-prompt text label)))))]

    [:section {:class (stl/css :hero)
               :aria-labelledby "ds-hero-title"
               :data-testid "ds-hero"}
     [:span {:class (stl/css :tag)} (tr "ds.hero.tag")]
     [:h2 {:id "ds-hero-title" :class (stl/css :title)} (tr "ds.hero.title")]

     [:form {:class (stl/css :prompt)
             :on-submit on-submit}
      [:input {:class (stl/css :input)
               :ref input-ref
               :type "text"
               :name "ds-craft-prompt"
               :value text
               :max-length rules/prompt-max
               :auto-complete "off"
               :placeholder (tr "ds.hero.placeholder")
               :aria-label (tr "ds.hero.label")
               :on-change on-change
               :data-testid "ds-hero-input"}]
      [:button {:class (stl/css :submit)
                :type "submit"
                :aria-busy (when busy? true)
                :data-testid "ds-hero-submit"}
       (tr "ds.hero.submit")]]

     [:ul {:class (stl/css :chips)
           :aria-label (tr "ds.hero.chips")}
      (for [[id label-key] chips]
        (let [label (tr label-key)]
          [:li {:key id}
           [:button {:class (stl/css :chip)
                     :type "button"
                     :data-chip id
                     :data-label label
                     :data-testid "ds-hero-chip"
                     :on-click on-chip}
            label]]))]]))
