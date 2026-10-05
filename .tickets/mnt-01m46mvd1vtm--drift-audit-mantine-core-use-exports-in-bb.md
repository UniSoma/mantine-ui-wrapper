---
id: mnt-01m46mvd1vtm
title: Drift-audit @mantine/core use* exports in bb coverage
status: open
type: chore
priority: 3
mode: hitl
created: '2026-10-05T18:22:38.641598340Z'
updated: '2026-10-05T18:22:46.635031040Z'
acceptance:
- title: bb coverage fails, naming the export, on a @mantine/core use* export that is neither wrapped nor excluded
  done: false
- title: The exclude list, or the exclude rule, carries the reason each excluded hook is not wrapped
  done: false
- title: bb ci passes on the current anchor
  done: false
links:
- mnt-01m46b13xkrp
- mnt-01m46ahjn88j
---

## Description

The 9.7.0 bump added useAppShellResize to @mantine/core. No check flagged that the wrapper does not cover it: it was caught by reading the bump diff by hand and filed as mnt-01m46b13xkrp. Core hooks are wrapped by hand in codegen/supplements/core.cljc, and nothing compares them against the barrel. The compound-part drift audit closed the same gap for parts.

Add a bb coverage check that enumerates the @mantine/core barrel's use* exports and fails on any that is neither defined in mantine.core nor on an explicit exclude list. There are about 60 such exports today. Most are context hooks (useComboboxContext, useTourContext, useAppShellContext...) that only work inside their component's compound tree. A few are standalone hooks (useCombobox, useTree, useMatches, useProps).

Decide before implementing:
- The exclude policy for context hooks: one exclude per name, or a rule such as a `*Context` suffix. A rule needs a guard so it cannot hide a real hook.
- Whether each standalone hook gets wrapped now or excluded with a reason. That decides what the first green run of the check covers.
- Whether the check lives in scripts/coverage-check.clj next to the compound-part audit or as its own task.
