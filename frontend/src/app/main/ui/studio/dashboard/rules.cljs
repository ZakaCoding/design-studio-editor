;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.dashboard.rules
  "The decisions of the Design Studio dashboard, as pure functions: which
  MCP key the \"Connect AI\" flow may reuse, what the card shows, how a
  refusal reads, and when the home is the empty first-design page.
  Tested in `frontend-tests.studio.dashboard-rules-test`."
  (:require
   [app.common.data :as d]
   [app.common.time :as ct]
   [cuerdas.core :as str]))

;; --- Connect AI

(defn usable-key
  "The key of the first MCP access token of `tokens` that has not expired
  at `now`, or nil."
  [tokens now]
  (some (fn [{:keys [type token expires-at]}]
          (when (and (= "mcp" type)
                     (string? token)
                     (not (str/blank? token))
                     (or (nil? expires-at)
                         (ct/is-after? expires-at now)))
            token))
        tokens))

(defn assistant-uri
  "The address a member adds to their AI assistant. It carries no secret:
  the assistant signs in through Studio."
  [origin]
  (str (str/rtrim (str origin) "/") "/studio/mcp"))

(defn card-mode
  "What the dashboard shows.

  - `:hidden`    Studio can't say yet, MCP is off here, or no AI access
  - `:card`      the two-step card, step 1 open (0 of 2)
  - `:pairing`   the two-step card, step 1 done, step 2 open (1 of 2)
  - `:connected` the slim status bar (F1 · 03c)

  `status` is Studio's answer: `:ai-access`, `:connected` (the editor link
  is stored: step 1), `:assistant` (the AI app that connected last, or
  nil) and `:assistant-known` (false for a Studio that does not say which
  app is connected: then a stored link counts as connected, as before).
  `activated` is true once step 1 finished in this visit, `adding` while
  the member connects one more app from the status bar."
  [{:keys [enabled status activated adding]}]
  (cond
    (not enabled)                        :hidden
    (not (:ai-access status))            :hidden
    (not (:connected status))            :card
    adding                               :pairing
    (some? (:assistant status))          :connected
    (:assistant-known status)            :pairing
    activated                            :pairing
    :else                                :connected))

(defn steps-done
  "How many of the two steps are done in `mode` (\"1 dari 2 selesai\")."
  [mode]
  (cond
    (= mode :pairing)   1
    (= mode :connected) 2
    :else               0))

(defn display-uri
  "The assistant address as the card shows it: without the scheme. The
  copy button still copies the whole address."
  [uri]
  (str/replace (str uri) #"^[a-z]+://" ""))

(def assistants
  "The AI apps step 2 offers, in the order of F1 · 03."
  [:claude :codex :other])

(defn assistant-choice
  "The app to preselect in step 2 for the name of the app that is already
  connected (or nil)."
  [assistant-name]
  (let [n (str/lower (str assistant-name))]
    (cond
      (str/includes? n "codex")  :codex
      (str/includes? n "claude") :claude
      (str/blank? n)             :claude
      :else                      :other)))

(defn failure
  "Why step 1 did not finish, from Studio's answer."
  [status reason]
  (cond
    (= 401 status)                        :session
    (= "account-changed" reason)          :session
    (and (= 403 status) (some? reason))   :no-access
    (= 409 status)                        :busy
    (= 429 status)                        :busy
    :else                                 :failed))

;; --- Home

(defn drafts
  "The Drafts project of the team on screen."
  [projects]
  (d/seek :is-default (remove :deleted-at projects)))

(defn empty-home?
  "True when the team has nothing yet: no project besides Drafts and no
  file in it, and the member may create one. A viewer keeps Penpot's own
  page, which has nothing to offer them to click."
  [projects can-edit]
  (let [projects (remove :deleted-at projects)
        drafts   (d/seek :is-default projects)]
    (boolean
     (and can-edit
          (some? drafts)
          (every? :is-default projects)
          (zero? (or (:count drafts) 0))))))

(defn first-name
  "What the greeting calls the member, or nil."
  [profile]
  (let [fullname (some-> (:fullname profile) str/trim)]
    (when-not (str/blank? fullname)
      (first (str/split fullname #"\s+")))))

;; --- Craft hero (F1 · 03 "Mulai dari satu kalimat")

(def prompt-max
  "Longest prompt handed to Craft, as its own box allows."
  2000)

(defn clean-prompt
  "The prompt as it is handed over: one line breaks kept, trimmed, cut to
  `prompt-max`; nil when empty."
  [text]
  (let [text (str/trim (str text))]
    (when-not (str/blank? text)
      (subs text 0 (min (count text) prompt-max)))))

(defn hero-prompt
  "The prompt Craft's box is filled with. `text` is what the member typed,
  `chip` the label of the chip they pressed (or nil): a chip alone is the
  start of a sentence, a chip with text names the kind of design first."
  [text chip]
  (let [text (clean-prompt text)
        chip (clean-prompt chip)]
    (cond
      (and (some? chip) (some? text)) (clean-prompt (str chip ": " text))
      (some? chip)                    chip
      :else                           text)))

(defn file-name
  "A name for the file a prompt starts: its first words, at most 40
  characters, cut at a word. nil when there is nothing to name it by, so
  Penpot gives its own \"New File\" name."
  [prompt]
  (when-let [text (clean-prompt prompt)]
    (let [line  (-> (first (str/split text #"\n"))
                    (str/replace #"\s+" " ")
                    (str/trim))
          short (if (<= (count line) 40)
                  line
                  (let [cut (subs line 0 41)
                        at  (.lastIndexOf cut " ")]
                    (if (>= at 12)
                      (subs cut 0 at)
                      (subs line 0 40))))
          short (-> short
                    (str/replace #"[\s.,;:!?…\"“”'‘’()-]+$" "")
                    (str/trim))]
      (when-not (str/blank? short)
        (str (str/upper (subs short 0 1)) (subs short 1))))))

(defn handoff
  "What the dashboard leaves for the workspace (the \"Craft prompt
  hand-off\" contract): the prompt, the new file and when. nil without a
  prompt: then only the request to open Craft is left."
  [prompt file-id now]
  (when-let [text (clean-prompt prompt)]
    {:prompt text
     :fileId (str file-id)
     :at     now}))

;; --- Start cards (F1 · 12)

(defn price-label
  "The Monthly price as the Craft card says it (\"US$5.49\", \"Rp99.000\"),
  from the `plans` of the shell state; nil while there is no price."
  [plans]
  (let [monthly (:monthly plans)
        usd     (:priceUsdCents monthly)
        idr     (:priceIdr monthly)]
    (cond
      (and (= "USD" (:currency plans)) (number? usd) (pos? usd))
      (str "US$" (.toFixed (/ usd 100) 2))

      (and (= "IDR" (:currency plans)) (number? idr) (pos? idr))
      (str "Rp" (str/replace (str (js/Math.round idr)) #"\B(?=(\d{3})+(?!\d))" "."))

      :else
      nil)))

;; --- Templates row (F1 · 03c "Template Design Studio")

(def template-tones
  "The six tile colours of the row, in the board's order."
  ["pink" "sky" "mint" "yellow" "lilac" "peach"])

(def ^:private hidden-templates
  "Upstream's onboarding files; its own templates list hides them too."
  #{"welcome" "tutorial-for-beginners"})

(defn visible-templates
  "The built-in templates a member can add, without the onboarding ones."
  [templates]
  (into [] (remove #(contains? hidden-templates (:id %))) templates))

(defn template-name
  "The name under a template card. \"Penpot Design System | Pencil\" was
  cut at the bar on the home; it reads \"Pencil Design System\"."
  [name]
  (let [name (str/trim (str name))]
    (if-let [[_ head tail] (re-matches #"(.+?)\s*\|\s*(.+)" name)]
      (str (str/trim tail) " " (str/trim (str/replace head #"^[Pp]enpot\s+" "")))
      name)))

(defn template-tiles
  "At most six templates with the colour of their tile."
  [templates]
  (->> (take 6 (visible-templates templates))
       (map-indexed (fn [index template]
                      {:template template
                       :tone (nth template-tones (mod index (count template-tones)))}))
       (vec)))
