---
id: mnt-01kxh6gevbzw
title: Publish the wrapper to Clojars with a versioned release process
status: in_progress
type: task
priority: 1
mode: hitl
created: '2026-07-14T20:53:45.963653419Z'
updated: '2026-07-16T17:31:10.054604627Z'
tags:
- needs-triage
acceptance:
- title: Clojars coordinate + version scheme decided (wrapper version <-> Mantine 9.4.1) and documented
  done: true
- title: Build produces a jar containing the wrapper source incl. the shipped npm-deps declaration, with a correct pom
  done: true
- title: A release (at least a snapshot) is deployed to Clojars and consumed from a scratch CLJS project that renders a generated component
  done: false
- title: Release procedure documented; optional CI publish hook
  done: true
---

## Description

A CLJS app can add mantine-ui-wrapper as a Clojars dependency and :require the generated namespaces without cloning the repo. Establish the Clojars coordinate, a build that produces a jar carrying the wrapper source plus the shipped npm-deps declaration and a correct pom, a deploy step, and a version scheme that maps the wrapper version to the wrapped Mantine version. Out-of-scope carve-out from the closed epic mnt-01kxgy8apnws. Blocked by: none — can start immediately.

## Notes

**2026-07-16T17:31:10.054604627Z**

Design grilled + ADR 0001 written. Implemented: build.clj (tools.build) + deps-deploy, bb jar/install/deploy tasks, MIT LICENSE + README attribution, docs/release.md. Coordinate io.github.unisoma/mantine-ui-wrapper, version 9.4.1.0-SNAPSHOT. Jar verified: deps.cljs at root + all mantine/*.cljc + impl + correct pom (MIT, SCM, only clojure dep). Consumption verified LOCALLY: scratch shadow-cljs project depending only on the ~/.m2 jar auto-installed @mantine/* from the shipped deps.cljs and rendered mc/button (0 warnings, jsdom green). REMAINING for AC#3: actual Clojars push is manual/user-run (needs CLOJARS_USERNAME + deploy token): 'bb deploy', then re-point the scratch project at the live snapshot. CI publish deferred by decision (option A).
