---
id: mnt-01m1n0rcn35x
title: Bump the Mantine anchor to 9.6.0
status: closed
type: chore
priority: 2
mode: hitl
created: '2026-09-04T01:32:59.427226197Z'
updated: '2026-09-04T01:49:22.292441835Z'
closed: '2026-09-04T01:49:22.292441835Z'
---

## Description

Move the anchor from 9.5.0 to 9.6.0: refresh codegen inputs from a 9.6.0 clone, bump every rendering (package.json pins, deps.cljs ranges, build.clj prefix, README/release prose), regenerate, verify with bb ci. Check whether 9.6.0 widens the dayjs/recharts peer ranges.

## Notes

**2026-09-04T01:49:22.292441835Z**

Anchor at 9.6.0. Inputs re-extracted from a 9.6.0 clone (vendored yarn 4.18 via node .yarn/releases; docgen path unchanged). Five new defs: core/action-bar, charts/{gauge-chart,waffle-chart,matrix-chart,candlestick-chart}; no removals or renames; none are value/onChange inputs so controlled-inputs.edn is untouched. ActionBar.CloseButton/Divider are static parts with top-level exports, so the compound-part check required them: backfilled in codegen/supplements/core.cljc. dayjs/recharts pins unchanged (peer ranges >=1.0.0 / >=3.2.1 did not move). bb ci green (drift, build 0 warnings, verify-demo, jvm-load, coverage 353 entries, plan/extract tests, release-check, cljs tests). New first-party @mantine/lightbox package NOT wrapped here — filed as mnt-01m1n1p5tz2q.
