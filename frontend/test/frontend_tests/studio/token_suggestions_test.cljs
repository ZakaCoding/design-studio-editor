;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns frontend-tests.studio.token-suggestions-test
  (:require
   [app.main.ui.studio.workspace.token-suggestions :as ts]
   [cljs.test :as t :include-macros true]))

(def ^:private tokens
  [{:group nil :sets ["core"]
    :tokens [{:name "red.500" :resolved-value "#ff0000"}
             {:name "red.400" :resolved-value "#f01010"}
             {:name "blue.500" :resolved-value "#0000ff"}
             {:name "size.4" :resolved-value 4}]}
   {:group "brand" :sets ["light" "dark"]
    :tokens [{:name "accent" :resolved-value "#fa0505"}
             {:name "red.500" :resolved-value "#ff0000"}]}])

(defn- to-hex [v] (when (string? v) v))

(defn- names [result] (mapv (comp :name :token) result))

(t/deftest rgb-distance
  (t/is (= 0.0 (ts/rgb-distance "#112233" "#112233")))
  (t/is (= 5.0 (ts/rgb-distance "#000000" "#000305")))
  (t/is (nil? (ts/rgb-distance "red" "#000000")))
  (t/is (nil? (ts/rgb-distance "#000000" nil))))

(t/deftest flatten-removes-duplicate-names
  (t/is (= ["red.500" "red.400" "blue.500" "size.4" "accent"]
           (mapv :name (ts/flatten-tokens tokens)))))

(t/deftest nearest-first-and-far-tokens-dropped
  (let [result (ts/suggest {:combined-tokens tokens :hex "#ff0000" :to-hex to-hex})]
    (t/is (= ["red.500" "accent" "red.400"] (names result)))
    (t/is (true? (:exact? (first result))))
    (t/is (false? (:exact? (second result))))))

(t/deftest applied-token-is-left-out
  (t/is (= ["accent" "red.400"]
           (names (ts/suggest {:combined-tokens tokens :hex "#ff0000"
                               :applied-token "red.500" :to-hex to-hex})))))

(t/deftest limit-and-non-colors
  (t/is (= ["red.500"]
           (names (ts/suggest {:combined-tokens tokens :hex "#ff0000"
                               :to-hex to-hex :limit 1}))))
  (t/is (= [] (ts/suggest {:combined-tokens tokens :hex "#00ff00" :to-hex to-hex})))
  (t/is (= [] (ts/suggest {:combined-tokens tokens :hex "not-a-color" :to-hex to-hex}))))
