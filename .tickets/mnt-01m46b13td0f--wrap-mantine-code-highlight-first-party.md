---
id: mnt-01m46b13td0f
title: Wrap @mantine/code-highlight (first-party; JsonViewer new in 9.7.0)
status: open
type: feature
priority: 3
mode: hitl
created: '2026-10-05T15:31:00.037430236Z'
updated: '2026-10-05T15:31:00.037430236Z'
links:
- mnt-01m46ahjn88j
- mnt-01kxzxsh9h5f
---

## Description

JsonViewer (new in 9.7.0) ships in @mantine/code-highlight, which the wrapper has never wrapped, so the 9.7.0 bump does not surface it. Wrap the whole package rather than JsonViewer alone: CodeHighlight, CodeHighlightTabs, InlineCodeHighlight, JsonViewer (all docgen entries), plus the non-docgen surface (CodeHighlightAdapterProvider, createHighlightJsAdapter/plainTextAdapter, normalizeCode, serializeJsonViewerPath, useCodeHighlightContext, CodeHighlightControl) via a supplement. Peers are only @mantine/core/@mantine/hooks/react, but highlighting needs an adapter (shiki or highlight.js), which stays the consumer's choice. Follow the @mantine/schedule path (9611560): add :code-highlight -> mdx-code-highlight-data.ts to extract/component-corpora (the docs data moved there in 9.7.0), install + pin, add the deps.cljs range, generate mantine.code-highlight, README row + install token, demo mount + verify-demo assertion. Note docgen also emits a bogus serializePath entry from JsonViewer.tsx; it is exported as serializeJsonViewerPath, so check the generator skips it rather than wrapping a function as a component.
