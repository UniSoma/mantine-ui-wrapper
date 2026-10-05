---
id: mnt-01m46bkxphfw
title: Move the changelog rule to CODING_STANDARDS.md and check it in release-check
status: open
type: chore
priority: 1
mode: hitl
created: '2026-10-05T15:41:16.361629475Z'
updated: '2026-10-05T15:41:16.361629475Z'
acceptance:
- title: CODING_STANDARDS.md exists with the changelog rule; AGENTS.md no longer carries it
  done: false
- title: bb release-check fails when CHANGELOG.md lacks the current anchor's release-notes link, and passes on main
  done: false
links:
- mnt-01m46b4ysbrv
---

## Description

mnt-01m46b4ysbrv added a rule to AGENTS.md: a consumer-visible change gets a CHANGELOG.md entry in the same commit. AGENTS.md loads into every agent turn, while this rule is a judgement call that belongs at review. The repo has no CODING_STANDARDS.md, so /code-review's Standards axis has no repo-specific source.

Create CODING_STANDARDS.md with the changelog rule (what counts as consumer-visible: a def added, removed or renamed, a behavior change, an upstream change that breaks an app with no code change) and remove the line from AGENTS.md. Add the mechanical part to bb release-check: fail when CHANGELOG.md does not contain the anchor's release-notes link (https://mantine.dev/changelog/<x-y-z>/). This catches a bump with no entry and keeps passing after the release cut.

hitl: deciding what else belongs in a first CODING_STANDARDS.md is a human call.
