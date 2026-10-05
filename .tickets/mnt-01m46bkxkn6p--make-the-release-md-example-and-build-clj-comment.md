---
id: mnt-01m46bkxkn6p
title: Make the release.md example and build.clj comment version-free
status: open
type: chore
priority: 1
mode: afk
created: '2026-10-05T15:41:16.272736273Z'
updated: '2026-10-05T15:41:16.272736273Z'
acceptance:
- title: the release.md N-reset example and the build.clj scheme comment contain no current-anchor version
  done: false
- title: bb release-check still passes
  done: false
links:
- mnt-01m46ahjn88j
---

## Description

Each bump rewrites two lines that are not coordinates: the N-reset example in docs/release.md and the 'Version scheme X.Y.Z.N' comment on line 3 of build.clj. During the 9.7.0 bump a blanket sed turned the example into '9.5.0.3 -> 9.7.0.0' and the build.clj comment was missed at first. bb release-check validates coordinates only, so it caught neither.

Remove the need to edit them. Pin the release.md example to fixed historical numbers (for example 9.4.1.3 -> 9.5.0.0) and write the build.clj comment without a version (M.m.p.N). Leave the real coordinates (README install line, release.md deps and cut steps, build.clj version) as they are, since release-check covers them.
