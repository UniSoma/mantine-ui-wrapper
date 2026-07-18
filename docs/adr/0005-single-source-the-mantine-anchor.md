# The Mantine anchor is single-sourced from the package.json pins

The wrapped Mantine version — the **Mantine anchor** (see `CONTEXT.md`) — appears as a
literal in at least six machine-consumed places: the exact `@mantine/*` pins in
`package.json`, the `^`-prefixed consumer ranges in `src/main/deps.cljs`, the
`mantine-version` `def` in `codegen/plan.clj` (which flows into every generated banner
and docstring), the `9.4.1.N` prefix of `build.clj`'s version, and the hardcoded
`(Mantine 9.4.1)` banners in `codegen/extract.clj`. Nothing enforced their agreement,
so a Mantine bump meant synchronizing each by hand and a half-applied bump drifted
silently — the generated banner could claim a version the enumerated surface didn't
match.

**Decision.** Treat the exact `@mantine/*` pins in `package.json` as the one canonical
anchor. `read-sources` already slurps `package.json`, and the pins are what npm actually
installs, so no competing source of truth is introduced. Every other appearance is a
*rendering* of the anchor and is either **derived** or **validated**:

- **Derive** the `codegen/plan.clj` generator constant — delete the `def`, read the
  agreed pin. It is codegen-internal and already in the pipeline.
- **Validate** `deps.cljs` ranges and `build.clj`'s version prefix — these are
  hand-authored, shipped artifacts (and `build.clj`'s string is the sole home of the
  wrapper revision `N`). A `bb release-check` task reads them as data and asserts
  agreement; they are never regenerated.
- **`extract`** stamps its banners from the anchor (not a second hardcoded literal),
  asserts the `<clone-dir>` version equals the anchor, and writes a machine-readable
  **provenance witness** — `codegen/input/mantine-version.edn`, the version the committed
  inputs were captured at. The witness is authoritative about provenance, not a second
  anchor; it is validated against the real anchor.

Three reality-checks guard the layers the anchor is a declaration *about*:

- **`generate`**: the installed versions in `node_modules/@mantine/*` must equal the
  anchor (checked where the surface is actually enumerated, catching a stray local
  `npm install`).
- **`extract`**: the fed clone must equal the anchor (catching "extracted the wrong
  clone / forgot to bump `package.json`").
- **`release-check`**: the committed provenance witness must equal the anchor. Because
  `extract` writes the witness only when `clone == anchor`, and a bump moves the anchor
  while a skipped re-extract leaves the witness stale, this fires exactly on "bumped the
  anchor but forgot `bb extract`" — the one bump-time slip the other checks miss (the
  committed `docgen.json` carries no version of its own to read).

The release-identity logic lives entirely on the `codegen`/`bb` classpath. `build.clj`
(under the `deps.edn` `:build` alias, which cannot see `codegen/`) is a read *target*,
never a consumer — so no bridge into the deploy build and no release logic in the shipped
`src/main` jar. `bb release-check` runs the standalone checks and is wired into `bb ci`.

This **preserves ADR 0001**: the artifact stays source-only and Mantine-anchored as
`9.4.1.N`. Only the mechanics of keeping the anchor consistent change.

## Considered options

- **A dedicated `release.edn`** as the anchor — rejected. It would compete with the pins
  npm already enforces; `package.json` is both already-read and operationally true.
- **Derive everything** (regenerate `deps.cljs` and `build.clj` from the anchor) —
  rejected. It turns two hand-curated, shipped files into opaque generated output for no
  safety the validation doesn't already give.
- **Bridge the module into `build.clj`** (add `codegen` to the `:build` alias) —
  rejected. Validation reads the literal fine; a bridge couples the deploy build to the
  codegen tree for zero gain.

## Out of scope

The provenance witness proves the committed inputs were *extracted at* the anchor
version; it does not prove `docgen.json` was not hand-edited afterward. Content
integrity would need a fingerprint (hash at extract, assert at check) — not worth it,
since hand-editing 1.5 MB of generated JSON is not a realistic failure mode.
