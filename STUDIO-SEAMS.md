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
| `frontend/src/app/main/ui/dashboard/sidebar.cljs` | WP1 (S2, S3): default org shown as Design Studio, create-organization button and menu item removed; G1 (F1 · 03): `sidebar-project*` is public (one metadata line removed), the Design Studio sidebar lists the projects with it. `sidebar*` and `sidebar-content*` stay in place and unused; P2: `teams-selector-dropdown*` and `team-options-dropdown*` are public (two `::mf/private` lines removed), the Design Studio team switcher shows both in one menu | - | WP1, G1, P2 |
| `frontend/src/app/main/ui/dashboard/sidebar.scss` | WP1 (S2): org badge shows the tile without the round frame | - | WP1 |
| `frontend/translations/en.po` | WP2: brand, Plans, MCP and autosave overrides in place; new keys in a `# Design Studio (WP2)` block at the end; WP4: `ds.*` strings in the "Design Studio (WP4)" block; `ds.*` strings, in a `# Design Studio (<WP>)` block at the end; WP2: brand, Plans, MCP and autosave overrides in place; new keys in a `# Design Studio (WP2)` block at the end. WP5: auth copy in a `# Design Studio (WP5)` block after it, and five Indonesian `ds.auth.*` values say "password" as F1 · 08/09 do; WP3: overrides `errors.save-failed` in place with the overlay text ("Keep this tab open ... MCP edits are paused"); G3: `ds.workspace.*` strings in a `# Design Studio (G3)` block at the end | - | F2, WP4, WP6, WP3, G3 |
| `frontend/translations/id.po` | WP2: same for Indonesian; WP4: `ds.*` strings in the "Design Studio (WP4)" block; `ds.*` strings, in a `# Design Studio (<WP>)` block at the end; WP3: adds `errors.save-failed` (Indonesian overlay text) in a `# Design Studio (WP3)` block at the end; the key did not exist in id.po; G3: `ds.workspace.*` strings in a `# Design Studio (G3)` block at the end, and two tab labels say what board F1 · 01 says, in place: `workspace.sidebar.layers` "Layer" (was "Lapisan"), `workspace.options.inspect` "Inspect" (was "Inspeksi") | - | F2, WP4, WP6, WP3, G3 |
| `frontend/src/app/main/ui/settings/password.cljs` | WP4: backup-password title and note (S5) | - | WP4 |
| `frontend/src/app/main/ui/settings/profile.cljs` | WP4: open sign-up shows an email note instead of change-email (S6) | - | WP4 |
| `frontend/src/app/main/ui/settings/sidebar.cljs` | WP4: "Backup password" and "Plans" sidebar labels (S5, S7); G1 (F1 · 10 chrome): `sidebar*` calls `studio.dashboard-shell.scope/use-scope` (the light chrome while Settings is open), the back link reads "Beranda" and the sidebar gets its title "Pengaturan" (one require, one call, one element, one key) | - | WP4, G1 |
| `frontend/src/app/main/ui/settings/subscription.cljs` | WP4: Settings > Plans renders the Design Studio plan summary (S7) | - | WP4 |
| `frontend/src/app/main/ui/workspace/main_menu.cljs` | WP4: power-up entry opens Design Studio Plans (S8); P1: that entry gets the menu's item class (`base-menu-item`, same padding as its neighbours) and a separator before it | - | WP4, P1 |
| `frontend/test/frontend_tests/runner.cljs` | F3-C: registers `frontend-tests.studio.token-suggestions-test`; WP4: registers `frontend-tests.studio.shell-state-test`; WP3: registers `frontend-tests.plugins.persistence-guard-test` (two lines); Registers the `frontend-tests.studio.*` test namespaces | - | WP4, WP3, WP6 |
| `frontend/src/app/main/ui/workspace/viewport.cljs` | WP7: requires `ui/studio/touch.js` and starts the touch layer once from `viewport*` | - | F2 |
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
| `docs/upstream-pr-persistence-guard.md` | WP3: new file, the pull request description for penpot/penpot. Not part of the image | Proposed upstream, text in `docs/upstream-pr-persistence-guard.md` (owner sends it after F2 is live) | WP3 |
| `frontend/src/app/main/ui.cljs` | Mounts `studio.widgets/global-widgets*` once in `app` (refusal card, first steps after sign-up, MCP tip and guide) | - | WP6 |
| `frontend/src/app/main/errors.cljs` | `handle-error :restriction` announces Design Studio guard refusal codes instead of reporting them as unhandled; WP3: the save-failed toast no longer offers "Reload page" (reloading can drop edits that exist only in the tab; the new text says to keep it open). Removes `reload-page-link` | - | WP6, WP3 |
| `frontend/src/app/main/repo.cljs` | `handle-response` reads the guard's JSON refusal (413/429/503/507, written by the proxy, not the backend) and raises it as `:restriction` with our code; without it these answers become generic `:validation`/`:rate-limit`/`:service-unavailable` errors and never reach the `:restriction` handler | - | WP6 |
| `frontend/src/app/main/ui/settings/integrations.cljs` | `data-testid` on the MCP switch, the copy button and the key windows, so the MCP guide points at them without CSS-module class names; F4-B (F1 · 10): mounts the `ai-access-card*` above the MCP section, whether or not MCP is on yet (the card hosts the Connect flow) | - | WP6, F4-B |
| `frontend/src/app/main/ui/auth.cljs` | WP5 (L2, L3, L7): auth pages render inside `studio.auth/shell*` (F1 · 08) instead of Penpot's logo and illustration; register routes leave for /start/signup with open sign-up | - | WP5 |
| `frontend/src/app/main/ui/auth/login.cljs` | WP5 (L2, L3): markup only. Form first, ZakaCoding ID (OIDC) button below, sign-up link to /start/signup; form state, events and redirects unchanged | - | WP5 |
| `frontend/src/app/main/ui/auth/recovery_request.cljs` | WP5 (L5): markup only. Own title, 15-minute note with open sign-up, DS "mail sent" card | - | WP5 |
| `frontend/src/app/main/ui/auth/recovery.cljs` | WP5 (L5, L6): markup only, plus a callback so a refused reset link shows "this link can't be used again"; schema and event unchanged | - | WP5 |
| `frontend/src/app/main/ui/auth/verify_token.cljs` | WP5 (L6): a used or expired sign-up link renders `studio.auth/used-link-page*` with open sign-up | - | WP5 |
| `frontend/src/app/main/ui/static.cljs` | WP5 (L4): signed-out login modal uses `studio.auth/modal*`; register link goes to /start/signup; G4: `error-container*` renders `studio.error-page/error-shell*` (light Design Studio page, official Logo, Back to Home, Penpot credit) and the 404/500/502/503 pages use DS copy and `studio.ds.button`; the login modal is untouched | - | WP5 |
| `frontend/src/app/main/ui/debug/playground.cljs` | Mounts the DS showcase (`app.main.ui.studio.ds.showcase/showcase*`) on the existing debug route `#/debug/playground`, so the release build compiles every DS base component. One require and one element. | - | F3/F4 foundation |
| `mcp/packages/plugin/src/task-handlers/ExecuteCodeTaskHandler.test.ts` | WP3: new file, node:test cases for the save confirmation around `execute_code` | - | WP3 |
| `mcp/packages/plugin/package.json` | WP3: `test` script runs `--experimental-transform-types` (the handler imports `Task`, which uses parameter properties) and the `task-handlers` tests | - | WP3 |
| `mcp/packages/plugin/tsconfig.json` | WP3: excludes `src/**/*.test.ts` from `tsc` like `src/*.test.ts` | - | WP3 |
| `frontend/test/frontend_tests/main_errors_test.cljs` | WP3 review fix: the save-failed toast has no reload link (keep the tab open), so the two upstream assertions on the reload link now assert its absence | - | F2 |
| `frontend/src/app/main/ui/workspace.cljs` | F3-A: mounts `studio.workspace.shell/shell*` once in `workspace*` (chrome theme scope on <body>, width rules, the `craft-dock` and `viewport-overlay` mount points, canvas focus islands). One require, one element | - | F3-A |
| `frontend/src/app/main/ui/workspace/sidebar.cljs` | F3-A: the collapsed left sidebar renders `studio.workspace.rail/layers-rail*` (the Layers rail) instead of `collapsed-button*`, which stays in place and unused. One require, one element; G3 (F1 · 01): `left-header*` and `right-header*` are referred from `studio.workspace.header` instead of upstream's two namespaces (still required, their components unused except the zoom widget), and the "Tokens" tab label is translated (`ds.workspace.tab.tokens`, "Token" in Indonesian). One require line, two comments, one label | - | F3-A, G3 |
| `frontend/src/app/main/ui/workspace/viewport/scroll_bars.cljs` | G3: on a page without shapes `base-objects-rect` is nil, so the scrollbar offsets are NaN or Infinity; when the canvas was resized there (the Craft dock opening) they reached the DOM as `<rect width="NaN" x="Infinity">`. The scrollbars are not shown without content. One binding, two conditions | Upstream bug, not reported yet | G3 |
| `frontend/src/app/main/ui/workspace/sidebar/sitemap.cljs` | F3-B: the current page's item carries `data-ds-page-name="<name>"` (one attribute), the stable hook Studio's `ds-craft.js` reads instead of the CSS-module class names | - | F3-B |
| `frontend/src/app/main/ui/workspace/colorpicker.cljs` | F3-C (F1 · 13): one require and one element, the token suggestions row (`app.main.ui.studio.workspace.token-suggestions-ui`) under the colour inputs; F3-C (F1 · 13): one require and one element, the token suggestions row (`app.main.ui.studio.workspace.token-suggestions-ui`) under the colour inputs; G4 (F1 · 13): the icon radio pair is replaced by `studio.workspace.components/picker-tabs*` (Color / Token), inside `guard*` | - | F3-C |
| `frontend/src/app/main/ui/dashboard.cljs` | G1 (F1 · 03, 12, 14), replaces the F4-A seam: the require of `sidebar*` points at `studio.dashboard-shell.sidebar` (the Design Studio sidebar, same props), the require of `projects-section*` at `studio.dashboard-shell.views/recent*` under the same name (home, Projects or Templates by the `view` parameter), and `dashboard-content*` mounts `views/top*` once (search, "+ File baru", Library / Font tabs). Two require lines and one element; the rest of the body is untouched; F4-A (F1 · 12): the require of `projects-section*` is replaced by `studio.dashboard.home/home*` under the same name. `home*` renders `projects-section*` with the same props, or the first-design page when the team has no project and no file. Require lines only, the body is untouched | - | F4-A, G1 |
| `frontend/src/app/main/ui/dashboard/files.cljs` | G1 (F1 · 14): the project page renders `studio.dashboard-shell.project/project-header*` instead of the private `header*` (which stays in place and unused), `files-heading*` ("File n" and the grid / list switch) above the grid and `new-file-tile*` as the child of `grid*`; the section carries `data-ds-dash="files"`. One require, three elements, one attribute | - | G1 |
| `frontend/src/app/main/ui/dashboard/grid.cljs` | G1 (F1 · 14): `grid*` renders its `children` after the last card (the dashed "+ File baru" tile). One destructured key, one expression | - | G1 |
| `frontend/src/app/main/ui/dashboard/deleted.cljs` | G1 (F1 · 03): the page title is "Sampah" (`ds.shell.nav.deleted`), the page of the sidebar, instead of "Projects". One key; P2: the empty Trash renders `studio.dashboard-shell.empty/trash-empty*` (one require, one element) | - | G1, P2 |
| `frontend/src/app/main/ui/dashboard/projects.cljs` | F4-A (F1 · 03): mounts `studio.dashboard.connect-card/home-card*` (the two-step "Connect AI" card and its status bar) as the first row of the projects scroll area. One require, one element | - | F4-A |
| `frontend/src/app/main/ui/viewer.scss` | F4-B (F1 · 11): `.viewer-layout` and `.inspect-layout` include `ds.viewer-scope`, which maps the theme variables the viewer reads to `ds/light` tokens in that subtree only | - | F4-B |
| `frontend/src/app/main/ui/viewer/header.scss` | F4-B (F1 · 11): viewer header and menus read DS tokens | - | F4-B |
| `frontend/src/app/main/ui/viewer/inspect.scss` | F4-B (F1 · 11): Inspect sidebar container reads DS tokens | - | F4-B |
| `frontend/src/app/main/ui/viewer/login.cljs` | F4-B (F1 · 11): share-link sign-in uses the Design Studio dialog and lockup (`studio/auth` modal*), sign-up link goes to /start/signup; Penpot register sections removed | - | F4-B |
| `frontend/src/app/main/ui/viewer/login.scss` | F4-B (F1 · 11): keeps only the scrim, the dialog is `studio/auth` | - | F4-B |
| `frontend/src/app/main/ui/dashboard/projects.scss` | F4-B (F1 · 14): Projects view restyled with DS tokens (project rows, show-more, team hero) | - | F4-B |
| `frontend/src/app/main/ui/workspace/context_menu.cljs` | G4 (F1 · 13): one require and one element, `studio.workspace.components/context-menu-craft*` ("Change with Craft", Ctrl Enter) before the hide/lock group; shown only when Studio says the member has Craft | - | G4 |
| `frontend/src/app/main/ui/workspace/sidebar/shortcuts.cljs` | G4 (F1 · 13): one require and one element (`craft-shortcuts*`, the Craft section) and the search placeholder key | - | G4 |
| `frontend/src/app/main/ui/exports/assets.cljs` | G4 (F1 · 13): dialog title "Export N layers" and the scale shown as "2x" | - | G4 |
| `frontend/src/app/main/ui/settings/integrations.scss` | G5 (F1 · 10): page heading and the folded Penpot keys section, content column 760 px | - | G5 |
| `frontend/src/app/main/ui/viewer/header.cljs` | G5 (F1 · 11): text mode buttons (Lihat, Inspect, Komentar) instead of icons, file name and frame counter in the left island | - | G5 |
| `frontend/src/app/main/ui/viewer/inspect.cljs` | G5 (F1 · 11): no layer list and no pagination in Inspect; the panel floats over the canvas | - | G5 |
| `frontend/src/app/main/ui/inspect/right_sidebar.cljs` | G5 (F1 · 11): in the viewer a selected layer renders `studio.viewer.inspect/viewer-inspect*` (inside a guard) instead of the tab switcher; the workspace Inspect and the empty state are unchanged | - | G5 |
| `frontend/src/app/util/i18n.cljs` | P1 (owner, 16 Nov): the default UI language is English. `autodetect` returns `cf/default-language` instead of the browser language, so a new member is never switched to Indonesian by the browser; a language saved in the profile still wins (`set-locale`). The unused `browser-locales` is removed | - | P1 |
| `frontend/src/app/main/ui/workspace/sidebar/layers.cljs` | P1 (F1 · 01): the bar over the layer tree says "Layers" (`workspace.sidebar.layers`) instead of the page name, so it reads as the section header next to "Pages"; `use-search` keeps its `page` argument as `_page` | - | P1 |
| `frontend/src/app/main/ui/dashboard/placeholder.cljs` | P2: the empty Library page renders `studio.dashboard-shell.empty/libraries-empty*` instead of upstream's illustration with its penpot.app link, and the "Add a library" card of an empty project opens the Templates page instead of penpot.app (one require, one element, one call; the unused require of `empty-placeholder*` removed) | - | P2 |
| `frontend/src/app/main/ui/dashboard/fonts.cljs` | P2: a team without custom fonts sees `studio.dashboard-shell.empty/fonts-empty*` (one require, one element); the upload above it is unchanged | - | P2 |
<!-- seams:end -->

## Mount points (F3)

Named places where one package renders into a surface another package owns.
They are not seams: every file below is DS-owned. The owner of the surface
lays the mount point out; the owner of the content fills it and never edits
the surface.

| Mount point | Element | Filled by | Rendered from | Contract |
| --- | --- | --- | --- | --- |
| `craft-dock` | `<aside id="ds-craft-dock" data-ds-mount="craft-dock">` | F3-B: `app.main.ui.studio.workspace.craft-dock/craft-dock*` (`studio/workspace/craft_dock.cljs`, a stub until F3-B lands) | `studio.workspace.shell/shell*` (F3-A) | Grid column `craft` of the workspace, between the canvas and the right sidebar, one panel edge from the top and bottom of the window. No width while the component renders nothing. The dock sets its own inline size (440 px) and surface (`ws.island`). Below 1280 px the aside lies over the canvas, right aligned. Not rendered in canvas focus (hide-ui). For a dock that takes room from the canvas; a dock that floats over the canvas with `position: fixed` (F3-B's first version) does not need it |
| `viewport-overlay` | `<div id="ds-viewport-overlay" data-ds-mount="viewport-overlay">` | F3-B: `app.main.ui.studio.workspace.quick-prompt/quick-prompt*` (`studio/workspace/quick_prompt.cljs`, a stub until F3-B lands); G3: the shell itself puts the floating toolbar (`studio.workspace.toolbar/toolbar*`, bottom centre, 20 px from the bottom, 48 px high) and the canvas focus islands there | `studio.workspace.shell/shell*` (F3-A) | Covers the canvas area (grid cell `viewport`, rulers included), above the viewport and below menus and dialogs. The layer has `pointer-events: none`; content sets `pointer-events: auto` on its own root and positions itself in CSS px from the top left corner of the canvas area |
| chrome scope | `<body class="ds-chrome light" data-ds-width="wide\|medium\|narrow">` | F3-B, F3-C and Studio's scripts read it | `studio.workspace.shell/shell*` (F3-A) | Set while the workspace is mounted. `body.ds-chrome` is the scope for workspace-only styles (SCSS: `ws.$scope` in `studio/workspace`); Penpot's `--color-*` variables point at `ds/light` tokens inside it, so a component that reads them is already recolored. `data-ds-width` follows the width rules (1600 px and wider, 1280 to 1599 px, below) |
| Craft open | `<html data-ds-craft-open>` | F3-B's dock sets it (its "Craft dock v1" contract) | read by `studio.workspace.state/use-craft-open` (F3-A) | While it is present at the rail widths (below 1600 px), the shell collapses Layers to the rail. The shell only observes the attribute; neither package requires the other |
| panel hooks | `[data-testid="right-sidebar"]`, `[data-testid="left-sidebar"]`, `[data-testid="left-sidebar-rail"]`, `#left-sidebar-aside`, `#right-sidebar-aside` | upstream and `studio.workspace.rail` | - | Stable hooks for measuring the panels. Each panel floats one `space.panel-edge` (12 px) from the window edge, so the free canvas starts `panel width + 2 × edge` from the window edge |

Both components get the props `file-id`, `page-id`, `layout` (workspace
layout flags) and `width-class` (`:wide`, `:medium`, `:narrow`).

Shared SCSS for anything that floats in the workspace:
`@use "studio/workspace" as ws;` gives `ws.island` (the floating panel
surface), `ws.$edge` (gap to the window and the canvas), `ws.$rail-size`,
`ws.$craft-size` and `ws.on("<module>__<class>")` (matches an upstream
element by its CSS-module class without editing its stylesheet).

## Mount points (dashboard, G1)

The dashboard shell (`frontend/src/app/main/ui/studio/dashboard_shell/**`,
boards F1 · 03, 03c, 12, 14 and the Settings chrome of F1 · 10) owns the
sidebar, the top bar and the layout of every dashboard route. Page content
renders inside it. All files below are DS-owned.

| Mount | Namespace | Props | Contract |
| --- | --- | --- | --- |
| `page*` | `app.main.ui.studio.dashboard-shell.page` | `title` (string or element), `eyebrow`, `actions` (element), `tabs` (element, `tabs*` of `tab*`), `title-id`, `children`; other props go to the body element | The one mount for a dashboard page: header, optional tabs, scrolling body. Render it as the root of the page (it returns a fragment whose parts are grid items of the content area; do not wrap it). The shell adds the search box and "+ File baru" to the right of the header itself, so `actions` is only for what the page adds |
| `home-page*` | `app.main.ui.studio.dashboard-shell.home` | `profile`, optional `title`, `children`, the rest as `page*` | `page*` with the greeting as title ("Selamat pagi / siang / sore / malam, <first name>"). `title` replaces the greeting (F1 · 12 "Selamat datang, Zaka"). **`app.main.ui.studio.dashboard.home/home*` (G2) renders this as its root and puts the home content inside** |
| `recent-files*` | `app.main.ui.studio.dashboard-shell.home` | `team`, `project` (the drafts: where a dropped file is imported), optional `limit` (4), `title` | "File terbaru": the newest files of the team as upstream grid cards, with "Lihat semua →" to the Projects page. Renders nothing while the team has no file. The home content places it where the board has it |
| `section-title*`, `text-link*`, `black-button*`, `soft-button*` | `app.main.ui.studio.dashboard-shell.page` | see the docstrings | The block heading ("File terbaru", "Lihat semua →") and the 40 px pill buttons of the boards, for page content that needs the same look |
| `use-create-file` | `app.main.ui.studio.dashboard-shell.home` | `team-id`, `project-id` → `(fn [origin])` | Penpot's own `create-file` in a project, then the workspace |
| Craft prompt | `[data-ds-craft-prompt]` | - | "Craft" in the sidebar opens the home and focuses the element with this attribute (the hero input). A Free member goes to Settings > Plans instead |
| `member` | `app.main.ui.studio.shell-state/use-state` → `:member` | `{:plan :credits {:left :total} :storage {:usedBytes :totalBytes}}` | The plan card of the sidebar. Absent or null fields are left out; without `member` there is no card |
| scope | `<body class="ds-dash light">` | - | Set while a dashboard or Settings page is mounted (`studio.dashboard-shell.scope/use-scope`). Scope of `studio/dashboard_shell/chrome.scss`, which maps Penpot's `--color-*` to `ds/light` and restyles the upstream pages by CSS-module class |
| `data-ds-dash` | attribute | `sidebar`, `header`, `top-actions`, `tabs`, `body`, `files`, `templates`, `settings-title`, `new-file-tile` | How `chrome.scss` finds the shell's own parts (grid areas, card grid). `[data-ds-dash="files"]` around an upstream `dashboard-grid` lays its cards out as the board's grid |

Pages of `/dashboard/recent`: no `view` is the home, `view=projects` is
upstream's Projects page ("Proyek"), `view=templates` the templates page
("Template"). Library and Font are one sidebar item with two tabs. "Sampah"
is upstream's deleted page.
## Mount points (dashboard home, G2)

| Mount point | Element | Filled by | Rendered from | Contract |
| --- | --- | --- | --- | --- |
| home content | `<div data-testid="ds-home">` (`ds-empty-home` for an account with nothing yet) | G2: `app.main.ui.studio.dashboard.home/home*` | the dashboard shell's page (G1), in the body of the home page | Props `team`, `projects`, `profile` (as `projects-section*` takes them) and `recent`, the element of the shell's "File terbaru" section, which `home*` places between the Craft hero and the Connect AI card. Inside `body.ds-dash` the home adds no padding, background or scrolling. The sections are exported one by one too (`craft-hero*`, `start-cards*`, `empty-files*`, `connect-card*`, `templates-row*`) |
| Craft prompt hand-off | `sessionStorage['ds-craft:prompt']` = `{prompt, fileId, at}`, `sessionStorage['ds-craft:open']` = `"1"` | G2: `studio.dashboard.handoff/leave!` (dashboard) | read by `studio.craft.dock` (G3: it opens the dock and removes the flag) and by Studio's `ds-craft.js` (fills Craft's box, does not send, removes the entry) | Same tab, five minutes, the file named in the entry. Any page may call `leave!` before it opens a file |
| shell state | `GET /studio/api/editor-shell/state` → `member {plan, credits {left,total}, storage {usedBytes,totalBytes}, renewsAt}`, `assistant {name, access}`, `apps [{name, access, connectedAt, lastUsedAt}]` | Studio | `studio.shell-state/use-state` (plan card, G1), `studio.dashboard.connect/fetch-status` (Connect AI card, G2) | Null or absent fields are not shown |

