# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versions follow the wrapper's own scheme, not Semantic Versioning: the first three
segments are the Mantine anchor (the wrapped Mantine release) and the fourth is the
wrapper's revision against it (`9.7.0.0`, `9.7.0.1`, ...).

This file records changes to the wrapper. Each anchor bump gets one line that links
to Mantine's release notes, which cover the upstream changes. An upstream change
appears here only when it can break an app that does not change its code.

Releases before the first entry below were SNAPSHOTs only and are not recorded.

## [Unreleased]

### Added

- `mantine.core/toggle`.
- `mantine.core/use-app-shell-resize`.
- `mantine.core/toolbar` and its parts: `toolbar-toggle`, `toolbar-toggle-group`,
  `toolbar-toggle-item`, `toolbar-group` and `toolbar-divider`.
- `mantine.core/tour` and its parts: `tour-root`, `tour-step`, `tour-overlay`,
  `tour-tooltip`, `tour-beacon`, `tour-title`, `tour-body`, `tour-close-button` and
  `tour-navigation`.

### Changed

- The Mantine anchor is now 9.7.0
  ([release notes](https://mantine.dev/changelog/9-7-0/)). The prop tables in the
  generated docstrings show the 9.7.0 props.
- Upstream: `tooltip` now hides when its target is detached from the page
  (`:hide-detached`, on by default). jsdom reports every element as detached, so
  tests that render a tooltip need `{:env "test"}` on `mantine-provider` or
  `{:hide-detached false}` on the tooltip.
- Upstream: `hover-card` no longer opens on a tap on touch devices. To get the old
  behavior, pass `{:events {:focus false :touch true} :close-on-escape false}`.
