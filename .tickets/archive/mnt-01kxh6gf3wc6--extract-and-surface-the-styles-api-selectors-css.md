---
id: mnt-01kxh6gf3wc6
title: Extract and surface the Styles API (selectors + CSS variables)
status: closed
type: feature
priority: 4
mode: hitl
created: '2026-07-14T20:53:46.236686649Z'
updated: '2026-07-18T21:26:32.909484659Z'
closed: '2026-07-18T21:26:32.909484659Z'
tags:
- wontfix
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

## Notes

**2026-07-18T21:26:32.714423902Z**

Wontfix rationale (triaged with maintainer):

The value doesn't clear the bar over the docs URL already in every docstring.

- Props are the 90% case and are already surfaced. Styling is the minority of time spent.
- Every docstring already links to mantine.dev/core/<component>, whose Styles API table is STRICTLY richer than any docstring dump: per-selector descriptions, nesting, and live copy-pasteable examples. A docstring list of selector/variable names is a lossy copy of a superior, one-click-away resource.
- The AC settles for 'at least in docstrings' — precisely the low-value outcome: it duplicates the docs and adds noise to every docstring, at the cost of a permanent new extraction input (*StylesNames / *CssVariables from .d.ts) that must track the anchor forever.
- The one thing docs can't give — typo-proof local selector names (Mantine fails SILENTLY on an unknown :styles/:class-names key) — is only really solved by typed/enumerated keys, not a docstring. And in CLJS that payoff is weak: no TS-grade autocomplete, and :styles/:class-names are open maps, so enforcement is awkward and un-idiomatic.

If the silent-selector footgun ever proves frequent in practice, reopen and reframe around typed keys specifically — but prototype CLJS ergonomics first before committing a new pipeline input.

**2026-07-18T21:26:32.909484659Z**

wontfix: value doesn't clear the bar over the docs URL already in every docstring. Props (the 90% case) are surfaced; the mantine.dev Styles API table is strictly richer than a docstring dump; the only thing docs can't give (typo-proof local selector names, since Mantine fails silently on unknown keys) needs typed keys, not docstrings, and CLJS ergonomics make that weak. See notes for full rationale.
