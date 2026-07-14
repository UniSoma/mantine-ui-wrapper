---
id: mnt-01kxe8h04teh
title: Build the four-pattern PoC (destination)
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-13T17:31:17.530183297Z'
updated: '2026-07-14T17:52:53.381691407Z'
closed: '2026-07-14T17:52:53.381691407Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:prototype
deps:
- mnt-01kxe8gzrhy6
- mnt-01kxe8gzvp9e
- mnt-01kxe8gzyj13
- mnt-01kxe8h01n5p
assignee: jonasrodrigues
---

## Description

## Question

The destination proof. Build a shadow-cljs harness that renders one of each pattern: a few codegen'd core components (incl. polymorphic component=, styles/classNames, section props), one imperative API end-to-end (e.g. notifications.show via its provider), and one hook (e.g. useDisclosure) — with Mantine's CSS actually loading. When all four render correctly, the pipeline is de-risked and the map is done.

## Notes

**2026-07-14T17:52:53.282520885Z**

Resolution — four-pattern PoC BUILT AND VERIFIED; pipeline de-risked, destination reached. The PoC is the repo itself, not a throwaway (commit 40f19c0; doc: docs/prototypes/four-pattern-poc.md):

BUILT: committed inputs (real docgen.json — 291 entries, generated from a 9.4.1 clone; component-docs.edn 150 descriptions/polymorphic/slugs incl. mdx-others-data.ts; hook-docs.edn 81; refresh scripted: bb extract <clone>); bb generate (scope-filtered via codegen/scope.edn, kebab defs w/ collision hard-error, auto :refer-clojure :exclude incl. supplement names (update excluded), docstrings = description+URL+docgen prop table, controlled-input curation w/ rot-logging, supplement require-merge + verbatim form hoist via edamame :read-cond :preserve, package/export resolution from node_modules); mantine.impl.props (settled converter + children path) + mantine.impl.factory (variadic factory, controlled shim, JVM stubs); notifications supplement; raw-React demo harness + plain-CSS links.

VERIFIED: shadow-cljs release :advanced = 0 warnings; generated .cljc load on the JVM and throw only when called; bb generate idempotent (stable checksums); 13/13 jsdom assertions executing the real release bundle — kebab props, left-section, styles (camelCase + --* verbatim), classNames space-join, polymorphic component= (a/span), children-seq flattening, controlled TextInput round-trip, use-disclosure tuple destructured positionally + .toggle re-render, notifications/show through the codegen'd+hoisted provider; every rendered hashed class (m_*) present in the linked @mantine/{core,notifications}/styles.css. CAVEAT: pixel paint not eyeballed in a real browser (no Chromium in sandbox) — npx shadow-cljs watch demo.

FINDINGS: (1) input refresh is CHEAP (~44s yarn install + <1min docgen), correcting the 'heavy/slow' research expectation; (2) imperative supplements must :refer the STANDALONE fns (showNotification, ...) — the notifications singleton object collides with the generated provider def; pattern for modals/spotlight too; (3) (declare notifications) + (def provider notifications) alias compiles warning-free standalone and hoisted; (4) controlled shim reads change values shape-agnostically (DOM event vs bare value), covering Select/NumberInput-style onChange; (5) extension-package MDX data lives in mdx-others-data.ts and keys Notifications directly.

Remaining work is post-map mechanical fan-out: widen codegen/scope.edn (+ modals/spotlight supplements on finding 2's pattern).

**2026-07-14T17:52:53.381691407Z**

Four-pattern PoC built into the repo and verified (:advanced 0 warnings, 13/13 jsdom assertions, CSS pairing confirmed) — pipeline de-risked, destination reached; fan-out = widen codegen/scope.edn
