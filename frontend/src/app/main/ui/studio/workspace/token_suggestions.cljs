;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.workspace.token-suggestions
  "Token suggestions for the colour picker (F1 · 13). Pure functions: given
  the colour tokens of the file and the colour being edited, return the
  tokens that are closest to it."
  (:require
   [app.common.colors :as cc]))

(def max-suggestions 4)

;; Largest RGB distance (0..441) at which a token is still a suggestion.
(def max-distance 96)

(defn rgb-distance
  "Euclidean distance between two #rrggbb colours, or nil when one of them is
  not a valid hex colour."
  [hex-a hex-b]
  (when (and (cc/valid-hex-color? hex-a) (cc/valid-hex-color? hex-b))
    (let [[r1 g1 b1] (cc/hex->rgb hex-a)
          [r2 g2 b2] (cc/hex->rgb hex-b)
          sq         (fn [a b] (let [d (- a b)] (* d d)))]
      (Math/sqrt (+ (sq r1 r2) (sq g1 g2) (sq b1 b2))))))

(defn flatten-tokens
  "Flattens the picker's `combined-tokens` (sets or groups, each with a
  `:tokens` list) into one list of tokens, without duplicates by name."
  [combined-tokens]
  (->> combined-tokens
       (mapcat :tokens)
       (reduce (fn [[seen acc] token]
                 (if (contains? seen (:name token))
                   [seen acc]
                   [(conj seen (:name token)) (conj acc token)]))
               [#{} []])
       second))

(defn suggest
  "Returns up to `max-suggestions` colour tokens whose resolved value is close
  to `hex`, nearest first, as `{:token t :hex h :distance d :exact? bool}`.
  `to-hex` turns a resolved token value into a hex string (nil when it is not
  a colour). The token named `applied-token` is left out."
  [{:keys [combined-tokens hex applied-token to-hex limit]
    :or {limit max-suggestions}}]
  (->> (flatten-tokens combined-tokens)
       (remove #(= (:name %) applied-token))
       (keep (fn [token]
               (let [token-hex (some-> (:resolved-value token) to-hex)
                     distance  (rgb-distance hex token-hex)]
                 (when (and distance (<= distance max-distance))
                   {:token    token
                    :hex      token-hex
                    :distance distance
                    :exact?   (zero? distance)}))))
       (sort-by (juxt :distance (comp :name :token)))
       (take limit)
       vec))
