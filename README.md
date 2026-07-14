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
- `bb ci` — run the full CI pipeline locally (same commands as CI, see below).

## Continuous integration

`.github/workflows/ci.yml` runs on every push/PR; each step is a plain command that runs
identically locally (`bb ci` chains them). There is no test framework — the release-bundle
jsdom harness is the single behavioral seam, and the generator-side checks are bb tasks:

1. `npm ci` — install deps.
2. `bb drift` — regenerate and fail on any git diff vs the committed generated sources.
3. `bb build` — `:advanced` release build; fails on any shadow-cljs warning (the JVM
   `sun.misc.Unsafe` deprecation lines are not build warnings and are ignored).
4. `node scripts/verify-demo.mjs` — jsdom verification of the release bundle.
5. `bb jvm-load` — JVM-load every generated namespace, sampling the call-time named throw.
6. `bb coverage` — per-package def-count coverage: scoped docgen entries vs generated defs,
   failing if a scope/resolution bug silently dropped components.

**Manual caveat:** pixel paint (that the linked stylesheets visually apply) is NOT asserted
in CI — jsdom has no layout/paint engine. Check it in a real browser via
`npx shadow-cljs watch demo`.

## Consumers

The jar ships `deps.cljs` (`:npm-deps` on `@mantine/*`), so shadow-cljs auto-installs
the npm packages; `react`/`react-dom` are deliberately NOT declared — your app owns
React. With other CLJS tooling, install manually:

```
npm install @mantine/core@^9.4.1 @mantine/hooks@^9.4.1 @mantine/notifications@^9.4.1 @mantine/modals@^9.4.1 @mantine/spotlight@^9.4.1 @mantine/dates@^9.4.1 @mantine/charts@^9.4.1 dayjs@^1.11.21 recharts@^3.9.2 react react-dom
```

The wrapper imports **no CSS**. Load Mantine's stylesheets yourself — for bundlers use
the layered variant so your app CSS wins regardless of import order:

```js
import '@mantine/core/styles.layer.css';          // core first
import '@mantine/notifications/styles.layer.css'; // other packages after
import '@mantine/spotlight/styles.layer.css';
import '@mantine/dates/styles.layer.css';
import '@mantine/charts/styles.layer.css';
```

`@mantine/modals` ships **no CSS** (its modals are styled by `@mantine/core`), so there is
nothing extra to link for it. `@mantine/spotlight` ships its own stylesheet — link it after
core like the other packages.

Note: `(:require ["@mantine/core/styles.css"])` does **not** work under shadow-cljs —
shadow-cljs doesn't process CSS. `<link>` the files or run a CSS bundling step, and
mount `MantineProvider` once at your app root.
