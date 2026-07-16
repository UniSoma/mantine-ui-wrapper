# Publishing to Clojars as a source-only jar

We publish the wrapper to Clojars under the coordinate
**`io.github.unisoma/mantine-ui-wrapper`** so a CLJS app can depend on it without
cloning the repo. The jar is **source-only** — it ships the generated `mantine/*.cljc`
namespaces, the hand-written `mantine/impl/`, and `src/main/deps.cljs` (which lands at
the jar root and drives shadow-cljs's `:npm-deps` auto-install). Consumers compile the
`.cljc` themselves; we do no AOT or CLJS→JS compilation. React/react-dom are deliberately
absent from `deps.cljs` (the consuming app owns React), and the `@mantine/*` packages are
npm deps in `deps.cljs`, **not** Maven dependencies in the pom — the only pom dependency
is `org.clojure/clojure`.

## Version scheme: `9.4.1.N`

The version is Mantine-anchored and four-segment: the first three segments **are** the
wrapped Mantine version, and `N` is the wrapper's own revision against that Mantine
release (`9.4.1.0`, `9.4.1.1`, … then `9.5.2.0` on the next Mantine bump). The dominant
question a consumer has is "which Mantine does this wrap?", and this scheme puts that
answer in the coordinate itself. It also falls straight out of the existing version-bump
model, which already treats the Mantine version as the anchor pin (see
`docs/version-bump.md`).

The dotted `.N` form is deliberate over a dashed `-N`: Maven reads a trailing `-N` as a
pre-release qualifier that sorts *before* `9.4.1`, which would be backwards and dangerous.
The trade-off accepted: the scheme carries no independent semver signal for wrapper-only
breaking changes — acceptable because the wrapper's API surface is derived from Mantine,
so such changes are rare and are called out in release notes instead.

## Build & deploy

`tools.build` (`build.clj`) writes the pom programmatically — so the coordinate, version,
MIT license, and SCM live in code, not a checked-in `pom.xml` that would drift — and jars
`src/main`. `deps-deploy` pushes to Clojars. Both are wrapped in `bb` tasks to stay inside
the repo's existing `bb` interface.

## License: MIT

The generated docstrings are derived from Mantine's own docs, and Mantine is MIT-licensed.
Licensing the wrapper MIT (with a Mantine attribution notice) matches the wrapped library
exactly, avoids any license-compatibility question over the derived content, and fits the
React-adjacent audience's expectations better than the Clojure-default EPL.

## Iteration on snapshots; CI publish deferred

While the design churns we stay on the mutable **`9.4.1.0-SNAPSHOT`**; the first immutable
"official cut" (`9.4.1.0`) is a deferred one-line version change once the surface is stable
— Clojars releases are immutable, so we don't burn the real number early. Deploy is a
**manual** `bb deploy` run with a Clojars token; automated publishing is intentionally not
built yet. When the first official cut is warranted, the paved path is a tag-triggered
GitHub Actions workflow (`v*` push → `deps-deploy` a release with the token from repo
secrets), which stays dormant during normal pushes.
