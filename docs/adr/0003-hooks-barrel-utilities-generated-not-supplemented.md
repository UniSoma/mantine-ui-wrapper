# Hooks-barrel utilities are generated, not supplemented

The `@mantine/hooks` barrel exports ~17 non-hook plain functions alongside its `use*`
hooks (`randomId`, `mergeRefs`, `getHotkeyHandler`, the `*Mask` family,
`read*StorageValue`, `clamp`, `range`, `upperFirst`, …). These carry no docgen entry —
so ADR 0002 ("backfill non-docgen surface via hand-written supplements") appears to
apply. It does not.

**Decision.** Wrap these utilities by **widening the generator's barrel enumeration**,
not by hand-writing them into a supplement. The `mantine.hooks` namespace is already
produced by enumerating the `@mantine/hooks` barrel and filtering to `use*` — it is
**not** a docgen-driven namespace. These utilities are *present and enumerable in the
very barrel we already crawl*; they are omitted only by our own `use*` predicate. So the
consistent fix is to relax the filter to everything-minus-excludes and reuse the existing
`emit-hook-def` (a raw-passthrough alias, identical in shape) — routing non-`use*` names
to a `util-docstring`/`util-docs.edn` pair for their docs.

This draws the boundary ADR 0002 left implicit:

- **docgen-omitted** surface (providers, `Box`, compound parts) → **supplement**.
- **barrel-enumerable, non-docgen** surface (the hooks-barrel utilities) → **widen the
  generator**.

## Considered and rejected

- **Backfill via a `codegen/supplements/hooks.cljc`** (the ADR 0002 path). Rejected: it
  would hand-write, as literal source text, def-aliases for names we can read straight off
  the barrel — the exact "hand-maintained crawler-substitute" ADR 0002's own rejected
  alternative warned against. It would also require *adding* a supplement-hoisting path to
  `emit-hooks-ns`, which today has none. Enumeration is the cheaper, drift-safe mechanism.

## Consequences

- **Scope is everything-minus-excludes.** All documented utilities are wrapped; the sole
  exclude is `normalizeRadialValue` — undocumented, so excluded on the same "we expose
  what the docs describe" rule that governs the whole pipeline, not as a value judgment.
- **Docstrings are hand-authored** in `codegen/input/util-docs.edn` (`{:desc, :page}` per
  entry). Upstream has no machine-readable source — the functions-reference page is
  hand-written MDX and the functions carry no JSDoc. The doc *URL* is derived from the JS
  name (lowercase, separators stripped: `.../guides/functions-reference/#randomid`,
  `.../hooks/use-hotkeys/#gethotkeyhandler`), so only the description is truly irreducible.
- **A barrel utility included by the filter but missing a `util-docs.edn` entry fails the
  build** — the same enumerate-and-guard discipline as the collision and drift checks, and
  what keeps everything-minus-excludes safe across version bumps.
- **Raw passthrough, both directions**, consistent with the sibling hooks (ADR 0002): the
  utilities pair with hooks that take the same options objects, so converting a utility's
  input while its sibling hook stays raw would be a partial veneer at adjacent call sites.
