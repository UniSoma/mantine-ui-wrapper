---
id: mnt-01kxf3n77ee6
title: 'Research: component docstring/metadata enrichment from docs-app MDX data'
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-14T01:25:27.406367684Z'
updated: '2026-07-14T01:32:39.670576206Z'
closed: '2026-07-14T01:32:39.670576206Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:research
assignee: agent
---

## Description

## Question

The component codegen decision (mnt-01kxe8gzrhy6) set component docstrings =
docgen prop-list + best-effort mantine.dev URL. But `docgen.json` STRIPS
component descriptions (and polymorphic/style-system props) — so component
docstrings currently carry no prose "what this component is" line.

The hooks ticket (mnt-01kxe8h01n5p) found that Mantine's docs app carries a
clean, structured, regex-extractable data source for hook descriptions:
`apps/mantine.dev/src/mdx/data/mdx-hooks-data.ts` (`hDocs('useX', 'desc')`).

Does an analogous source exist for COMPONENTS — and if so, could component
codegen extract prose descriptions (and any metadata docgen drops) from it, the
way hooks do?

Investigate at Mantine 9.4.1:
1. Whether a sibling MDX data file/structure exists for components under
   `apps/mantine.dev/src/mdx/data/` (or similar).
2. How clean/extractable it is (structured object literal vs prose-in-JSX).
3. What fields it carries — description, category, and possibly the
   polymorphic/style-system props that docgen strips.
4. Whether it is keyed by component name matching `docgen.json` entries.

If clean, adoption is a near-automatic EXTENSION of the closed codegen decision:
mirror the hooks extraction — a second committed `{component -> description/
metadata}` input that the generator merges into component docstrings.

REFINES (does not invalidate) the closed decision mnt-01kxe8gzrhy6.
NON-BLOCKING for the PoC (mnt-01kxe8h04teh) — docstring/metadata polish, not
rendering risk.

## Notes

**2026-07-14T01:32:39.579070179Z**

Findings: docs/research/component-mdx-metadata.md. VERDICT: yes — near-automatic extension of the hooks extraction. Docs-app files apps/mantine.dev/src/mdx/data/mdx-{core,dates,charts}-data.ts export Record<string,Frontmatter> as INLINE OBJECT LITERALS keyed by the EXACT PascalCase component name (matches docgen.json top-level keys). ~140+ real components across core(111)/dates(~15)/charts(~16), each with a one-line prose 'description' (the gap docgen.json leaves), e.g. Button->'Button component to render button or link', Modal->'An accessible overlay dialog'. Also recoverable (docgen drops these): polymorphic:true flag, Styles-API GROUP names (styles[]), the compound prop-key set (props[]), componentPrefix. NOT here: real Styles-API selectors/CSS-vars (separate backtick-heavy packages/@docs/styles-api/src/data/*.styles-api.ts) and category/group (unset). Extraction LOW difficulty — Mantine itself regex-parses these (scripts/llm/compile-mcp-data.ts parseMdxEntries); all 149 descriptions verified plain single-quoted single-line (no quotes/backticks/multiline). Hand-authored, re-pull per version bump. Adopt = mirror hooks: commit {component->{description,polymorphic,...}} input merged into component docstrings. Bounded caveats: (1) sub-components (Button.Group/Table.Tr) are NOT top-level keys -> inherit parent desc or stay prose-less; (2) real Styles-API selector text is a separate, harder follow-up (now fog in Not-yet-specified) — don't couple it; (3) no category data.

**2026-07-14T01:32:39.670576206Z**

CONFIRMED feasible+easy: mdx-{core,dates,charts}-data.ts carry per-component prose descriptions keyed by exact docgen.json PascalCase name (~140+), regex-extractable like hooks, plus polymorphic flag + props/styles key-sets. Adopt by mirroring the hooks extraction (committed {component->description/meta} input merged into docstrings). Real Styles-API selectors/CSS-vars = separate harder follow-up (now fog); sub-components have no own prose.
