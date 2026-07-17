# mantine-ui-wrapper

[![Clojars Project](https://img.shields.io/clojars/v/io.github.unisoma/mantine-ui-wrapper.svg)](https://clojars.org/io.github.unisoma/mantine-ui-wrapper)
[![CI](https://github.com/UniSoma/mantine-ui-wrapper/actions/workflows/ci.yml/badge.svg)](https://github.com/UniSoma/mantine-ui-wrapper/actions/workflows/ci.yml)

Framework-agnostic ClojureScript wrapper of [Mantine](https://mantine.dev) (pinned to
**Mantine 9.4.1**), with per-component factories generated from Mantine's `docgen.json`.
Usable from Fulcro, Reagent/re-frame, UIx, Helix, or raw React interop. The wrapper
depends only on `react/createElement`.

Status: **full Mantine 9.4.1 surface**, published to Clojars. The pipeline covers all
seven wrapped packages: `@mantine/core` (188 components), `dates` (32), `charts` (18),
`hooks` (80 `use*` hooks), plus `modals`, `notifications`, and `spotlight` (their
components and imperative APIs). The only behavioral check is a single jsdom seam over
the `:advanced` release bundle; there is no unit-test suite, and pixel paint is not
asserted in CI (see [Continuous integration](#continuous-integration)).

## Installation

Published to Clojars as **`io.github.unisoma/mantine-ui-wrapper`**. The version is
Mantine-anchored: `9.4.1.N`, where the first three segments are the wrapped Mantine
version and `N` is the wrapper revision against it (see
[`docs/adr/0001-clojars-release-process.md`](docs/adr/0001-clojars-release-process.md)).

```clojure
;; deps.edn
io.github.unisoma/mantine-ui-wrapper {:mvn/version "9.4.1.0-SNAPSHOT"}
```

The jar ships `deps.cljs` (`:npm-deps` on `@mantine/*`), so shadow-cljs auto-installs
the npm packages; `react`/`react-dom` are deliberately NOT declared. Your app owns
React. With other CLJS tooling, install manually:

```
npm install @mantine/core@^9.4.1 @mantine/hooks@^9.4.1 @mantine/notifications@^9.4.1 @mantine/modals@^9.4.1 @mantine/form@^9.4.1 @mantine/spotlight@^9.4.1 @mantine/dates@^9.4.1 @mantine/charts@^9.4.1 @mantine/dropzone@^9.4.1 dayjs@^1.11.21 recharts@^3.9.2 react react-dom
```

The wrapper imports **no CSS**. Load Mantine's stylesheets yourself. For bundlers, use
the layered variant so your app CSS wins regardless of import order:

```js
import '@mantine/core/styles.layer.css';          // core first
import '@mantine/notifications/styles.layer.css'; // other packages after
import '@mantine/spotlight/styles.layer.css';
import '@mantine/dates/styles.layer.css';
import '@mantine/charts/styles.layer.css';
import '@mantine/dropzone/styles.layer.css';
```

`@mantine/modals` ships **no CSS** (its modals are styled by `@mantine/core`), so there is
nothing extra to link for it. `@mantine/spotlight` ships its own stylesheet; link it after
core like the other packages.

Note: `(:require ["@mantine/core/styles.css"])` does **not** work under shadow-cljs,
which doesn't process CSS. `<link>` the files or run a CSS bundling step, and
mount `MantineProvider` once at your app root.

## Usage

Every component is a `def` in its package namespace, named in kebab-case (`Button` →
`mantine.core/button`, `LineChart` → `mantine.charts/line-chart`). A factory takes an
optional **leading props map** followed by children; props are kebab-case keywords
converted to Mantine's camelCase (`:left-section` → `leftSection`), and `:styles` /
`:class-names` maps are converted recursively. Mount `MantineProvider` once at the root.

```clojure
(ns my-app
  (:require ["react" :as react]
            ["react-dom/client" :as rdom]
            ["@mantine/core" :refer [MantineProvider]]
            [mantine.core :as mc]
            [mantine.hooks :as mh]
            [mantine.notifications :as mn]))

(defn panel []
  ;; hooks return raw JS, unconverted: a tuple here (destructured positionally)
  (let [[opened handlers] (mh/use-disclosure false)]
    (mc/stack {:gap "md" :p "xl"}
      (mc/button
        {:color "teal"
         :left-section "🔔"
         :styles {:root {:font-weight 900}}
         :on-click (fn [_] (mn/show {:title "It works" :message "Hello from Mantine"}))}
        "Notify")
      ;; polymorphic component: renders an <a>
      (mc/button {:component "a" :href "https://mantine.dev" :variant "outline"} "Docs")
      (mc/button {:variant "light" :on-click (fn [_] (.toggle ^js handlers))}
        (if opened "Hide" "Show"))
      (mc/collapse {:expanded opened} (mc/text "Toggled content")))))

(defn app []
  (react/createElement
    MantineProvider #js {}
    (mn/provider {:position "top-right"})   ; imperative-API packages provide a provider
    (react/createElement panel)))

(defn init []
  (.render (rdom/createRoot (js/document.getElementById "app"))
           (react/createElement app)))
```

Each generated `def` carries a docstring with the component's Mantine.dev URL and its
full 9.4.1 prop table, readable from your editor or via `(clojure.repl/doc mc/button)`.
Imperative packages (`notifications`, `modals`, `spotlight`) expose both a provider and
call functions (`mn/show`, `mm/open`/`mm/close`, `ms/toggle`). Every namespace also loads
on the JVM (factories throw a named error at call time) so tooling like clj-kondo and
cljdoc works. A runnable end-to-end sample of all patterns lives in
[`demo/mantine/demo.cljs`](demo/mantine/demo.cljs).

## Layout

- `src/main/mantine/*.cljc`: generated wrapper namespaces (one per `@mantine/*` package;
  committed, ship as source). Regenerate with `bb generate`; do not edit.
- `src/main/mantine/impl/`: hand-written internals: `props` (the CLJS→JS props
  converter) and `factory` (variadic factories, controlled-input shim, JVM stubs).
- `codegen/`: the generator (`generate.clj`), its committed inputs (`input/docgen.json`,
  `input/*-docs.edn`, `scope.edn`, `controlled-inputs.edn`) and the hand-written
  supplements hoisted into generated namespaces (`supplements/*.cljc`).
- `demo/` + `public/`: dev-only browser harness.

## Tasks

- `bb generate`: regenerate `src/main/mantine/*.cljc` from the committed inputs.
- `bb extract <mantine-clone>`: refresh the committed inputs on a Mantine version bump
  (clone the pinned tag, `yarn install`, `yarn tsx scripts/docgen`, then extract).
  See [`docs/version-bump.md`](docs/version-bump.md) for the full bump procedure.
- `npx shadow-cljs watch demo`: dev harness at http://localhost:8090.
- `npx shadow-cljs release demo && node scripts/verify-demo.mjs`: build `:advanced`
  and verify the wrapping patterns in jsdom.
- `bb ci`: run the full CI pipeline locally (same commands as CI, see below).

## Continuous integration

`.github/workflows/ci.yml` runs on every push/PR; each step is a plain command that runs
identically locally (`bb ci` chains them). There is no test framework; the release-bundle
jsdom harness is the single behavioral seam, and the generator-side checks are bb tasks:

1. `npm ci`: install deps.
2. `bb drift`: regenerate and fail on any git diff vs the committed generated sources.
3. `bb build`: `:advanced` release build; fails on any shadow-cljs warning (the JVM
   `sun.misc.Unsafe` deprecation lines are not build warnings and are ignored).
4. `node scripts/verify-demo.mjs`: jsdom verification of the release bundle.
5. `bb jvm-load`: JVM-load every generated namespace, sampling the call-time named throw.
6. `bb coverage`: per-package def-count coverage comparing scoped docgen entries vs
   generated defs, failing if a scope/resolution bug silently dropped components.

**Manual caveat:** pixel paint (that the linked stylesheets visually apply) is NOT asserted
in CI. jsdom has no layout/paint engine. Check it in a real browser via
`npx shadow-cljs watch demo`.

## Releasing

Build and publish are `tools.build` + `deps-deploy`, wrapped as `bb` tasks. See
[`docs/release.md`](docs/release.md) for the full procedure.

## License

MIT. See [`LICENSE`](LICENSE). This project wraps [Mantine](https://mantine.dev)
(also MIT); generated docstrings are derived from Mantine's documentation.
