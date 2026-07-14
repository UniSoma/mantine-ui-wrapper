---
id: mnt-01kxe8gzyj13
title: 'Decide: imperative-API wrapping pattern'
status: closed
type: task
priority: 2
mode: hitl
created: '2026-07-13T17:31:17.330854343Z'
updated: '2026-07-14T02:20:58.954680945Z'
closed: '2026-07-14T02:20:58.954680945Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:prototype
deps:
- mnt-01kxe8gzj38z
assignee: jonas.rodrigues@unisoma.com
---

## Description

## Question

Define the pattern for wrapping the imperative extensions (notifications/modals/spotlight): how to expose the provider/portal component(s) plus thin CLJS fns over the imperative API (converting CLJS option maps -> JS). Establish one reusable shape all three follow.

## Notes

**2026-07-14T02:20:48.364452259Z**

Resolution — imperative-API wrapping pattern

REFRAMING (fact-driven, confirmed): the provider/portal COMPONENTS are already in docgen.json (docgen research §2: scripts/docgen's explicit file-list pulls whole components from @mantine/spotlight, @mantine/modals, @mantine/notifications) and the codegen decision groups ONE ns per package resolved from node_modules — so Notifications->mantine.notifications, ModalsProvider->mantine.modals, Spotlight.* (8-component family)->mantine.spotlight ride the existing codegen path unchanged. This ticket therefore decides only what is NOT in docgen.json and NOT in the @mantine/hooks barrel: the imperative singleton fns + the 3 package-shipped reactive hooks. Docs-site "Feedback" Notification (singular) is a @mantine/core component (mantine.core/notification) and is untouched here; the extension packages map cleanly by npm package boundary.

THE ONE REUSABLE SHAPE (all 3 packages follow it): each extension package = a single public ns `mantine.<pkg>` = codegen'd components (auto-docstringed) + a HOISTED hand-written supplement carrying the thin imperative fns and the reactive hook.

1. GLUE MECHANISM — a new committed generator INPUT: per-package supplement files, each a real compilable .cljc ns (full editor/clj-kondo support; consumed as input, never shipped as-is). At emit, the generator hoists the supplement's :require and top-level forms INTO the generated `mantine.<pkg>` ns (dedup requires against the impl/react requires it already emits; append forms after the codegen'd defs). Fully reproducible / pure-transform (supplements are committed inputs, no read-existing-output). Result: NO .imperative sibling nses, NO package excludes — providers stay codegen'd and auto-documented.

2. IMPERATIVE FNS (hand-written in the supplement):
   - Names mirror the JS singleton methods, kebab-cased 1:1 (reads like the Mantine docs):
     notifications -> show, hide, update, clean, clean-queue
     modals -> open, open-confirm-modal, open-context-modal, close, close-all, update-modal, update-context-modal
     spotlight -> open, close, toggle
   - Fixed single-arg arities (no map?-first-arg omission magic; imperative fns have no children): (show {..}) / (hide id) / (clean).
   - Conversion: option maps -> p/convert (the settled public converter — one-converter invariant, same rules as any component's props); bare-id args raw; returned ids raw.
   - Odd shapes special-cased IN the supplement (payoff of hand-writing): modals context-modal registry ({name->component} on ModalsProvider) keeps arbitrary string keys verbatim + component values passthrough; openContextModal innerProps (user's private payload) passed raw while surrounding ModalSettings is converted.
   - Nested config maps (cancelProps/confirmProps/searchProps, spotlight actions) INHERIT p/convert's shallow semantics — identical to how every component's *Props pass-throughs already behave; deeper conversion is a props-conversion revisit (Not-yet-specified), not this ticket.
   - Single-instance only for v1: the optional trailing `store` arg is dropped (default singleton is the common case); multi-store -> Not-yet-specified.
   - clojure.core shadowing: only notifications/update collides — keep the faithful name; the generator's existing :refer-clojure :exclude derivation covers it, PROVIDED supplement-defined names feed that derivation (handoff).

3. REACTIVE HOOKS — use-notifications / use-modals / use-spotlight: raw passthrough def-aliases in the supplement (per the hooks decision — zero conversion, read via interop, no shape table). No provider-absence guarding (useModals throws if ModalsProvider absent -> delegated to Mantine, as rules-of-hooks were delegated to React).

4. PROVIDER NAMING — providers keep faithful codegen names (notifications/notifications, modals/modals-provider, spotlight/spotlight) so they stay auto-documented; the supplement adds a one-line (def provider ...) alias for notifications/modals (opt-in tidiness for the mount-once component).

5. .cljc SHAPE — supplement forms carry their own reader conditionals (:cljs real call / :clj (f/not-implemented "<name>")), matching the generated defs; supplement ns is .cljc and JVM-loadable.

HANDOFFS to Decide: build tooling, npm interop & CSS delivery (mnt-01kxe8gzvp9e):
- Generator gains the supplement-HOIST step + a committed supplements input dir.
- Supplement-defined def names must feed the generator's :refer-clojure :exclude derivation (covers notifications/update, and any future supplement shadower).
- Concrete require sugar for the imperative singletons + package hooks (e.g. ["@mantine/notifications" :refer [notifications useNotifications]]) rides the same npm-interop decision as react/createElement.

NOT-YET-SPECIFIED additions: multi-instance stores; deeper conversion for nested *Props/actions if the shallow default proves insufficient.

**2026-07-14T02:20:58.954680945Z**

One reusable shape for all 3 extension packages: single ns mantine.<pkg> = codegen'd components (providers/UI already in docgen.json, ride existing per-package path + auto-docstrings) + a HOISTED hand-written supplement (real .cljc ns, new committed generator input) carrying thin imperative fns + reactive hook. NO .imperative siblings, NO excludes. Fns mirror JS singleton methods kebab 1:1, fixed single-arg arities, option maps->p/convert, ids raw, odd shapes (modals context registry, openContextModal innerProps) special-cased in supplement, nested *Props/actions inherit shallow p/convert, single-instance only (store arg dropped). Hooks = raw passthrough def-aliases. Providers keep faithful names + one-line (def provider) alias. Handoffs to build-tooling: supplement-hoist generator step + supplements input dir; supplement names feed :refer-clojure :exclude; imperative-singleton/hook require sugar.
