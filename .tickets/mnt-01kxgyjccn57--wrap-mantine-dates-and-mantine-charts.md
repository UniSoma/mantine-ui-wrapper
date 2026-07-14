---
id: mnt-01kxgyjccn57
title: Wrap @mantine/dates and @mantine/charts
status: open
type: task
priority: 2
mode: afk
created: '2026-07-14T18:35:00.371203965Z'
updated: '2026-07-14T18:35:00.371203965Z'
parent: mnt-01kxgy8apnws
acceptance:
- title: mantine.dates and mantine.charts generated with full docgen coverage and rich docstrings; dayjs and recharts shipped caret-ranged in npm-deps and pinned exact in devDependencies
  done: false
- title: Dates value/onChange inputs added to the controlled curation; a date picker verified controlled end-to-end
  done: false
- title: Demo links dates+charts stylesheets after core; verify-demo asserts a rendered component per package incl. CSS pairing of hashed classes; 0 release warnings; JVM-loads
  done: false
deps:
- mnt-01kxgyhzgc0f
---

## Description

## Parent

Spec: mnt-01kxgy8apnws.

## What to build

A consumer can build date-input forms and render Mantine charts from CLJS. Install @mantine/dates and @mantine/charts pinned exact 9.4.1 plus their peers dayjs and recharts (at the ranges Mantine 9.4.1 declares), mirroring the @mantine/* treatment: exact in devDependencies, caret-ranged in the shipped npm-deps declaration. React stays unshipped. Generation picks the new packages up through the inverted scope; dates' controlled inputs join the curation. Demo harness gains representative usages and the per-package CSS links (core first); verify script gains assertions including CSS pairing for the new stylesheets. README notes the CSS links for the new packages.

Verification loop: bb generate → npx shadow-cljs release demo → node scripts/verify-demo.mjs.

## Blocked by

- mnt-01kxgyhzgc0f (scope inversion + full core)
