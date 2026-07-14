---
id: mnt-01kxgykp56fz
title: Document the Mantine version-bump procedure
status: open
type: task
priority: 2
mode: afk
created: '2026-07-14T18:35:43.135367607Z'
updated: '2026-07-14T18:35:43.135367607Z'
parent: mnt-01kxgy8apnws
acceptance:
- title: 'A repo docs page walks the full bump: clone the target Mantine tag, install + run its docgen script, run the input-extraction task against the clone, bump every pin (package manifest, shipped npm-deps incl. dayjs/recharts ranges, generator version constant), regenerate, run the verification loop, review the generated diff'
  done: false
- title: The doc explains what the diff review looks for (new/removed/renamed components and hooks picked up automatically by the everything-minus-excludes scope) and how the exclude list is the escape hatch
  done: false
deps:
- mnt-01kxgyhzgc0f
- mnt-01kxgyjccn57
- mnt-01kxgyjnpzrj
- mnt-01kxgyk1rsfn
---

## Description

## Parent

Spec: mnt-01kxgy8apnws.

## What to build

A maintainer can move the wrapper to a newer Mantine version as a scripted afternoon, not archaeology. Write the version-bump procedure into the repo docs, reflecting the final fan-out state: refresh committed inputs from a Mantine clone (the PoC measured this at roughly a minute of compute), bump every pin in one pass, regenerate, verify, and review the generated diff — which, under the everything-minus-excludes scope, is where new/removed components and hooks surface automatically. Also documents when a refresh of the docs-app extraction inputs is needed versus skippable.

## Blocked by

- mnt-01kxgyhzgc0f (scope inversion + full core)
- mnt-01kxgyjccn57 (dates + charts)
- mnt-01kxgyjnpzrj (modals + spotlight)
- mnt-01kxgyk1rsfn (all hooks)
