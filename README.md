# mantine-ui-wrapper

Framework-agnostic ClojureScript wrapper of [Mantine](https://mantine.dev) (pinned to
**Mantine 9.4.1**), with per-component factories generated from Mantine's `docgen.json`.
Usable from Fulcro, Reagent/re-frame, UIx, Helix, or raw React interop — the wrapper
depends only on `react/createElement`.

Status: **four-pattern proof of concept** — a few core components, the notifications
imperative API, and hooks render end-to-end (see `docs/prototypes/four-pattern-poc.md`).
Widening `codegen/scope.edn` fans the same pipeline out across the rest of Mantine.

## Layout

- `src/main/mantine/*.cljc` — generated wrapper namespaces (one per `@mantine/*` package;
  committed, ship as source). Regenerate with `bb generate` — do not edit.
- `src/main/mantine/impl/` — hand-written internals: `props` (the CLJS→JS props
  converter) and `factory` (variadic factories, controlled-input shim, JVM stubs).
- `codegen/` — the generator (`generate.clj`), its committed inputs (`input/docgen.json`,
  `input/*-docs.edn`, `scope.edn`, `controlled-inputs.edn`) and the hand-written
  supplements hoisted into generated namespaces (`supplements/*.cljc`).
- `demo/` + `public/` — dev-only browser harness.

## Tasks

- `bb generate` — regenerate `src/main/mantine/*.cljc` from the committed inputs.
- `bb extract <mantine-clone>` — refresh the committed inputs on a Mantine version bump
  (clone the pinned tag, `yarn install`, `yarn tsx scripts/docgen`, then extract).
- `npx shadow-cljs watch demo` — dev harness at http://localhost:8090.
- `npx shadow-cljs release demo && node scripts/verify-demo.mjs` — build `:advanced`
  and verify the four patterns in jsdom.

## Consumers

The jar ships `deps.cljs` (`:npm-deps` on `@mantine/*`), so shadow-cljs auto-installs
the npm packages; `react`/`react-dom` are deliberately NOT declared — your app owns
React. With other CLJS tooling, install manually:

```
npm install @mantine/core@^9.4.1 @mantine/hooks@^9.4.1 @mantine/notifications@^9.4.1 react react-dom
```

The wrapper imports **no CSS**. Load Mantine's stylesheets yourself — for bundlers use
the layered variant so your app CSS wins regardless of import order:

```js
import '@mantine/core/styles.layer.css';          // core first
import '@mantine/notifications/styles.layer.css'; // other packages after
```

Note: `(:require ["@mantine/core/styles.css"])` does **not** work under shadow-cljs —
shadow-cljs doesn't process CSS. `<link>` the files or run a CSS bundling step, and
mount `MantineProvider` once at your app root.
