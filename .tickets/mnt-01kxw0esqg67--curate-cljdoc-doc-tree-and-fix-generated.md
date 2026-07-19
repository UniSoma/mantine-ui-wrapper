---
id: mnt-01kxw0esqg67
title: Curate cljdoc doc tree and fix generated-docstring Markdown rendering
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-19T01:39:38.864133394Z'
updated: '2026-07-19T01:39:38.864133394Z'
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
