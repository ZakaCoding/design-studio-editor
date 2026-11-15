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
| `frontend/translations/en.po` | WP2: brand, Plans, MCP and autosave overrides in place; new keys in a `# Design Studio (WP2)` block at the end | - | F2 |
| `frontend/translations/id.po` | WP2: same for Indonesian | - | F2 |
<!-- seams:end -->
