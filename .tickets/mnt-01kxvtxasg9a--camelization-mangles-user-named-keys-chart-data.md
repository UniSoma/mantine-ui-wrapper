---
id: mnt-01kxvtxasg9a
title: Camelization mangles user-named keys (chart :data rows, :modals registry)
status: open
type: task
priority: 2
mode: afk
created: '2026-07-19T00:02:43.628302822Z'
updated: '2026-07-19T00:02:43.628302822Z'
acceptance:
- title: 'README props section documents the user-named-keys caveat: chart :data field names and :modals registry names camelize; recommend dash-free names, with mc/raw / :& as escapes'
  done: false
- title: 'No converter behavior change: bb ci stays green with no src/main changes beyond regenerated docs (if any)'
  done: false
links:
- mnt-01kxh6gf162j
---

## Description

Residue of the deep-by-default converter (ADR 0006, shipped in mnt-01kxh6gf162j): two prop slots hold maps whose KEYS are user-chosen names, not Mantine prop names. Deep conversion camelizes those keys while the STRING values that must match them stay untouched, so a kebab-case name silently mismatches — no error, just missing output.

Concrete cases (swept src/main/mantine generated prop tables; these are the only two):
1. Chart :data rows (Record<string, any>[], charts.cljc) — :data [{:total-sales 100}] camelizes the field to totalSales, but the paired :data-key "total-sales" / :series {:name "total-sales"} strings are values and stay verbatim -> the series silently plots nothing.
2. ModalsProvider :modals registry (Record<string, FC<ContextModalProps>>, modals.cljc) — {:modals {:confirm-order comp}} camelizes the registry key to confirmOrder, but open-context-modal {:modal "confirm-order"} looks up the verbatim string -> modal not found.

NOT candidates for the raw-value denylist: both maps must BECOME JS (they feed Mantine); the hazard is key renaming, not conversion itself. Theme maps are already immune (create-theme bypasses convert via plain clj->js).

Working idioms today (why this is priority 2, docs-first): dash-free names (the demo uses :sales, :demo — zero cost); (mc/raw v) + #js/clj->js; the :& escape hatch (plain clj->js preserves hyphens).

Scope: document the sharp edge — README props section gets a caveat line naming both cases and the dash-free-names idiom. A converter mechanism (a verbatim-keys treatment: convert container, leave keys as written) is explicitly OUT of scope until a real consumer is bitten; if that happens, reopen the design via grilling + a new ADR rather than bolting it on here.
