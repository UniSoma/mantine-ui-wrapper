---
id: mnt-01kxh6gf6ny3
title: Wrap non-hook @mantine/hooks barrel utilities
status: open
type: feature
priority: 4
mode: hitl
created: '2026-07-14T20:53:46.325721780Z'
updated: '2026-07-14T20:59:14.645918691Z'
tags:
- needs-triage
acceptance:
- title: Non-hook barrel exports enumerated (everything-minus-excludes, non-use*) and wrapped as raw-passthrough fns with docstrings
  done: false
- title: Emission path/namespace decided and documented
  done: false
- title: At least one utility verified; JVM-loads; verify loop green
  done: false
---

## Description

The plain-function utilities from the @mantine/hooks barrel (randomId, getHotkeyHandler, mergeRefs, ...) are available from CLJS, wrapped on an appropriate path — NOT the use* hook def-alias path. Design to grill: extend mantine.hooks vs a new namespace, and conversion semantics (raw passthrough vs converted). Out-of-scope carve-out from mnt-01kxgy8apnws. Touches the hooks barrel (adjacent to the companion-hooks ticket) but neither blocks the other. Blocked by: none — can start immediately.
