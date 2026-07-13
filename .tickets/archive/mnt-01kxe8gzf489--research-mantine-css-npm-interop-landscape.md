---
id: mnt-01kxe8gzf489
title: 'Research: Mantine CSS + npm-interop landscape'
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-13T17:31:16.836216554Z'
updated: '2026-07-13T17:36:41.818418649Z'
closed: '2026-07-13T17:36:41.818418649Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:research
assignee: agent
---

## Description

## Question

How does Mantine (pinned major) require CSS to be loaded, and how does that land in a shadow-cljs build? Capture: which packages ship CSS files and how they're imported, MantineProvider setup, PostCSS requirements, and concrete options for getting Mantine's CSS into a shadow-cljs app (npm deps, :js-options, CSS import side-effects).

Capture findings on a throwaway research/ branch with a pointer back to this ticket.

## Notes

**2026-07-13T17:36:41.818418649Z**

Findings: docs/research/mantine-css-interop.md. Mantine v9 ships pre-compiled plain CSS per package (@mantine/<pkg>/styles.css) + a layered variant (styles.layer.css) — load ONE, core first. CSS shipped by: core (+ per-component styles/*.css), dates, charts, notifications, spotlight; @mantine/modals ships NONE (reuses core). PostCSS is NOT needed by consumers (only for authoring). KEY: shadow-cljs does NOT process CSS — (:require ["@mantine/core/styles.css"]) fails. Recommended delivery: demo harness <link>s styles.css files from node_modules (core first), no extra tooling; downstream consumers use a bundler CSS entry; the wrapper itself imports NO CSS and stays CSS-agnostic. MantineProvider wraps root (all props optional in v7+); ColorSchemeScript in <head> avoids color-scheme flash.
