---
id: mnt-01kxgy8apnws
title: Fan out the wrapping pipeline to full Mantine 9.4.1 coverage
status: open
type: feature
priority: 2
mode: afk
created: '2026-07-14T18:29:30.965520099Z'
updated: '2026-07-14T18:41:58.292316748Z'
---

## Description

## Problem Statement

The wrapping pipeline is proven but the wrapper is not usable. The four-pattern PoC (map mnt-01kxe8fz1ert, closed) locked the design and verified generator, impl namespaces, supplement hoisting, and CSS pairing end-to-end — but only on a deliberate subset: 8 core components, 3 hooks, and the notifications imperative API. A ClojureScript developer who adds `mantine-ui-wrapper` today cannot build a real app: most of `@mantine/core` is missing, there are no date pickers or charts, no modals or spotlight, only 3 of ~81 hooks, no test/CI safety net guarding the generated output, and no documented way to move to a newer Mantine version.

## Solution

Widen the existing, unchanged pipeline to full Mantine 9.4.1 coverage — the "mechanical fan-out" the map explicitly deferred. Every locked decision (props-conversion, codegen conventions, hooks pattern, imperative-API supplement shape, build tooling) stands as-is; this work only widens scope and adds the two remaining operational pieces the map listed as not-yet-specified: a testing/CI strategy for the generated wrappers and a documented version-bump procedure.

Delivered as five slices, each ending green on the existing verification loop (`bb generate` → `npx shadow-cljs release demo` → `node scripts/verify-demo.mjs`):

1. Full `@mantine/core` component coverage.
2. `@mantine/dates` + `@mantine/charts` (with their `dayjs` / `recharts` peers).
3. `@mantine/modals` + `@mantine/spotlight` — codegen'd components plus hand-written supplements using the standalone-exports pattern from the PoC findings (not the singleton object).
4. All `use*` hooks from the `@mantine/hooks` barrel.
5. Testing/CI harness for the generated wrappers + the documented version-bump procedure.

## User Stories

1. As a CLJS app developer, I want every `@mantine/core` component available as a kebab-case factory in `mantine.core`, so that I can build complete UIs without falling back to raw interop.
2. As a CLJS app developer, I want compound sub-components (Button.Group, Table.Tr, Menu.Item, …) wrapped wherever docgen exposes them as named exports, so that composite Mantine patterns work from CLJS.
3. As a CLJS app developer, I want polymorphic `:component` and section props (`:left-section` etc.) to work on every generated component exactly as they did in the PoC, so that the conversion semantics are uniform across the whole surface.
4. As a CLJS app developer, I want every generated component to carry a rich docstring (description, mantine.dev URL, docgen prop table), so that I can discover props from my editor without opening the Mantine docs.
5. As a CLJS app developer, I want `mantine.dates` with all date/time pickers and calendar components, so that I can build forms with date input.
6. As a CLJS app developer, I want `dayjs` declared as a shipped npm dep alongside `@mantine/dates`, so that shadow-cljs auto-installs everything the dates package needs.
7. As a CLJS app developer, I want `mantine.charts` with all chart components, so that I can render Mantine charts from CLJS.
8. As a CLJS app developer, I want `recharts` declared as a shipped npm dep alongside `@mantine/charts`, so that charts render without me chasing peer-dependency errors.
9. As a CLJS app developer, I want `mantine.modals` with the full imperative surface (`open`, `open-confirm-modal`, `open-context-modal`, `close`, `close-all`, `update-modal`, `update-context-modal`), so that I can drive modals imperatively with converted option maps.
10. As a CLJS app developer, I want a `modals-provider` component and a `provider` alias in `mantine.modals`, so that mounting the modals system reads the same as notifications.
11. As a CLJS app developer, I want context-modal registry keys passed verbatim and `innerProps` passed raw, so that my modal registry round-trips exactly as the imperative-API decision specifies.
12. As a CLJS app developer, I want `mantine.spotlight` with the imperative surface (`open`, `close`, `toggle`) plus the codegen'd Spotlight components, so that I can add a command palette.
13. As a CLJS app developer, I want the reactive hooks `use-notifications`, `use-modals`, and `use-spotlight` exposed from their package namespaces as raw-passthrough aliases, so that I can subscribe to imperative-API state.
14. As a CLJS app developer, I want all `use*` hooks from the `@mantine/hooks` barrel wrapped in `mantine.hooks`, so that the full hooks toolkit is one `:require` away.
15. As a CLJS app developer, I want every hook to return its raw JS value (tuples destructured positionally, objects via interop), so that behavior matches the CLJS ecosystem norm the hooks decision locked.
16. As a CLJS app developer, I want every hook docstring to carry the extracted description and its mantine.dev URL, so that hook discovery works from the editor.
17. As a CLJS app developer, I want every value/onChange input across core and dates covered by the controlled-input shim curation, so that controlled inputs work uniformly regardless of the underlying onChange signature.
18. As a CLJS app developer, I want the README to state which packages need a CSS `<link>` (and that modals ships none), so that I can wire styles for any subset of packages I use.
19. As a CLJS app developer on the JVM (SSR, tests, Fulcro server-side), I want every generated namespace to load on the JVM and throw a named not-implemented error only at call time, so that requiring the wrapper never breaks a JVM build.
20. As a wrapper maintainer, I want the generation scope to default to everything docgen knows about in the wrapped packages (with an explicit exclude list) instead of a hand-enumerated allowlist, so that fan-out and future version bumps don't require maintaining a 200-name set.
21. As a wrapper maintainer, I want within-package kebab-name collisions and unresolvable exports to keep their PoC behavior (hard-error and log+skip respectively) at full scale, so that widening scope can't silently emit broken defs.
22. As a wrapper maintainer, I want CI to fail when `bb generate` produces a diff against the committed generated sources, so that generated code can never drift from the committed inputs.
23. As a wrapper maintainer, I want CI to fail when the `:advanced` release build emits any warning, so that interop regressions (missing `^js` hints etc.) are caught on every change.
24. As a wrapper maintainer, I want CI to run the jsdom verification against the real release bundle, so that behavioral regressions in the converter, factory, shim, supplements, or hooks are caught without a browser.
25. As a wrapper maintainer, I want CI to load all generated namespaces on the JVM, so that reader-conditional or emission bugs surface immediately.
26. As a wrapper maintainer, I want a cheap coverage check comparing generated def counts against scoped docgen entries per package, so that a scope or resolution bug can't silently drop components.
27. As a wrapper maintainer, I want a documented, step-by-step version-bump procedure (refresh inputs from a Mantine clone, bump pins, regenerate, verify), so that moving to Mantine 9.5/10 is a scripted afternoon, not archaeology.
28. As an agent picking up this work, I want each slice independently verifiable by the same three-command loop, so that I can land slices incrementally with confidence.

## Implementation Decisions

All locked decisions from map mnt-01kxe8fz1ert carry over unchanged — the props converter, factory conventions, generated-file shape, hooks pattern, supplement hoisting, require sugar, and CSS delivery are not revisited here. Decisions specific to the fan-out:

- **Scope model inverts from allowlist to everything-minus-excludes.** The generation scope config moves from enumerating component/hook names to covering every docgen entry belonging to a wrapped package and every `use*` export in the hooks barrel, with an explicit exclude set for deliberate omissions. Existing guards (collision hard-error, unresolvable-export log+skip) are the safety net at scale. This makes version bumps pick up new components automatically, surfaced by generated-diff review.
- **Four new packages installed and wrapped**: dates, charts, modals, spotlight — each pinned exact 9.4.1 in devDependencies and added caret-ranged to the shipped npm-deps declaration, producing generated namespaces `mantine.dates`, `mantine.charts`, `mantine.modals`, `mantine.spotlight` (one ns per package, no aggregate, per the codegen decision).
- **Peer deps `dayjs` and `recharts` are shipped npm-deps too** (at the ranges Mantine 9.4.1 declares) and pre-declared exact in our devDependencies, mirroring the `@mantine/*` treatment. React/react-dom remain unshipped — the split-context rationale only applies to React itself.
- **Modals and spotlight supplements use the standalone-exports pattern**, binding on the PoC finding: refer the standalone functions (`openModal`, `openConfirmModal`, `openContextModal`, `closeModal`, `closeAllModals`, `updateModal`, `updateContextModal`; `openSpotlight`, `closeSpotlight`, `toggleSpotlight`) rather than the `modals`/`spotlight` singleton objects, which collide with same-named generated component defs in the merged namespace. Public fn names follow the imperative-API decision 1:1 kebab-cased; option maps go through the public converter, bare ids stay raw both directions; fixed single-arg arities; single-instance only.
- **Provider aliasing follows the PoC finding**: `(declare <component>)` + `(def provider <component>)` in the supplement, compiling warning-free both standalone and hoisted. Modals gets `modals-provider` (its faithful codegen'd name) plus the alias; spotlight needs no provider (its component is the UI) so its supplement carries only the imperative fns and `use-spotlight`.
- **Reactive hooks** (`use-notifications`, `use-modals`, `use-spotlight`) are raw-passthrough def-aliases in their package supplements, no provider-absence guarding, per the imperative-API decision.
- **Controlled-input curation widens** to the full set of value/onChange inputs across core and dates. The PoC's shape-agnostic shim (DOM-event `.target` vs bare value) already covers both signature families, so widening is curation-file work plus rot-logging review, not shim changes.
- **Committed inputs refresh as needed via the existing extraction task** if dates/charts/spotlight/modals descriptions show gaps — the extractor already reads the extension-package MDX data file (PoC finding), so this is a re-run, not new extraction code.
- **Demo harness widens per slice** with representative usages of each new package (and CSS `<link>`s for dates/charts/spotlight; modals ships no CSS), keeping it the single executable specification the verify script asserts against.
- **CI is a single pipeline** running on every change: install deps, regenerate and fail on git diff (drift check), release-build the demo failing on any warning, run the jsdom verification, JVM-load all generated namespaces, and run the def-count coverage check. Same commands locally and in CI — the loop is the contract.
- **Version-bump procedure is documented in the repo docs**: clone the target Mantine tag → install and run its docgen script → run the input-extraction task against the clone → bump the pinned versions (package manifest, shipped npm-deps, the generator's version constant) → regenerate → run the verification loop → review the generated diff for new/removed/renamed components and new hooks (which the everything-minus-excludes scope picks up automatically). The PoC measured the refresh at roughly a minute of compute.
- **Slice order**: full core → dates+charts → modals+spotlight → hooks → testing/CI + version-bump doc. Each slice lands independently green; the CI slice formalizes the loop the first four slices already ran by hand.

## Testing Decisions

- **One seam, the highest available**: the `:advanced` release bundle executed in jsdom by the existing verify script. Tests assert externally observable behavior — rendered DOM, Mantine class presence, event flow, imperative-API effects — never generator internals or emitted-source text. The demo harness is the fixture; the verify script is the assertion suite. Prior art: the PoC's 13/13 assertions and the build-tooling PoC verification.
- **No per-component tests.** Generated defs are mechanical output of one code path; the pipeline is the unit under test. Per new package, the verify script gains representative assertions: at least one codegen'd component rendering with its hashed Mantine classes, the imperative fns driving visible DOM (modal opens/closes, spotlight toggles), one controlled input from the newly curated set, and hooks sampled across the return-shape split (tuple, object, scalar).
- **Generator-side checks ride CI, not a test framework**: idempotency/drift (regenerate must produce no diff), zero release-build warnings, JVM-loadability of every generated ns (with the call-time named throw sampled), and the def-count coverage check per package.
- **CSS pairing assertions extend to new packages**: rendered hashed classes must exist as selectors in the corresponding linked stylesheet, as the PoC established for core and notifications.
- **Pixel paint stays a manual caveat** — no browser in CI; jsdom verifies structure and behavior, an occasional real-browser eyeball verifies paint, as documented in both PoCs.

## Out of Scope

- Clojars publishing, release process, versioning.
- Multi-instance imperative stores (createNotificationsStore / createSpotlight / the trailing `store` arg) — v1 wraps the default singletons only.
- Deep conversion for nested `*Props` config maps (cancelProps/confirmProps/searchProps, spotlight actions) — shallow converter semantics stand until proven insufficient.
- Styles API selector / CSS-variable extraction from the styles-api data files.
- Non-hook `@mantine/hooks` barrel utilities (randomId, getHotkeyHandler, mergeRefs, …).
- Generating specs/malli from prop metadata.
- Supporting multiple Mantine majors simultaneously; community extensions.

## Further Notes

- Source of truth for all inherited decisions: closed map mnt-01kxe8fz1ert (its Decisions-so-far section carries every resolution) and the four-pattern PoC findings doc under docs/prototypes/.
- PoC findings that bind this work: standalone-exports supplements (finding 2), declare-then-def provider alias (finding 3), shape-agnostic controlled shim (finding 4), extension MDX data location (finding 5), cheap input refresh (finding 1).
- The hooks input already carries 81 descriptions and the component docs input ~150 entries including extension packages, so slices 2–4 likely need no input refresh — treat a refresh as contingency, not a planned step.
- If Mantine 9.4.1's docgen surfaces components whose packages aren't installed/wrapped, they fall out of scope naturally (scope is keyed to wrapped packages), not via the exclude list.
