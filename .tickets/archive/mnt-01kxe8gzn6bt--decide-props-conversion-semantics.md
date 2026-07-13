---
id: mnt-01kxe8gzn6bt
title: 'Decide: props-conversion semantics'
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-13T17:31:17.030092082Z'
updated: '2026-07-13T19:20:30.977384679Z'
closed: '2026-07-13T19:20:30.977384679Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:grilling
deps:
- mnt-01kxe8gz8v4w
- mnt-01kxe8gzbttp
assignee: jonas.rodrigues@unisoma.com
---

## Description

## Question

Define the single generic CLJS-map -> React-props conversion the factories funnel through: kebab->camel key mapping, `:style` maps, Mantine's `styles`/`classNames` object props, the polymorphic `component=` prop, section props (`leftSection` etc.), event handlers, refs, and children / render-prop handling. What are the special cases and how are they handled?

## Notes

**2026-07-13T17:37:58.510045200Z**

Research context (R2/docgen): docgen.json DELIBERATELY strips the exact props this ticket owns — polymorphic 'component', 'className', 'styles'/'classNames', 'variant', spacing/sizing shorthands, data-*. These are shared across all Mantine components (StylesApiProps + polymorphic factory), so the generic converter must handle them uniformly rather than per-component. R1: the reference does bare clj->js with none of these conveniences — this ticket is the deliberate divergence.

**2026-07-13T19:20:24.489599424Z**

**2026-07-13T19:20:30.977384679Z**

Single public converter fn: hybrid kebab->camel keys (data-/aria- exempt), shallow-default values with curated deep-convert set {style,styles,classNames,vars} (map->convert, fn->passthrough), style leaf (--* verbatim, numeric passthrough) + class leaf (collection space-join), :class alias+merge, children path (prune nil/flatten/render-prop passthrough), component/section/renderRoot passthrough, raw JS SyntheticEvent to handlers, reserved escape-hatch merge key (plain clj->js, wins last). Controlled-input wrapping + factory call convention scoped out to codegen ticket.

## Resolution — props-conversion semantics

ONE public converter fn, called by every generated factory. Deterministic pipeline: (1) lift out escape-hatch key; (2) per entry — resolve :class/:className, key-map, value-handle; (3) merge escape-hatch raw object last. Children on a separate path.

**1. Key casing — hybrid kebab->camel.** Hyphenated keys -> camelCase (:label-position -> labelPosition); hyphen-free keys pass through (camelCase still works). EXEMPT data-* and aria-* (React requires hyphens). Round-trip-safe: every Mantine prop is camelCase/single-word; React hyphenates only data/aria. Optional perf: memoize per-keyword casing (bounded vocabulary, a la Reagent).

**2. Value handling — shallow default + curated deep-convert set.** Top-level values pass through untouched (React elements, fns, primitives survive). Deep-convert ONLY the Styles-API set: style, styles, classNames, vars. Per member: map -> convert; function -> passthrough (function-form Styles API NOT auto-wrapped in v1 — use escape hatch).

**3. Leaves.** Style leaf (:style + inner maps of styles/vars): camelCase keys, --* verbatim, values passthrough (numbers stay numeric; React adds px). Class leaf (top-level class + classNames values): string or sequential collection -> space-joined, nils/false dropped. styles/classNames/vars objects: outer selector keys camelCased; styles values -> style leaf; classNames values -> class leaf; vars inner keys verbatim --*.

**4. :class alias & merge.** Accept :class (Reagent/hiccup alias); merge with :className if both present. Map-form (:class {:active on?}) deferred past v1.

**5. Children (separate path).** Prune nil; flatten sequential collections (seqs/vectors); strings/numbers/elements passthrough; render-prop functions passthrough; :key rides in child props (React extracts it).

**6. Passthrough-only props (kept OUT of deep-convert set).** component, renderRoot, section props (leftSection/label/icon...): pure passthrough. Documented gotchas: pass a tag string or real React component to component= (never a ui-* factory); sections take built elements/strings, not hiccup. No CLJS-component->component= adapter in v1.

**7. Event handlers & refs.** Passthrough; handlers receive the RAW JS SyntheticEvent (no js->clj — expensive, lossy, framework-opinionated).

**8. Escape hatch.** One reserved merge key (spelling TBD — :& or ::raw). Value = raw JS object OR CLJS map run through PLAIN clj->js (hyphens preserved — good for SVG stroke-width, function-form styles). Merged LAST, overrides normal conversion.

**Scoped out of this ticket** (flagged onto Decide: codegen pipeline & generated output): controlled-input wrapping (SUIW wrap-form-element analogue — behavioral wrapper on a curated component set, layered AROUND the converter); factory call convention (props-map optional, arity detection).

Deliberate divergence from the reference (SUIW does bare clj->js): this is the batteries-included converter the map called for.
