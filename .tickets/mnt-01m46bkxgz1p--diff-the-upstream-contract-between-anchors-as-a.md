---
id: mnt-01m46bkxgz1p
title: Diff the upstream contract between anchors as a bump step
status: open
type: chore
priority: 1
mode: afk
created: '2026-10-05T15:41:16.186886658Z'
updated: '2026-10-05T15:41:16.186886658Z'
acceptance:
- title: docs/version-bump.md lists the upstream contract paths in one section
  done: false
- title: the clone-anchor clone can diff the previous anchor tag against the new one
  done: false
- title: one bb command prints the contract-path diff stat between the two anchors, and the procedure runs it before step 3
  done: false
links:
- mnt-01m46ahjn88j
---

## Description

Step 3 of docs/version-bump.md says to read the release notes. The 9.7.0 notes did not mention either change that hit the pipeline: the removed vendored yarn (.yarn/releases, yarnPath) and CodeHighlight moving to a new docs-data file. Both surfaced only from a source diff between the 9.6.0 and 9.7.0 tags. That diff needed the list of upstream paths the pipeline depends on, rebuilt by reading extract.clj and clone_anchor.clj, and a second clone, because clone-anchor clones one tag with --depth 1.

Add an 'Upstream contract' section to docs/version-bump.md listing those paths: scripts/docgen/, apps/mantine.dev/src/mdx/data/, apps/mantine.dev/src/mdx/mdx-data.ts, apps/mantine.dev/src/.docgen/ (docgen output), .yarnrc.yml, and the packageManager field of the root package.json. Make clone-anchor clone with --filter=blob:none and also fetch the previous anchor tag, read from codegen/input/mantine-version.edn before extract overwrites it. Add a bb task (e.g. bb upstream-diff) that prints git diff --stat <old> <new> over the contract paths in that clone, and make it a procedure step that runs before the release notes are read.
