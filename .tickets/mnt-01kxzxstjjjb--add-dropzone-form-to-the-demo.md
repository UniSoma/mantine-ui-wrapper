---
id: mnt-01kxzxstjjjb
title: Add dropzone + form to the demo
status: open
type: task
priority: 3
mode: afk
created: '2026-07-20T14:10:12.174578466Z'
updated: '2026-07-20T14:10:12.174578466Z'
tags:
- demo
acceptance:
- title: demo requires mantine.dropzone and mantine.form and mounts a minimal usage of each
  done: false
- title: bb build + verify-demo green with both compiled
  done: false
links:
- mnt-01kxzxsh9h5f
---

## Description

demo/mantine/demo.cljs currently requires only 7 of the wrapped namespaces (core, dates, charts, hooks, notifications, modals, spotlight). mantine.dropzone and mantine.form are never cljs-compiled/bundled in CI — only jvm-load exercises them, which checks the Clojure side but does NOT resolve their JS imports or cljs-compile the ns.

Surfaced while wrapping @mantine/schedule (mnt-01kxzxsh9h5f), where we chose to add a minimal demo render precisely to avoid extending this blind spot. This ticket closes the pre-existing gap for dropzone/form.

Add minimal demo usages: require mantine.dropzone + mantine.form and mount a minimal Dropzone and a minimal form-backed input, so CI cljs-compiles and bundles both. Keep it minimal — mount-only, mirroring the schedule demo addition.
