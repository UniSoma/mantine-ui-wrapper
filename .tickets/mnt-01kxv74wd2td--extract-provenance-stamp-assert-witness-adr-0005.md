---
id: mnt-01kxv74wd2td
title: 'extract provenance: stamp, assert, witness (ADR 0005)'
status: open
type: feature
priority: 3
mode: afk
created: '2026-07-18T18:17:19.516135709Z'
updated: '2026-07-18T18:17:19.516135709Z'
acceptance:
- title: extract stamps its committed-input banners from the anchor, not a hardcoded literal
  done: false
- title: extract asserts the <clone-dir> version equals the anchor, failing loudly on mismatch
  done: false
- title: extract writes codegen/input/mantine-version.edn as the provenance witness
  done: false
- title: codegen/input/mantine-version.edn is committed at the current anchor
  done: false
- title: release-check asserts witness == anchor; mutating the committed witness fails release-check
  done: false
deps:
- mnt-01kxv74avbq4
- mnt-01kxv74k1exp
---

## Description

Single-source the Mantine anchor — slice 3 of 3 for ADR 0005 (docs/adr/0005-single-source-the-mantine-anchor.md).

What to build: extract stamps its banners from the anchor instead of the hardcoded (Mantine 9.4.1) literals, asserts the fed <clone-dir> version equals the anchor, and writes a committed provenance witness codegen/input/mantine-version.edn (the version the committed inputs were captured at, written only when clone==anchor). release-check gains a witness==anchor assertion, closing the 'bumped the anchor but forgot bb extract' gap that the other checks miss (docgen.json carries no version of its own). The witness file is committed at "9.4.1".
