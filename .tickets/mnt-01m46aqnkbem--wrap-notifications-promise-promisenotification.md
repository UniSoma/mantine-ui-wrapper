---
id: mnt-01m46aqnkbem
title: Wrap notifications.promise (promiseNotification, new in 9.7.0)
status: open
type: feature
priority: 3
mode: hitl
created: '2026-10-05T15:25:50.559446265Z'
updated: '2026-10-05T15:25:50.559446265Z'
links:
- mnt-01m46ahjn88j
---

## Description

9.7.0 adds promiseNotification (notifications.promise): shows a loading notification while a promise is pending, then updates it to success/error. The mantine.notifications imperative API is hand-curated in codegen/supplements/notifications.cljc (show/hide/update/clean/clean-queue), so the bump does not surface it. Add a `promise` entry following the existing ones; decide how success/error accept fns of the resolved value/rejection reason under the props converter.
