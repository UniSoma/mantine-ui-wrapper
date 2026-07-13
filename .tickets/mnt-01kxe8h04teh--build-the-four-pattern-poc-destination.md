---
id: mnt-01kxe8h04teh
title: Build the four-pattern PoC (destination)
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-13T17:31:17.530183297Z'
updated: '2026-07-13T17:31:26.621158319Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:prototype
deps:
- mnt-01kxe8gzrhy6
- mnt-01kxe8gzvp9e
- mnt-01kxe8gzyj13
- mnt-01kxe8h01n5p
---

## Description

## Question

The destination proof. Build a shadow-cljs harness that renders one of each pattern: a few codegen'd core components (incl. polymorphic component=, styles/classNames, section props), one imperative API end-to-end (e.g. notifications.show via its provider), and one hook (e.g. useDisclosure) — with Mantine's CSS actually loading. When all four render correctly, the pipeline is de-risked and the map is done.
