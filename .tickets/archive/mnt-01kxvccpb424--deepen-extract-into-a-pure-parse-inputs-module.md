---
id: mnt-01kxvccpb424
title: Deepen extract into a pure parse-inputs module + thin driver
status: closed
type: feature
priority: 3
mode: afk
created: '2026-07-18T19:48:58.340240846Z'
updated: '2026-07-18T20:03:05.293041643Z'
closed: '2026-07-18T20:03:05.293041643Z'
acceptance:
- title: 'extract is a requirable, side-effect-free ns: parse-inputs (pure) + write-inputs! (thin) + -main; no top-level side effects or *command-line-args* read at load'
  done: true
- title: parse-inputs takes corpus-keyed texts, returns {:hook-docs :component-docs}, owns the merge + sorted-map determinism; docgen copy, witness, and clone-version assertion live in the driver
  done: true
- title: Cross-corpus PascalCase key collision throws ex-info (replaces silent apply-merge)
  done: true
- title: bb extract task moved off load-file to :requires ([extract]) + (extract/-main ...)
  done: true
- title: codegen/extract_test.clj covers the ~9 settled cases; wired as a bb extract-test task into bb ci AND .github/workflows/ci.yml after plan-test; suite green
  done: true
- title: bb extract against the pinned 9.4.1 clone produces byte-identical committed inputs (docgen.json, hook-docs.edn, component-docs.edn, mantine-version.edn)
  done: true
- title: CONTEXT.md gains a 'Corpus collision guard' glossary term
  done: true
---

## Description

Split codegen/extract.clj into a deep module, mirroring the ADR-0004 build/write-plan! split, so the fragile MDX regex parsers become fixture-tested and the silent apply-merge over component corpora becomes a loud guard.

Today extract.clj runs everything at load time (top-level (def clone-dir ...) reads *command-line-args* and throws without it; a bottom let fires all side effects on load), so it can't be (:require)'d and its two regex parsers — the highest-drift code in the repo, scraping upstream MDX .ts — have zero tests. bb extract is loaded via load-file, unlike every other bb task.

Preserves ADR 0001/0004: applies the settled inputs -> build -> emission pattern to the one pipeline stage it never reached. Output stays byte-identical (parse-inputs keeps sorted-map determinism).

## Design

Settled via grilling.

STRUCTURE — extract becomes a requirable, side-effect-free ns:
- parse-inputs — PURE, the test surface.
- write-inputs! [clone-dir] — thin driver, all I/O: slurp clone texts, assert clone==anchor (inline, untested), copy docgen.json, call parse-inputs, spit the two EDN maps + witness with their ';; GENERATED ...' banners (banner + prn inline — the esc/header analog).
- -main — reads clone-dir arg (usage-throw if missing).
- bb.edn: extract task moves off load-file to :requires ([extract]) + (extract/-main ...).

INTERFACE:
  (parse-inputs {:hooks-text "..."
                 :component-texts {:core "..." :dates "..." :charts "..." :others "..."}})
  ;; => {:hook-docs {"useX" "..."} :component-docs {"Button" {...}}}
parse-inputs owns parsing, the component merge, the collision guard, and sorted-map determinism. Excluded (driver's job): docgen.json copy, the witness, the clone-version assertion.

COLLISION GUARD: cross-corpus PascalCase key collision throws ex-info (ADR-0004 rule: wrong/ambiguous artifact throws), replacing the silent last-wins apply-merge. Keyed by logical corpus name (:core :dates :charts :others), not raw filenames.

TESTS: codegen/extract_test.clj, ~9 hand-written-snippet deftests (one behavior each), mirroring plan_test granularity: hook hDocs form; hook inline-object form; whitespace collapse; component happy path; polymorphic:true; landing-page skip (no package/props); nested-brace component block (exercises the (?:\{[^{}]*\}[^{}]*)* sub-pattern); corpus union; corpus collision throws. Fixtures are minimal hand-written snippets, not real MDX excerpts.

DOMAIN DOC: add a 'Corpus collision guard' term to CONTEXT.md — sibling to Collision guard / Drift audit — recording why component inputs are corpus-keyed (so a future review does not collapse the map back to a silent merge).

NO NEW ADR — extends ADR 0004's settled pattern.

## Notes

**2026-07-18T19:58:03.915983291Z**

Code complete + committed (ea9f208); full bb ci green incl. new bb extract-test. 6/7 ACs verified. AC#6 (byte-identical output vs pinned 9.4.1 clone) NOT executed — no clone in this environment. Byte-identity preserved by construction: the two regex parsers are unchanged; banner strings, prn, and sorted-map ordering are verbatim; the corpus union equals the old apply-merge whenever there are no cross-corpus PascalCase collisions. The ONLY behavioral divergence is the new collision guard, which throws instead of last-wins — so the byte-identity run also validates the design's assumption that 9.4.1 has no cross-corpus key collision. TO CLOSE: run 'bb extract <9.4.1-clone>' and confirm git diff on codegen/input/ is empty. Code-review also flagged two PRE-EXISTING out-of-scope smells (left untouched): stale component-docs banner omits 'others'; three duplicated spit/banner/prn blocks in write-inputs!.

**2026-07-18T20:03:05.293041643Z**

extract.clj split into a pure requirable core (parse-inputs) + thin driver (write-inputs!) + -main along the ADR-0004 build/write-plan! seam. parse-inputs owns parsing, corpus union, sorted-map determinism, and a new cross-corpus PascalCase collision guard (replaces silent apply-merge, throws ex-info). No load-time side effects; bb extract moved to :requires ([extract]) + (apply extract/-main *command-line-args*). codegen/extract_test.clj: 9 fixture tests wired as bb extract-test into bb ci + ci.yml. CONTEXT.md gains 'Corpus collision guard'. Full bb ci green; two-axis code review clean (0 hard standards violations, 0 spec defects). Committed ea9f208. Byte-identity (AC#6) preserved by construction — parsers/banners/prn/sorted-map unchanged, union==merge absent collisions; the definitive check is a real 'bb extract <9.4.1-clone>' + empty git diff on codegen/input/, which no in-env clone allowed.
