# mantine-ui-wrapper

[![Clojars Project](https://img.shields.io/clojars/v/io.github.unisoma/mantine-ui-wrapper.svg)](https://clojars.org/io.github.unisoma/mantine-ui-wrapper)
[![Mantine](https://img.shields.io/github/package-json/dependency-version/UniSoma/mantine-ui-wrapper/dev/@mantine/core?label=Mantine&color=339af0)](https://mantine.dev)
[![CI](https://github.com/UniSoma/mantine-ui-wrapper/actions/workflows/ci.yml/badge.svg)](https://github.com/UniSoma/mantine-ui-wrapper/actions/workflows/ci.yml)

Framework-agnostic ClojureScript wrapper of [Mantine](https://mantine.dev). It works
from Fulcro, Reagent/re-frame, UIx, Helix, or raw React interop, and depends only on
`react/createElement`, never on a rendering framework. It wraps the complete Mantine
surface (generated from Mantine's `docgen.json`), and every factory's docstring carries the
component's Mantine.dev URL and full prop table, so you can read it from your editor or via
`(clojure.repl/doc mc/button)`.

Available on Clojars as SNAPSHOT builds while the API settles. There is no stable release yet.

## Coverage

The wrapper exposes the full surface of nine `@mantine/*` packages: every component, hook,
and imperative API. Each package is a ClojureScript namespace; load the matching CSS yourself
(see [Installation](#installation)).

| Package | Namespace | CSS to import |
|---|---|---|
| `@mantine/core` | `mantine.core` | `@mantine/core/styles.layer.css` (load first) |
| `@mantine/hooks` | `mantine.hooks` | none |
| `@mantine/dates` | `mantine.dates` | `@mantine/dates/styles.layer.css` · needs `dayjs` |
| `@mantine/charts` | `mantine.charts` | `@mantine/charts/styles.layer.css` · needs `recharts` |
| `@mantine/notifications` | `mantine.notifications` | `@mantine/notifications/styles.layer.css` |
| `@mantine/spotlight` | `mantine.spotlight` | `@mantine/spotlight/styles.layer.css` |
| `@mantine/dropzone` | `mantine.dropzone` | `@mantine/dropzone/styles.layer.css` |
| `@mantine/modals` | `mantine.modals` | none (styled by `@mantine/core`) |
| `@mantine/form` | `mantine.form` | none (logic only) |

## Installation

```clojure
;; deps.edn
io.github.unisoma/mantine-ui-wrapper {:mvn/version "9.4.1.0-SNAPSHOT"}
```

Versions are `<mantine-version>.N`: the wrapped Mantine version plus a wrapper revision, so
the coordinate tells you exactly which Mantine you get (see
[ADR 0001](docs/adr/0001-clojars-release-process.md)).

The jar ships `deps.cljs` (`:npm-deps` on `@mantine/*`), so shadow-cljs auto-installs the npm
packages. It does not declare `react`/`react-dom`: your app owns React. With other CLJS
tooling, install the npm deps manually:

```
npm install @mantine/core@^9.4.1 @mantine/hooks@^9.4.1 @mantine/notifications@^9.4.1 @mantine/modals@^9.4.1 @mantine/form@^9.4.1 @mantine/spotlight@^9.4.1 @mantine/dates@^9.4.1 @mantine/charts@^9.4.1 @mantine/dropzone@^9.4.1 dayjs@^1.11.21 recharts@^3.9.2 react react-dom
```

### CSS

The wrapper imports no CSS. You load Mantine's stylesheets yourself, using the per-package
files in the [Coverage](#coverage) table. The rules:

- Use the `.layer.css` variant so your app's CSS wins regardless of import order.
- Load `@mantine/core` first, then the other packages.
- Mount `MantineProvider` once at your app root.
- Under shadow-cljs, `(:require ["@mantine/core/styles.css"])` does not work, because shadow
  does not process CSS. `<link>` the files or run a CSS bundling step.

## Usage

Every component is a `def` in its package namespace, named in kebab-case (`Button` →
`mantine.core/button`, `LineChart` → `mantine.charts/line-chart`).

Conventions:

- A factory takes an optional leading props map, then children.
- Props are kebab-case keywords converted to camelCase (`:left-section` → `leftSection`).
- `:styles` and `:class-names` maps convert recursively.
- Hooks return raw JS, unconverted; destructure tuples positionally.
- Polymorphic components accept `:component` (`{:component "a" :href …}`).
- Imperative packages (`notifications`, `modals`, `spotlight`) expose both a provider and
  call functions (`mn/show`, `mm/open`/`mm/close`, `ms/toggle`).
- Every `def`'s docstring carries the Mantine.dev URL and full prop table.

```clojure
(ns my-app
  (:require ["react" :as react]
            ["react-dom/client" :as rdom]
            ["@mantine/core" :refer [MantineProvider]]
            [mantine.core :as mc]
            [mantine.hooks :as mh]
            [mantine.notifications :as mn]))

(defn panel []
  ;; hooks return raw JS: a tuple here, destructured positionally
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

A runnable sample of every pattern lives in
[`demo/mantine/demo.cljs`](demo/mantine/demo.cljs).

## License

MIT. See [`LICENSE`](LICENSE). This project wraps [Mantine](https://mantine.dev), also MIT;
the generated docstrings come from Mantine's documentation.
