---
id: mnt-01kxzxsh9h5f
title: Wrap @mantine/schedule (first-party)
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-20T14:10:02.668759802Z'
updated: '2026-07-20T14:30:38.550057599Z'
closed: '2026-07-20T14:30:38.550057599Z'
tags:
- schedule
- wrapper
acceptance:
- title: src/main/mantine/schedule.cljc is generated; every schedule docgen component is covered (drift + coverage pass)
  done: true
- title: Plain utils + labels remain out of coverage; no speculative supplement wrapping them
  done: true
- title: deps.cljs adds @mantine/schedule ^9.4.1 (no rrule) with the peer-vs-transitive comment; release-check passes
  done: true
- title: README updated (ten packages, table row, install command) and cljdoc doc tree includes mantine.schedule
  done: true
- title: demo requires + mounts a minimal schedule view; bb ci is fully green
  done: true
links:
- mnt-01kxh6gfendq
- mnt-01kxzxstjjjb
---

## Description

Wrap the first-party @mantine/schedule@9.4.1 package as mantine.schedule. This is the pipeline's first-party generalization path (as anticipated by the wontfix note on mnt-01kxh6gfendq), NOT the declined community/extension-architecture work.

Package facts (verified against the 9.4.1 monorepo + npm):
- First-party (author rtivital), version lock-stepped to the anchor (9.4.1).
- Peers @mantine/core / @mantine/dates / @mantine/hooks — all already wrapped.
- Sole runtime dep: rrule@^2.8.1 — a REGULAR (non-peer) dep, so npm installs it transitively whenever @mantine/schedule is installed. It is categorically unlike dayjs/recharts (which are PEER deps of dates/charts and therefore must be declared in deps.cljs).
- Ships its own styles.css / styles.layer.css.
- ~18 components (Schedule, {Day,Week,Month,Year}View, Resources{Day,Week,Month}View, ResourcesSchedule, AgendaView, MobileMonthView, ScheduleEvent, ScheduleHeader, CurrentTimeIndicator, MoreEvents, DragContext).
- ~50 plain util fns + getLabel/DEFAULT_SCHEDULE_LABELS exported from the barrel; schedule's src/hooks is NOT re-exported (adds nothing to the hooks story).

Decisions (from grilling session):
1. DoD = MODEL-CONSISTENT wrap: only docgen COMPONENTS generate into mantine.schedule. The ~50 plain utils + labels stay OUT of coverage (omissions, not decisions — per the Coverage glossary term, exactly as @mantine/dates plain utils are). Promote a specific util to a supplement later only if a real need appears.
2. deps.cljs = BATTERIES-INCLUDED: add @mantine/schedule ^9.4.1, uniform with the other 8 packages. Do NOT add rrule (transitive). Non-users carry only an unused, tree-shaken schedule+rrule in node_modules — the same 'cost' already paid for unused charts/dropzone/etc.
3. No new ADR (every decision lands on existing precedent: ADR-0002/0004/0005 + glossary).
4. Supplement / controlled-inputs / compound-part treatment is DISCOVER-THEN-DECIDE: driven by the real docgen.json output via the existing drift/collision/coverage guards + jvm-load — not pre-guessed.

## Design

Ordered implementation (run from repo root; Mantine 9.4.1 clone available for docgen):

1. Install: add `@mantine/schedule` `9.4.1` (exact) to package.json devDependencies; `npm install`. rrule lands transitively.
2. Extract wiring: add `:schedule "mdx-schedule-data.ts"` to `component-corpora` in codegen/extract.clj (schedule docs live in their own MDX data file — required for prose descriptions/slugs; without it components still generate with docgen prop tables only).
3. Refresh inputs: in the 9.4.1 clone run `yarn install && yarn tsx scripts/docgen`; then `bb extract <clone>` → refreshes codegen/input/{docgen.json,component-docs.edn,mantine-version.edn}.
4. Generate: `bb generate` → emits src/main/mantine/schedule.cljc. scope.edn `:components {:all true}` auto-covers the docgen components; pkg->ns/pkg->file derive the ns/path.
5. Discover-then-decide (guard-driven — drift/collision/coverage + jvm-load): 
   - compound parts → cover, or add an explicit scope.edn `:components :exclude`;
   - provider-ish surface (DragContext) → backfill via codegen/supplements/schedule.cljc if docgen doesn't describe it;
   - controlled-inputs candidates (view date/onChange) → add to controlled-inputs.edn ONLY if the shape-agnostic value/onChange shim genuinely fits;
   - plain utils stay out of coverage.
6. deps.cljs: add `"@mantine/schedule" "^9.4.1"` + a one-line comment: regular deps ride transitively (rrule); only PEER deps are declared here (dayjs, recharts). Do NOT add rrule.
7. README: bump 'nine' → 'ten' packages; add table row (mantine.schedule | @mantine/schedule/styles.layer.css · needs @mantine/dates); add `@mantine/schedule@^9.4.1` to the install command (release-check anchor-validates the token).
8. Demo: require mantine.schedule in demo/mantine/demo.cljs; mount ONE minimal view (e.g. MonthView/Schedule) with a trivial hard-coded event or two. (Only end-to-end path that cljs-compiles the ns + resolves the @mantine/schedule + rrule JS imports.)
9. cljdoc: add mantine.schedule to the curated doc tree (match recent curation).
10. Verify: `bb ci` green.

## Notes

**2026-07-20T14:20:00.104682787Z**

Discover-then-decide outcomes: (1) all 10 schedule docgen components generate into mantine.schedule; drift/collision/coverage/jvm-load all pass with NO supplement and NO scope excludes. (2) DragContext is a bare React.createContext object (not a provider component, no docs page) — internal drag-state plumbing; left out, not backfilled. (3) AgendaView/ScheduleEvent/ScheduleHeader/CurrentTimeIndicator/MoreEvents are docgen omissions (AgendaView has an MDX docs entry but no docgen entry) — stay out of coverage per the Coverage glossary term. (4) Schedule views are date/onDateChange, not value/onChange — controlled-inputs.edn untouched.

**2026-07-20T14:30:38.550057599Z**

Shipped in commit f647ac4. mantine.schedule generated from docgen (10/10 components; drift/coverage/collision/jvm-load all pass) with no supplement, no scope excludes, no controlled-inputs additions (schedule views are date/onDateChange). Plain utils/labels stay out of coverage; DragContext left out (bare React.createContext, not provider surface). deps.cljs adds @mantine/schedule ^9.4.1 with the peer-vs-transitive comment (no rrule); README bumped to ten packages with table row + install token; cljdoc picks the ns up automatically from the jar. Demo mounts a minimal MonthView with hard-coded events, asserted in verify-demo. Code review fixed the stale extract banner by deriving the corpus file list from component-corpora. bb ci fully green.
