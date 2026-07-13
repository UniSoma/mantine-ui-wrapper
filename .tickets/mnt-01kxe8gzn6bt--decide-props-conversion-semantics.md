---
id: mnt-01kxe8gzn6bt
title: 'Decide: props-conversion semantics'
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-13T17:31:17.030092082Z'
updated: '2026-07-13T17:37:58.510045200Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:grilling
deps:
- mnt-01kxe8gz8v4w
- mnt-01kxe8gzbttp
---

## Description

## Question

Define the single generic CLJS-map -> React-props conversion the factories funnel through: kebab->camel key mapping, `:style` maps, Mantine's `styles`/`classNames` object props, the polymorphic `component=` prop, section props (`leftSection` etc.), event handlers, refs, and children / render-prop handling. What are the special cases and how are they handled?

## Notes

**2026-07-13T17:37:58.510045200Z**

Research context (R2/docgen): docgen.json DELIBERATELY strips the exact props this ticket owns — polymorphic 'component', 'className', 'styles'/'classNames', 'variant', spacing/sizing shorthands, data-*. These are shared across all Mantine components (StylesApiProps + polymorphic factory), so the generic converter must handle them uniformly rather than per-component. R1: the reference does bare clj->js with none of these conveniences — this ticket is the deliberate divergence.
