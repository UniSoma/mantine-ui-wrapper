---
id: mnt-01kxr60b7xe8
title: Lock converter compatibility invariants as behavioral tests
status: open
type: task
priority: 2
mode: afk
created: '2026-07-17T13:59:39.005570513Z'
updated: '2026-07-17T14:04:44.871494817Z'
acceptance:
- title: Behavioral test suite covers the converter invariants above (camelCase passthrough, :class alias + vector join, data/aria/-- exemption, children pruning/flattening, deep-convert set, :& escape hatch)
  done: false
- title: Suite runs in bb ci and is green
  done: false
- title: A deliberate converter mutation is caught by the suite (verified via a scratch edit)
  done: false
tags:
- gap
---

## Description

The prop converter (mantine.impl.props) is the load-bearing runtime piece and has ZERO behavioral tests — bb ci only checks compilation, drift, and def-counts. Lock the compatibility invariants that make consumer migration a namespace rename (recorded in gaps.md) as the repo's first behavioral test suite so they cannot silently regress:

- camelCase props pass through as identity (dash-free keywords untouched), alongside kebab->camel.
- :class aliases :className; class vectors space-join with nil/false dropped; :class + :className merge.
- data-*/aria-*/--* keys bypass camelization.
- Children pruned (nil) and flattened (nested seqs from for), with strings/numbers/elements/render-prop fns passed through.
- Deep-convert set (:style/:styles/:classNames/:vars) behaves per docstring; the :& escape hatch merges last (raw JS or clj->js, hyphens preserved).

Wire the suite into bb ci.
