# Coding standards

## Changelog

A change a consumer can see gets an entry under `## [Unreleased]` in
[`CHANGELOG.md`](CHANGELOG.md), in the same commit. A consumer sees:

- a def added, removed or renamed;
- a change in behavior;
- an upstream change that can break an app that does not change its code.

The header of `CHANGELOG.md` says what an entry looks like. An anchor bump follows
[`docs/anchor-bump.md`](docs/anchor-bump.md); `bb release-check` fails when
`CHANGELOG.md` does not link the anchor's release notes.
