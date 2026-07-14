---
id: mnt-01kxgyk1rsfn
title: Wrap all use* hooks from the @mantine/hooks barrel
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-14T18:35:22.263640675Z'
updated: '2026-07-14T20:22:17.322914915Z'
closed: '2026-07-14T20:22:17.322914915Z'
parent: mnt-01kxgy8apnws
acceptance:
- title: mantine.hooks covers every use* export of the @mantine/hooks barrel minus explicit excludes (~81), each a def-alias with description + mantine.dev URL docstring
  done: false
- title: 'verify-demo exercises hooks sampled across the return-shape split: tuple destructured positionally, object via interop, scalar'
  done: false
- title: Release build 0 warnings (js hints added where object returns need them); generated ns JVM-loads with call-time named throws
  done: false
deps:
- mnt-01kxgyhzgc0f
---

## Description

## Parent

Spec: mnt-01kxgy8apnws.

## What to build

The full Mantine hooks toolkit is one :require away. Flip the hooks dimension of the inverted scope to everything-minus-excludes: every use* export enumerated from the @mantine/hooks barrel (~81) generates a thin def-alias per the hooks decision — raw JS args in, raw JS returns out (zero conversion), rich docstring from the committed hook-docs input (already 81 descriptions) plus the derivable mantine.dev/hooks URL. Non-hook barrel utilities stay excluded. Rules-of-hooks not enforced.

Under :advanced, object-returning hooks used in the demo need js hints — the demo/verify additions sample the return-shape split (tuple, object, scalar) so the interop path is proven, not just compiled.

Verification loop: bb generate → npx shadow-cljs release demo → node scripts/verify-demo.mjs.

## Blocked by

- mnt-01kxgyhzgc0f (scope inversion + full core)

## Notes

**2026-07-14T20:22:17.322914915Z**

mantine.hooks wraps all 80 use* barrel exports as def-aliases with rich docstrings; return-shape split (tuple/object-^js/scalar) exercised; 0 warnings, JVM-loads. Committed 7bb2fd9.
