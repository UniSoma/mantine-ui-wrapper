---
id: mnt-01m46aqnkbem
title: Wrap notifications.promise (promiseNotification, new in 9.7.0)
status: in_progress
type: feature
priority: 3
mode: afk
created: '2026-10-05T15:25:50.559446265Z'
updated: '2026-10-05T18:41:41.857192692Z'
links:
- mnt-01m46ahjn88j
tags:
- settled
acceptance:
- title: mantine.notifications/promise is generated from the notifications supplement, and bb generate passes the collision guard
  done: false
- title: A demo button calls mn/promise on a promise that resolves to a known value, with a :success fn that puts the value in :message, plus :title and :auto-close false; verify-demo.mjs clicks it and asserts one notification shows both the resolved value and the title
  done: false
- title: A second demo button calls mn/promise on a rejected promise, with an :error fn that puts the reason in :message and :auto-close false; verify-demo.mjs clicks it and asserts that message renders
  done: false
- title: CHANGELOG.md lists mantine.notifications/promise under [Unreleased] / Added
  done: false
- title: bb ci passes
  done: false
---

## Description

Mantine 9.7.0 adds `notifications.promise` (`promiseNotification`). It shows a loading notification while a promise is pending, then updates the same notification to a success or error state. The imperative fns in `mantine.notifications` are wrapped by hand in its supplement, so the anchor bump did not add this one.

Add `mantine.notifications/promise`, called as `(promise p opts)`. `opts` takes `:id`, `:loading`, `:success` and `:error`. Each state is a notification-data map, converted the same way as `show`'s argument. `:success` and `:error` can instead be fns: Mantine passes the fn the resolved value or the rejection reason as-is, and the CLJS map the fn returns is converted before the notification shows. `promise` returns the promise it was given.

### Decisions

- Supplement, not generator (ADR 0002). `promise` goes in `codegen/supplements/notifications.cljc` after `update`, and `promiseNotification` joins the `:refer` list.
- Name: `promise`, following `show`/`hide`/`update` (the Mantine name minus `Notification`). The generator already adds it to `:refer-clojure :exclude` in the generated ns (`codegen/plan.clj` filters def names against `clojure.core` publics). Also add `promise` to the supplement's own `(:refer-clojure :exclude [update])`, so the file still compiles for the editor and clj-kondo.
- Fn-valued `:success` / `:error` are wrapped inside `promise`, so the fn's return value goes through `p/convert` and its argument (resolved value or rejection reason) passes through as-is. This is ADR 0002's conversion boundary: the maps a caller builds are converted, and values Mantine hands back are not. Mantine dispatches on `typeof data === "function"` and spreads the result, so an unconverted CLJS map would show an empty notification with no error. `mantine.impl.props/convert` is not changed.
- Check for a fn with `fn?`, not `ifn?`. A static `:success {...}` map is `ifn?`.
- Change `:success` / `:error` with `assoc`, `cond->` or `clojure.core/update`. A bare `update` in this ns is `mantine.notifications/update`.
- After the fns are wrapped, the whole options map goes through `p/convert`, like `show`'s argument. `:loading` and static state maps use the existing deep conversion.
- Only the `[p opts]` arity. No `store` argument, same as the five existing fns.
- Returns the JS promise Mantine returns (the one passed in), unchanged.
- The JVM branch uses `f/not-implemented "mantine.notifications/promise"`, like its siblings.
- The docstring covers the options, the fn form, and what is converted versus passed through as-is. It also says Mantine resets `autoClose` when the promise settles, so the success or error notification falls back to the provider's auto-close default unless it sets `:auto-close` itself.
- Verification through the demo, like `show`. One button resolves a promise to a known value, with a `:success` fn that puts the value in `:message`. A second button rejects a promise, with an `:error` fn that puts the reason in `:message`. Both set `:auto-close false`. `scripts/verify-demo.mjs` waits for the text of the final state, not for any notification, because the loading notification has the same id. The rejected promise does not crash Node: Mantine attaches `.then(ok, err)` to the original promise, so the rejection counts as handled.
- Changelog: add `mantine.notifications/promise` under `## [Unreleased]` / `### Added` in `CHANGELOG.md`, in the same commit.
