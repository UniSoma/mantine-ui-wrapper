---
id: mnt-01kxe8gzvp9e
title: 'Decide: build tooling, npm interop & CSS delivery'
status: open
type: task
priority: 2
mode: hitl
created: '2026-07-13T17:31:17.238456887Z'
updated: '2026-07-13T20:31:46.380964330Z'
parent: mnt-01kxe8fz1ert
tags:
- wayfinder:prototype
deps:
- mnt-01kxe8gzf489
---

## Description

## Question

Decide the shadow-cljs project setup: how @mantine/* npm deps are declared (deps vs peer-deps), and the concrete strategy for delivering Mantine's required CSS into both the wrapper's demo harness and downstream consumers. Prototype the smallest build that renders one Mantine component with its CSS applied.

## Notes

**2026-07-13T20:31:46.380964330Z**

Handoffs from Decide: codegen pipeline & generated output (mnt-01kxe8gzrhy6, now closed):

1. REACT createElement REQUIRE FORM — mantine.impl.factory needs a concrete, framework-agnostic react/createElement (the factory closes over a Mantine component, runs mantine.impl.props/convert on props, then calls createElement). Decide the exact shadow-cljs require sugar here (e.g. ["react" :as react] / :refer createElement), coherent with the rest of npm interop.

2. GENERATOR RUNNER MECHANISM — the codegen generator is settled as a headless/CI-runnable/pinned/single-entry-point plain-Clojure task reading a committed docgen.json + installed node_modules and emitting .cljc. The exact runner (bb task vs clj -X alias) was handed here to be chosen coherently with deps.edn/bb.edn/shadow-cljs. Lean: bb (fast startup, trivial JSON + fs reads, no JVM/deps compile for a pure text-emitting script).
