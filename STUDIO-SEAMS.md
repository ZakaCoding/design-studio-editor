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
- `frontend/resources/fonts/studio/**`
<!-- ds-owned:end -->

## Seams

One row per upstream file we touch. `Path` is the exact repository path in
backticks. `Why` says what we mount or change there. `Upstream` links the
upstream issue or PR when the change is a fix we also propose; otherwise `-`.

<!-- seams:begin -->
| Path | Why | Upstream | Since |
| --- | --- | --- | --- |
| `frontend/translations/en.po` | WP2: brand, Plans, MCP and autosave overrides in place; new keys in a `# Design Studio (WP2)` block at the end. WP5: auth copy in a `# Design Studio (WP5)` block after it, and five Indonesian `ds.auth.*` values say "password" as F1 · 08/09 do | - | F2 |
| `frontend/translations/id.po` | WP2: same for Indonesian | - | F2 |
| `frontend/src/app/main/ui/auth.cljs` | WP5 (L2, L3, L7): auth pages render inside `studio.auth/shell*` (F1 · 08) instead of Penpot's logo and illustration; register routes leave for /start/signup with open sign-up | - | WP5 |
| `frontend/src/app/main/ui/auth/login.cljs` | WP5 (L2, L3): markup only. Form first, ZakaCoding ID (OIDC) button below, sign-up link to /start/signup; form state, events and redirects unchanged | - | WP5 |
| `frontend/src/app/main/ui/auth/recovery_request.cljs` | WP5 (L5): markup only. Own title, 15-minute note with open sign-up, DS "mail sent" card | - | WP5 |
| `frontend/src/app/main/ui/auth/recovery.cljs` | WP5 (L5, L6): markup only, plus a callback so a refused reset link shows "this link can't be used again"; schema and event unchanged | - | WP5 |
| `frontend/src/app/main/ui/auth/verify_token.cljs` | WP5 (L6): a used or expired sign-up link renders `studio.auth/used-link-page*` with open sign-up | - | WP5 |
| `frontend/src/app/main/ui/static.cljs` | WP5 (L4): signed-out login modal uses `studio.auth/modal*`; register link goes to /start/signup | - | WP5 |
<!-- seams:end -->
