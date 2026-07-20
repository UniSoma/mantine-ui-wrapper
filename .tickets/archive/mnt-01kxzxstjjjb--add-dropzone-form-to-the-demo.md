---
id: mnt-01kxzxstjjjb
title: Add dropzone + form to the demo
status: closed
type: task
priority: 3
mode: afk
created: '2026-07-20T14:10:12.174578466Z'
updated: '2026-07-20T15:09:49.396823596Z'
closed: '2026-07-20T15:09:49.396823596Z'
tags:
- demo
acceptance:
- title: demo requires mantine.dropzone and mantine.form and mounts a minimal usage of each
  done: true
- title: bb build + verify-demo green with both compiled
  done: true
links:
- mnt-01kxzxsh9h5f
---

## Description

demo/mantine/demo.cljs currently requires only 7 of the wrapped namespaces (core, dates, charts, hooks, notifications, modals, spotlight). mantine.dropzone and mantine.form are never cljs-compiled/bundled in CI — only jvm-load exercises them, which checks the Clojure side but does NOT resolve their JS imports or cljs-compile the ns.

Surfaced while wrapping @mantine/schedule (mnt-01kxzxsh9h5f), where we chose to add a minimal demo render precisely to avoid extending this blind spot. This ticket closes the pre-existing gap for dropzone/form.

Add minimal demo usages: require mantine.dropzone + mantine.form and mount a minimal Dropzone and a minimal form-backed input, so CI cljs-compiles and bundles both. Keep it minimal — mount-only, mirroring the schedule demo addition.

## Notes

**2026-07-20T15:09:49.396823596Z**

Added minimal dropzone + form mounts to demo/mantine/demo.cljs. mantine.dropzone: a Dropzone with required :on-drop + text child; mantine.form: controlled useForm backing a TextInput via the :& escape hatch (raw getInputProps merged in), echo reads state through interop. Linked @mantine/dropzone/styles.css in public/index.html and extended verify-demo.mjs (dropzone added to linked stylesheet set + render/CSS-pairing asserts; form initial-value + interop-readback asserts). bb build (0 warnings) and verify-demo both green — both namespaces now cljs-compiled/bundled in CI, closing the pre-existing blind spot.
