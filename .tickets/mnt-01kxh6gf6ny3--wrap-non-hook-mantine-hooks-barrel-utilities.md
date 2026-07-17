---
id: mnt-01kxh6gf6ny3
title: Wrap non-hook @mantine/hooks barrel utilities
status: open
type: feature
priority: 4
mode: hitl
created: '2026-07-14T20:53:46.325721780Z'
updated: '2026-07-17T20:43:48.871446157Z'
acceptance:
- title: At least one utility verified; JVM-loads; verify loop green
  done: false
- title: '16 documented utilities generated into mantine.hooks: use* filter relaxed to everything-minus-excludes, normalizeRadialValue excluded, emit-hook-def reused (raw-passthrough aliases); range handled by refer-clojure :exclude'
  done: false
- title: codegen/input/util-docs.edn ({:desc,:page} per entry) + util-docstring fn; doc URL derived from JS name; emit-hooks-ns routes use*->hook-docstring else util-docstring; a barrel util missing its util-docs.edn entry fails the build
  done: false
---

## Description

Expose the ~16 documented non-hook plain functions from the @mantine/hooks barrel (randomId, getHotkeyHandler, mergeRefs, the *Mask family, read*StorageValue, clamp, range, upperFirst, ...) to CLJS consumers.

Design resolved in a grill-with-docs session (see docs/adr/0003 + CONTEXT.md 'Barrel utility'):
- Scope: everything-minus-excludes over the barrel. Wrap all 16 documented utilities; exclude ONLY normalizeRadialValue (undocumented -> same rule as docgen omissions). CLJS-redundant ones (clamp/range/upperFirst/lowerFirst) are still wrapped; they're namespaced and harmless, and curation would break the mechanical-parity promise.
- Mechanism: GENERATED, not supplement. mantine.hooks is barrel-enumerated (not docgen-driven), so ADR 0002's backfill path does not apply. Relax the use* filter to everything-minus-excludes; reuse emit-hook-def verbatim.
- Path: same ns, mantine.hooks (1:1 package->ns). Soften the ns docstring to note plain-function members.
- Conversion: raw passthrough both directions (consistent with sibling hooks; forced by reusing emit-hook-def).
- Docstrings: new codegen/input/util-docs.edn ({:desc, :page} per entry) + a util-docstring fn. URL derived from JS name (lowercase, separators stripped) -> functions-reference/#name OR hooks/<hook-slug>/#name. emit-hooks-ns routes use* -> hook-docstring, else -> util-docstring. Missing entry MUST fail the build (drift guard). Upstream has no structured source (hand-written MDX, no JSDoc, not in docgen) so descriptions are hand-copied.

## Notes

**2026-07-17T20:43:48.871446157Z**

Design settled in grill-with-docs session (2026-07-17). Recorded: docs/adr/0003-hooks-barrel-utilities-generated-not-supplemented.md + CONTEXT.md term 'Barrel utility'. Key call: GENERATED not supplement (mantine.hooks is barrel-enumerated, so ADR 0002 backfill path doesn't govern); everything-minus-excludes with normalizeRadialValue the sole exclude; raw passthrough; same ns; hand-authored util-docs.edn (upstream has no structured source — MDX prose, no JSDoc, not in docgen). Emission-path + enumeration decisions are now DECIDED/DOCUMENTED (were the old ACs); remaining ACs are implementation.
