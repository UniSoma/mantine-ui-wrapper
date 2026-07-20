---
id: mnt-01ky00p1yhhj
title: Rename mantine.core/raw to mantine.interop/no-convert (hard rename, no alias)
status: closed
type: task
priority: 2
mode: afk
created: '2026-07-20T15:00:34.381366157Z'
updated: '2026-07-20T15:34:57.870204307Z'
closed: '2026-07-20T15:34:57.870204307Z'
acceptance:
- title: mantine.interop/no-convert tags a value so convert passes it through untouched (same behavior as old raw)
  done: true
- title: 'mc/raw is gone: absent from supplement and regenerated core.cljc, no alias remains'
  done: true
- title: README and demo reference mi/no-convert; ADR 0006 unchanged
  done: true
- title: Full check suite green (drift/coverage/collision/jvm-load) and demo compiles
  done: true
deps:
- mnt-01ky00nnhfcv
---

## Description

Move the converter opt-out tag out of the generated core namespace: mantine.core/raw becomes mantine.interop/no-convert with identical semantics (tag a value so the props converter emits it untouched at any depth; a wrapper VALUE, not metadata). Hard rename — no deprecated alias; nothing stable is published (only 9.4.1.0-SNAPSHOT).

Scope: delete raw from codegen/supplements/core.cljc and regenerate core.cljc (bb generate) so mc/raw is gone; add no-convert to mantine.interop delegating to the impl fn; rename internal mantine.impl.props/raw to no-convert for consistency and fix its docstring (it currently says it is hoisted into mantine.core/raw); update the two mc/raw mentions in README and the demo call site. ADR 0006 stays untouched — it records the escape-mechanism decision, which is unchanged; only the public name moved.

Rationale for the name: no-convert names the mechanism (skipping conversion) and does not collide with the 'raw passthrough' terminology used for hook returns, which is why raw was too broad.

## Notes

**2026-07-20T15:34:57.870204307Z**

Hard-renamed mantine.core/raw -> mantine.interop/no-convert (no alias). Deleted raw + its mantine.impl.props require from codegen/supplements/core.cljc and regenerated core.cljc (mc/raw and the p require both gone); added no-convert to mantine.interop delegating to the impl fn; renamed internal mantine.impl.props/raw -> no-convert and fixed its docstring. README both mentions updated; ADR 0006 untouched. Demo now references mi/no-convert (ticket premise of a demo mc/raw call site was wrong — none existed) via a raw-JS :style passthrough + verify-demo assertion. Green: drift/coverage/jvm-load/test/plan-test/extract-test/build/verify-demo. Commit c4baa72.
