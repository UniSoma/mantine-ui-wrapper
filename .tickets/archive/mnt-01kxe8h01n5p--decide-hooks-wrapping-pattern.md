---
id: mnt-01kxe8h01n5p
title: 'Decide: hooks wrapping pattern'
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-13T17:31:17.429585641Z'
updated: '2026-07-14T01:25:27.312539996Z'
closed: '2026-07-14T01:25:27.312539996Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:prototype
deps:
- mnt-01kxe8gzj38z
assignee: jonas
---

## Description

## Question

Define how @mantine/hooks are exposed to framework-agnostic CLJS: how each hook is enumerated/wrapped, how return values (tuples vs objects) are converted to idiomatic CLJS, and the constraints around calling hooks in React render context.

## Notes

**2026-07-14T01:25:27.219862896Z**

Resolution — hooks wrapping pattern

1. WRAP hooks in a GENERATED `mantine.hooks` .cljc namespace (one ns for the
   @mantine/hooks package), inheriting all component-codegen conventions from
   mnt-01kxe8gzrhy6: kebab-case no-prefix defs; per-symbol tree-shakeable
   requires; reader-conditional `:cljs` real / `:clj` not-implemented throw;
   auto `(:refer-clojure :exclude ...)`; within-package kebab-collision
   hard-error.

2. SCOPE: `use*` hooks only (~79). Non-hook barrel utilities (randomId, clamp,
   range, upperFirst, getHotkeyHandler, mergeRefs, readLocalStorageValue, mask
   helpers, ...) are EXCLUDED — no docstring source for them, and `range`
   collides with clojure.core. Enumerate the barrel filtering to `^use`.

3. RETURNS: ZERO conversion / raw passthrough. Tuple hooks destructure
   positionally straight off the raw JS array (no `into-array` needed); object
   hooks are read via interop (`.-hovered`); scalars as-is. NO shape table.
   Rationale: near-unanimous CLJS ecosystem convention (UIx, Helix, Reagent 2.x,
   rum, Fulcro all leave returns raw and NONE keywordize object returns); avoids
   per-render `js->clj` cost on DOM hooks; dissolves the load-bearing
   tuple-vs-object split instead of paying a curated-table tax to manage it.

4. ARGUMENTS: raw, no conversion. Callers pass JS-shaped args (`#js {...}`,
   `#js [...]`). The already-public props converter (mnt-01kxe8gzn6bt) is
   available OPT-IN for callers who want to build an options map idiomatically
   — the wrapper itself carries NO per-hook argument metadata. (A blanket
   convert-every-map heuristic was rejected: it still breaks `useToggle`, whose
   `[false true]` arg must stay a JS array.)

5. GENERATED FORM: thin def-alias with docstring —
   `(def use-x "<docstring>" #?(:cljs <js-import> :clj <not-implemented throw>))`.
   No `defn`, no factory, no per-hook logic. About as thin as generated output
   gets, and it naturally handles every arity because it IS the JS fn.

6. DOCSTRINGS: RICH. Description extracted from
   `apps/mantine.dev/src/mdx/data/mdx-hooks-data.ts` (Mantine 9.4.1 docs-app
   source) via regex over `hDocs('useX', 'description')` literals (DOTALL for
   multi-line) + one inline-object exception for `useElementSize`; verified no
   description contains a quote/apostrophe, so the parse is trivial. Plus a
   derivable `https://mantine.dev/hooks/use-*/` URL; optionally the TS signature
   from the package `.d.ts`. (The `.d.ts` JSDoc route is NOT viable for prose:
   only 23/83 source files carry any `/**`, documenting option/return props, not
   a hook-level summary.)

7. RULES-OF-HOOKS / render-context: NOT enforced — delegated entirely to React,
   matching the ecosystem norm. Documented in the ns docstring. One documented
   caveat: property access on object/handler returns needs `^js`/js-interop
   under `:advanced` compilation (inherent to raw interop).

8. BUILD-TOOLING HANDOFFS (-> mnt-01kxe8gzvp9e):
   (a) NEW committed generator input = the extracted `{hook -> description}`
       data. Symmetric with the committed `docgen.json`, because
       `mdx-hooks-data.ts` lives in the docs-app SOURCE (`apps/mantine.dev/`),
       NOT in the published `@mantine/hooks` npm package / node_modules. Extract
       once at the pinned version and commit; re-extract + re-commit on version
       bump.
   (b) Barrel enumeration of `use*` exports + per-symbol require resolution for
       `@mantine/hooks`.

References: Fulcro hooks (com.fulcrologic.fulcro.react.hooks) + CLJS-ecosystem
survey (UIx/Helix/Reagent/hx/rum) both confirm raw passthrough as the dominant
convention.

**2026-07-14T01:25:27.312539996Z**

GENERATED mantine.hooks .cljc ns, use* hooks only (~79); raw passthrough returns (no shape table) + raw JS-shaped args (opt-in props converter); thin def-alias w/ rich docstrings extracted from docs-app mdx-hooks-data.ts + mantine.dev URL; rules-of-hooks delegated to React. Build-tooling handoffs: committed {hook->description} input + barrel use* enumeration.
