---
id: mnt-01m46bkxe85s
title: Fail bb extract on an unknown or missing docs-data file
status: closed
type: chore
priority: 1
mode: afk
created: '2026-10-05T15:41:16.100031175Z'
updated: '2026-10-05T19:21:29.952981666Z'
closed: '2026-10-05T19:21:29.952981666Z'
acceptance:
- title: bb extract throws, naming the file, on an mdx-*-data.ts that is neither parsed nor ignored
  done: true
- title: bb extract throws, naming the file, when a parsed docs-data file is missing from the clone
  done: true
- title: extract-test covers the unknown-file, ignored-file and missing-file cases, and asserts the corpora and the ignore set are disjoint
  done: true
- title: bb clone-anchor at 9.7.0 passes and leaves codegen/input/ unchanged, with mdx-code-highlight-data.ts on the ignore set
  done: true
links:
- mnt-01m46ahjn88j
tags:
- settled
---

## Description

`bb extract` parses five component docs-data files and the hooks file, and silently skips every other `mdx-*-data.ts` in the clone's docs-data directory. Mantine 9.7.0 moved CodeHighlight into a new `mdx-code-highlight-data.ts`, and extract dropped it from `component-docs.edn` without an error. That was harmless only because `@mantine/code-highlight` is not wrapped. If a wrapped package's entries move the same way, its components lose descriptions and slugs, and `bb ci` stays green because plan degrades to thinner docstrings.

After this change, every docs-data file is known to extract: either parsed (a component corpus or hooks) or on an explicit ignore set (guides, meta, styles, theming, form, code-highlight). Extract fails before writing anything when the directory holds an `mdx-*-data.ts` that is neither parsed nor ignored, or when a parsed file is missing from the clone. The error names each file and says where to list it. Files not matching `mdx-*-data.ts` never fail the check.

### Decisions

- The driver slurps every `mdx-*-data.ts` in the directory and passes `parse-inputs` a `{filename → text}` map. `parse-inputs` owns which file is which corpus, which is hooks and which are ignored, and throws on an unknown file, on a missing parsed file, and on a corpus collision as today. `write-inputs!` knows nothing about files beyond the glob. The two existing `parse-inputs` fixtures are rekeyed by filename and carry all six parsed files.
- No lost-entry check against the committed `component-docs.edn`. Residual risk, accepted: a wrapped component's entry that moves into an existing ignored file still drops silently.
- The ignore set and the hooks filename are private defs beside `component-corpora` in `codegen/extract.clj`.
- Only names matching `mdx-*-data.ts` are checked; any other file in the directory passes.
- Each throw is an `ex-info` that lists the files, sorted, and points to `component-corpora` or the ignore set in `codegen/extract.clj`; ex-data is `{:unknown [...]}` or `{:missing [...]}`.
- `extract-test` asserts the corpora files and the ignore set are disjoint, reaching the private defs by var.
- The `component-docs.edn` banner keeps listing the five corpus files only.
- The `docs/anchor-bump.md` sentence listing `mdx-{core,…,hooks}-data.ts` changes to say extract reads every `mdx-*-data.ts` and fails on one it does not know. The error message carries the remediation.
- The `extract.clj` header comment adds the two file checks to what `parse-inputs` owns.
- No CHANGELOG entry: the change is not consumer-visible.

## Notes

**2026-10-05T19:21:29.952981666Z**

bb extract now reads every mdx-*-data.ts in the clone and parse-inputs, keyed by file name, throws before any write on a file that is neither parsed nor in ignored-files ({:unknown [...]}) and on a parsed file the clone lacks ({:missing [...]}), naming the files and the def to edit in codegen/extract.clj. Ignore set at 9.7.0: guides, meta, styles, theming, form, code-highlight. extract-test covers unknown, ignored and missing files and asserts parsed and ignored files are disjoint (13 tests). Verified on fake clones (both throws, index.ts skipped, codegen/input untouched) and on the real 9.7.0 clone via bb clone-anchor (codegen/input unchanged). docs/anchor-bump.md updated. bb ci green. Accepted residual risk: an entry moving into an existing ignored file still drops silently.
