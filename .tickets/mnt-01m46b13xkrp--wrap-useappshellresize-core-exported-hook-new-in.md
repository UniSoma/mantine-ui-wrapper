---
id: mnt-01m46b13xkrp
title: Wrap useAppShellResize (core-exported hook, new in 9.7.0)
status: open
type: feature
priority: 3
mode: hitl
created: '2026-10-05T15:31:00.143158669Z'
updated: '2026-10-05T15:31:00.143158669Z'
links:
- mnt-01m46ahjn88j
---

## Description

9.7.0 adds useAppShellResize, exported from @mantine/core (not @mantine/hooks): AppShell's new resize prop takes its return value to make Navbar/Aside/Header/Footer resizable. Hook generation only enumerates the @mantine/hooks barrel, and core hooks are wrapped by hand in codegen/supplements/core.cljc (only useMantineTheme, useMantineColorScheme, useComputedColorScheme today), so it does not surface. Add it to the supplement following those entries; decide how its options map and the returned instance cross the props converter (the instance is passed straight back into AppShell :resize, so it likely wants to stay raw). The wider gap — about 60 core use* exports, most of them context hooks (useComboboxContext, useTourContext...) plus a few real ones (useCombobox, useTree, useMatches, useProps) — is out of scope here; note it if a pattern emerges.
