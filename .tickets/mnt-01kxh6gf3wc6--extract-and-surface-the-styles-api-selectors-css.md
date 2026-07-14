---
id: mnt-01kxh6gf3wc6
title: Extract and surface the Styles API (selectors + CSS variables)
status: open
type: feature
priority: 4
mode: hitl
created: '2026-07-14T20:53:46.236686649Z'
updated: '2026-07-14T20:59:14.552023980Z'
tags:
- needs-triage
acceptance:
- title: Extraction reads the styles-api data files into a committed input, refreshed by the existing extract task
  done: false
- title: Generator surfaces per-component selectors + CSS variables (at least in docstrings)
  done: false
- title: Verified for a representative component; version-bump/extract path updated; verify loop green
  done: false
---

## Description

Consumers can discover each component's Styles API — the selector names usable in :styles / :class-names and the component CSS variables — from the wrapper itself (docstrings and/or typed keys), extracted from Mantine's styles-api data files. Out-of-scope carve-out from mnt-01kxgy8apnws. Blocked by: none — can start immediately.
