---
id: mnt-01kxe8gzj38z
title: 'Research: imperative + hooks API surface (notifications/modals/spotlight/hooks)'
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-13T17:31:16.931198737Z'
updated: '2026-07-13T17:36:41.921190605Z'
closed: '2026-07-13T17:36:41.921190605Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:research
assignee: agent
---

## Description

## Question

Enumerate the non-component API surface that docgen.json won't cover: the function/store APIs of @mantine/notifications (notifications.show/hide/update/clean), @mantine/modals (modals.open*/close*), @mantine/spotlight (spotlight.open/close + Spotlight component), and the shape of @mantine/hooks (list of hooks, their signatures, and return-value shapes — tuples vs objects). Note the provider/portal each imperative package needs mounted.

Capture findings on a throwaway research/ branch with a pointer back to this ticket.

## Notes

**2026-07-13T17:36:41.921190605Z**

Findings: docs/research/imperative-and-hooks-api.md. The 3 imperative packages share one shape: a callable singleton (notifications/modals/spotlight) usable anywhere with no React context at call site; state lives outside React (store or DOM event bus); each REQUIRES a renderer mounted once (<Notifications/>, <ModalsProvider>, <Spotlight/>) or calls no-op/throw; open* fns return a string id for update/close. Methods: notifications show/hide/update/clean/cleanQueue; modals open/openConfirmModal/openContextModal/close/closeAll/update* (+context modals registered via ModalsProvider modals={} map); spotlight open/close/toggle. HOOKS (79, enumerable from package barrel index.ts): tuple-vs-object split is load-bearing — state hooks return TUPLES [value,handlers] (useDisclosure/useToggle/useCounter/useListState; storage = 3-tuple), DOM/status hooks return OBJECTS (useHover/useElementSize/useClipboard), some bare scalars (useMediaQuery->bool). A blanket js->clj map conversion breaks tuple hooks — wrapper must special-case tuple (vector) vs object (keyword map).
