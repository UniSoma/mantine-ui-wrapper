---
id: mnt-01m46ahjn88j
title: Bump the Mantine anchor to 9.7.0
status: closed
type: chore
priority: 2
mode: hitl
created: '2026-10-05T15:22:30.952091595Z'
updated: '2026-10-05T18:22:38.641598340Z'
closed: '2026-10-05T15:32:37.323985934Z'
links:
- mnt-01m46aqnkbem
- mnt-01m46b13td0f
- mnt-01m46b13xkrp
- mnt-01m46b4ysbrv
- mnt-01m46bkxe85s
- mnt-01m46bkxgz1p
- mnt-01m46bkxkn6p
- mnt-01m46mvd1vtm
---

## Description

Move the anchor from 9.6.0 to 9.7.0: refresh codegen inputs from a 9.7.0 clone, bump every rendering (package.json pins, deps.cljs ranges, build.clj prefix, README/release prose), regenerate, verify with bb ci. 9.7.0 removed the vendored yarn (.yarn/releases + yarnPath) that bb clone-anchor relies on — fix clone-anchor to fall back to the packageManager-pinned yarn. dayjs/recharts peer ranges unchanged (>=1.0.0 / >=3.2.1).

## Notes

**2026-10-05T15:25:50.682431050Z**

Anchor at 9.7.0. 9.7.0 deleted the vendored yarn (.yarn/releases + yarnPath), which broke bb clone-anchor; it now runs the packageManager-pinned yarn (4.18.0) via npx corepack, verified end to end. The docs-data move of CodeHighlight into the new mdx-code-highlight-data.ts drops it from component-docs.edn — harmless, @mantine/code-highlight is not wrapped. Upstream compile-mcp-data.ts dropped the regex parse extract.clj mirrored; comment updated, parse unchanged and still matches the new entries. Eleven new defs: core/toggle, toolbar{,-toggle,-toggle-group,-toggle-item}, tour{,-beacon,-overlay,-root,-step,-tooltip}. Six compound statics docgen omits backfilled in codegen/supplements/core.cljc: Toolbar.Divider/Group, Tour.Body/CloseButton/Navigation/Title. No removals/renames, no dropped props; 28 existing prop tables changed (hideDetached, eventOverlapMode, schedule drop validation, withAutoCloseProgress, renderPill, HoverCard/AppShell additions). controlled-inputs.edn untouched (Toggle is active/onChange boolean; ToolbarToggleGroup is a selection group). dayjs/recharts pins unchanged (peers >=1.0.0 / >=3.2.1 did not move). bb ci green (drift, build 0 warnings, jvm-load, coverage 364 entries, plan/extract tests, release-check, cljs tests). Not wrapped: promiseNotification (mnt-01m46aqnkbem), useAppShellResize (core-exported hook, same gap as useCombobox), JsonViewer (@mantine/code-highlight unwrapped).

**2026-10-05T15:32:37.323985934Z**

Anchor moved 9.6.0 -> 9.7.0. bb clone-anchor fixed for the removed vendored yarn (now npx corepack yarn, honoring packageManager). Eleven new defs (Toggle, Toolbar family, Tour family) plus six compound-part supplements; no removals, no dropped props. bb ci green. Follow-ups: mnt-01m46aqnkbem (notifications.promise), mnt-01m46b13td0f (@mantine/code-highlight incl. JsonViewer), mnt-01m46b13xkrp (useAppShellResize).
