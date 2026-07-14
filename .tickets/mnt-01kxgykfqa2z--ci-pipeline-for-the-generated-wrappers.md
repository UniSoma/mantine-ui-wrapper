---
id: mnt-01kxgykfqa2z
title: CI pipeline for the generated wrappers
status: open
type: task
priority: 2
mode: afk
created: '2026-07-14T18:35:36.547684050Z'
updated: '2026-07-14T18:35:36.547684050Z'
parent: mnt-01kxgy8apnws
acceptance:
- title: 'CI runs on every change: install deps, regenerate and fail on git diff (drift check), release-build the demo failing on any warning, run the jsdom verification, JVM-load every generated ns, run the per-package def-count coverage check'
  done: false
- title: Every check is a plain command runnable identically locally; the pipeline is green on the full fan-out surface
  done: false
deps:
- mnt-01kxgyhzgc0f
- mnt-01kxgyjccn57
- mnt-01kxgyjnpzrj
- mnt-01kxgyk1rsfn
---

## Description

## Parent

Spec: mnt-01kxgy8apnws.

## What to build

Generated code can never silently drift, regress, or lose coverage. One CI pipeline formalizes the loop the fan-out slices ran by hand: install, bb generate + fail-on-diff, zero-warning release build, jsdom verification of the release bundle, JVM-load of all generated namespaces (sampling the call-time named throw), and a cheap per-package def-count coverage check comparing generated defs against scoped docgen entries. No test framework — the release-bundle jsdom harness stays the single behavioral seam; generator-side checks are plain commands, same locally and in CI. Pixel paint remains a documented manual caveat (no browser in CI).

## Blocked by

- mnt-01kxgyhzgc0f (scope inversion + full core)
- mnt-01kxgyjccn57 (dates + charts)
- mnt-01kxgyjnpzrj (modals + spotlight)
- mnt-01kxgyk1rsfn (all hooks)
