---
id: mnt-01kxs1gpqrcb
title: Normalize the Wrapper plan into a deep module
status: closed
type: feature
priority: 4
mode: afk
created: '2026-07-17T22:00:26.616683462Z'
updated: '2026-07-18T03:30:19.963683866Z'
closed: '2026-07-18T03:30:19.963683866Z'
tags:
- codegen
- refactor
acceptance:
- title: build is a pure fn of a sources map (no shelling out, no writes); classification/naming/docstrings/supplement-hoisting/require-merging all move behind it
  done: true
- title: emit-ns performs zero domain decisions — no kebab, no docstring lookup, no controlled-inputs, no merge-requires, no derive-exclude; it escapes and templates pre-computed fields
  done: true
- title: Collision guard throws inside build; skips and controlled-input rot are returned as :skipped/:notes data
  done: true
- title: Plan-level fixture tests run with no Node and no writes (hand-built sources map)
  done: true
- title: bb generate, bb drift, bb build, bb coverage, bb jvm-load, bb test all stay green; generated sources are byte-identical (no drift)
  done: true
- title: coverage-check.clj keeps its independent recount and does not consume build's :namespaces
  done: true
---

## Description

Insert one deep module (the Wrapper plan) between the committed codegen inputs and text emission, per ADR 0004 (docs/adr/0004-wrapper-plan-deep-module.md).

Today codegen/generate.clj has no single home for 'what to wrap and how': resolve-component (package assignment), dimension-names (scope expansion), the top-level let (component/hook/supplement-only classification), derive-exclude, and emit-component-def (which reaches into controlled-inputs AND calls component-docstring mid-emission). scripts/coverage-check.clj then duplicates the whole classification to re-derive the same surface.

Goal: a pure 'build' fn (codegen/plan.clj, stub already committed) that turns a sources map into a plan (namespaces + defs + requires + excludes + hoisted supplement body, all data). Emission collapses to a thin emit-ns that only templates and escapes. The plan is the test surface; fixtures are hand-written sources maps with no Node and no writes.

## Design

Interface (codegen/plan.clj stub is the settled shape):
- read-sources : all I/O (slurp committed EDN/JSON + Node export enumeration)
- build        : THE deep module — pure fn of sources; all domain decisions
- emit-ns      : thin — one ns-plan -> {:file :text}; owns escaping + templating only
- write-plan!  : thin — the bb generate driver (emit-ns + spit + summary)

Settled decisions (see ADR 0004, amended 2026-07-18 after grilling):
- Docstrings live UN-escaped in the plan; emit-ns escapes. emit-ns has exactly two text rules: escape docstrings, splice supplement bodies VERBATIM (a body is already valid .cljc; re-escaping corrupts it).
- Naming is a domain decision: :symbol (kebab) and :refer-clojure-exclude are pre-computed. ORDERING is keyed on :js-name, NOT kebab :symbol: within a ns, :defs + the :refer list + refer-clojure excludes sort by :js-name (the Mantine export name), matching the current emitter byte-for-byte. Kebab sort diverges under ASCII ('-' vs case) and would break the byte-identity criterion. :namespaces sort by :ns-name.
- Build failures throw; skips/warnings are data. Principle: throw IFF the emitted artifact would be wrong/ambiguous, else record data. Collision guard throws inside build (emitting = redefinition). Data, printed by write-plan!: unresolvable -> :skipped; controlled-input rot -> :notes :controlled-input-rot; multi-package name (core wins) -> :notes :multi-package (correctly resolved, so a note not a throw).
- Supplement pipeline splits three ways: read-sources does only the raw slurp (:supplements = suffix -> verbatim text); build owns ALL parse work (edamame parse, require extraction, supplement-vs-generated collision throw, declare-drop using the ns's computed def-names); emit-ns splices :body verbatim. Cleaves the current slurp-and-parse parse-supplement.
- The compound-part Drift audit stays OUT of build (needs component-statics the emitter never uses; statics stay OUT of sources too) — lives in the observation/Coverage layer.
- Coverage keeps its INDEPENDENT recount. Load-bearing independence is CLASSIFICATION, not observation: Coverage SHOULD share read-sources (same observed facts) so any divergence is provably a classification bug; the firewall is it MUST re-derive the surface itself and MUST NOT consume build's :namespaces. Load-bearing comment marks the boundary.
- REJECTED: a pluggable kind registry. The surface is closed and tiny — three def kinds (:component/:hook/:util) plus one degenerate package shape (supplement-only, emits zero defs) — against one emission target. Inline the kinds as private fns.

Preserves ADR 0001/0002/0003 — relocates existing policy behind a seam, changes none of it.

## Notes

**2026-07-18T03:29:51.987109920Z**

Code review (two-axis) found no implementation defects. Two spec-wording amendments made instead of code changes: (1) ADR 0004 ordering consequence corrected — refer-clojure excludes sort by kebab def-name, not :js-name (they mix in supplement def-names, which have no js-name; matches the emitter byte-for-byte). (2) :util kind explicitly marked reserved (Barrel utilities, mnt-01kxh6gf6ny3 — no util-docs.edn yet) in plan.clj shape comment + ADR. Interface amendment vs stub: ns-plan carries :mantine-version so emit-ns stays global-free.

**2026-07-18T03:30:19.963683866Z**

Implemented ADR 0004 (commit 5a85d69). codegen/plan.clj now hosts the four-entry-point deep module: read-sources (I/O: committed EDN/JSON + Node export enumeration), build (pure sources->plan: classification, core-precedence resolution, kebab naming, refer-clojure excludes, docstrings incl. Companion-hook page mapping, supplement parse/declare-drop/require-merge; collisions throw, skips+rot+multi-package are data), emit-ns (escape docstrings, splice supplement verbatim), write-plan! (spit + summary). generate.clj is the two-line driver; coverage-check shares read-sources for observation only with the load-bearing firewall comment. 11 fixture tests / 42 assertions on hand-built sources (bb plan-test, in bb ci). Generated sources byte-identical; drift/coverage/jvm-load/test/build green. ADR amended: excludes sort by kebab def-name; :util reserved for mnt-01kxh6gf6ny3.
