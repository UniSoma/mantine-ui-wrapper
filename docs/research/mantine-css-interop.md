> Research for ticket mnt-01kxe8gzf489 (Mantine CSS + npm interop)

# Mantine v9 CSS loading & shadow-cljs interop

Verified against a shallow clone of `mantinedev/mantine` at **v9.4.1** (`packages/@mantine/*`
`package.json` `exports`) plus the official docs (getting-started, styles, guides) via Context7.

Mantine v7+ dropped Emotion/CSS-in-JS in favor of **static, precompiled native CSS files** shipped
in each npm package. Nothing generates styles at runtime — the consumer must load the CSS files
themselves. This is the whole reason CSS interop is a distinct problem for a cljs wrapper.

---

## 1. Which packages ship CSS and what are the import paths

Each package that ships styles exposes **two** subpath exports plus a per-component glob:

- `@mantine/<pkg>/styles.css` — the full bundled stylesheet, **not** wrapped in a CSS layer.
- `@mantine/<pkg>/styles.layer.css` — identical rules but wrapped in `@layer mantine { … }`.
  It is a **drop-in replacement** for `styles.css` — import one or the other, **never both**.
- `@mantine/core/styles/*` — per-component files, e.g. `@mantine/core/styles/Button.css` and
  `@mantine/core/styles/Button.layer.css` (core only; the `./styles/*` export glob).

Confirmed from `exports` keys in each `package.json` — packages that ship CSS
(`styles.css` + `styles.layer.css`):

| Package | Ships CSS? |
|---|---|
| `@mantine/core` | **yes** (+ `./styles/*` per-component) |
| `@mantine/dates` | **yes** |
| `@mantine/charts` | **yes** |
| `@mantine/notifications` | **yes** |
| `@mantine/spotlight` | **yes** |
| `@mantine/carousel` | yes |
| `@mantine/code-highlight` | yes |
| `@mantine/dropzone` | yes |
| `@mantine/nprogress` | yes |
| `@mantine/tiptap` | yes |
| `@mantine/schedule` | yes |
| **`@mantine/modals`** | **NO** — relies on core's ModalBase styles (already in `@mantine/core/styles.css`) |
| `@mantine/form`, `@mantine/hooks`, `@mantine/store`, `@mantine/emotion`, `@mantine/vanilla-extract`, `@mantine/colors-generator` | NO (logic-only / no visual output) |

So of the packages the ticket asks about: **dates, charts, notifications, spotlight each ship their
own CSS; modals does NOT.** The docs rule of thumb: *"all packages except `@mantine/hooks` require
styles imports"* is close but imprecise — `modals`/`form`/`store` also need none.

**Ordering constraint (plain, non-layer files):** `@mantine/core/styles.css` **must be imported
first**, before any other Mantine package CSS, because the others build on core's variables/resets.

Source note: this is the **source** repo, so no built `*.css` artifacts exist on disk — the
`exports` map is authoritative for what an installed npm package exposes. Authored inputs live at
`core/src/core/MantineProvider/{baseline,default-css-variables,global}.css` + 100 `*.module.css`
component files, all concatenated into the shipped `styles.css` at build time. `sideEffects` is set
to `["*.css"]` so bundlers keep the CSS.

## 2. MantineProvider (app root)

`<MantineProvider>` must wrap the entire app once, at the root. In v7+ **all props are optional**
(verified in `MantineProvider.tsx`):

- `theme?: MantineThemeOverride` — merged over the default theme; omit for defaults.
- `defaultColorScheme?: 'light' | 'dark' | 'auto'` — **default `'light'`**.
- `forceColorScheme?: 'light' | 'dark'` — if set, ignores manager + defaultColorScheme.

**`<ColorSchemeScript />`** is a separate component that must be rendered in the document `<head>`
(SSR) — it injects a tiny inline script that reads the saved scheme from `localStorage`
(key default `'mantine-color-scheme-value'`) and sets `data-mantine-color-scheme` on `<html>`
**before first paint**, preventing a flash. Props: `defaultColorScheme` (default `'light'`),
`localStorageKey`. For a client-only cljs SPA there is no SSR head, so the equivalent is either
rendering `ColorSchemeScript` into the DOM early, or simply letting MantineProvider set the attribute
on mount (accepting a possible one-frame flash). React/Next also use `mantineHtmlProps` on `<html>`;
irrelevant to a client-rendered cljs app.

## 3. PostCSS — required for consumers?

**No.** `postcss-preset-mantine` (+ `postcss-simple-vars` for breakpoint vars) is needed only to
**author** Mantine-flavored CSS (its `rem()`/`em()` functions, `light-dark()` / `hover` / responsive
mixins, `@mantine/breakpoint-*` vars) and to build Mantine itself. Docs state it is *"not strictly
required… highly recommended"* — the CRA guide explicitly shows using Mantine with **plain CSS and no
preset**. The **precompiled `styles.css` a consumer imports has already been through PostCSS** — it is
plain, static CSS with the vendored CSS custom properties baked in. Mantine's theming at runtime is
driven by **CSS variables** (e.g. `--mantine-color-*`, `--mantine-spacing-*`) that `MantineProvider`
injects into the DOM, not by PostCSS.

**Bottom line for this wrapper's downstream consumers:** they need PostCSS **only if they write their
own `.module.css` using Mantine mixins/functions.** Consuming the wrapper + Mantine's shipped CSS
requires **zero PostCSS setup.** The wrapper should not force a PostCSS toolchain on consumers.

## 4. shadow-cljs specifics — getting the CSS in

Core fact: **shadow-cljs does NOT process or bundle CSS.** Its JS layer bundles JavaScript modules
(via its own resolver + Closure); it has no CSS loader/asset pipeline.

**(a) `(:require ["@mantine/core/styles.css"])` in a cljs ns — does NOT work.**
shadow-cljs resolves the string as a JS module. A `.css` file is not JS, so shadow-cljs errors
(cannot parse/convert the file) — it does **not** silently inject a `<link>` or `<style>`. There is
no CSS-import side effect like webpack's `css-loader`/`style-loader`. Do **not** rely on this.

**(b) Link the CSS directly in `index.html` from `node_modules` — simplest, works today.**
```html
<link rel="stylesheet" href="/node_modules/@mantine/core/styles.css">
<!-- add dates/notifications/etc. after core, in dependency order -->
```
Serve `node_modules` (shadow-cljs dev server `:dev-http` can serve the project root, or copy/symlink
the files into the served `public/` dir). Pin nothing extra; the file is already compiled. This is
the pragmatic path for a **demo/dev harness**.

**(c) A separate JS/CSS bundling step — the robust path for shippable consumers.**
Run a real CSS-aware bundler alongside shadow-cljs purely to produce one CSS artifact:
- an `import "@mantine/core/styles.css"` (etc.) entry fed through **esbuild/Vite/webpack**, emitting a
  single `mantine.css` you `<link>` in the page, **or**
- concatenate the package `styles.css` files with a tiny build script (they're static). This gives
  content-hashing, correct import ordering, and works in production hosting where `node_modules`
  isn't served.

**Recommendation:**
- **Demo harness:** option (b) — `<link>` the handful of `styles.css` files from `node_modules` in
  `index.html`, core first. Fast, no extra tooling, matches how the shadow-cljs dev server already
  serves assets.
- **Downstream consumers:** document both, recommend (c) for anything production — either a small
  esbuild CSS entry or their existing bundler. Explicitly warn them that `(:require [... ".css"])`
  will not work under shadow-cljs. The wrapper itself should **not** import any CSS; it should stay
  CSS-agnostic and leave loading to the app (so consumers control ordering, layers, and which
  optional packages' CSS they pull in).

## 5. Gotchas

- **Layers vs plain, pick one globally.** `styles.layer.css` wraps everything in `@layer mantine`.
  Rules in a layer always lose to non-layered rules **regardless of source order**, so app/module CSS
  overrides Mantine even if imported *before* it — this solves import-order fragility. But **never mix
  `styles.css` and `styles.layer.css`** (double rules, unpredictable precedence). If any Mantine pkg
  is loaded as `.layer.css`, load them **all** that way.
- **Plain (`styles.css`) import order matters.** `@mantine/core` first; other packages after; **your
  app CSS last** so it wins. Wrong order = Mantine overrides your styles.
- **Per-component ordering.** If importing granular files, a dependent's CSS must come after its base,
  e.g. `UnstyledButton.css` before `Button.css`.
- **Third-party libs with their own styles:** layers let you order `@layer` blocks to control
  precedence between Mantine and other CSS sources.
- **No tree-shaking of the bundled file.** `styles.css` is monolithic — importing it ships every
  component's CSS whether used or not. To trim, import only the per-component `@mantine/core/styles/*`
  files you use (core only; other packages ship a single bundle). For a wrapper exposing the whole
  surface, the monolithic file is the sane default; per-component trimming is a consumer-side
  optimization, not something to bake into the wrapper.
- **Color-scheme flash:** without an early `ColorSchemeScript`/attribute set, a client-rendered SPA
  can flash the default (light) scheme for one frame before MantineProvider applies the stored scheme.
