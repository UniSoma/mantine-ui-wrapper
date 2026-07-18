---
id: mnt-01kxv74k1exp
title: bb release-check task wired into CI (ADR 0005)
status: closed
type: feature
priority: 3
mode: afk
created: '2026-07-18T18:17:09.929490260Z'
updated: '2026-07-18T18:33:09.382161703Z'
closed: '2026-07-18T18:33:09.382161703Z'
acceptance:
- title: bb release-check validates deps.cljs @mantine ranges equal ^anchor
  done: false
- title: bb release-check validates build.clj version prefix (first 3 segments) equals the anchor
  done: false
- title: bb release-check asserts the package.json pins are uniform
  done: false
- title: release-check is wired into bb ci and green on the current tree
  done: false
- title: Changing a deps.cljs range or the build.clj prefix out of agreement fails release-check loudly
  done: false
deps:
- mnt-01kxv74avbq4
---

## Description

Single-source the Mantine anchor — slice 2 of 3 for ADR 0005 (docs/adr/0005-single-source-the-mantine-anchor.md).

What to build: A standalone 'bb release-check' task that validates the hand-authored renderings against the anchor with no Node — the deps.cljs @mantine/* ^-ranges and build.clj's 9.4.1.N version prefix — plus the pins-uniform check. These files stay hand-authored and shipped; release-check reads them as data and never regenerates them. Wire release-check explicitly into 'bb ci'.

## Notes

**2026-07-18T18:33:09.382161703Z**

Added codegen/release_check.clj (pure violations checker + -main runner) and the bb release-check task, wired into bb ci. Validates deps.cljs @mantine ranges == ^anchor, build.clj version prefix == anchor, and pins uniform. Green on current tree; negative cases unit-tested in plan_test. Commit ae78ea4.
