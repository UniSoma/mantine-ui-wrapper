---
id: mnt-01m46b13xkrp
title: Wrap useAppShellResize (core-exported hook, new in 9.7.0)
status: closed
type: feature
priority: 3
mode: afk
created: '2026-10-05T15:31:00.143158669Z'
updated: '2026-10-05T18:32:15.703338681Z'
closed: '2026-10-05T18:32:15.703338681Z'
links:
- mnt-01m46ahjn88j
- mnt-01m46mvd1vtm
acceptance:
- title: mantine.core/use-app-shell-resize is generated from the core supplement with the docstring above, and bb generate passes the collision guard
  done: true
- title: 'The demo app-shell passes the hook''s controller via :resize, and verify-demo.mjs asserts the "Resize header" handle renders inside #app-shell'
  done: true
- title: CHANGELOG.md lists mantine.core/use-app-shell-resize under [Unreleased] / Added
  done: true
- title: bb ci passes (it runs bb generate via drift, bb build, verify-demo and bb coverage)
  done: true
---

## Description

Mantine 9.7.0 adds useAppShellResize, exported from @mantine/core (not @mantine/hooks). AppShell's new resize prop takes its return value to make Navbar/Aside/Header/Footer resizable. Hook generation only enumerates the @mantine/hooks barrel, and core hooks are wrapped by hand in codegen/supplements/core.cljc (useMantineTheme, useMantineColorScheme, useComputedColorScheme today), so the new hook does not surface.

The wider gap (about 60 core use* exports, mostly context hooks) is out of scope. It is filed separately as a coverage drift check.

## Design

### Decisions (grilled 2026-10-05)

- Supplement, not generator. Add a def-alias `use-app-shell-resize` to codegen/supplements/core.cljc after `use-computed-color-scheme`, and add `useAppShellResize` to the `:refer` list. ADR 0003's "barrel-enumerable → widen the generator" rule rests on mantine.hooks already crawling its barrel. mantine.core is docgen-driven and crawls no barrel, so ADR 0002's supplement path applies. No ADR needed.
- Raw passthrough in both directions (ADR 0002/0003). Options are JS-shaped (`#js {:navbar #js {:min 200 :max 500}}`, camelCase keys, callbacks receive JS `sizes`). The return is the raw JS controller.
- No converter work for `:resize`. `convert-value` in src/main/mantine/impl/props.cljs passes non-map, non-sequential values through (`:else v`), so `(mc/app-shell {:resize ctrl} ...)` hands Mantine the controller untouched. Do not wrap it in `no-convert`.
- Docstring, verbatim:

```clojure
(def use-app-shell-resize
  "useAppShellResize — makes AppShell sections resizable. Pass its return value
  to app-shell's :resize prop; only the sections configured in the options get a
  resize handle.

  https://mantine.dev/core/app-shell/#resizable-sections

  Raw passthrough: pass JS-shaped options (#js {:navbar #js {:min 200 :max 500}});
  returns the raw JS controller (.-sizes, .resetAll, .-navbar ...), read via
  interop (^js under :advanced)."
  #?(:cljs useAppShellResize
     :clj (f/not-implemented "mantine.core/use-app-shell-resize")))
```

- Verification through the demo. In demo/mantine/demo.cljs add `:resize (mc/use-app-shell-resize #js {:header #js {:min 48 :max 120}})` to the existing `mc/app-shell` props (call the hook in the `let` with the other hooks). In scripts/verify-demo.mjs assert that an element with `aria-label="Resize header"` renders inside `#app-shell`. That is Mantine's default handle label (AppShellResizeHandle.mjs), and the handle also carries `data-section="header"`.
- Prose. Add `use-app-shell-resize` to the backfilled-core-surface comment at demo.cljs:41-44. Leave the demo ns docstring and the verify-demo header comment alone, since both stay accurate.
- Changelog. Add `mantine.core/use-app-shell-resize` under `## [Unreleased]` / `### Added` in CHANGELOG.md, in the same commit.

## Notes

**2026-10-05T18:32:15.703338681Z**

mantine.core/use-app-shell-resize is now a def-alias in the core supplement: raw passthrough both ways, with the ticket's docstring. The demo app-shell passes its controller via :resize, verify-demo asserts the 'Resize header' handle inside #app-shell, and CHANGELOG lists it under [Unreleased] / Added. bb ci is green.
