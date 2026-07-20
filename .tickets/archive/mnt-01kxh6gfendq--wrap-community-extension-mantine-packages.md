---
id: mnt-01kxh6gfendq
title: Wrap community/extension Mantine packages
status: closed
type: feature
priority: 4
mode: hitl
created: '2026-07-14T20:53:46.581197533Z'
updated: '2026-07-20T14:10:02.668759802Z'
closed: '2026-07-18T21:27:56.538898497Z'
tags:
- wontfix
acceptance:
- title: A representative community/extension package installed, shipped in npm-deps, and generated into its own namespace via the existing scope model
  done: false
- title: Docstrings and coverage work via the existing path or a documented adaptation (incl. how its docgen/MDX inputs are extracted)
  done: false
- title: Verify loop green; a short guide documents how to add an extension package
  done: false
links:
- mnt-01kxzxsh9h5f
---

## Description

Third-party Mantine extension packages that follow Mantine's docgen conventions can be installed and wrapped through the same everything-minus-excludes pipeline, the way @mantine/dates and @mantine/charts were. Out-of-scope carve-out from mnt-01kxgy8apnws (first-party packages only in v1). Blocked by: none — can start immediately.

## Notes

**2026-07-18T21:27:56.318594785Z**

Wontfix (triaged with maintainer): no third-party/community extension package is in mind to actually wrap. This was a speculative 'the pipeline could also do this' carve-out from mnt-01kxgy8apnws, not a demand pulling for it — YAGNI. The everything-minus-excludes pipeline already demonstrably generalizes across first-party packages (@mantine/dates, @mantine/charts), so the capability is effectively proven; building + documenting an extension-package path with no concrete consumer would be speculative surface to maintain. Reopen when a specific extension package is chosen — the concrete package will also settle the open question here (how its docgen/MDX inputs get extracted).

**2026-07-18T21:27:56.538898497Z**

wontfix: no third-party/community extension package in mind to wrap. Speculative carve-out from mnt-01kxgy8apnws; the pipeline already generalizes across first-party packages (dates, charts). Reopen when a concrete extension package is chosen. See notes.
