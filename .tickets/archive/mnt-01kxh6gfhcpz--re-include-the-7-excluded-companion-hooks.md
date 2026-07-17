---
id: mnt-01kxh6gfhcpz
title: Re-include the 7 excluded companion hooks
status: closed
type: task
priority: 1
mode: hitl
created: '2026-07-14T20:53:46.668789202Z'
updated: '2026-07-17T18:22:39.588312689Z'
closed: '2026-07-17T18:22:39.588312689Z'
tags:
- needs-triage
acceptance:
- title: The 7 hooks generate as def-aliases with a sensible docstring (derived description or their shared docs-page URL)
  done: true
- title: scope.edn hook excludes trimmed accordingly; the def-count coverage check reflects the new count
  done: true
- title: Release build 0 warnings; generated ns JVM-loads; verify loop green
  done: true
---

## Description

The companion hooks currently excluded for carrying no description (useCallbackRef, useFullscreenDocument, useFullscreenElement, useHorizontalCollapse, useMousePosition, useMutationObserverTarget, useSessionStorage) are wrapped in mantine.hooks like the rest. Small, self-contained. Implementation-level deferral from mnt-01kxgy8apnws (dropped only to keep every generated def documented). Blocked by: none — can start immediately.

## Notes

**2026-07-17T17:59:54.116536218Z**

Grilling/domain-modeling session — decisions locked:

APPROACH: minimal, docgen-faithful. Fix the broken-URL bug; NO hand-written descriptions.
Root cause found: hook-docstring unconditionally emits https://mantine.dev/hooks/<kebab>,
which 404s for 6 of these 7 (and useCallbackRef has no public page at all). So this is a
URL-correctness fix, not just deleting excludes.

DECISIONS:
1. Docstring bar = correct URL only (option a). First line stays just the hook name (no
   invented description); URL points to the real shared docs page. Faithful to the
   docgen-driven / no-invented-prose stance in CONTEXT.md.
2. Mapping lives in a NEW input data file codegen/input/hook-docs-page.edn (option a),
   consistent with hook-docs.edn / component-docs.edn. Plain {name -> slug} map.
3. useCallbackRef has NO docs page -> point to the hooks index /hooks/package/ (verified
   real; /hooks/hooks-list and /hooks/getting-started both 404). Slug "package".
4. Domain model: add "Companion hook" term to CONTEXT.md — a use* export documented on a
   sibling hook's docs page, carrying no standalone description; mapped to that shared page
   via hook-docs-page.edn.
5. Verification: NO new demo coverage — pure def-aliases, same machinery as the 80 existing
   hooks. Gate = bb coverage (87 hooks, 0 missing) + release build 0 warnings + jvm-load +
   existing verify loop.
No ADR — easily reversible, low stakes; new input file + glossary term are self-explaining.

VERIFIED slug map (all target pages confirmed to exist and to document the companion):
  useCallbackRef            -> package
  useFullscreenDocument     -> use-fullscreen
  useFullscreenElement      -> use-fullscreen
  useHorizontalCollapse     -> use-collapse
  useMousePosition          -> use-mouse
  useMutationObserverTarget -> use-mutation-observer
  useSessionStorage         -> use-local-storage   (its own kebab use-session-storage 404s)

Coverage note: check derives expected-hooks from scope.edn dynamically (scripts/coverage-check.clj),
so no hard-coded count to bump; 80 -> 87 automatically.

**2026-07-17T18:02:37.185103353Z**

Implemented + verified. All 3 acceptance criteria met.

CHANGES:
- codegen/scope.edn: :hooks :exclude -> #{} (7 companion hooks re-included); comment updated.
- codegen/input/hook-docs-page.edn: NEW — 7-entry companion -> docs-slug map.
- codegen/generate.clj: load hook-docs-page; hook-docstring consults slug override,
  falls back to (kebab nm).
- CONTEXT.md: added "Companion hook" glossary term.
- Regenerated src/main/mantine/hooks.cljc (80 -> 87 def-aliases).

VERIFICATION:
- bb coverage: hooks expected 87 / present 87 / missing 0; COVERAGE OK.
- bb build: 0 shadow warnings; BUILD OK.
- bb jvm-load: 9 nses load (sampled use-callback-ref named-throw OK).
- node scripts/verify-demo.mjs: all patterns verified.
- bb test: 17 tests / 34 assertions / 0 failures.
- Generated companion URLs all point to real (verified non-404) shared pages:
  use-callback-ref -> /hooks/package, fullscreen-document/element -> /hooks/use-fullscreen,
  horizontal-collapse -> /hooks/use-collapse, mouse-position -> /hooks/use-mouse,
  mutation-observer-target -> /hooks/use-mutation-observer, session-storage -> /hooks/use-local-storage.

REMAINING: `bb drift` (and thus `bb ci`) is red only because regenerated sources are not yet
committed — the diff is exactly the 7 intended hooks. Commit makes drift/ci green. Left
uncommitted pending user go-ahead.

**2026-07-17T18:22:39.588312689Z**

Re-included the 7 companion hooks in mantine.hooks (80->87 def-aliases). Root fix: hook-docstring emitted mantine.dev/hooks/<kebab> which 404s for companions; added codegen/input/hook-docs-page.edn mapping each to its real shared docs page, consulted in hook-docstring with kebab fallback. Emptied scope.edn hooks :exclude; coverage auto-derives 87. Added 'Companion hook' glossary term to CONTEXT.md. Verified: coverage 87/0, build 0 warnings, jvm-load, verify-demo, 17 tests all green.
