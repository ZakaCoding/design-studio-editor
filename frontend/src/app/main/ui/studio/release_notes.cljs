;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) ZakaCoding

(ns app.main.ui.studio.release-notes
  "Design Studio's own release notes (owner, 19 Nov 2026): what changed for
  members, newest first, in place of Penpot's \"What's new\" slides.

  `notes` is the only source: written from the release records in
  zakacoding-cloud `docs/*release*.md`, member-visible changes only (no
  SEO, marketing or operations). No network. Upstream's `:release-notes`
  modal (main menu, profile menu, Settings, and the popup after an
  upgrade) renders `release-notes-dialog*`."
  (:require-macros [app.main.style :as stl])
  (:require
   [app.main.data.modal :as modal]
   [app.main.data.profile :as du]
   [app.main.store :as st]
   [app.main.ui.studio.ds :as sds]
   [app.util.i18n :as i18n :refer [tr]]
   [rumext.v2 :as mf]))

(def notes
  "Newest first. `:date` is the day it went live (WIB); the entry without
  a date is what comes next. Each `:items` holds 2–4 short lines."
  [{:id "2026-11-20"
    :date "2026-11-20"
    :title {:en "The new editor"
            :id "Editor baru"}
    :items {:en ["The editor is rebuilt around Design Studio: calmer panels, the tools at the bottom and Craft next to your canvas."
                 "Your design files stay as they are and keep working."]
            :id ["Editor dibangun ulang untuk Design Studio: panel yang lebih tenang, alat di bawah, dan Craft di samping kanvas."
                 "File desain Anda tetap seperti sekarang dan tetap bisa dipakai."]}}

   {:id "2026-11-11"
    :date "2026-11-11"
    :title {:en "Engine update"
            :id "Pembaruan mesin"}
    :items {:en ["The editor now runs on Penpot 2.18.7."
                 "Image and file uploads are checked more strictly."
                 "Importing design files (.penpot) checks their libraries more carefully."]
            :id ["Editor sekarang berjalan di Penpot 2.18.7."
                 "Unggahan gambar dan file diperiksa lebih ketat."
                 "Impor file desain (.penpot) memeriksa pustakanya dengan lebih teliti."]}}

   {:id "2026-11-08"
    :date "2026-11-08"
    :title {:en "New tools for Craft"
            :id "Alat baru untuk Craft"}
    :items {:en ["Design review with a Fix button on each finding, and an accessibility report (WCAG 2.2 AA)."
                 "Variants side by side, edits on the layers you select, and a save point before every Craft change."
                 "Handoff to HTML/CSS or React, tokens as CSS variables or JSON, and a brand kit per project."
                 "A wider chat you can resize. Plain words always work; / commands and @mentions are shortcuts."]
            :id ["Review desain dengan tombol Perbaiki di setiap temuan, dan laporan aksesibilitas (WCAG 2.2 AA)."
                 "Varian berdampingan, edit pada layer yang Anda pilih, dan titik simpan sebelum setiap perubahan Craft."
                 "Handoff ke HTML/CSS atau React, token sebagai CSS variables atau JSON, dan brand kit per proyek."
                 "Chat yang lebih lebar dan bisa diubah ukurannya. Kata-kata biasa selalu cukup; perintah / dan @mention hanya jalan pintas."]}}

   {:id "2026-11-07"
    :date "2026-11-07"
    :title {:en "Design Studio is open to everyone"
            :id "Design Studio terbuka untuk semua"}
    :items {:en ["Sign up with your email and a password. No invitation needed."
                 "The Free plan has the editor and MCP for your own AI assistant."
                 "Craft, your design partner, comes with the paid plans."
                 "Password reset emails now come from Design Studio."]
            :id ["Daftar dengan email dan kata sandi. Tanpa undangan."
                 "Paket Free berisi editor dan MCP untuk asisten AI Anda sendiri."
                 "Craft, partner desain Anda, ada di paket berbayar."
                 "Email reset kata sandi sekarang dikirim atas nama Design Studio."]}}

   {:id "2026-10-20"
    :date "2026-10-20"
    :title {:en "Touch editing and safer saving"
            :id "Edit dengan sentuhan dan penyimpanan lebih aman"}
    :items {:en ["On iPad, Hand starts on: two fingers pan and zoom. Turn Hand off to select, move and draw."
                 "Tap to place text, then use Show keyboard to type."
                 "When saving fails, the editor tells you, keeps your changes in the tab and pauses AI edits until saving works again."]
            :id ["Di iPad, Hand aktif sejak awal: dua jari untuk menggeser dan zoom. Matikan Hand untuk memilih, memindahkan, dan menggambar."
                 "Ketuk untuk menaruh teks, lalu pakai Tampilkan keyboard untuk mengetik."
                 "Saat penyimpanan gagal, editor memberi tahu, menyimpan perubahan di tab ini, dan menjeda edit AI sampai penyimpanan berhasil lagi."]}}])

(defn- lang [] (if (= "id" (deref i18n/locale)) :id :en))

(defn- locale-tag [] (if (= "id" (deref i18n/locale)) "id-ID" "en-US"))

(defn format-date
  "\"11 Nov 2026\". Through Intl.DateTimeFormat: inside the editor
  Date.prototype.toLocaleDateString is replaced upstream."
  [value]
  (let [date (js/Date. (str value "T12:00:00+07:00"))]
    (when-not (js/isNaN (.getTime date))
      (.format (js/Intl.DateTimeFormat. (locale-tag)
                                        #js {:day "numeric" :month "short" :year "numeric"
                                             :timeZone "Asia/Jakarta"})
               date))))

(mf/defc note*
  {::mf/private true}
  [{:keys [note]}]
  (let [lng   (lang)
        next? (nil? (:date note))]
    [:li {:class (stl/css-case :note true :note-next next?)
          :data-testid (str "ds-release-" (:id note))}
     [:div {:class (stl/css :note-head)}
      (if next?
        [:span {:class (stl/css :note-tag)} (tr "ds.release-notes.next")]
        [:time {:class (stl/css :note-date) :date-time (:date note)}
         (format-date (:date note))])
      [:h3 {:class (stl/css :note-title)} (get-in note [:title lng])]]
     [:ul {:class (stl/css :note-items)}
      (for [[index item] (map-indexed vector (get-in note [:items lng]))]
        [:li {:key index :class (stl/css :note-item)} item])]]))

(mf/defc release-notes-dialog*
  "Our release notes in the DS dialog. `version` is upstream's (the
  `:main` version): closing marks it as seen, as upstream's slides did, so
  the popup after an upgrade does not come back."
  [{:keys [version]}]
  (let [on-close (mf/use-fn #(st/emit! (modal/hide)))]

    (mf/with-effect [version]
      #(when (some? version)
         (st/emit! (du/mark-onboarding-as-viewed {:version version}))))

    [:> sds/dialog* {:title (tr "ds.release-notes.title")
                     :description (tr "ds.release-notes.description")
                     :size "lg"
                     :class (stl/css :dialog)
                     :on-close on-close
                     :footer (mf/html
                              [:button {:type "button"
                                        :class (stl/css :pill-black)
                                        :on-click on-close}
                               (tr "ds.release-notes.done")])}
     [:ol {:class (stl/css :notes) :data-testid "ds-release-notes"}
      (for [note notes]
        [:> note* {:key (:id note) :note note}])]]))
