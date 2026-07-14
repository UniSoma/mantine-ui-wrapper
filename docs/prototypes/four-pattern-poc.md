> Destination proof for ticket mnt-01kxe8h04teh (Build the four-pattern PoC)

# Four-pattern PoC — the pipeline, built and verified

Unlike the throwaway build-tooling PoC, this one **is** the repo: the real generator,
impl namespaces, committed inputs, and demo harness, all landed on the locked design
(props-conversion, codegen, hooks, imperative-API, build-tooling decisions).
Verified against Mantine 9.4.1, shadow-cljs 2.28.x, React 19, node 24, Java 25.

## What was built

- **Committed inputs** (`codegen/input/`): the real `docgen.json` (291 entries, 1.5 MB,
  generated from a Mantine 9.4.1 clone), `component-docs.edn` (150 descriptions +
  polymorphic flags + slugs from `mdx-{core,dates,charts,others}-data.ts`),
  `hook-docs.edn` (81 hook descriptions). Refresh is scripted: `bb extract <clone>`.
- **Generator** (`bb generate`, plain-Clojure babashka): scope-filtered docgen entries →
  one `.cljc` ns per package (`mantine.core`, `mantine.hooks`, `mantine.notifications`).
  Kebab defs, collision hard-error, auto `(:refer-clojure :exclude ...)` (sees supplement
  names — `update` is excluded in `mantine.notifications`), rich docstrings
  (description + mantine.dev URL + docgen prop table), controlled-input curation with
  rot-logging, supplement require-merge + verbatim form hoisting (edamame with
  `:read-cond :preserve`), package/export resolution via `node -e Object.keys(require(...))`.
- **Impl namespaces**: `mantine.impl.props` (the settled converter + children path),
  `mantine.impl.factory` (variadic factory, `controlled` shim, `not-implemented` JVM stubs).
- **Demo harness**: raw-React demo (`demo/mantine/demo.cljs`), `public/index.html`
  linking plain CSS from `node_modules` (core first, notifications after).

## Verification results

- `:advanced` release: **871 files, 0 warnings**.
- Generated nses **load on the JVM** (bb): call-time throw names the wrapper, docstrings
  readable via `(meta #'mantine.hooks/use-disclosure)`.
- `bb generate` is **idempotent** (checksums stable across re-runs).
- **13/13 jsdom assertions pass** executing the real `:advanced` bundle
  (`node scripts/verify-demo.mjs`):
  - codegen'd Button: Mantine classes, `:class-names` space-joined onto root,
    `:styles {:root {:font-weight 900}}` → camelCased inline style, `--poc-var`
    verbatim, `:left-section` kebab→camel section content;
  - polymorphic `:component "a"` renders an anchor; Badge as `"span"`; children from a
    lazy seq flattened;
  - controlled TextInput: renders external `:value`, typing flows shim → onChange →
    state → back in;
  - `use-disclosure` raw tuple destructured positionally; `.toggle` re-renders;
  - `mantine.notifications/show` (converted options map) renders a notification through
    the codegen'd + supplement-hoisted provider.
- **CSS pairing confirmed**: every hashed class the bundle renders (`m_87cf2631` … on
  Button, `m_5ed0edd0` … on Notification) exists as a selector in the linked
  `@mantine/core/styles.css` / `@mantine/notifications/styles.css`.
- **Pixel paint still not browser-verified** (no Chromium in this sandbox — same caveat
  as the build-tooling PoC). `npx shadow-cljs watch demo` + a real browser is the
  remaining eyeball check.

## Findings & corrections

1. **Input refresh is cheap, not heavy.** The docgen research called a full monorepo
   install "heavy/slow"; in practice `yarn install` took ~44 s and `tsx scripts/docgen`
   under a minute. Version bumps are a low-cost scripted step (`bb extract`).
2. **Singleton `:refer` collides with the provider def — use the standalone exports.**
   The build-tooling require-sugar (`:refer [notifications useNotifications]`) collides
   with the generated `notifications` component def inside the merged ns. The supplement
   refers the standalone fns instead (`showNotification`, `hideNotification`, …) — 1:1,
   no `:rename` needed. Pattern for modals/spotlight supplements too.
3. **Provider alias via `(declare notifications)` + `(def provider notifications)`**
   compiles warning-free standalone and hoisted (CLJS `declare` after `def` is a no-op).
4. **The controlled shim reads change values shape-agnostically** (DOM-event `.target`
   vs bare value) so the same shim covers TextInput-style and Select/NumberInput-style
   `onChange` signatures.
5. **Extension-package MDX data lives in `mdx-others-data.ts`** (not just
   core/dates/charts) and keys `Notifications` directly — extraction now includes it,
   so provider components get real descriptions.
