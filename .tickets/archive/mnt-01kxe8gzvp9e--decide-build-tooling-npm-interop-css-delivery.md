---
id: mnt-01kxe8gzvp9e
title: 'Decide: build tooling, npm interop & CSS delivery'
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-13T17:31:17.238456887Z'
updated: '2026-07-14T17:12:45.792030778Z'
closed: '2026-07-14T17:12:45.792030778Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:prototype
deps:
- mnt-01kxe8gzf489
assignee: jonas.rodrigues@unisoma.com
---

## Description

## Question

Decide the shadow-cljs project setup: how @mantine/* npm deps are declared (deps vs peer-deps), and the concrete strategy for delivering Mantine's required CSS into both the wrapper's demo harness and downstream consumers. Prototype the smallest build that renders one Mantine component with its CSS applied.

## Notes

**2026-07-13T20:31:46.380964330Z**

Handoffs from Decide: codegen pipeline & generated output (mnt-01kxe8gzrhy6, now closed):

1. REACT createElement REQUIRE FORM — mantine.impl.factory needs a concrete, framework-agnostic react/createElement (the factory closes over a Mantine component, runs mantine.impl.props/convert on props, then calls createElement). Decide the exact shadow-cljs require sugar here (e.g. ["react" :as react] / :refer createElement), coherent with the rest of npm interop.

2. GENERATOR RUNNER MECHANISM — the codegen generator is settled as a headless/CI-runnable/pinned/single-entry-point plain-Clojure task reading a committed docgen.json + installed node_modules and emitting .cljc. The exact runner (bb task vs clj -X alias) was handed here to be chosen coherently with deps.edn/bb.edn/shadow-cljs. Lean: bb (fast startup, trivial JSON + fs reads, no JVM/deps compile for a pure text-emitting script).

**2026-07-14T02:20:48.454890382Z**

Handoffs from Decide: imperative-API wrapping pattern (mnt-01kxe8gzyj13, now closed):

1. SUPPLEMENT-HOIST GENERATOR STEP — a new committed generator INPUT: per-package supplement files (each a real compilable .cljc ns) for the 3 extension packages (notifications/modals/spotlight). At emit, the generator must hoist a supplement's :require + top-level forms into the generated `mantine.<pkg>` ns: dedup its requires against the impl/react/per-component requires already emitted, and append its forms after the codegen'd defs. Keep it a pure reproducible transform (supplements are committed inputs; no read-existing-output). This replaces both the "exclude these packages" and ".imperative sibling" alternatives — providers stay codegen'd + auto-documented, imperative fns/hooks land in the same clean ns.

2. :refer-clojure :exclude MUST SEE SUPPLEMENT NAMES — the generator already auto-derives (:refer-clojure :exclude [...]) per ns for clojure.core shadowers among docgen components. Supplement-defined defs must also feed that derivation (concretely: notifications/update shadows clojure.core/update). Any supplement shadower otherwise compiles with a warning/collision.

3. IMPERATIVE-SINGLETON / PACKAGE-HOOK REQUIRE SUGAR — decide coherently with the react/createElement require form: the supplements need to :refer the imperative singleton object + the package's reactive hook, e.g. ["@mantine/notifications" :refer [notifications useNotifications]], ["@mantine/modals" :refer [modals useModals]], ["@mantine/spotlight" :refer [spotlight useSpotlight]]. (Provider COMPONENTS are required per-component by codegen already; these are the ADDITIONAL non-component named exports.)

**2026-07-14T17:12:45.691427Z**

RESOLUTION — build tooling, npm interop & CSS delivery. De-risked by a throwaway shadow-cljs PoC that rendered one Mantine component (findings + verified skeleton: docs/prototypes/build-tooling-poc.md).

1. PROJECT STRUCTURE — single shadow-cljs project at repo root: one package.json / shadow-cljs.edn / deps.edn / bb.edn. src/ = wrapper nses (generated .cljc + mantine.impl.props/factory + supplements); demo/ = harness. One :builds entry :demo (:target :browser). Wrapper ships source; demo is dev-only.

2. NPM DEPS DECLARATION — ship src/main/deps.cljs with :npm-deps = wrapped @mantine/* ONLY, caret-ranged (^9.4.1); shadow-cljs auto-installs these for consumers (CONFIRMED: it runs npm install --save --save-exact on build). react/react-dom NOT shipped — the app is sole owner of React (avoids the split-context/duplicate-React hazard). package.json = devDependencies ONLY: @mantine/* pinned exact 9.4.1 (+lockfile) for reproducible codegen; react/react-dom/shadow-cljs/mantine-docgen-script/CSS-bundler caret-ranged. NO dependencies, NO peerDependencies (the Clojars jar is never npm-installed, so peerDeps would be seen by nobody). README lists the npm-install set + CSS <link> set (fallback for non-shadow-cljs tooling, which does not honor :npm-deps). NOTE: because shadow-cljs auto-install uses --save --save-exact (writes an exact pin into DEPENDENCIES), our own repo must pre-declare @mantine/* in devDependencies so the auto-install never fires for us and never pollutes package.json dependencies.

3. NPM-INTEROP REQUIRE SUGAR (resolves the 3 handoffs) — bundle-neutral (:refer vs :as does NOT affect tree-shaking; whole @mantine/core barrel lands regardless — CONFIRMED ~1MB bundle). Convention: named ESM exports we bind -> :refer (components ["@mantine/core" :refer [Button ...]]; imperative singleton + package hook ["@mantine/notifications" :refer [notifications useNotifications]]; use* hooks ["@mantine/hooks" :refer [useDisclosure ...]]); a module used as a general-purpose namespace -> :as (react only: react/createElement, react/forwardRef, react/Fragment). NO :default / $default anywhere (nothing we touch is default-only). react/createElement is the framework-agnostic factory primitive.

4. GENERATOR RUNNER — babashka, single 'generate' task in bb.edn. Pure JSON->text (reads committed docgen.json + MDX-extracted descriptions + supplements + node_modules exports, emits .cljc); instant startup, CI-runnable, no JVM/deps warmup. Compilation validated separately by the :demo build.

5. CSS DELIVERY — wrapper imports NO CSS. Harness: <MantineProvider> wrapping the app once + a single <link> to @mantine/core/styles.css from node_modules (served via :dev-http, project-dir root or a public/node_modules symlink), plain (non-layer), core-first; skip ColorSchemeScript (accept one-frame light-scheme flash on a client SPA). Consumer docs: recommend styles.layer.css (app CSS wins regardless of order) + a real bundler CSS step for production; warn that (:require [".css"]) does NOT work under shadow-cljs.

CROSS-TICKET CORRECTION — the codegen decision (mnt-01kxe8gzrhy6) gist 'per-component named-export requires (tree-shakeable under :advanced)' is FALSE: shadow-cljs does not tree-shake npm; the named-requires decision stands on clarity/correctness grounds only. Noted on that ticket + map gist corrected.

**2026-07-14T17:12:45.792030778Z**

Single shadow-cljs project at root. npm deps: ship src/main/deps.cljs :npm-deps=@mantine/* caret-ranged (shadow-cljs auto-installs for consumers, CONFIRMED); react NOT shipped (app owns React); package.json devDeps-only, @mantine/* pinned exact 9.4.1 (pre-declared so auto-install never pollutes deps); no deps/peerDeps; README fallback. Require sugar (bundle-neutral): :refer named exports for all Mantine (components/singletons/hooks), :as react for the grab-bag ns. Generator runner = bb 'generate' task. CSS: wrapper imports none; harness = MantineProvider + <link> core styles.css from node_modules (plain, core-first, skip ColorSchemeScript); consumers get layer-CSS + bundler-step docs. De-risked by a PoC that rendered a Mantine Button (:advanced clean, DOM-verified): docs/prototypes/build-tooling-poc.md. CORRECTS codegen gist: NO npm tree-shaking under shadow-cljs.
