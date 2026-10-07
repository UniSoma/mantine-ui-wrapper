---
id: mnt-01m46mvd1vtm
title: Drift-audit the use* exports of every wrapped package in bb coverage
status: closed
type: feature
priority: 3
mode: afk
created: '2026-10-05T18:22:38.641598340Z'
updated: '2026-10-07T13:07:01.465326754Z'
closed: '2026-10-07T13:07:01.465326754Z'
acceptance:
- title: The exclude list, or the exclude rule, carries the reason each excluded hook is not wrapped
  done: true
- title: bb ci passes on the current anchor
  done: true
- title: bb coverage fails, naming the export, on a use* export of any wrapped package other than @mantine/hooks that is neither wrapped nor excluded
  done: true
- title: A use<X>Context export whose X is not an export of the same package fails the check unless it is excluded by name
  done: true
- title: mantine.core defines use-combobox, use-virtualized-combobox, use-tree, use-matches, use-props, use-styles, use-direction, use-drawers-stack and use-modals-stack as raw-passthrough defs whose docstrings carry the mantine.dev URL, and CHANGELOG.md lists them under [Unreleased] / Added
  done: true
- title: mantine.form defines use-field, and CHANGELOG.md lists it under [Unreleased] / Added
  done: true
- title: docs/anchor-bump.md says what the new bb coverage failure means and how to clear it
  done: true
links:
- mnt-01m46b13xkrp
- mnt-01m46ahjn88j
tags:
- settled
---

## Description

The 9.7.0 bump added useAppShellResize to @mantine/core. No check flagged it: it was caught by reading the bump diff by hand and filed as mnt-01m46b13xkrp. Hooks that a component package exports (as opposed to @mantine/hooks, whose barrel the generator enumerates) are wrapped by hand in supplements, and nothing compares them against the package's exports. The compound-part Drift audit closed the same gap for compound parts.

`bb coverage` gains a second Drift audit. For every wrapped package except @mantine/hooks, it enumerates the package's `use*` exports and fails, naming each one, on any export that is neither a def in that package's namespace nor excluded. A Context hook (`use<X>Context` where `X` is an export of the same package) is excluded by rule. Any other unwrapped hook is excluded by name, with a reason.

At the 9.7.0 anchor @mantine/core exports 55 `use*` hooks and wraps 4. Of the other 51, 23 are Context hooks under the rule, 19 are undocumented internals (including `useMantineContext`, which has no `Mantine` export behind it), and 9 are documented standalone hooks: useCombobox, useVirtualizedCombobox, useTree, useMatches, useProps, useStyles, useDirection, useDrawersStack, useModalsStack. Outside core, @mantine/form exports `useField` (documented) and @mantine/dates exports `useDatesContext` (undocumented; no `Dates` export). This ticket wraps the 10 documented hooks, so the first green run excludes only Context hooks and undocumented internals.

### Decisions

- The audit is a per-package fn in `scripts/coverage-check.clj` beside `check-compound-parts`, run inside `bb coverage`: no new bb task, no `ci.yml` change.
- It enumerates hooks from `(:exports sources)` filtered to `^use`, with no new Node call, keeping the ADR 0004 boundary at the top of the file. @mantine/hooks is skipped; `expected-hooks` already covers it.
- "Wrapped" means a kebab def in the generated `src/main/mantine/<suffix>.cljc`, read with the existing `def-lines`.
- The Context-hook rule matches `use<X>Context` only when `X` is a top-level export of the same package. The guard reads exports, not docgen: `MenubarMenu` is an export with no docgen entry. A `…Context` name that fails the guard needs a named exclude.
- Named excludes are a `{name -> reason}` map in the check beside `compound-machinery`, not in `scope.edn`: the list is a check input, and the generator never reads it. The Context rule carries its reason in the rule's comment.
- Undocumented internals (the 18 non-Context core internals, `useMantineContext`, `useDatesContext`) are named excludes with reason "undocumented upstream", following ADR 0003's "we expose what the docs describe". A new undocumented hook on a bump fails the check until it is listed.
- Stale excludes (a name no longer exported, or now wrapped) are not flagged, matching `compound-machinery`.
- The 9 core hooks go in `codegen/supplements/core.cljc` and `useField` in `codegen/supplements/form.cljc`, each a def-alias on the `use-app-shell-resize` template: raw passthrough both directions (ADR 0002), docstring with the mantine.dev URL. The collision guard plus `bb jvm-load` and `bb build` verify them; no demo assertion per hook.
- `docs/anchor-bump.md` "Reading the generated diff" gets a bullet for the new failure: wrap the hook in its package's supplement, or add a named exclude with a reason.
- GLOSSARY.md: Drift audit is redrafted to cover `use*` exports, and Context hook is a new term. No ADR: this is ADR 0002's `bb coverage` guard applied to a sibling surface.

## Notes

**2026-10-07T13:07:01.465326754Z**

bb coverage now runs a second Drift audit over the use* exports of every wrapped package but @mantine/hooks, failing with the export's name on any that no def covers and no exclude names. Context hooks (use<X>Context with X an export of the same package) are excluded by rule; 20 undocumented hooks are excluded by name in unwrapped-hooks with reason 'undocumented upstream'. The ten documented hooks the first run found unwrapped are now wrapped: nine in mantine.core (use-combobox, use-virtualized-combobox, use-tree, use-matches, use-props, use-styles, use-direction, use-drawers-stack, use-modals-stack) and use-field in mantine.form, listed in CHANGELOG.md. docs/anchor-bump.md explains the new failure; GLOSSARY.md redrafts Drift audit and adds Context hook.
