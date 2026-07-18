---
id: mnt-01kxv74avbq4
title: Anchor module + derived generator constant (ADR 0005)
status: closed
type: feature
priority: 3
mode: afk
created: '2026-07-18T18:17:01.547415067Z'
updated: '2026-07-18T18:33:09.285873906Z'
closed: '2026-07-18T18:33:09.285873906Z'
acceptance:
- title: Anchor module reads package.json pins, asserts the 9 @mantine/* pins are uniform, returns the version
  done: false
- title: plan.clj holds no version literal; the generator constant is derived from the anchor
  done: false
- title: generate asserts installed node_modules versions == anchor, failing loudly on mismatch
  done: false
- title: bb generate is drift-clean (generated output byte-identical to committed)
  done: false
- title: Mutating a single package.json pin fails the uniformity assertion
  done: false
---

## Description

Single-source the Mantine anchor — slice 1 of 3 for ADR 0005 (docs/adr/0005-single-source-the-mantine-anchor.md).

What to build: A codegen-side module (on the bb/codegen classpath) that reads the exact @mantine/* pins from package.json, asserts all nine agree, and returns the one Mantine anchor version. Delete plan.clj's (def mantine-version "9.4.1") and derive it from the module so every generated banner/docstring flows from the anchor. generate also asserts node_modules installed versions == anchor (the read-sources path already has Node). Release-identity logic stays on the codegen classpath, not shipped in src/main.

## Notes

**2026-07-18T18:33:09.285873906Z**

Created codegen/anchor.clj (pure pins parse + anchor-version asserting the nine @mantine/* pins are uniform). plan.clj derives its version from the anchor (def literal deleted); read-sources asserts installed node_modules versions == anchor. Drift-clean, plan-test green. Commit ae78ea4.
