---
id: mnt-01kxh6gfhcpz
title: Re-include the 7 excluded companion hooks
status: open
type: task
priority: 1
mode: hitl
created: '2026-07-14T20:53:46.668789202Z'
updated: '2026-07-14T20:59:14.353870887Z'
tags:
- needs-triage
acceptance:
- title: The 7 hooks generate as def-aliases with a sensible docstring (derived description or their shared docs-page URL)
  done: false
- title: scope.edn hook excludes trimmed accordingly; the def-count coverage check reflects the new count
  done: false
- title: Release build 0 warnings; generated ns JVM-loads; verify loop green
  done: false
---

## Description

The companion hooks currently excluded for carrying no description (useCallbackRef, useFullscreenDocument, useFullscreenElement, useHorizontalCollapse, useMousePosition, useMutationObserverTarget, useSessionStorage) are wrapped in mantine.hooks like the rest. Small, self-contained. Implementation-level deferral from mnt-01kxgy8apnws (dropped only to keep every generated def documented). Blocked by: none — can start immediately.
