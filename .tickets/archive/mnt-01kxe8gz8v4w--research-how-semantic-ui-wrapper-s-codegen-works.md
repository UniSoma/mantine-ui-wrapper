---
id: mnt-01kxe8gz8v4w
title: 'Research: how semantic-ui-wrapper''s codegen works'
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-13T17:31:16.634942567Z'
updated: '2026-07-13T17:36:23.035432504Z'
closed: '2026-07-13T17:36:23.035432504Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:research
assignee: agent
---

## Description

## Question

How does fulcrologic/semantic-ui-wrapper generate its wrappers? Capture: the source of truth it reads, the shape of a generated namespace, the factory pattern, and how it converts CLJS props -> React props. We want the reusable pattern to adapt for Mantine.

Investigate the repo at https://github.com/fulcrologic/semantic-ui-wrapper . Capture findings on a throwaway research/ branch with a pointer back to this ticket.

## Notes

**2026-07-13T17:36:23.035432504Z**

Findings: docs/research/reference-codegen.md. Source of truth = pre-built JSON (docs/src/componentInfo/*.json) — direct analogue of our docgen.json. Generator = plain Clojure (src/dev/user.clj) emitting one .cljc per component + aggregate factories.cljc; factory shape (def ui-x #?(:clj stub :cljs (h/factory-apply Comp))); docstring carries a Props: list but NO runtime prop metadata. Prop conversion is essentially clj->js only (no kebab->camel, no :style/:className/:event conveniences) — this is exactly where we should diverge for Mantine. Interop via pkg$Export requires (tree-shakeable). Reuse: JSON->per-ns+aggregate arch, name/ns derivation, docstring assembly, reader-conditional SSR split. Won't transfer: SUIR build:docs step (use TS docgen), icon-enum model, Fulcro coupling; make the generator a headless clj -X/bb task pinned to a Mantine version.
