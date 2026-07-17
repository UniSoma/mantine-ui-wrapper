---
id: mnt-01kxr60ktb7a
title: Wrap @mantine/dropzone (first-party, missed in the fan-out)
status: closed
type: feature
priority: 3
mode: afk
created: '2026-07-17T13:59:47.784385009Z'
updated: '2026-07-17T14:36:19.055259711Z'
closed: '2026-07-17T14:36:19.055259711Z'
acceptance:
- title: '@mantine/dropzone installed, in scope, and generated into mantine.dropzone; declared in deps.cljs + package.json + README install + CSS import lines'
  done: false
- title: Compound parts docgen omits are backfilled in a dropzone supplement, verified by the compound-part coverage check passing
  done: false
- title: bb ci green
  done: false
deps:
- mnt-01kxr5zr86zq
tags:
- gap
---

## Description

@mantine/dropzone is a first-party package already present in docgen (Dropzone, DropzoneFullScreen) but NOT installed, so resolve-component skips it for lack of an installed export — it was missed in the original fan-out. Install it and bring it in scope so it auto-generates like dates/charts (the everything-minus-excludes :components dimension already covers it once installed + docgen-present).

Declare it in deps.cljs (:npm-deps), package.json devDeps, README install line, and README CSS imports (dropzone ships styles.css). Backfill any compound parts docgen omits (e.g. Dropzone.Accept/Reject/Idle) in codegen/supplements/dropzone.cljc, using the compound-part coverage check from the core-supplements ticket to enumerate exactly what's missing.

Blocked by the core-supplements ticket: needs its compound-part coverage check to find and gate the missing dropzone compound parts.

## Notes

**2026-07-17T14:36:19.055259711Z**

Wrapped @mantine/dropzone. Installed @mantine/dropzone@9.4.1; declared in package.json devDeps, src/main/deps.cljs (:npm-deps ^9.4.1), README install line, and README CSS import block (styles.layer.css). Auto-generated into mantine.dropzone (dropzone, dropzone-full-screen from docgen); backfilled the 3 compound parts docgen omits (Dropzone.Accept/Idle/Reject) in codegen/supplements/dropzone.cljc. Compound-part coverage check passes (dropzone expected 2, present-defs 5). bb ci green.
