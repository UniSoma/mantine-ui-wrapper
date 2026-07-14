---
id: mnt-01kxh6gevbzw
title: Publish the wrapper to Clojars with a versioned release process
status: open
type: task
priority: 1
mode: hitl
created: '2026-07-14T20:53:45.963653419Z'
updated: '2026-07-14T20:59:14.254398490Z'
tags:
- needs-triage
acceptance:
- title: Clojars coordinate + version scheme decided (wrapper version <-> Mantine 9.4.1) and documented
  done: false
- title: Build produces a jar containing the wrapper source incl. the shipped npm-deps declaration, with a correct pom
  done: false
- title: A release (at least a snapshot) is deployed to Clojars and consumed from a scratch CLJS project that renders a generated component
  done: false
- title: Release procedure documented; optional CI publish hook
  done: false
---

## Description

A CLJS app can add mantine-ui-wrapper as a Clojars dependency and :require the generated namespaces without cloning the repo. Establish the Clojars coordinate, a build that produces a jar carrying the wrapper source plus the shipped npm-deps declaration and a correct pom, a deploy step, and a version scheme that maps the wrapper version to the wrapped Mantine version. Out-of-scope carve-out from the closed epic mnt-01kxgy8apnws. Blocked by: none — can start immediately.
