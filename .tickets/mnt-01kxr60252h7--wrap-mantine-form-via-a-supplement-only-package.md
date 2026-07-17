---
id: mnt-01kxr60252h7
title: Wrap @mantine/form via a supplement-only package path
status: open
type: feature
priority: 3
mode: afk
created: '2026-07-17T13:59:29.698386029Z'
updated: '2026-07-17T14:04:44.772301982Z'
acceptance:
- title: Generator supports supplement-only packages; mantine.form generated from codegen/supplements/form.cljc alone, exposing use-form (raw) and the package's pure validator exports; no conversion accessors
  done: false
- title: '@mantine/form installed and declared in deps.cljs + package.json + README install line'
  done: false
- title: bb ci green
  done: false
tags:
- gap
---

## Description

@mantine/form has no docgen entries, so it never enters the by-pkg grouping and gets no generated namespace — there is no path to wrap it today (supplements only hoist into a namespace a package's docgen components already create).

Add a supplement-only-package path to the generator: a package declared supplement-only (e.g. scope :supplement-only-packages) gets a generated namespace whose body is entirely its hoisted supplement — no components, empty :refer. parse-supplement already returns everything needed (requires, def-names, body); the change is letting emit-package-ns run with an empty component list for declared supplement-only packages.

Then add codegen/supplements/form.cljc as a RAW PASSTHROUGH: use-form aliasing useForm, plus the package's pure function exports (validators: is-email, has-length, ...), enumerated from the installed package. NO js<->clj conversion accessors — consumers layer interop themselves (same reasoning gaps.md uses to exclude the field-DSL). This follows the wrapper's principle: convert the input construction surface (props), pass runtime returns through raw (every hook is raw passthrough).

Install @mantine/form; declare it in deps.cljs (:npm-deps), package.json devDeps, and the README install line (no CSS).
