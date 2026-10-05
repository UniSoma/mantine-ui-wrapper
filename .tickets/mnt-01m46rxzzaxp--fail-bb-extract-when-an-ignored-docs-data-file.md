---
id: mnt-01m46rxzzaxp
title: Fail bb extract when an ignored docs-data file holds a component entry
status: open
type: chore
priority: 1
mode: afk
created: '2026-10-05T19:33:57.856430150Z'
updated: '2026-10-05T19:33:57.856430150Z'
acceptance:
- title: parse-inputs throws, naming the file and the entry keys, when an ignored docs-data file holds an entry with package and props
  done: false
- title: extract-test covers a component entry in an ignored file and an ignored file without one
  done: false
- title: bb clone-anchor at the anchor passes and leaves codegen/input/ unchanged
  done: false
deps:
- mnt-01m46b13td0f
links:
- mnt-01m46bkxe85s
---

## Description

`bb extract` reads every `mdx-*-data.ts` in the clone but parses components only from the corpus files. A component entry that moves upstream into an ignored file (guides, meta, styles, theming, form) drops from `component-docs.edn` without an error, and plan degrades to thinner docstrings while `bb ci` stays green. mnt-01m46bkxe85s accepted this risk because `mdx-code-highlight-data.ts` held component entries while on the ignore set. The code-highlight wrap moves that file to a parsed corpus, and at 9.7.0 no remaining ignored file holds an entry with both `package` and `props`.

After this change, extract fails before writing anything when an ignored docs-data file holds a component entry (one with `package` and `props`), naming the file and the entries.
