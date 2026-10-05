# Releasing to Clojars

The wrapper publishes to Clojars as **`io.github.unisoma/mantine-ui-wrapper`**, a
source-only jar (the generated `.cljc` namespaces, `mantine/impl/`, and `deps.cljs`; no
AOT or JS compilation). Rationale and the full set of decisions are in
[`docs/adr/0001-clojars-release-process.md`](adr/0001-clojars-release-process.md).

## Version scheme: `<anchor>.N`

Four segments: the **first three are the Mantine anchor**, and `N` is the wrapper's
own revision against that Mantine release.

- Wrapper-only change (generator fix, `impl/` fix, scope widening) against the same
  Mantine → bump `N`: `9.4.1.0` → `9.4.1.1`.
- Anchor bump (see [`anchor-bump.md`](anchor-bump.md)) → new anchor, reset
  `N`: `9.4.1.3` → `9.5.0.0`.

The version lives in one place: `version` in [`build.clj`](../build.clj). Clojars
**release** versions are immutable; while the design churns we stay on the mutable
`9.7.0.0-SNAPSHOT`.

## Build tasks

| Task | What it does |
|------|--------------|
| `bb jar` | Build `target/mantine-ui-wrapper-<version>.jar` (source + generated pom) |
| `bb install` | Build + install into local `~/.m2` (for local consumer verification) |
| `bb deploy` | Build + deploy to Clojars |

All three delegate to `clojure -T:build` (`tools.build` + `deps-deploy`).

## Deploying a snapshot (manual)

You deploy by hand, with a Clojars **deploy token** (Clojars → Settings → Deploy
Tokens) passed via env:

```sh
export CLOJARS_USERNAME=<your-clojars-username>
export CLOJARS_PASSWORD=<deploy-token>   # the token, NOT your account password
bb deploy
```

Verify the round-trip from a scratch CLJS project that has only the Clojars dep:

```clojure
;; deps.edn
{:deps {io.github.unisoma/mantine-ui-wrapper {:mvn/version "9.7.0.0-SNAPSHOT"}}}
```

`:require` a generated namespace (e.g. `mantine.core`) and render a component; shadow-cljs
auto-installs the `@mantine/*` npm packages from the shipped `deps.cljs`. Bring your own
`react`/`react-dom`.

**cljdoc + the SCM tag.** cljdoc checks out the pom's `<scm><tag>` to read sources, so it
must be a revision that exists on GitHub. A SNAPSHOT has no `v<version>` tag, so `build.clj`
sets the tag to the **built commit SHA**. Run `git push` before `bb deploy` so that SHA is on
GitHub. (Releases keep `v<version>`, cut and pushed per the section below.)
Redeploying to Clojars re-triggers a cljdoc build; if it doesn't pick up, request one for the
exact version at <https://cljdoc.org/> (or `POST https://cljdoc.org/api/request-build2` with
`project` + `version`).

## The first official cut (deferred)

When the surface is stable, cut the immutable release:

1. Change `version` in `build.clj` from `"9.7.0.0-SNAPSHOT"` to `"9.7.0.0"`.
2. In [`CHANGELOG.md`](../CHANGELOG.md), rename `## [Unreleased]` to
   `## [9.7.0.0] - YYYY-MM-DD` and add an empty `## [Unreleased]` above it.
3. `bb ci` green, then commit.
4. Tag the commit `v9.7.0.0`, then push the commit and the tag. The pom's `<scm><tag>` is
   `v9.7.0.0` and cljdoc checks it out as soon as the deploy lands, so the tag must be on
   GitHub first.
5. `bb deploy`.
6. Change `version` in `build.clj` to the next snapshot, `"9.7.0.1-SNAPSHOT"`, and commit.
   Clojars rejects a second upload of `9.7.0.0`, so later work builds as a SNAPSHOT.

Once there are official cuts, the paved path for automating this is a **tag-triggered**
GitHub Actions workflow (runs only on `v*` tag pushes, so it stays dormant on normal
pushes) that runs `bb deploy` with `CLOJARS_USERNAME`/`CLOJARS_PASSWORD` from repo
secrets. That workflow does not exist yet; deploy stays manual by choice during iteration.
