;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.brand
  "Design Studio's own links and version, read by the upstream menus that
  P4-A rebranded (owner, 18 and 19 Nov 2026).

  - Penpot's community links (newsletter, community, Penpot hub, Penpot
    changelog, learning center, tutorials, GitHub, libraries and
    templates) are hidden: every such menu row is guarded by
    `penpot-community?`, which is false in our build.
  - Help: we have no general help page yet, so the help rows open the
    Penpot user guide, labelled as the engine guide, never as our help.
  - Terms, privacy and contact are ours. The credit \"Built on Penpot by
    Kaleidos\" and the MPL 2.0 link stay where they are."
  (:require
   [app.common.data.macros :as dm]
   [app.config :as cf]
   [cuerdas.core :as str]))

(def penpot-community?
  "False: the upstream rows that lead to Penpot's community, hub,
  changelog, newsletter, tutorials and templates are not rendered."
  false)

(def terms-uri "/start/legal/terms")
(def privacy-uri "/start/legal/privacy")
(def contact-email "hello@zakacoding.dev")
(def contact-uri (dm/str "mailto:" contact-email))
(def mpl-uri "https://mozilla.org/MPL/2.0/")
(def engine-guide-uri "https://help.penpot.app/user-guide/")

(def ^:private engine-tag
  "The upstream release this fork is built on (DS_BASE_TAG). Used when the
  bundle has no version (development builds report 0.0.0)."
  "2.18.7")

(defn terms-of-service-uri
  "The configured terms page, or ours."
  []
  (or (not-empty cf/terms-of-service-uri) terms-uri))

(defn engine-version
  "\"2.18.7\": the Penpot release under the editor."
  []
  (let [base (:base cf/version)]
    (if (or (str/blank? base) (= "0.0.0" base))
      engine-tag
      base)))

(defn version-label
  "Our version, \"2.18.7-ds\": the fork's release tags are
  `<engine>-ds.<n>`; the bundle only knows the engine part."
  []
  (dm/str (engine-version) "-ds"))

(defn build-label
  "\"888187b\" (the commit of this build), or nil in a development build."
  []
  (when-let [hash (not-empty (:commit-hash cf/version))]
    (subs hash 0 (min 7 (count hash)))))

(defn build-date
  "The build date the bundle carries, or nil."
  []
  (let [date cf/build-date]
    (when-not (or (str/blank? date) (= "unknown" date))
      date)))
