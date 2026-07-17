---
id: mnt-01kxr5zr86zq
title: 'Core supplements layer: backfill non-docgen @mantine/core surface + generator guards'
status: closed
type: feature
priority: 1
mode: afk
created: '2026-07-17T13:59:19.558200517Z'
updated: '2026-07-17T14:28:45.557303636Z'
closed: '2026-07-17T14:28:45.557303636Z'
acceptance:
- title: core.cljc hoisted into mantine.core exposing MantineProvider, Box, DirectionProvider, create-theme (clj->js, camelCase-only), rem, use-mantine-theme/-color-scheme/-computed-color-scheme, and compound parts menu-dropdown/menu-label/menu-divider/appshell-main; a full screen (provider + shell + menu) renders in the demo
  done: true
- title: Collision guard fails bb generate on a supplement/generated def-name clash; compound-part coverage check (in bb coverage) fails on any uncovered Capitalized static compound part
  done: true
- title: bb ci green
  done: true
tags:
- gap
---

## Description

The wrapper is purely docgen-driven, so @mantine/core exports docgen omits fall through unwrapped — today the wrapper cannot render a screen (no MantineProvider, no Box, no Menu.Dropdown, no AppShell.Main). Add a hand-written core supplement (codegen/supplements/core.cljc, hoisted like modals/notifications/spotlight) backfilling the non-docgen core surface, plus two generate-time guards so the hand-written list can't silently rot across version bumps.

Backfill contents:
- Providers/primitives (plain factories): MantineProvider, Box, DirectionProvider.
- Functions: create-theme wraps clj->js (theme keys camelCase-only — clj->js does not camelize; documented in the docstring); rem is a raw passthrough.
- Hooks (raw passthrough, per the wrapper's hook philosophy): use-mantine-theme, use-mantine-color-scheme, use-computed-color-scheme.
- Compound parts (siblings like combobox-dropdown/appshell-header are already generated): menu-dropdown, menu-label, menu-divider, appshell-main.

Guards:
- Collision guard: bb generate FAILS when a supplement def name collides with a generated def ('docgen now covers this — delete the supplement entry'), instead of the current silent last-wins redefinition.
- Compound-part coverage check (fold into bb coverage): enumerate each wrapped component's Capitalized static keys minus machinery (extend/withProps/displayName/classes/varsResolver); FAIL on any not covered by docgen or a supplement — turning a silently-unwrapped part into a loud 'wrap it or exclude it'. This check, not a prose list, is the source of truth for the backfill.

## Notes

**2026-07-17T14:28:45.557303636Z**

Added codegen/supplements/core.cljc (hoisted into mantine.core): MantineProvider/Box/DirectionProvider factories, create-theme (clj->js, camelCase-only, documented) + rem passthrough, the use-mantine-theme/-color-scheme/-computed-color-scheme hooks, and every dot-notation compound part docgen omits. The compound-part coverage check (source of truth) enumerated 61 uncovered real-export core parts (not just the 4 named), so the full set was wrapped; the 3 uncovered spotlight parts were added to its supplement too. Guards: generate.clj now throws on a supplement/generated def-name collision; coverage-check.clj enumerates each wrapped component's Capitalized static parts (minus machinery, filtered to real exports) and fails on any not covered. Demo renders a full mantine-provider + AppShell + Menu-with-Dropdown screen (verify-demo asserts app-shell-main, menu-dropdown/label/divider). bb ci green; generated sources committed.
