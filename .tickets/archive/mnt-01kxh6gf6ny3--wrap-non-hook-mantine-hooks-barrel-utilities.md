---
id: mnt-01kxh6gf6ny3
title: Wrap non-hook @mantine/hooks barrel utilities
status: closed
type: feature
priority: 4
mode: afk
created: '2026-07-14T20:53:46.325721780Z'
updated: '2026-07-18T04:29:07.078712177Z'
closed: '2026-07-18T04:29:07.078712177Z'
acceptance:
- title: mantine.hooks JVM-loads and at least one utility (e.g. randomId) is exercised end-to-end; verify loop green
  done: true
- title: 'Barrel enumerated as everything-minus-excludes: non-use* utilities emit into mantine.hooks as raw-passthrough aliases via a :util def-kind; normalizeRadialValue excluded (plus any other export lacking a doc entry, e.g. verify clampUseMovePosition); range auto-excluded via the existing refer-clojure-exclude path; the randomId-is-skipped behavior/test flipped'
  done: true
- title: New codegen/input/util-docs.edn ({:desc,:page} per entry) + util-docstring fn; doc URL derived from JS name; hooks-ns-plan routes use*->hook-docstring, else->util-docstring; a barrel util missing its util-docs.edn entry fails the build
  done: true
---

## What to build

Expose the non-hook plain functions of the `@mantine/hooks` barrel (`randomId`,
`mergeRefs`, `getHotkeyHandler`, the `*Mask` family, `read*StorageValue`, `clamp`,
`range`, `upperFirst`, …) to CLJS consumers as raw-passthrough def-aliases in
`mantine.hooks`, alongside the existing hooks.

Per ADR 0003, this is done by **widening the generator's barrel enumeration**, not a
supplement. The barrel is already crawled and filtered to `use*`; relax that to
everything-minus-excludes so the utilities flow through the same `hooks-ns-plan` ->
`emit-def` path as the hooks. Utilities render via a `:util` def-kind (the
raw-passthrough shape already reserved in the `def-plan` comment). `refer-clojure-exclude`
already covers `range` via the existing `clojure-core-names` filter — no new mechanism.

Docstrings are hand-authored (upstream has no docgen/JSDoc — hand-written MDX): a new
`codegen/input/util-docs.edn` (`{:desc, :page}` per entry) and a `util-docstring` fn.
The doc URL is derived from the JS name (lowercase, separators stripped) ->
`guides/functions-reference/#name` or `hooks/<slug>/#name`. `hooks-ns-plan` routes each
def: `use*` -> `hook-docstring`, else -> `util-docstring`. A barrel utility enumerated
but missing its `util-docs.edn` entry **fails the build** — the drift guard, which also
forces any further undocumented export into the exclude list.

Excludes are everything-minus-documented: `normalizeRadialValue` is the known
undocumented exclude; confirm whether `clampUseMovePosition` is documented — if not, it's
a second exclude (the drift guard enforces this either way).

The old "`randomId` is skipped as a non-`use*` export" behavior — and its test — is
flipped.

## Blocked by

None — can start immediately. (The deep-module normalization it builds on,
mnt-01kxs1gpqrcb, is already closed.)

## Notes

**2026-07-18T04:27:00.506005817Z**

Implemented: widened build's barrel enumeration to all @mantine/hooks exports (dropped the use* filter); hooks-ns-plan routes use*->hook-docstring, else->:util/util-docstring. New codegen/input/util-docs.edn (16 documented utils, {:desc :page}); util-docstring derives anchored URL (https://mantine.dev/<page>/#<lowercased,separators-stripped>) and throws if an enumerated util lacks an entry (drift guard). emit-def gained a (:hook :util) case (same raw-passthrough alias shape). Research finding: clampUseMovePosition IS documented (hooks/use-move#clampusemoveposition), so it is INCLUDED, not excluded; the sole exclude is normalizeRadialValue (no upstream docs), set in scope.edn :hooks :exclude. range flows through as a :util and is auto refer-clojure-excluded. randomId-skip test flipped to assert a :util def + a new barrel-not-exported skip test. Verified: bb plan-test (15/55 green), bb generate idempotent, hooks=103 defs (87+16), jvm-load OK (samples assign-ref util), verify-demo exercises mh/random-id end-to-end, bb test green.

**2026-07-18T04:29:07.078712177Z**

Widened barrel enumeration to everything-minus-excludes; 16 documented @mantine/hooks utilities now emit into mantine.hooks as raw-passthrough :util aliases (87->103 defs). New util-docs.edn + drift-guarded util-docstring; normalizeRadialValue sole exclude (clampUseMovePosition documented, included); range auto refer-clojure-excluded. Commit 4677784. plan-test 15/55, generate idempotent, jvm-load, verify-demo (mh/random-id end-to-end), cljs suite all green. Reviewed both axes.
