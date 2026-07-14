---
id: mnt-01kxh6gf162j
title: Deep-convert nested *Props config maps
status: open
type: feature
priority: 4
mode: hitl
created: '2026-07-14T20:53:46.150505173Z'
updated: '2026-07-14T20:59:14.448806917Z'
tags:
- needs-triage
acceptance:
- title: Converter deep-converts a declared/curated set of nested *Props keys; raw-passthrough payloads (ids, innerProps) still pass through untouched
  done: false
- title: A nested *Props map verified round-tripping end-to-end in the demo (e.g. a modal cancelProps)
  done: false
- title: Existing shallow behavior preserved everywhere else; verify loop green
  done: false
---

## Description

Nested option maps (cancelProps / confirmProps / searchProps, spotlight actions, and *Props generally) get the same kebab->camel props conversion as top-level props, so consumers write idiomatic CLJS all the way down. Currently the converter is shallow. Design decision to grill: curated set of nested keys vs general recursion, and how to avoid converting raw passthrough payloads. Out-of-scope carve-out from mnt-01kxgy8apnws. Blocked by: none — can start immediately.
