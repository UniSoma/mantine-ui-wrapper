# The Wrapper plan is a deep module between inputs and emission

`codegen/generate.clj` has no single place that decides *what to wrap and how*. The
domain facts are smeared across the file: `resolve-component` assigns a name to a
package, `dimension-names` expands scopes, the top-level `let` classifies names into
components / hooks / supplement-only, `derive-exclude` computes `:refer-clojure
:exclude`, and — the sharpest leak — `emit-component-def` reaches back into
`controlled-inputs` and calls `component-docstring` while emitting, so **emission
re-runs classification and docs policy**. `scripts/coverage-check.clj` then duplicates
the whole classification (docgen, scope, exports-index, resolve, kebab) to re-derive
the same surface independently.

So "is this a Companion hook or a Barrel utility? which package? controlled? what
docstring? what refer-clojure excludes?" gets re-answered inside each emitter and once
more in the Coverage audit.

**Decision.** Insert one **deep module** — the **Wrapper plan** — between the committed
inputs and text emission. A pure function `build` turns a `sources` map into a `plan`:
a concrete data structure naming every generated namespace, every def (kind, symbol,
docstring, controlled?), its merged requires, its `refer-clojure` excludes, and its
hoisted Supplement body. Emission collapses to a thin `emit-ns` that templates and
escapes text with **zero domain decisions**. The plan *is* the test surface.

The module presents four entry points (`codegen/plan.clj`):

- `read-sources` — all I/O: slurp committed EDN/JSON, enumerate installed exports via Node.
- `build` — **the deep module.** Pure function of the `sources` map. All domain decisions.
- `emit-ns` — thin. One ns-plan → `{:file :text}`. Owns escaping and templating only.
- `write-plan!` — thin. The `bb generate` driver: `emit-ns` + `spit` + summary/skips.

`build` is **category-1 in-process** (per the codebase-design DEEPENING notes): it never
shells out. The only true-external fact — installed-package exports — is *injected* as
plain data in `sources`, so fixtures are hand-written maps with no Node and no writes.

## Design decisions settled

- **Docstrings live UN-escaped in the plan; `emit-ns` escapes.** `esc` turns a string
  into a valid Clojure string *literal* — target-syntax mechanics, not a domain fact.
  Keeping plan docstrings plain makes fixtures readable and gives `emit-ns` exactly one
  job (data → valid text). `emit-ns` therefore has exactly two text-handling rules:
  **escape docstrings, and splice Supplement bodies through verbatim** (a body is already
  valid `.cljc`, including its own docstrings, so re-escaping it would corrupt it).
- **Naming IS a domain decision, so it is pre-computed.** `:symbol` (kebab) and
  `:refer-clojure-exclude` are baked into the plan; `emit-ns` never calls `kebab`,
  `derive-exclude`, or `merge-requires`.
- **Build failures throw; skips/warnings are data.** The governing principle:
  **throw iff the emitted artifact would be wrong or ambiguous; otherwise record data.**
  The Collision guard (kebab collision within a namespace; a Supplement def colliding with
  a generated def) throws `ex-info` inside `build` — emitting anyway would produce a
  redefinition — so emission may assume uniqueness. Everything else is inert with respect
  to the emitted text and becomes data printed by the thin `write-plan!`: unresolvable
  names (not in docgen / no installed package / non-`use*` hook) → `:skipped`;
  controlled-input rot (a `controlled-inputs.edn` entry not among the resolved components)
  → `:notes` `:controlled-input-rot`; a name exported by more than one package (`@mantine/core`
  wins) → `:notes` `:multi-package`. A multi-package name is *correctly* resolved, so it is
  a note, not a throw. Failures become assertions, not scraped stdout.
- **The compound-part Drift audit stays OUT of `build`.** It needs `component-statics`
  (an extra Node call) that the emitter never uses; hosting it in `build` would bloat the
  pure path with observation unrelated to emission. It stays in the observation / Coverage
  layer. Consequently "all guards in one module" is deliberately *not* a goal — validation
  is placed by which facts it needs, not for tidiness.
- **Coverage keeps its independent recount.** The Coverage guard's value is that it
  recomputes the scoped surface *separately*, so a generator classification bug diverges
  from the check. The independence that is load-bearing is **classification**, not
  observation: Coverage SHOULD share `read-sources` (the observed facts), because then any
  divergence is *provably* a classification bug rather than an artifact of two separate Node
  enumerations. The firewall is specifically that Coverage MUST re-derive the scoped surface
  itself and MUST NOT consume `build`'s `:namespaces` — DRYing the two through the plan would
  silently destroy the guard. A load-bearing comment marks this boundary.
- **The Supplement pipeline splits three ways across the purity boundary.** `read-sources`
  does only the raw slurp (`:supplements` is `suffix → verbatim file text`). `build` owns all
  Supplement domain work: edamame-parse, `:requires`/`:cljs-requires` extraction, the
  Supplement-vs-generated-def Collision throw, and dropping satisfied `declare`s (which needs
  the namespace's own computed def-names — so parsing cannot move earlier than `build`).
  `emit-ns` splices the finished `:body` verbatim. The old `parse-supplement`, which slurps
  *and* parses in one function, is cleaved accordingly.

## Considered and rejected

- **A pluggable kind registry** — model classification as an ordered list of
  `{:claims? :resolve :collision}` kinds so new surface is added by `conj`. Rejected:
  the surface is a closed, tiny set — **three def kinds** (`:component`, `:hook`, `:util`)
  plus **one degenerate package shape** (supplement-only, which emits zero defs and whose
  body is entirely its hoisted Supplement) — against one emission target. A `kinds`
  parameter is a one-adapter hypothetical seam — indirection, not a real seam. Inline the
  kinds as private fns; promote to a registry only if a genuinely new kind or a second
  emission target actually lands.
- **A single `build-plan` with no I/O split** — leave `read-sources`/`write-plan!` at the
  caller's edge. Rejected as underspecified: naming the I/O and driver entry points keeps
  `build`'s purity unambiguous and gives the drift check and Coverage a clear, shared
  observation seam.
- **An injected `Observer` port for exports** (ports & adapters). Deferred: the only
  variance is live-Node vs fixture-map, expressed by hand-building `sources`. One real
  adapter today — introduce a port only when a second one (Candidate 02's inspection
  module) actually exists.

## Consequences

- Classification, naming, and docstring policy have exactly one home. The deletion test:
  delete `build` and every decision re-scatters across the emitter, the drift check, the
  Coverage audit, and the top-level `let`; delete a private helper like `resolve-component`
  and it merely folds back up into `build`.
- The interface is the test surface: Companion-hook page mapping, `@mantine/core`
  precedence, collision throwing, and Supplement declare-dropping all become fixture
  assertions with no Node and no writes.
- Deterministic ordering becomes a plan invariant, so drift stability no longer depends on
  emitter accident: `:namespaces` sorted by `:ns-name`, and within a namespace `:defs` — and
  the `:refer` list and `refer-clojure` excludes derived from them — sorted by **`:js-name`**
  (the Mantine export name), matching the current emitter byte-for-byte. Ordering is keyed on
  `:js-name`, not the kebab `:symbol`: the two diverge under ASCII sort (`-` vs. case
  boundaries), and `:js-name` both preserves byte-identity and mirrors the upstream naming a
  reader cross-references.
- Preserves ADR 0001 (source-only, Mantine-anchored artifact), ADR 0002 (supplements for
  docgen-omitted surface), and ADR 0003 (Barrel utilities generated, not supplemented) —
  this ADR relocates existing policy behind a seam; it does not change any of it.
