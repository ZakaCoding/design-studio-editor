# Studio DS

The Design Studio design system inside the editor fork: tokens, base
components and the rules for building F3 (new workspace) and F4 (dashboard,
auth, settings, viewer) on them.

Two design systems live in this repository. Keep them apart:

| | Namespace | Styles | Owner |
| --- | --- | --- | --- |
| Penpot DS (upstream) | `app.main.ui.ds.*` | `--color-*`, `--sp-*`, `ds/…scss` | Upstream. Do not edit. |
| **Studio DS (ours)** | `app.main.ui.studio.ds.*` | `--ds-*`, `studio/ds` | This document. |

New Design Studio screens use Studio DS only. Do not mix a Penpot DS
component into a Studio DS surface, except the icon sprite (see `icon*`).

## 1. Where things are

| What | Path |
| --- | --- |
| Token values (SCSS maps), `token()` | `frontend/resources/styles/studio/_ds-tokens.scss` |
| Mixins (`root`, `type`, `focusable`, `motion`, surfaces) | `frontend/resources/styles/studio/_ds-helpers.scss` |
| Font faces | `frontend/resources/styles/studio/_ds-fonts.scss` |
| SCSS index (`@use "studio/ds" as ds;`) | `frontend/resources/styles/studio/_ds.scss` |
| Emits `--ds-*` on `:root` (the only place) | `frontend/src/app/main/ui/studio/ds/tokens.scss` |
| Components | `frontend/src/app/main/ui/studio/ds/*.cljs` + `.scss` |
| One-import index | `frontend/src/app/main/ui/studio/ds.cljs` |
| Showcase | `frontend/src/app/main/ui/studio/ds/showcase.cljs`, route `#/debug/playground` (development builds only) |

## 2. Tokens

Source of truth: the token library of the Penpot file "Concept", sets
`ds/core` (64 primitives) and `ds/light` (40 semantic), theme
"Design Studio · Terang" (board F1 · 07). Read through the Penpot MCP on
2026-11-15. Change the Penpot token first, then `_ds-tokens.scss`.

**Naming rule:** token `a.b.c` is the custom property `--ds-a-b-c`
(`bg.panel` → `--ds-bg-panel`, `color.gray.150` → `--ds-color-gray-150`).

**Scope:** the properties are defined on `:root`. Every name starts with
`--ds-`, a prefix upstream does not use, and no upstream selector reads
them, so the Penpot chrome keeps its own theme (owner decision for F2). A
Studio DS component looks the same under Penpot's dark and light themes.
F3 recolors the chrome by making workspace styles read `--ds-*`; it does
not redefine Penpot's `--color-*` globally.

**Use the semantic layer.** Components read `ds/light` names. A `ds/core`
color (`--ds-color-*`) in a component is a review finding, except the
`color.fun.*` sticker colors, which have no semantic alias. A dark theme
later replaces the semantic layer only.

In SCSS use `ds.token("name")`: it returns `var(--ds-name)` and fails the
build on a name that does not exist.

### ds/light (semantic, 40)

| Token | Custom property | Points to | Value |
| --- | --- | --- | --- |
| bg.canvas | `--ds-bg-canvas` | gray.150 | #E4E4EA |
| bg.panel | `--ds-bg-panel` | gray.0 | #FFFFFF |
| bg.subtle | `--ds-bg-subtle` | gray.50 | #F5F5F8 |
| bg.selected | `--ds-bg-selected` | indigo.50 | #EEEEFC |
| bg.inverse | `--ds-bg-inverse` | gray.900 | #15151A |
| border.default | `--ds-border-default` | gray.100 | #EBEBEF |
| border.strong | `--ds-border-strong` | gray.300 | #D9D9E0 |
| text.primary | `--ds-text-primary` | gray.900 | #15151A |
| text.body | `--ds-text-body` | gray.800 | #3A3A44 |
| text.secondary | `--ds-text-secondary` | gray.700 | #6A6A75 |
| text.placeholder | `--ds-text-placeholder` | gray.600 | #737380 |
| text.disabled | `--ds-text-disabled` | gray.500 | #9C9CA8 |
| text.on-accent | `--ds-text-on-accent` | gray.0 | #FFFFFF |
| accent.default | `--ds-accent-default` | indigo.500 | #5B5BD6 |
| accent.subtle | `--ds-accent-subtle` | indigo.50 | #EEEEFC |
| accent.text-strong | `--ds-accent-text-strong` | indigo.700 | #2E2E8A |
| craft.default | `--ds-craft-default` | indigo.500 | #5B5BD6 |
| craft.subtle | `--ds-craft-subtle` | indigo.50 | #EEEEFC |
| status.success | `--ds-status-success` | green.500 | #22A06B |
| status.success-text | `--ds-status-success-text` | green.700 | #1B7A52 |
| status.success-bg | `--ds-status-success-bg` | green.50 | #F3FBF7 |
| status.success-border | `--ds-status-success-border` | green.200 | #CFEFDF |
| status.warning | `--ds-status-warning` | amber.500 | #C98A00 |
| status.warning-text | `--ds-status-warning-text` | amber.800 | #7A5300 |
| status.warning-bg | `--ds-status-warning-bg` | amber.50 | #FFF6E0 |
| status.danger-text | `--ds-status-danger-text` | red.700 | #B42318 |
| status.danger-bg | `--ds-status-danger-bg` | red.50 | #FDECEC |
| guide.measure | `--ds-guide-measure` | fun.measure | #FF3D8B |
| brand.wordmark-ink | `--ds-brand-wordmark-ink` | brand.ink | #182421 |
| brand.wordmark-muted | `--ds-brand-wordmark-muted` | brand.muted | #627169 |
| radius.panel | `--ds-radius-panel` | radius.xl | 16px |
| radius.control | `--ds-radius-control` | radius.sm | 8px |
| radius.card | `--ds-radius-card` | radius.lg | 14px |
| radius.button | `--ds-radius-button` | radius.pill | 999px |
| space.panel-edge | `--ds-space-panel-edge` | space.12 | 12px |
| space.panel-padding | `--ds-space-panel-padding` | space.16 | 16px |
| space.row | `--ds-space-row` | space.8 | 8px |
| elevation.panel | `--ds-elevation-panel` | shadow.float | 0 8px 24px #15151A14 |
| elevation.popover | `--ds-elevation-popover` | shadow.pop | 0 6px 16px #15151A24 |
| elevation.control | `--ds-elevation-control` | shadow.raised | 0 1px 3px #15151A1A |

### ds/core (primitives, 64)

| Group | Custom properties | Values |
| --- | --- | --- |
| Gray (10) | `--ds-color-gray-{0,50,100,150,300,500,600,700,800,900}` | #FFFFFF #F5F5F8 #EBEBEF #E4E4EA #D9D9E0 #9C9CA8 #737380 #6A6A75 #3A3A44 #15151A |
| Indigo (3) | `--ds-color-indigo-{50,500,700}` | #EEEEFC #5B5BD6 #2E2E8A |
| Green (5) | `--ds-color-green-{50,100,200,500,700}` | #F3FBF7 #E7F6EE #CFEFDF #22A06B #1B7A52 |
| Amber (3) | `--ds-color-amber-{50,500,800}` | #FFF6E0 #C98A00 #7A5300 |
| Red (2) | `--ds-color-red-{50,700}` | #FDECEC #B42318 |
| Fun (6) | `--ds-color-fun-{orange,mint,sky,yellow,pink,measure}` | #FF6B3D #7ED9B0 #6EA8FF #FFC93D #FF8FC7 #FF3D8B |
| Brand (2) | `--ds-color-brand-{ink,muted}` | #182421 #627169 |
| Font family (4) | `--ds-font-{display,text,mono,wordmark}` | Funnel Display, Geist, Geist Mono, Inter (each with a system fallback stack) |
| Font size (9) | `--ds-size-{10,11,12,13,14,16,18,24,32}` | px |
| Font weight (4) | `--ds-weight-{regular,medium,semibold,bold}` | 400 500 600 700 |
| Radius (6) | `--ds-radius-{xs,sm,md,lg,xl,pill}` | 4 8 10 14 16 999 px |
| Space (7) | `--ds-space-{4,8,12,16,20,24,32}` | px |
| Shadow (3) | `--ds-shadow-{raised,float,pop}` | see elevation.* above |

### Code-only values

Needed by components and not in the Penpot file yet. Keep the list short;
promote a value to the Penpot file before using it widely.

| Custom property | Value | Use |
| --- | --- | --- |
| `--ds-accent-hover`, `--ds-accent-pressed` | accent.default mixed with 14% / 26% black | Primary button states |
| `--ds-danger-solid`, `--ds-danger-solid-hover` | red.700, and 14% darker | Danger button |
| `--ds-overlay-scrim` | #15151A at 40% | Dialog scrim |
| `--ds-focus-ring-color/-width/-offset` | accent.default, 2px, 2px | Keyboard focus |
| `--ds-control-height-{sm,md,lg}` | 28, 32, 44 px | Control heights |
| `--ds-motion-{fast,base,slow}` | 120, 180, 280 ms | Transitions |
| `--ds-ease-out`, `--ds-ease-spring` | the /start V4 curves | Transitions |
| `--ds-z-{panel,dialog,popover,toast,tooltip}` | 100, 300, 400, 500, 600 | Same scale as Penpot's `--z-index-*` |

### Type styles

`@include ds.type("<style>")` sets family, size, weight, line height and
letter spacing (F1 · 07, "Tipografi").

| Style | Font | Use |
| --- | --- | --- |
| `display` | Funnel Display 32 / 700 | Page greeting, auth title |
| `title` | Funnel Display 18 / 600 | Section and dialog titles |
| `panel-title` | Geist 13 / 600 | Panel headers |
| `body` | Geist 12 / 400 | Editor text (default of `ds.root`) |
| `body-lg` | Geist 14 / 400 | Dashboard, auth and settings text |
| `control` | Geist 13 / 500 | Buttons, inputs |
| `control-lg` | Geist 14 / 600 | Large buttons |
| `label` | Geist Mono 10 / 500, caps | Field group labels ("POSISI & UKURAN") |
| `value` | Geist Mono 12 / 500 | Numeric values (X, Y, W, H) |

Fonts: the four variable fonts are loaded from `/start/assets/fonts/`,
the files /start serves on the same origin. A browser downloads a face only
when rendered text uses it. On a stack without those files the system
fallback applies. See "Open points".

## 3. Components

```clojure
(:require
 [app.main.ui.studio.ds :as sds])

[:> sds/button* {:variant "primary" :on-click on-save} (tr "labels.save")]
```

Or require one component namespace directly
(`[app.main.ui.studio.ds.button :refer [button*]]`). All components are
rumext `*` components: props are kebab-case in hiccup, enumerated values are
strings, boolean props start with `is-`, and props not listed go to the
root element (`on-click`, `disabled`, `aria-*`, `data-testid`, …) unless
the row says otherwise. Every component takes `class` for layout (margin,
width, grid placement) only.

| Component | Namespace | Props |
| --- | --- | --- |
| `button*` | `…ds.button` | `variant` primary (default) \| secondary \| ghost \| danger · `size` sm 28 \| md 32 (default) \| lg 44 · `icon-id` (Penpot sprite id) · `to` (renders `<a>`) · `type` · `is-loading` · `is-full-width` |
| `icon-button*` | `…ds.button` | `aria-label` (required) · `icon-id` or one child (`icon*`) · `variant` (default ghost) · `size` · `is-active` (sets `aria-pressed`) |
| `input*` | `…ds.field` | `label` · `hint` · `error` · `label-end` (element) · `leading` / `trailing` (elements) · `size` md \| lg · `id` · the rest goes to `<input>` · forwards `ref` |
| `field*` | `…ds.field` | `id` · `label` · `label-end` · `hint` · `error` · children = any control. No pass-through props |
| `card*` | `…ds.card` | `variant` outlined (default) \| subtle \| raised · `padding` none \| md \| lg · `on-click` (renders `<button>`) · `to` (renders `<a>`) · `is-selected` |
| `panel*` | `…ds.panel` | `title` · `actions` (element) · `footer` (element) · `is-flush` · `aria-label` when there is no title |
| `toolbar*` | `…ds.toolbar` | `aria-label` (required) · `orientation` horizontal \| vertical · `variant` floating (default) \| plain |
| `toolbar-group*` | `…ds.toolbar` | `aria-label` |
| `toolbar-separator*` | `…ds.toolbar` | none |
| `badge*` | `…ds.badge` | `tone` neutral (default) \| accent \| craft \| solid \| success \| warning \| danger · `is-mono` |
| `tag*` | `…ds.badge` | `tone` · `on-remove` · `remove-label` |
| `toast*` | `…ds.toast` | `tone` neutral \| success \| warning \| danger · `title` · `action` (element) · `on-close` · `close-label` |
| `toast-region*` | `…ds.toast` | `position` bottom-center (default) \| bottom-right \| top-right · `aria-label` |
| `dialog*` | `…ds.dialog` | `title` (required) · `description` · `footer` (element) · `on-close` (required) · `size` md 440 \| lg 640 · `is-dismissible` · `close-label`. No pass-through props |
| `tooltip*` | `…ds.tooltip` | `content` (required) · `shortcut` · `placement` top (default) \| bottom \| left \| right · children = the trigger. No pass-through props |
| `icon*` | `…ds.icon` | `icon-id` (Penpot sprite id) or children (SVG paths) with `view-box` (default 24 grid) · `size` s 12 \| m 16 \| l 20 \| xl 24 · `label`. No pass-through props |
| `logo*` | `…ds.logo` | `variant` lockup (default) \| mark · `size` sm \| md \| lg · `to` |
| `logo-mark*` | `…ds.logo` | `size` (px) · `label`. No pass-through props |

Element props (`actions`, `footer`, `leading`, `label-end`, …) take a React
element: `(mf/html [:> sds/button* {…} "…"])`.

Behavior that is built in, so do not rebuild it:

- **Dialog:** rendered when mounted (`(when open? [:> sds/dialog* …])`).
  Focus moves in and returns to the opener, Tab stays inside, Esc and a
  scrim click call `on-close`. Portal on `<body>`.
- **Toast:** `toast*` is presentational. The feature owns the list and the
  timing; `toast-region*` is the live region. Mount one region per screen
  and keep it mounted while empty. Do not auto-dismiss a toast that has an
  action or reports an error.
- **Toolbar:** arrow keys move focus between its controls (Left/Right, or
  Up/Down when vertical).
- **Tooltip:** shows on hover and keyboard focus, hides on Esc. It is CSS
  positioned, so an ancestor with `overflow: hidden` clips it; pick a
  `placement` that points into free space.
- **Input:** `error` sets `aria-invalid`, links the message with
  `aria-describedby` and announces it (`role="alert"`).
- **Loading button:** keeps focus and look, sets `aria-busy`, ignores clicks
  and does not submit.

Icons: `icon-id` values are the upstream sprite ids
(`[app.main.ui.ds.foundations.assets.icon :as i]`, then `i/add`). An icon
that the sprite does not have is passed as children on the 24 px grid; do
not add files to the upstream sprite for it.

## 4. Usage rules

1. **Start every component stylesheet with the index and the root mixin.**

   ```scss
   @use "studio/ds" as ds;

   .my-screen {
     @include ds.root; // font, color and box model, cut off from the Penpot theme
     background: ds.token("bg-panel");
   }
   ```

2. **No raw values.** Colors, radii, spacing, shadows, font sizes and
   weights come from `ds.token()`. A hex color or a `px` radius in a
   `studio/**` stylesheet needs a comment that says why no token fits.
   Layout sizes (a 440 px panel, a 1 px hairline) are fine.
3. **No Penpot variables in Studio DS surfaces** (`--color-*`, `--sp-*`,
   `$s-*`, `deprecated.*`): they change with the Penpot theme.
4. **Brand.** Use `logo*` / `logo-mark*` wherever the logo appears. Do not
   redraw the mark, recolor it or set the wordmark in another font. The
   wordmark is always dual tone ("Design" ink, "Studio" muted). Place the
   lockup on `bg.panel` or `bg.subtle` (on `bg.canvas` the muted word is
   4.1:1). The word "Penpot" stays out of Design Studio UI except in
   attribution text.
5. **Contrast (WCAG AA, 4.5:1 for text).** Checked pairs: every component
   in this DS on its own surface. Two pairs fail and are not allowed:
   `text.secondary` on `bg.canvas` (4.2:1; use `text.body`) and
   `text.disabled` anywhere except a disabled control. Placeholders use
   `text.placeholder`. Text on a status background uses the matching
   `status.*-text`.
6. **Keyboard.** Everything clickable is a `<button>` or `<a>` (the
   components do this). For your own controls: `@include ds.focusable;`,
   never `outline: none` alone. Icon-only controls need `aria-label`.
7. **Motion.** Use `@include ds.motion((properties))`: it only animates
   when the user has not asked for reduced motion. Wrap keyframe
   animations in `@media (prefers-reduced-motion: no-preference)`.
8. **Copy.** Components carry no text of their own except the translated
   "Close" label. Screens pass translated strings; keys go in the
   `# Design Studio (<WP>)` block at the end of `en.po` and `id.po`, with
   the `ds.` prefix (the sprint brief has the rule).
9. **Sizes.** Editor surfaces use `md` controls (32 px) and `body` text
   (12 px). Dashboard, auth and settings use `lg` controls (44 px) and
   `body-lg` (14 px), as on F1 · 08.
10. **Missing something?** Add a variant here (component + showcase + this
    table) in its own small pull request to `ds/main`, instead of a
    one-off copy in a feature folder. Feature-specific components live in
    the feature namespace and build on these.

## 5. Mount seams

A seam is an upstream file we edit. Each one costs a rebase conflict on
every Penpot release, so:

1. **One require, one element.** The upstream file gets a `:require` line
   and one mount expression. Logic, state, markup and styles live in
   `app.main.ui.studio.*`.

   ```clojure
   ;; upstream file (seam)
   [app.main.ui.studio.workspace.shell :refer [shell*]]
   …
   ;; Design Studio seam (STUDIO-SEAMS.md): <why>
   [:> shell* {:file-id file-id}]
   ```

2. **Mark it.** The mount line carries the comment
   `;; Design Studio seam (STUDIO-SEAMS.md): <why>`, so a rebase finds it.
3. **List it.** Add the exact path to the Seams table of
   `STUDIO-SEAMS.md` in the same pull request. CI ("Seams guard") fails
   otherwise. New files under `frontend/src/app/main/ui/studio/**` and
   `frontend/resources/styles/studio/**` need no row.
4. **Replace by wrapping, not by rewriting.** To swap an upstream
   component, mount ours at the call site and leave the upstream component
   in place and unused. Do not delete or reformat upstream code.
5. **Stay out of** `frontend/src/app/main/data/**`, `common/**`,
   `render-wasm/**` and `backend/**`. Studio components read state through
   the existing refs and emit the existing events.
6. **One owner per seam file.** Before editing an upstream file, check the
   Seams table and the open pull requests; if another package owns the
   file, ask that owner to add your mount line.
7. **Namespaces.** One folder per surface, so parallel work does not
   collide:

   | Surface | Namespace | Package |
   | --- | --- | --- |
   | Base components | `app.main.ui.studio.ds.*` | foundation (this) |
   | Workspace shell, panels, toolbars | `app.main.ui.studio.workspace.*` | F3 |
   | Dashboard | `app.main.ui.studio.dashboard.*` | F4 |
   | Auth | `app.main.ui.studio.auth.*` | F4 |
   | Settings | `app.main.ui.studio.settings.*` | F4 |
   | Viewer | `app.main.ui.studio.viewer.*` | F4 |

   Styles are co-located (`thing.cljs` + `thing.scss`); class names are
   scoped per file by the build, so short names (`.header`) are safe.
8. **Lazy modules.** `shadow-cljs.edn` splits the app into modules
   (`main-auth`, `main-dashboard`, `main-workspace`, …). Require a studio
   namespace from the entry of the module it belongs to, not from
   `app.main.ui`, or it lands in the first-load bundle.

The foundation's own seam is `frontend/src/app/main/ui/debug/playground.cljs`:
it mounts the showcase on the debug route `#/debug/playground`. Upstream
registers that route only when `*assert*` is on, so it exists in development
builds and not in the release image; the release build still compiles the
showcase and, through it, every base component.

## 6. Checks

- CI "DS: frontend image" compiles every component through the showcase.
- Local, without a JVM: `sass` for the stylesheets (load paths
  `resources/styles/common`, `resources/styles`, `src/app/main/ui`) and
  `clj-kondo --config-dir ../.clj-kondo --lint src` from `frontend/`.
- Look at the showcase: `/#/debug/playground` in a development build
  (`./manage.sh run-devenv`). A release image does not have the route.

## 7. Open points

- **Fonts.** The faces point at `/start/assets/fonts/`, which the Studio
  app serves in production, not this image. If the editor must be
  self-contained, copy the four woff2 files into the image and change
  `$base-path` in `_ds-fonts.scss`.
- **Input border.** `border.strong` on `bg.panel` is 1.4:1. That is the
  approved F1 · 08 look, but below the 3:1 that WCAG 1.4.11 asks for a
  control boundary. Needs an owner decision (a darker `border.control`
  token) before the auth screens ship.
- **Tooltip** is CSS positioned. A portal-based one is needed if F3 puts
  tooltips inside scrolling or clipped panels.
- **Dark theme.** Only `ds/light` exists. Components already read the
  semantic layer only.
