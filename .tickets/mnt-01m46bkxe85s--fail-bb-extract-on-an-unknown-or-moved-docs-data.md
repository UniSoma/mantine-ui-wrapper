---
id: mnt-01m46bkxe85s
title: Fail bb extract on an unknown or moved docs-data file
status: open
type: chore
priority: 1
mode: afk
created: '2026-10-05T15:41:16.100031175Z'
updated: '2026-10-05T15:41:16.100031175Z'
acceptance:
- title: bb extract throws, naming the file, on an mdx-*-data.ts that is neither parsed nor ignored
  done: false
- title: extract-test covers the unknown-file case and the ignored-file case
  done: false
- title: bb clone-anchor on the 9.7.0 clone still passes with mdx-code-highlight-data.ts on the ignore set
  done: false
links:
- mnt-01m46ahjn88j
---

## Description

extract/component-corpora reads a fixed list of five mdx-*-data.ts files. Mantine 9.7.0 moved CodeHighlight into a new mdx-code-highlight-data.ts and extract dropped it from component-docs.edn without an error. It was harmless only because @mantine/code-highlight is not wrapped. If a wrapped package's entries move the same way, its components lose descriptions and slugs, and bb ci stays green because plan degrades to thinner docstrings.

Make write-inputs! throw when apps/mantine.dev/src/mdx/data/ holds an mdx-*-data.ts that is neither in component-corpora nor in an explicit ignore set (today: guides, meta, styles, theming, form, hooks, code-highlight). Keep the check in the pure core so extract_test.clj can cover it with a fixture file list. Optionally, also fail when a docgen component of a wrapped package has a component-docs entry in the committed file but not in the new one.
