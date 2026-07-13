---
id: mnt-01kxe8gzrhy6
title: 'Decide: codegen pipeline & generated output'
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-13T17:31:17.137540377Z'
updated: '2026-07-13T19:20:36.530989444Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:grilling
deps:
- mnt-01kxe8gzn6bt
- mnt-01kxe8gzbttp
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
