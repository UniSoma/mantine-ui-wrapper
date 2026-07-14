---
id: mnt-01kxgyjnpzrj
title: Wrap @mantine/modals and @mantine/spotlight with standalone-exports supplements
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-14T18:35:09.917631040Z'
updated: '2026-07-14T20:22:17.220000803Z'
closed: '2026-07-14T20:22:17.220000803Z'
parent: mnt-01kxgy8apnws
acceptance:
- title: mantine.modals exposes open, open-confirm-modal, open-context-modal, close, close-all, update-modal, update-context-modal plus modals-provider with a provider alias and use-modals
  done: false
- title: mantine.spotlight exposes open, close, toggle plus the codegen'd Spotlight components and use-spotlight; spotlight stylesheet linked in the demo (modals ships none)
  done: false
- title: Supplements refer the standalone exports (never the singleton objects); option maps converted, bare ids raw, context-modal registry keys verbatim, innerProps raw; verify-demo drives a modal open/close and spotlight toggle through the release bundle; 0 warnings; JVM-loads
  done: false
deps:
- mnt-01kxgyhzgc0f
---

## Description

## Parent

Spec: mnt-01kxgy8apnws.

## What to build

A consumer can drive modals imperatively and add a command palette. Install @mantine/modals and @mantine/spotlight pinned exact 9.4.1 (devDependencies + shipped npm-deps). Each package gets a generated namespace: codegen'd components ride the existing path; a hand-written supplement (committed generator input, hoisted at emit) carries the imperative fns and reactive hook, following the notifications supplement as the template.

Binding PoC finding: refer the STANDALONE exports (openModal, openConfirmModal, openContextModal, closeModal, closeAllModals, updateModal, updateContextModal; openSpotlight, closeSpotlight, toggleSpotlight) — the modals/spotlight singleton objects collide with same-named generated component defs in the merged ns. Public fn names per the imperative-API decision, kebab-cased 1:1, fixed single-arg arities. Option maps go through the public converter; bare ids stay raw both directions; context-modal registry keys verbatim; openContextModal innerProps raw; nested *Props inherit shallow semantics. Modals provider alias via the declare-then-def pattern; spotlight needs no provider. Reactive hooks use-modals / use-spotlight are raw-passthrough aliases. Single-instance only (no store arg).

Demo mounts the modals provider and spotlight, drives a modal and the spotlight toggle; verify asserts visible DOM effects. Spotlight stylesheet linked after core; modals ships no CSS (README notes both).

Verification loop: bb generate → npx shadow-cljs release demo → node scripts/verify-demo.mjs.

## Blocked by

- mnt-01kxgyhzgc0f (scope inversion + full core)

## Notes

**2026-07-14T20:22:17.220000803Z**

mantine.modals + mantine.spotlight via standalone-exports supplements (never the singletons); imperative fns + provider alias + reactive hooks; modal open/close and spotlight toggle verified through the release bundle. Committed 5d1c3b5.
