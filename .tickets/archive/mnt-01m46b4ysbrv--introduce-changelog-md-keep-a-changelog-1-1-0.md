---
id: mnt-01m46b4ysbrv
title: Introduce CHANGELOG.md (Keep a Changelog 1.1.0)
status: closed
type: chore
priority: 2
mode: hitl
created: '2026-10-05T15:33:05.958793372Z'
updated: '2026-10-05T15:41:16.361629475Z'
closed: '2026-10-05T15:33:57.978034911Z'
links:
- mnt-01m46ahjn88j
- mnt-01m46bkxphfw
---

## Description

Add a CHANGELOG.md in Keep a Changelog 1.1.0 format before the first non-SNAPSHOT release (Clojars has only 9.4.1/9.5.0/9.6.0 SNAPSHOTs; no git tags), so there is no history to backfill. Scope: wrapper-level changes; each anchor bump gets one line linking Mantine's release notes plus the defs it added/removed; upstream behaviour changes only when an app would silently break on them. First entry: the 9.7.0 bump under [Unreleased]. Wire it into docs/version-bump.md (add the entry) and docs/release.md (cut [Unreleased] into the version), and into the curated cljdoc tree (doc/cljdoc.edn).

## Notes

**2026-10-05T15:33:57.978034911Z**

CHANGELOG.md added in Keep a Changelog 1.1.0 format, opening with an [Unreleased] entry for the 9.7.0 bump (new Toggle/Toolbar/Tour defs, anchor line linking Mantine's notes, Tooltip hideDetached and HoverCard touch upstream notes). Wired into docs/version-bump.md (step 7 + 'Recording the bump' section), docs/release.md (cut [Unreleased] at release), doc/cljdoc.edn, and AGENTS.md (consumer-visible change -> entry in the same commit). No backfill: only SNAPSHOTs were ever published.
