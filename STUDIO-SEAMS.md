# Studio seams

This file is the contract that keeps `ds/main` cheap to rebase onto new Penpot
releases. CI (`.github/workflows/ds-seams-guard.yml`) reads the two marked
blocks below and compares the checked-out tree with the upstream tag named in
`DS_BASE_TAG`. Edit the blocks by hand, in the same pull request as the code.

## Rules

1. New Design Studio code lives in our own namespace:
   `frontend/src/app/main/ui/studio/**` (ClojureScript `app.main.ui.studio.*`
   and its co-located `.scss` modules).
2. An upstream file (a path that exists in the base tag) may only differ from
   the base tag when its exact path is listed in **Seams**. No wildcards there:
   every touched upstream file is named on its own row.
3. A new file (a path that does not exist in the base tag) must match a pattern
   in **DS-owned paths** or be listed in **Seams**.
4. DS-owned patterns may never match a file that exists in the base tag.
5. Never touch `LICENSE` or the MPL header of any upstream file. Do not modify
   `main/data/**`, `common/**` or `render-wasm/**` except for a bug fix that is
   also proposed upstream; link that upstream PR in the row.
6. Upstream workflows under `.github/workflows/` stay unmodified; they are
   disabled in the repository settings instead (see `README-DS.md`).
7. A row whose file no longer differs from the base tag is reported as stale;
   remove it in the next change.

## DS-owned paths

Glob patterns, one per bullet, inside backticks. `*` matches within one path
segment, `**` matches any depth.

<!-- ds-owned:begin -->
- `DS_BASE_TAG`
- `README-DS.md`
- `STUDIO-SEAMS.md`
- `.github/workflows/ds-*.yml`
- `.github/ds/**`
- `frontend/src/app/main/ui/studio/**`
- `frontend/test/frontend_tests/studio/**`
<!-- ds-owned:end -->

## Seams

One row per upstream file we touch. `Path` is the exact repository path in
backticks. `Why` says what we mount or change there. `Upstream` links the
upstream issue or PR when the change is a fix we also propose; otherwise `-`.

<!-- seams:begin -->
| Path | Why | Upstream | Since |
| --- | --- | --- | --- |
| `frontend/src/app/main/ui/settings/password.cljs` | WP4: backup-password title and note (S5) | - | WP4 |
| `frontend/src/app/main/ui/settings/profile.cljs` | WP4: open sign-up shows an email note instead of change-email (S6) | - | WP4 |
| `frontend/src/app/main/ui/settings/sidebar.cljs` | WP4: "Backup password" sidebar label (S5) | - | WP4 |
| `frontend/src/app/main/ui/settings/subscription.cljs` | WP4: Settings > Plans renders the Design Studio plan summary (S7) | - | WP4 |
| `frontend/src/app/main/ui/workspace/main_menu.cljs` | WP4: power-up entry opens Design Studio Plans (S8) | - | WP4 |
| `frontend/translations/en.po` | WP4: `ds.*` strings in the "Design Studio (WP4)" block | - | WP4 |
| `frontend/translations/id.po` | WP4: `ds.*` strings in the "Design Studio (WP4)" block | - | WP4 |
| `frontend/test/frontend_tests/runner.cljs` | WP4: registers `frontend-tests.studio.shell-state-test` | - | WP4 |
<!-- seams:end -->
