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
- `STUDIO-DS.md`
- `.github/workflows/ds-*.yml`
- `.github/ds/**`
- `frontend/src/app/main/ui/studio/**`
- `frontend/test/frontend_tests/studio/**`
- `frontend/resources/fonts/studio/**`
- `frontend/resources/styles/studio/**`
<!-- ds-owned:end -->

## Seams

One row per upstream file we touch. `Path` is the exact repository path in
backticks. `Why` says what we mount or change there. `Upstream` links the
upstream issue or PR when the change is a fix we also propose; otherwise `-`.

<!-- seams:begin -->
| Path | Why | Upstream | Since |
| --- | --- | --- | --- |
| `frontend/resources/templates/index.mustache` | WP1 (L1): tab title, description, og/twitter tags, drop @penpotapp, SVG favicon link, Studio shell loader script tag before </body> | - | WP1 |
| `frontend/resources/images/icons/penpot-logo.svg` | WP1 (S1): official Design Studio tile + dual-tone wordmark; symbol name unchanged | - | WP1 |
| `frontend/resources/images/icons/penpot-logo-icon.svg` | WP1 (S1): official Design Studio tile; symbol name unchanged | - | WP1 |
| `frontend/resources/images/icons/penpot-logo-icon-loader.svg` | WP1 (S1): official Design Studio tile; symbol name unchanged | - | WP1 |
| `frontend/resources/images/icons/logo-error-screen.svg` | WP1 (S1): official Design Studio tile; symbol name unchanged | - | WP1 |
| `frontend/resources/images/favicon.svg` | WP1 (L1): new file, official Design Studio SVG favicon (not in base tag) | - | WP1 |
| `frontend/src/app/config.cljs` | WP1 (S4): mcp-help-center-uri points to /start/guides/ai-agent-mcp | - | WP1 |
| `frontend/src/app/util/dom.cljs` | WP1 (L9): set-html-title replaces the Penpot brand with Design Studio in every locale | - | WP1 |
| `frontend/src/app/main/ui/dashboard/subscription.cljs` | WP1 (S3): nitrate-sidebar*, nitrate-current-plan*, cta-power-up*, dashboard-cta* render nil; originals kept private | - | WP1 |
| `frontend/src/app/main/ui/dashboard/sidebar.cljs` | WP1 (S2, S3): default org shown as Design Studio, create-organization button and menu item removed | - | WP1 |
| `frontend/src/app/main/ui/dashboard/sidebar.scss` | WP1 (S2): org badge shows the tile without the round frame | - | WP1 |
| `frontend/translations/en.po` | WP2: brand, Plans, MCP and autosave overrides in place; new keys in a `# Design Studio (WP2)` block at the end; WP4: `ds.*` strings in the "Design Studio (WP4)" block; `ds.*` strings, in a `# Design Studio (<WP>)` block at the end; WP2: brand, Plans, MCP and autosave overrides in place; new keys in a `# Design Studio (WP2)` block at the end. WP5: auth copy in a `# Design Studio (WP5)` block after it, and five Indonesian `ds.auth.*` values say "password" as F1 · 08/09 do | - | F2, WP4, WP6 |
| `frontend/translations/id.po` | WP2: same for Indonesian; WP4: `ds.*` strings in the "Design Studio (WP4)" block; `ds.*` strings, in a `# Design Studio (<WP>)` block at the end | - | F2, WP4, WP6 |
| `frontend/src/app/main/ui/settings/password.cljs` | WP4: backup-password title and note (S5) | - | WP4 |
| `frontend/src/app/main/ui/settings/profile.cljs` | WP4: open sign-up shows an email note instead of change-email (S6) | - | WP4 |
| `frontend/src/app/main/ui/settings/sidebar.cljs` | WP4: "Backup password" and "Plans" sidebar labels (S5, S7) | - | WP4 |
| `frontend/src/app/main/ui/settings/subscription.cljs` | WP4: Settings > Plans renders the Design Studio plan summary (S7) | - | WP4 |
| `frontend/src/app/main/ui/workspace/main_menu.cljs` | WP4: power-up entry opens Design Studio Plans (S8) | - | WP4 |
| `frontend/test/frontend_tests/runner.cljs` | WP4: registers `frontend-tests.studio.shell-state-test`; WP3: registers `frontend-tests.plugins.persistence-guard-test` (two lines); Registers the `frontend-tests.studio.*` test namespaces | - | WP4, WP3, WP6 |
| `frontend/src/app/main/ui/workspace/viewport.cljs` | WP7: requires `ui/studio/touch.js` and starts the touch layer once from `viewport*` | - | F2 |
| `frontend/src/app/plugins/persistence_guard.cljs` | WP3 (R1, R2): new file, `app.plugins.persistence-guard`. The whole guard: refuses MCP writes while saving is `:error`, `:retrying`, failing or stalled, and confirms the save of an MCP task. Lives beside `register.cljs` instead of `ui/studio/**` because it is plugin API logic that is proposed upstream | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/src/app/plugins/register.cljs` | WP3 (R1): `check-permission` calls `pguard/note-write!` first (one require, one call). This is the write boundary of the ClojureScript plugin API | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/src/app/plugins/api.cljs` | WP3 (R1): the plugin context gets a non-enumerable `$checkPersistence` so the plugins runtime can report the permissions it checks on its side (one require, one property) | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/src/app/main/data/workspace/mcp.cljs` | WP3 (R2), rule 5 exception approved in the F2 spec: the MCP extension object gets `persistenceCheckpoint` and `settlePersistence`, wired to the existing `force-persist` event, `wait-persisted-or-error` and `flash-persistence`. No change to `main/data/persistence.cljs` | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `plugins/libs/plugins-runtime/src/lib/api/index.ts` | WP3 (R1): `checkPermission` reports each granted permission to the optional host hook `context.$checkPersistence`, and `runtime-ready` once. Needed because calls like `createRectangle` are only permission-checked in the runtime | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `plugins/libs/plugins-runtime/src/lib/api/persistence-hook.spec.ts` | WP3 (R1): new file, vitest spec for the host hook | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `mcp/packages/plugin/src/task-handlers/ExecuteCodeTaskHandler.ts` | WP3 (R2): `execute_code` takes a checkpoint before the script and awaits `settlePersistence` after it (also when the script threw) before reporting success. The plugin is built into the frontend image at `plugins/mcp/plugin.js` | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `mcp/packages/plugin/src/index.d.ts` | WP3 (R2): types of the two optional `mcp` extension methods | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/test/frontend_tests/plugins/persistence_guard_test.cljs` | WP3: new file, unit tests (port of `frontend-reliability/persistence-guard.test.mjs` plus one case per persistence state) | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/test/frontend_tests/data/workspace_mcp_test.cljs` | WP3 (R2): adds `test-persistence-ops`, the wiring of the MCP save confirmation to the persistence API | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `docs/upstream-pr-persistence-guard.md` | WP3: new file, the pull request description for penpot/penpot. Not part of the image | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/src/app/main/ui.cljs` | Mounts `studio.widgets/global-widgets*` once in `app` (refusal card, first steps after sign-up, MCP tip and guide) | - | WP6 |
| `frontend/src/app/main/errors.cljs` | `handle-error :restriction` announces Design Studio guard refusal codes instead of reporting them as unhandled | - | WP6 |
| `frontend/src/app/main/repo.cljs` | `handle-response` reads the guard's JSON refusal (413/429/503/507, written by the proxy, not the backend) and raises it as `:restriction` with our code; without it these answers become generic `:validation`/`:rate-limit`/`:service-unavailable` errors and never reach the `:restriction` handler | - | WP6 |
| `frontend/src/app/main/ui/settings/integrations.cljs` | `data-testid` on the MCP switch, the copy button and the key windows, so the MCP guide points at them without CSS-module class names | - | WP6 |
| `frontend/src/app/main/ui/auth.cljs` | WP5 (L2, L3, L7): auth pages render inside `studio.auth/shell*` (F1 · 08) instead of Penpot's logo and illustration; register routes leave for /start/signup with open sign-up | - | WP5 |
| `frontend/src/app/main/ui/auth/login.cljs` | WP5 (L2, L3): markup only. Form first, ZakaCoding ID (OIDC) button below, sign-up link to /start/signup; form state, events and redirects unchanged | - | WP5 |
| `frontend/src/app/main/ui/auth/recovery_request.cljs` | WP5 (L5): markup only. Own title, 15-minute note with open sign-up, DS "mail sent" card | - | WP5 |
| `frontend/src/app/main/ui/auth/recovery.cljs` | WP5 (L5, L6): markup only, plus a callback so a refused reset link shows "this link can't be used again"; schema and event unchanged | - | WP5 |
| `frontend/src/app/main/ui/auth/verify_token.cljs` | WP5 (L6): a used or expired sign-up link renders `studio.auth/used-link-page*` with open sign-up | - | WP5 |
| `frontend/src/app/main/ui/static.cljs` | WP5 (L4): signed-out login modal uses `studio.auth/modal*`; register link goes to /start/signup | - | WP5 |
| `frontend/src/app/main/ui/debug/playground.cljs` | Mounts the DS showcase (`app.main.ui.studio.ds.showcase/showcase*`) on the existing debug route `#/debug/playground`, so the release build compiles every DS base component. One require and one element. | - | F3/F4 foundation |
<!-- seams:end -->
