---
id: mnt-01kxe8gzrhy6
title: 'Decide: codegen pipeline & generated output'
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-13T17:31:17.137540377Z'
updated: '2026-07-13T17:37:58.612849725Z'
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
