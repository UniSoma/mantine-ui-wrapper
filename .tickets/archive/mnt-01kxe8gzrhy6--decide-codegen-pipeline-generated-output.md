---
id: mnt-01kxe8gzrhy6
title: 'Decide: codegen pipeline & generated output'
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-13T17:31:17.137540377Z'
updated: '2026-07-13T20:31:46.195995038Z'
closed: '2026-07-13T20:31:46.195995038Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:grilling
deps:
- mnt-01kxe8gzn6bt
- mnt-01kxe8gzbttp
assignee: jonas.rodrigues@unisoma.com
---

## Description

## Question

Design the generator: read Mantine's docgen.json -> emit rich per-component factories carrying prop metadata. Decide namespace layout (one ns per component? per package?), factory naming conventions (kebab-case), cross-package name-collision handling, how metadata/docstrings are exposed, and how the generic props converter (see props-conversion ticket) is wired in.

## Notes

**2026-07-13T17:37:58.612849725Z**

Research context (R2/docgen): docgen.json is keyed by component display name -> {props:{...}} only; it DROPS component descriptions/methods and all style-system/polymorphic props, and omits export-location info. So (a) generated docstrings can't include a component description from docgen (see map 'Not yet specified' — decide the docstring source here), and (b) the per-component prop metadata will be the FILTERED set; the generic style-system/polymorphic props come from the props-conversion layer, not per-component. Output path: apps/mantine.dev/src/.docgen/docgen.json (gitignored). Generator: mantine-docgen-script@^1.6.0 (react-docgen-typescript).

**2026-07-13T19:20:36.530989444Z**

Handed off from Decide: props-conversion semantics (mnt-01kxe8gzn6bt, now closed). Two concerns that surfaced there but belong to THIS codegen ticket, not the generic converter:

1. CONTROLLED-INPUT WRAPPING — SUIW's wrap-form-element analogue. Text inputs (TextInput, Textarea, Select, NumberInput, ...) need a behavioral wrapper so the cursor doesn't jump on re-render. This is keyed to a CURATED component set (metadata won't tell us which), layered AROUND the converter at factory-generation time — decide the curated set + wrapping mechanism here.

2. FACTORY CALL CONVENTION — whether a factory allows omitting the props map, e.g. (ui-button "Click") vs (ui-button {} "Click"). Arity/first-arg-is-map detection lives in the generated factory, not the converter. Decide here.

The converter itself is settled: one public fn, see mnt-01kxe8gzn6bt resolution.

**2026-07-13T20:31:46.098722744Z**

Resolution — codegen pipeline & generated output

GENERATED OUTPUT
1. Runtime payload: docstring-ONLY into runtime for v1. Parsed prop metadata stays generator-side; emitting runtime metadata later (malli/specs/validation — all Not-yet-specified) is a one-line change to the emit step.
2. Structure: ONE ns per package (mantine.core / mantine.dates / mantine.charts). Each component a `def`; per-component named-export requires (["@mantine/core$Button" :as Button]) so :advanced DCE tree-shakes per component. NO aggregate/import-everything ns (avoids the SUIW :advanced-bloat warning).
3. Grouping source: package + the require string both resolved from installed node_modules exports (pinned 9.4.1) — docgen.json has no location info. One lookup yields target ns AND require string. Unresolvable components logged + skipped, never silently dropped.
4. Naming: no prefix, kebab-case (core/button, core/text-input). Within-package kebab collisions = HARD ERROR (fail, don't overwrite). Generator auto-derives (:refer-clojure :exclude [...]) per ns for clojure.core shadowers (list, text, ...). Cross-package same-names are distinct qualified symbols (per-package ns bought this for free).
5. Docstrings: prop-list (docgen name/type/defaultValue/required/description) + best-effort derived docs URL (mantine.dev/<pkg>/<slug>). Slug mismatches -> logged, non-load-bearing.

WIRING & FORMAT
6. Wiring: thin generated public nses -> hand-written impl. Generated files are just requires + one `(def name "<doc>" #?(:clj ... :cljs (f/factory Component)))` line each. Real logic in mantine.impl.props (the settled converter) and mantine.impl.factory (factory helper + controlled wrapping). ALL non-end-user code under mantine.impl.*. Factory = variadic `def` (not a macro). Concrete framework-agnostic react/createElement require form -> deferred to build-tooling.
7. Format: generated nses are .cljc, JVM-loadable. :cljs = real factory; :clj = (f/not-implemented "<name>") — a call-time throw naming the component (loads fine for clj-kondo/cljdoc/JVM tests; throws only when CALLED on the JVM). Docstring shared across branches. mantine.impl.factory is .cljc (:clj exposes only not-implemented, no JS); mantine.impl.props is effectively CLJS (referenced only from the :cljs branch).
8. Controlled inputs: REAL wrapper in v1 — one minimal hand-written React shim in mantine.impl.factory (controlled/uncontrolled value + onChange, nothing more, not a general form abstraction), applied to a curated EDN set in the generator (starts with @mantine/core text inputs: TextInput, Textarea, Select, NumberInput, PasswordInput, Autocomplete, ...). Names absent from resolved components are logged so the set can't silently rot.
9. Call convention: optional props via map? first-arg detection. (core/button "Click") and (core/button {:c :red} "Click") both work; first arg map? -> props, else props={} and all args are children. Lives inside the variadic factory fn.

GENERATOR
10. Input contract: COMMIT a pinned docgen.json into the repo as the generator input (+ installed node_modules the other input). Codegen becomes a pure reproducible transform, runnable in CI with NO Mantine clone / TS toolchain. Refresh = documented per-version-bump step (clone Mantine 9.4.1 -> tsx scripts/docgen -> copy the gitignored output).
11. Runner: generator is headless / CI-runnable / version-pinned / single entry point / plain Clojure. Exact runner mechanism (bb task vs clj -X alias) handed to the build-tooling ticket, LEANING bb.

HANDOFFS to Decide: build tooling, npm interop & CSS delivery (mnt-01kxe8gzvp9e):
- Concrete framework-agnostic react/createElement require form used by mantine.impl.factory.
- Generator runner mechanism (bb task vs clj -X), leaning bb.

Deliberate divergence from SUIW: SUIW does bare clj->js + REPL-run codegen + one file per component + optional aggregate. We do a batteries-included converter, per-package nses, no aggregate, committed docgen input, and a headless CI-runnable generator.

**2026-07-13T20:31:46.195995038Z**

Generator = headless/CI-runnable/pinned plain-Clojure transform of committed docgen.json + node_modules -> one .cljc ns per package (core/dates/charts, no aggregate). kebab no-prefix defs, per-component named-export requires (tree-shakeable), package+require resolved from node_modules exports. Docstring-only runtime (metadata generator-side) = docgen prop-list + best-effort mantine.dev URL. Thin gen files -> mantine.impl.props (converter) + mantine.impl.factory (variadic-def factory + controlled-input shim on curated set). .cljc JVM-loadable, :clj throws on call. Optional props via map? first-arg. Handoffs to build-tooling: react/createElement require form + runner (lean bb).
