---
id: mnt-01kxgyhzgc0f
title: Invert generation scope and cover all of @mantine/core
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-14T18:34:47.180557399Z'
updated: '2026-07-14T20:22:17.038050126Z'
closed: '2026-07-14T20:22:17.038050126Z'
parent: mnt-01kxgy8apnws
acceptance:
- title: Scope config expresses everything-minus-excludes for both components and hooks; mantine.core covers every @mantine/core docgen entry minus explicit excludes
  done: false
- title: Controlled-input curation covers all core value/onChange inputs; a newly curated input verified end-to-end
  done: false
- title: bb generate idempotent; release demo builds with 0 warnings; verify-demo passes with new representative core assertions; generated ns JVM-loads
  done: false
---

## Description

## Parent

Spec: mnt-01kxgy8apnws (fan out to full Mantine 9.4.1 coverage). Inherited decisions: closed map mnt-01kxe8fz1ert + docs/prototypes/four-pattern-poc.md.

## What to build

A consumer can use every @mantine/core component from mantine.core as a kebab-case factory with rich docstrings — no raw interop. This slice also carries the prefactor the whole fan-out rests on: the generation scope config inverts from a hand-enumerated allowlist to everything-minus-excludes (components keyed to wrapped packages' docgen entries, hooks keyed to the barrel's use* exports), with an explicit exclude set for deliberate omissions. Only core is flipped to full coverage here; hooks stay at their current subset until their own ticket.

Existing guards are the safety net at scale: within-package kebab collisions hard-error, unresolvable exports log+skip. Controlled-input curation widens to the full set of core value/onChange inputs (the shim is already shape-agnostic — curation-file work plus rot-logging review, not shim changes). The demo harness and verify script gain representative assertions for the widened surface, including at least one newly curated controlled input.

Verification loop: bb generate → npx shadow-cljs release demo → node scripts/verify-demo.mjs.

## Blocked by

None — can start immediately.

## Notes

**2026-07-14T20:22:17.038050126Z**

Scope inverted to everything-minus-excludes; mantine.core covers all 188 @mantine/core docgen entries; controlled-input curation widened to 23 core inputs (NativeSelect verified). Committed 0030e1b; verify loop green.
