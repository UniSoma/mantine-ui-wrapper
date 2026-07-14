---
id: mnt-01kxh6gfendq
title: Wrap community/extension Mantine packages
status: open
type: feature
priority: 4
mode: hitl
created: '2026-07-14T20:53:46.581197533Z'
updated: '2026-07-14T20:59:14.733617875Z'
tags:
- needs-triage
acceptance:
- title: A representative community/extension package installed, shipped in npm-deps, and generated into its own namespace via the existing scope model
  done: false
- title: Docstrings and coverage work via the existing path or a documented adaptation (incl. how its docgen/MDX inputs are extracted)
  done: false
- title: Verify loop green; a short guide documents how to add an extension package
  done: false
---

## Description

Third-party Mantine extension packages that follow Mantine's docgen conventions can be installed and wrapped through the same everything-minus-excludes pipeline, the way @mantine/dates and @mantine/charts were. Out-of-scope carve-out from mnt-01kxgy8apnws (first-party packages only in v1). Blocked by: none — can start immediately.
