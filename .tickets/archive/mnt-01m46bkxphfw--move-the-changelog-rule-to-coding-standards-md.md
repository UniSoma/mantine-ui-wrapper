---
id: mnt-01m46bkxphfw
title: Move the changelog rule to CODING_STANDARDS.md and check it in release-check
status: closed
type: chore
priority: 1
mode: afk
created: '2026-10-05T15:41:16.361629475Z'
updated: '2026-10-05T21:02:48.068465115Z'
closed: '2026-10-05T21:02:48.068465115Z'
acceptance:
- title: bb release-check fails when CHANGELOG.md lacks the current anchor's release-notes link, and passes on main
  done: true
- title: CODING_STANDARDS.md exists with the changelog rule; AGENTS.md carries only a one-line pointer to it
  done: true
- title: The release-check failure names CHANGELOG.md and the expected release-notes link
  done: true
- title: docs/anchor-bump.md records the CHANGELOG.md entry before its first bb ci step
  done: true
links:
- mnt-01m46b4ysbrv
tags:
- settled
---

## Description



The rule that a consumer-visible change gets a CHANGELOG.md entry in the same commit sits in AGENTS.md, which loads into every agent turn. It is a judgement call that belongs at review, and /code-review's Standards axis reads CODING_STANDARDS.md, which the repo does not have.

Create CODING_STANDARDS.md with the changelog rule and what triggers it: a def added, removed or renamed; a behavior change; an upstream change that can break an app with no code change. AGENTS.md drops the rule and keeps one line that points to CODING_STANDARDS.md.

`bb release-check` checks the part a script can: it fails when CHANGELOG.md does not contain the current anchor's release-notes link (`https://mantine.dev/changelog/<x-y-z>/`), and the failure names the file and the expected link. This catches an anchor bump with no changelog entry. It keeps passing after a release cut renames `## [Unreleased]`, because the link stays in the file. The anchor-bump procedure records the changelog entry before it runs `bb ci`, so a bump done as written passes.

### Decisions

- CODING_STANDARDS.md, at the repo root, holds only the changelog rule; later standards join it as they come up. It says when an entry is needed; CHANGELOG.md's header stays the home for what an entry looks like, and `docs/anchor-bump.md` for the bump's format.
- AGENTS.md's `## Changelog` section becomes a one-line pointer: coding standards live in `CODING_STANDARDS.md`; read it before you commit; `/code-review` checks it.
- `docs/anchor-bump.md` reorders its procedure to stage → review the generated diff → record the bump in CHANGELOG.md → `bb ci`. Its "Recording the bump" section says `release-check` enforces the anchor-link line.
- ADR 0005 is not amended. The link is a record of the bump, not a rendering of the anchor; `codegen/release_check.clj`'s header comment says why the check lives there.
- `release-check/violations` takes one new input key holding CHANGELOG.md's text, not a precomputed boolean, so the link derivation and lookup stay in the pure checker; `-main` slurps the file beside the others.
- The link derives from the anchor with dots turned to dashes (`9.7.0` → `9-7-0`); the message follows the existing violation style.
- Tests sit beside the other `release-check` tests in `codegen/plan_test.clj`: `rc-ok` gains changelog text carrying the link, and one test flags its absence.
- Every place that lists what `release-check` covers gains the item: the `bb.edn` `:doc`, the `release_check.clj` header comment and docstrings, and the OK message in `-main`.
- No CHANGELOG.md entry for this ticket: it changes repo tooling, not the consumer surface.

## Notes

**2026-10-05T21:02:48.068465115Z**

The changelog rule moved from AGENTS.md to a new CODING_STANDARDS.md, and AGENTS.md now has a one-line pointer to it. bb release-check fails when CHANGELOG.md does not contain the anchor's release-notes link (https://mantine.dev/changelog/<x-y-z>/). The tests cover both the passing and the failing case. docs/anchor-bump.md now records the changelog entry before bb ci. ADR 0005 is not amended; the release_check.clj header comment says why the check lives there. bb ci is green.
