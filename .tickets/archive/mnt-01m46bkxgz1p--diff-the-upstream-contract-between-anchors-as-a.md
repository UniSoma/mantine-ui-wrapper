---
id: mnt-01m46bkxgz1p
title: Diff the upstream contract between anchors before clone-anchor
status: closed
type: chore
priority: 1
mode: afk
created: '2026-10-05T15:41:16.186886658Z'
updated: '2026-10-05T20:13:22.139972163Z'
closed: '2026-10-05T20:13:22.139972163Z'
acceptance:
- title: bb upstream-diff fetches the previous anchor tag into the target/mantine-<anchor> clone, making the clone when absent, and bb clone-anchor reuses that clone
  done: true
- title: with HEAD at 9876c38^ and the pins bumped to 9.7.0, bb upstream-diff lists .yarn/releases/yarn-4.18.0.cjs and mdx-code-highlight-data.ts
  done: true
- title: with HEAD's anchor equal to package.json's, bb upstream-diff prints that there is nothing to diff and exits 0
  done: true
- title: the docs/anchor-bump.md procedure runs bb upstream-diff after the pin bump and before bb clone-anchor
  done: true
- title: docs/anchor-bump.md has an Upstream contract section that points at the path list in code and does not restate it
  done: true
links:
- mnt-01m46ahjn88j
tags:
- settled
---

## Description

When Mantine changes the files that the clone, docgen and extract steps depend on, the release notes often don't mention it. In 9.7.0 the vendored yarn under `.yarn/releases` was removed, and `bb clone-anchor` failed at `yarn install`. Only a source diff between the 9.6.0 and 9.7.0 tags showed why. `bb extract` now catches a moved docs-data file by itself. Toolchain changes still get through.

After the pins are bumped, `bb upstream-diff` prints the `git diff --stat` between the previous anchor and the new anchor over the upstream contract: the upstream paths those steps depend on. It makes the `target/mantine-<anchor>` clone if it doesn't exist, and fetches the previous anchor tag into it. `bb clone-anchor` then reuses that clone. When the previous anchor equals the new one, it prints that there is nothing to diff. `docs/anchor-bump.md` runs it as the step after the pin bump, and gets an "Upstream contract" section that explains the contract and points to where the path list lives.

### Decisions

- The contract path list has one home: a def in `codegen/clone_anchor.clj`, one comment per path naming what reads it. The "Upstream contract" section of `docs/anchor-bump.md` points at that def and does not restate the paths.
- The paths are `.yarn/`, `.yarnrc.yml`, root `package.json` (for its `packageManager` field), `scripts/docgen/`, `apps/mantine.dev/src/mdx/data/` and `apps/mantine.dev/src/mdx/mdx-data.ts`. `apps/mantine.dev/src/.docgen/` stays out: upstream gitignores it, so it has no diff between tags.
- `bb upstream-diff` is its own task, run between the pin bump and `bb clone-anchor`; `clone-anchor` does not print the diff. It must run before `clone-anchor`'s `yarn install`, the step a toolchain change breaks.
- The task lives in `clone_anchor.clj` beside `-main`. The `target/mantine-<anchor>` path and the clone-if-absent block move into one fn both entries call.
- The previous anchor is `anchor/anchor-version` over `git show HEAD:package.json`; `mantine-version.edn` is not read.
- The clone stays `--depth 1`; the previous tag arrives by `git fetch --depth 1 origin tag <old>`. No `--filter=blob:none`.
- Output is `git diff --stat` only, no patch.
- When the anchors are equal, the task prints one line saying there is nothing to diff and exits 0.
- The new procedure step renumbers the rest; the "step 3" reference in "What the pipeline cannot see" follows the new number.
- No `CHANGELOG.md` entry: internal tooling, invisible to consumers.

## Notes

**2026-10-05T20:13:22.139972163Z**

bb upstream-diff (codegen/clone_anchor.clj) prints the git diff --stat of the upstream contract between HEAD's anchor (anchor-version over git show HEAD:package.json) and package.json's, in the target/mantine-<anchor> clone. It makes a --depth 1 clone when absent through clone!, which clone-anchor now shares, and fetches the old tag with --depth 1. When the anchors are equal it prints one line and exits 0. The contract list (.yarn, .yarnrc.yml, package.json, scripts/docgen, mdx/data, mdx-data.ts) lives in upstream-contract, with one comment per path and the exclusions stated (.docgen is gitignored upstream; core package.json changes only by version). docs/anchor-bump.md runs it as step 2, before clone-anchor, renumbers the later steps and gains an Upstream contract section; GLOSSARY.md gains Upstream contract. Verified: equal anchors print the nothing-to-diff line and exit 0; a worktree at 9876c38^ with pins at 9.7.0 cloned, fetched 9.6.0 and listed .yarn/releases/yarn-4.18.0.cjs and mdx-code-highlight-data.ts; bb clone-anchor reused the clone with codegen/input unchanged. bb ci green.
