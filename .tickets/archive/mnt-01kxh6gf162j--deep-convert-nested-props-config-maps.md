---
id: mnt-01kxh6gf162j
title: Deep-convert nested *Props config maps
status: closed
type: feature
priority: 4
mode: afk
created: '2026-07-14T20:53:46.150505173Z'
updated: '2026-07-18T23:52:11.816732179Z'
closed: '2026-07-18T23:52:11.816732179Z'
acceptance:
- title: convert recurses into nested maps and vectors-of-maps by default, camelizing keys and applying :class/:style/:styles/:classNames/:vars leaf handling at every depth
  done: true
- title: :inner-props (denylist) passes through as a raw CLJS map; a context modal round-trips CLJS domain data end-to-end in the demo
  done: true
- title: A wrapper-type raw escape helper skips conversion for any tagged value and survives merge/select-keys
  done: true
- title: A nested *Props map (modal cancelProps) and a vector-of-maps (spotlight :actions) verified round-tripping in the demo
  done: true
- title: Top-level conversion, :& escape hatch, and children path unchanged; verify loop green
  done: true
- title: 'Docs synced with the new semantics: mantine.impl.props ns docstring rewritten; codegen/supplements/modals.cljc open docstring fixed (drop ''shallow passthrough'') and regenerated into src/main/mantine/modals.cljc; README props section updated (deep-by-default + :inner-props denylist + raw escape)'
  done: true
---

## Description

Nested config maps (modal :confirm-props/:cancel-props/:group-props/:labels, spotlight :actions, and *Props generally) get the same camelizing conversion as top-level props, so consumers write idiomatic CLJS all the way down. Today the converter is shallow-default, so a nested CLJS map lands on the JS props object untouched and Mantine reads none of it — the styled control silently renders as a bare default.

DESIGN — settled by grilling; full rationale + rejected alternatives in docs/adr/0006-converter-deep-by-default-with-camelize-denylist-and-raw-escape.md. Read the ADR before implementing.

Deep-by-default (NOT a curated allowlist): mantine.impl.props/convert recurses into nested maps and vectors-of-maps automatically, applying the SAME rules at every depth (kebab->camel keys with data-/aria-/-- exempt, and :class/:style/:styles/:classNames/:vars leaf handling). Deep-by-default is what makes this reach GENERATED factories too (e.g. spotlight :actions): factory calls (p/convert props) with no opts (src/main/mantine/impl/factory.cljc:31), so an opt-in allowlist could not have reached them without generator changes — this design needs none. Camelization is KEPT. The :& escape hatch (raw JS, merged last) and the children path are UNCHANGED.

Raw-passthrough safety (the old shallow default gave this for free):
- Denylist #{\"innerProps\"} matched against the camelized key ck. The key still camelizes (:inner-props -> innerProps so Mantine's context-modal machinery finds it) but the VALUE passes through as a raw CLJS map (gobj/set o ck v, no recurse). This is the context-modal slot: open-context-modal hands :inner-props to a CLJS modal component that reads it as a CLJS map; converting it would destroy the CLJS->CLJS handoff (qualified keywords become dead string keys; sets/records mangle). Global denylist is safe: grep confirms innerProps is on ZERO generated components (only the hand-written open-context-modal uses it).
- raw escape helper: a WRAPPER VALUE (deftype Raw), not bare metadata — metadata is silently stripped by merge/select-keys/rebuilds and would reintroduce the bug when a real app assembles the payload. convert checks (instance? Raw v) at each node before recursing; if tagged, emit (.-v v) untouched. Works at any depth.

IMPLEMENTATION (file by file):
1. src/main/mantine/impl/props.cljs — convert recurses: for a converting key, cond on the value — (instance? Raw v) -> emit (.-v v); (map? v) -> (convert v); (sequential? v) -> JS array of (map #(if (map? %) (convert %) %) v); else -> passthrough (today's behavior: fns/elements/primitives/#js survive). Denylisted ck -> emit v raw (no recurse). Keep the existing :class/:style/:styles/:classNames/:vars leaf clauses and the :& lift-and-merge-last. Do NOT stringify keyword VALUES (convert is not clj->js; string values stay the consumer's responsibility, unchanged). Rewrite the ns docstring (it currently describes shallow-default + curated deep set + refs mnt-01kxe8gzn6bt).
2. src/main/mantine/impl/props.cljs — add (deftype Raw [v]) + (defn raw [x] (->Raw x)) + the denylist constant.
3. codegen/supplements/core.cljc — add a hoisted public (def raw mantine.impl.props/raw) with a docstring (precedent: create-theme, rem live here and hoist into mantine.core). Consumers call (mc/raw {...}). The Raw type + logic stay in impl.props so the converter has no dependency on mantine.core.
4. codegen/supplements/modals.cljc:26-27 — fix the open docstring: drop 'nested *Props supply JS-shaped values (shallow passthrough)'; describe deep conversion. Then bb generate to regenerate src/main/mantine/modals.cljc (ADR 0004: supplement is source of truth; do not edit the generated file directly). The :inner-props '...RAW' lines stay accurate (now backed by the denylist).
5. demo/mantine/demo.cljs — prove three round-trips end to end: (a) register a context modal on mm/provider (currently {} at :158) and open it via modals/open-context-modal with an :inner-props CLJS map carrying qualified keywords + a fn; the modal body reads them as a CLJS map and acts on them (denylist round-trip); (b) rewrite the spotlight :actions (currently #js at :146-147) as plain CLJS [{...}] with :left-section/:on-click (vector-of-maps round-trip); (c) an open-confirm-modal with kebab :confirm-props {:color \"red\" :left-section ...} (nested *Props round-trip). Keep verify-demo.mjs assertions green.
6. src/test/mantine/impl/props_test.cljs — add: nested map recurses (kebab->camel at depth), vector-of-maps recurses, denylisted :inner-props passes raw (keyword lookup still works on the emitted value), Raw wrapper skips conversion and survives (merge default (raw {...})) then convert, :& still merges last at depth. Check camelcase-props-pass-through-as-identity only if it feeds a map value.
7. README.md:73 — update 'styles and class-names maps convert recursively' to state deep-by-default + :inner-props denylist + (mc/raw ...) escape. Line 72 (kebab->camel) stays.

VERIFY: bb ci (full local pipeline: drift, build, verify-demo, jvm-load, coverage, plan-test, extract-test, release-check, test). Must end green.

NON-GOALS: no camelization removal; no keyword-value stringification; no generator/plan changes (deep-by-default is a pure runtime-converter change); :& and children path untouched. Supersedes the value-handling half of mnt-01kxe8gzn6bt (key-casing, :&, children stand).

## Notes

**2026-07-18T23:12:04.287266648Z**

Design locked via grilling. ADR 0006 records it: deep-by-default converter (not the curated allowlist this ticket originally scoped) + denylist(:inner-props) + wrapper-type raw escape helper; camelization kept. SUIW researched as baseline (blind clj->js — rejected: kills kebab keys, mangles CLJS payloads, no keep-as-CLJS escape).

**2026-07-18T23:49:15.909438281Z**

Implementation complete pending review: deep-by-default convert (convert-value recursion) + Raw deftype + innerProps denylist in impl/props.cljs; raw hoisted into mantine.core via supplement; modals open docstring fixed + regenerated; demo proves confirm-props/cancel-props/labels, :inner-props context modal (qualified kw + fn), spotlight :actions vector-of-maps — all asserted in verify-demo.mjs; README updated. bb ci green except drift (uncommitted regen; passes post-commit).

**2026-07-18T23:52:11.816732179Z**

Deep-by-default converter shipped per ADR 0006: convert recurses nested maps + vectors-of-maps with kebab->camel and leaf handling at every depth; innerProps denylist keeps the context-modal CLJS handoff raw; Raw wrapper type exposed as mantine.core/raw (survives merge/select-keys). :&, children, camelization unchanged. Demo + verify-demo prove confirm/cancel-props, :inner-props (qualified kw + fn), and spotlight :actions round-trips; docs synced (props ns docstring, modals open docstring + regen, README). bb ci fully green. Commit c2b471e.
