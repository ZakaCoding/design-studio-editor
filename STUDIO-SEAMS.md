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
<!-- ds-owned:end -->

## Seams

One row per upstream file we touch. `Path` is the exact repository path in
backticks. `Why` says what we mount or change there. `Upstream` links the
upstream issue or PR when the change is a fix we also propose; otherwise `-`.

<!-- seams:begin -->
| Path | Why | Upstream | Since |
| --- | --- | --- | --- |
| `frontend/src/app/plugins/persistence_guard.cljs` | WP3 (R1, R2): new file, `app.plugins.persistence-guard`. The whole guard: refuses MCP writes while saving is `:error`, `:retrying`, failing or stalled, and confirms the save of an MCP task. Lives beside `register.cljs` instead of `ui/studio/**` because it is plugin API logic that is proposed upstream | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/src/app/plugins/register.cljs` | WP3 (R1): `check-permission` calls `pguard/note-write!` first (one require, one call). This is the write boundary of the ClojureScript plugin API | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/src/app/plugins/api.cljs` | WP3 (R1): the plugin context gets a non-enumerable `$checkPersistence` so the plugins runtime can report the permissions it checks on its side (one require, one property) | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/src/app/main/data/workspace/mcp.cljs` | WP3 (R2), rule 5 exception approved in the F2 spec: the plugin loads as `plugin.js?ds-persistence=v1&v=<version-tag>` so a cached unguarded plugin.js is never reused; the MCP extension object gets `persistenceCheckpoint` and `settlePersistence`, wired to the existing `force-persist` event, `wait-persisted-or-error` and `flash-persistence`. No change to `main/data/persistence.cljs` | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `plugins/libs/plugins-runtime/src/lib/api/index.ts` | WP3 (R1): `checkPermission` reports each granted permission to the optional host hook `context.$checkPersistence`, and `runtime-ready` once. Needed because calls like `createRectangle` are only permission-checked in the runtime | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `plugins/libs/plugins-runtime/src/lib/api/persistence-hook.spec.ts` | WP3 (R1): new file, vitest spec for the host hook | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `mcp/packages/plugin/src/task-handlers/ExecuteCodeTaskHandler.ts` | WP3 (R2): `execute_code` takes a checkpoint before the script and awaits `settlePersistence` after it (also when the script threw) before reporting success. The plugin is built into the frontend image at `plugins/mcp/plugin.js` | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `mcp/packages/plugin/src/index.d.ts` | WP3 (R2): types of the two optional `mcp` extension methods | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/test/frontend_tests/plugins/persistence_guard_test.cljs` | WP3: new file, unit tests (port of `frontend-reliability/persistence-guard.test.mjs` plus one case per persistence state) | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/test/frontend_tests/data/workspace_mcp_test.cljs` | WP3 (R2): adds `test-persistence-ops`, the wiring of the MCP save confirmation to the persistence API | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/test/frontend_tests/runner.cljs` | WP3: registers `frontend-tests.plugins.persistence-guard-test` (two lines) | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `docs/upstream-pr-persistence-guard.md` | WP3: new file, the pull request description for penpot/penpot. Not part of the image | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/src/app/main/errors.cljs` | WP3: the save-failed toast no longer offers "Reload page" (reloading can drop edits that exist only in the tab; the new text says to keep it open). Removes `reload-page-link` | - | WP3 |
| `frontend/translations/en.po` | WP3: overrides `errors.save-failed` in place with the overlay text ("Keep this tab open ... MCP edits are paused") | - | WP3 |
| `frontend/translations/id.po` | WP3: adds `errors.save-failed` (Indonesian overlay text) in a `# Design Studio (WP3)` block at the end; the key did not exist in id.po | - | WP3 |
| `mcp/packages/plugin/src/task-handlers/ExecuteCodeTaskHandler.test.ts` | WP3: new file, node:test cases for the save confirmation around `execute_code` | - | WP3 |
| `mcp/packages/plugin/package.json` | WP3: `test` script runs `--experimental-transform-types` (the handler imports `Task`, which uses parameter properties) and the `task-handlers` tests | - | WP3 |
| `mcp/packages/plugin/tsconfig.json` | WP3: excludes `src/**/*.test.ts` from `tsc` like `src/*.test.ts` | - | WP3 |
<!-- seams:end -->
