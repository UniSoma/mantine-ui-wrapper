---
id: mnt-01kyn2mw31rr
title: Bump the Mantine anchor to 9.5.0 (through 9.4.2)
status: closed
type: chore
priority: 2
mode: hitl
created: '2026-07-28T19:18:55.841366100Z'
updated: '2026-07-28T19:19:13.101781299Z'
closed: '2026-07-28T19:19:13.101781299Z'
---

## Description

Move the anchor from 9.4.1 to 9.5.0: refresh codegen inputs from a 9.5.0 clone, bump every rendering (package.json pins, deps.cljs ranges, build.clj prefix, README/release prose), regenerate, verify with bb ci. 9.5.0 adds Cascader (core) + SunburstChart/BulletChart/ChartBrush (charts); Cascader is a value/onChange input so it needs a controlled-inputs.edn entry.

## Notes

**2026-07-28T19:19:13.101781299Z**

Anchor at 9.5.0. Inputs re-extracted from a 9.5.0 clone (docgen path survived Mantine's oxc migration). Four new defs: core/cascader, charts/{sunburst-chart,bullet-chart,chart-brush}; no removals or renames. Cascader added to controlled-inputs.edn. dayjs/recharts pins unchanged. bb ci green (drift, build 0 warnings, verify-demo, jvm-load, coverage 348 entries, plan/extract tests, release-check, cljs tests). Known gap: FloatingWindow.ResizeHandle is not a top-level @mantine/core export, so the compound-part check does not require it and it is unwrapped.
