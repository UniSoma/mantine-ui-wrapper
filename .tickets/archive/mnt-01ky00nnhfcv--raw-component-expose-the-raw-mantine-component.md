---
id: mnt-01ky00nnhfcv
title: 'raw-component: expose the raw Mantine component from any wrapper var'
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-20T15:00:21.679485048Z'
updated: '2026-07-20T15:25:28.399006857Z'
closed: '2026-07-20T15:25:28.399006857Z'
acceptance:
- title: (mantine.interop/raw-component mc/button) returns Mantine Button (raw JS component)
  done: true
- title: raw-component on a controlled-shimmed input (e.g. mc/text-input) returns the real Mantine component, not the shim
  done: true
- title: Works for wrappers from non-core generated namespaces (e.g. mantine.dates)
  done: true
- title: 'Non-wrapper argument: console.error fires and nil is returned'
  done: true
- title: README documents raw-component; check suite green and demo compiles
  done: true
---

## Description

New handwritten mantine.interop namespace (.cljc, not generated, no supplement) with raw-component: takes a wrapper fn from any generated namespace (core, charts, dates, dropzone, schedule), returns the raw Mantine component; on misuse (argument is not one of our wrappers) js/console.error with the offending value and return nil. Main use case: passing the raw component to slots like the :component prop, and any interop the wrapper does not cover.

Mechanism: mantine.impl.factory/factory tags each wrapper fn with its underlying component as a JS property under the string key "mantine$raw" (unchecked-set at def time — zero per-render cost, :advanced-safe; NOT fn metadata, which would wrap every render call in a MetaFn). controlled tags its shim with the underlying component and factory reads through it, so raw-component on a curated input (e.g. text-input) returns the true Mantine component, never our controlled shim.

Var-based design chosen over name-based lookup (gobj/get on the module object): no string/PascalCase questions, no not-found failure mode, and one implementation in the shared factory covers every generated namespace. Accepted tradeoff: cannot resolve components the wrapper does not cover — those get fixed by supplementing.

Docs: one README bullet in the conventions/escape-hatch area. CLJ branch of cljs-only fns via the existing not-implemented pattern.

## Notes

**2026-07-20T15:25:28.399006857Z**

Added mantine.interop/raw-component (handwritten .cljc, not generated). factory tags each wrapper fn with its underlying component under JS key mantine via unchecked-set at def time; controlled tags its shim and factory reads through (component-of), so raw-component on curated inputs returns the true component. Non-wrapper -> console.error + nil. CLJ branch via not-implemented stub. README bullet added; demo mounts Button-as-Anchor via mi/raw-component mc/anchor. Tests: src/test/mantine/interop_test.cljs (mechanism + controlled + misuse). Checks green: drift/jvm-load/coverage/build/verify-demo/test. Committed a4bc7ba.
