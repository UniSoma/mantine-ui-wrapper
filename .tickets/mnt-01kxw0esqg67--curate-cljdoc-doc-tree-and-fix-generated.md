---
id: mnt-01kxw0esqg67
title: Curate cljdoc doc tree and fix generated-docstring Markdown rendering
status: in_progress
type: task
priority: 2
mode: hitl
created: '2026-07-19T01:39:38.864133394Z'
updated: '2026-07-20T13:06:53.441508580Z'
tags:
- codegen
- docs
external_refs:
- https://cljdoc.org/d/io.github.unisoma/mantine-ui-wrapper/9.4.1.0-SNAPSHOT/doc/readme
acceptance:
- title: |-
    - `doc/cljdoc.edn` exists and its `:cljdoc.doc/tree` lists only curated `.md` files; internal docs (`agents/`, `prototypes/`, `research/`) no longer appear in cljdoc's articles sidebar.
    - Generated component docstrings render on cljdoc with each prop on its own line, not collapsed into a paragraph.
    - `mantine.impl.*` implementation namespaces are excluded from the cljdoc API section.
    - Docstring change is produced through codegen (`bb generate` / `codegen/plan.clj`), not hand-edited into generated files, and existing codegen tests still pass.
    - A rebuilt cljdoc SNAPSHOT (via the `bb cljdoc` task) reflects the fixes.
  done: false
---

## Description

The cljdoc site for the library needs polish (https://cljdoc.org/d/io.github.unisoma/mantine-ui-wrapper/9.4.1.0-SNAPSHOT/doc/readme). Two distinct problems:

1. Unfiltered doc tree. There is no `doc/cljdoc.edn`, so cljdoc auto-discovers and publishes the entire `docs/` folder — including reader-irrelevant internals (`docs/agents/`, `docs/prototypes/`, `docs/research/`, `docs/adr/`, `docs/release.md`, `docs/version-bump.md`). We want an explicit, curated list of the `.md` files that belong in the published articles tree.

2. Smashed-together prop docstrings. Generated component docstrings put each prop on its own source line (`codegen/plan.clj:150-171`, `prop-lines`), but cljdoc renders docstrings as Markdown, which collapses single newlines into spaces — so every prop runs together into one wall of text (see the `button` docgen block). The prop descriptions also contain literal `<code>...</code>` from Mantine's docgen source.

## Design

- Doc tree: Add `doc/cljdoc.edn` with an explicit `:cljdoc.doc/tree` listing only the articles readers should see (README plus a curated selection, e.g. `CONTEXT.md` and the ADRs if desired). Exclude the agent/prototype/research/release internals. `build.clj` already anticipates `doc/cljdoc.edn`, so SCM linking will pick it up.
- Docstring formatting: Make `prop-lines` / `component-docstring` emit Markdown that survives cljdoc's renderer — e.g. blank-line-separated list items or a trailing hard-break (`  \n`) between props, so each prop stays on its own rendered line. Decide whether to keep or strip the embedded `<code>` HTML. Regenerate and confirm the rendered output.
- Impl namespaces: Mark `mantine.impl.*` namespaces `^:no-doc` (cljdoc hides no-doc namespaces/vars) so only the public API surface is documented.

## Notes

**2026-07-20T13:01:27.878557647Z**

Design settled via grilling session (ADR 0007 written: docs/adr/0007-generated-docstrings-are-markdown-safe.md).

DECISIONS:

1. Doc tree — new doc/cljdoc.edn with explicit :cljdoc.doc/tree:
   [["Readme" {:file "README.md"}]
    ["Concepts & vocabulary" {:file "CONTEXT.md"}]
    ["Architecture decisions"
     ["Releasing to Clojars"           {:file "docs/adr/0001-clojars-release-process.md"}]
     ["Backfilling non-docgen surface" {:file "docs/adr/0002-backfilling-non-docgen-surface.md"}]
     ["Generated hooks barrel"         {:file "docs/adr/0003-hooks-barrel-utilities-generated-not-supplemented.md"}]
     ["Wrapper plan (deep module)"     {:file "docs/adr/0004-wrapper-plan-deep-module.md"}]
     ["Single-sourced Mantine anchor"  {:file "docs/adr/0005-single-source-the-mantine-anchor.md"}]
     ["Deep-by-default converter"      {:file "docs/adr/0006-...raw-escape.md"}]]]
   Internal dirs (agents/prototypes/research), release.md, version-bump.md are omitted (drop from sidebar). ADR 0007 to be added to this tree too when created.

2. Docstring formatting (through codegen, codegen/plan.clj prop-lines/component-docstring) — Markdown bullet per prop:
   - **{name}** `{type}`{ **(required)**}{ _(default `{d}`)_} — {description}
   Rationale: bullet list chosen over fenced code block for native cljdoc typography. Type in backticks so |, ", and TS-generic <...> are literal. 'Props (docgen X):' header + blank line precede the list so Markdown parses it.

3. Description sanitizer — escape-then-convert invariant: FIRST escape any literal backtick already in the raw description, THEN convert <code>...</code> -> backticks. Guarantees only our emitted (balanced) code spans are active. Fixes the one genuinely broken prop (DateTimePicker.defaultTimeValue upstream stray-backtick typo) + future ones. docgen.json is NOT patched (verbatim upstream copy per codegen/extract.clj).
   Audit basis (Explore agent over all 4530 non-empty descriptions): zero prose hazards for _ * < > | ; two [text](url) render as intended links; multiline '- ' descriptions flattened by squash-ws.

4. Impl namespaces — add ^:no-doc to ns forms of mantine.impl.factory (factory.cljc) and mantine.impl.props (props.cljs). Direct source edits (hand-written, not codegen); the 'must be codegen' criterion applies only to the component-docstring change.

VERIFY: bb generate/drift regenerates + codegen tests pass; spot-check rendered bullets (action-icon, DateTimePicker.defaultTimeValue); bb cljdoc rebuilds SNAPSHOT.

Implementation not yet started — awaiting go-ahead.

**2026-07-20T13:06:53.441508580Z**

Implemented per settled design:
- doc/cljdoc.edn added with explicit :cljdoc.doc/tree (README, CONTEXT.md, ADRs 0001-0007); internal dirs/release.md/version-bump.md dropped from sidebar.
- codegen/plan.clj: prop-lines now emits Markdown bullet per prop (- **name** `type`{ **(required)**}{ _(default `d`)_} — desc); added md-description sanitizer (escape-then-convert: escape literal backticks FIRST, then <code>...</code> -> backticks); Props header now followed by a blank line so cljdoc parses the list.
- ^:no-doc added to ns forms of mantine.impl.factory and mantine.impl.props.
- codegen/plan_test.clj updated to new bullet format + new prop-description-is-markdown-safe test.

VERIFIED: bb plan-test 27 tests/78 assertions 0 fail; bb generate regenerated 9 ns; bb jvm-load OK; no <code> leftover in any generated ns; stray-backtick prop (DateTimePicker.defaultTimeValue) neutralized (escaped + balanced span, no doc-eating open span).

REMAINING (user-gated): 'bb cljdoc' rebuild reflects fixes only after a new SNAPSHOT is DEPLOYED to Clojars (analysis is cached against the deployed artifact). That is an outward publish step — awaiting go-ahead. Generated sources also need committing for bb drift to pass.
