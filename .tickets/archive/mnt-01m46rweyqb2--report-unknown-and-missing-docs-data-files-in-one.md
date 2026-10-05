---
id: mnt-01m46rweyqb2
title: Report unknown and missing docs-data files in one extract error
status: closed
type: chore
priority: 2
mode: afk
created: '2026-10-05T19:33:07.666313569Z'
updated: '2026-10-05T19:33:50.719393827Z'
closed: '2026-10-05T19:33:50.719393827Z'
tags:
- settled
acceptance:
- title: parse-inputs throws one ex-info naming both the unknown and the missing files when both occur, with ex-data {:unknown [...] :missing [...]}
  done: true
- title: a single-cause error keeps its current ex-data shape ({:unknown [...]} or {:missing [...]})
  done: true
- title: bb ci passes
  done: true
links:
- mnt-01m46bkxe85s
---

## Description

When an upstream bump renames a parsed docs-data file, the clone holds an unknown file and lacks a parsed one at once. Today `bb extract` reports only the unknown file; the missing one surfaces on the next run, so the rename is not obvious. After this change, one error lists both.

### Decisions

- `parse-inputs` computes the unknown and missing lists, then throws once when either is non-empty. The ex-data holds `:unknown` and/or `:missing`, each only when non-empty, so a single-cause error keeps today's shape.
- The message carries one clause per non-empty list, each with its existing remediation.
- This supersedes the mnt-01m46bkxe85s decision of separate throws.

## Notes

**2026-10-05T19:33:50.719393827Z**

parse-inputs now computes the unknown and missing docs-data lists and throws one ex-info when either is non-empty: one message clause per non-empty list, ex-data holding :unknown and/or :missing only when non-empty, so single-cause errors keep their shape. New extract-test case for an upstream rename (unknown + missing in one error); 14 tests. bb ci green. Supersedes the separate-throws decision of mnt-01m46bkxe85s.
