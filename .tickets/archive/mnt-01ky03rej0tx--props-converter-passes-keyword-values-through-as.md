---
id: mnt-01ky03rej0tx
title: Props converter passes keyword VALUES through as Keyword objects - Mantine crashes on {:color :red}
status: closed
type: bug
priority: 1
mode: afk
created: '2026-07-20T15:54:18.560659214Z'
updated: '2026-07-20T15:57:10.972759891Z'
closed: '2026-07-20T15:57:10.972759891Z'
acceptance:
- title: keyword leaf values stringify at top level, at depth, in vectors-of-maps, and in style/styles/vars leaves
  done: true
- title: no-convert-tagged and :inner-props payloads keep keywords intact
  done: true
- title: bb test green
  done: true
---

## Description

convert-value/style-leaf pass keyword leaf values through unconverted (documented as 'keyword VALUES are NOT stringified'). Mantine requires string enum/color/size values and hard-crashes: rendering (button {:color :red}) via renderToString throws '[@mantine/core] Failed to parse color. Expected color to be a string, instead got object'. The props-test suite's stated goal is 'consumer migration a namespace rename' - but a consumer migrating from a clj->js-based wrapper (fulcro react-factory, semantic-ui-wrapper style) passes keyword values like :sm/:red/:dimmed everywhere, so migration crashes at mount. Fix: stringify keyword leaf values ((name kw)) in convert-value and in style/selector leaf values, matching clj->js semantics; no-convert and :inner-props already opt CLJS payloads out. Keys, :&, children unchanged (mnt-01kxe8gzn6bt stands); ADR 0006 container semantics unchanged.

## Notes

**2026-07-20T15:57:10.972759891Z**

convert-value stringifies keyword leaf values via name (top level, nested maps, vectors); style-leaf stringifies keyword style values. Values never camelized. Docstrings + README updated; 3 new locked tests; bb test/jvm-load/build/drift all green.
