---
id: mnt-01kxe8gzvp9e
title: 'Decide: build tooling, npm interop & CSS delivery'
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-13T17:31:17.238456887Z'
updated: '2026-07-14T02:20:48.454890382Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:prototype
deps:
- mnt-01kxe8gzf489
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
