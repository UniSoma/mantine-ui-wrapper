# Hooks-barrel utilities are generated, not supplemented

The `@mantine/hooks` barrel exports ~17 non-hook plain functions alongside its `use*`
hooks (`randomId`, `mergeRefs`, `getHotkeyHandler`, the `*Mask` family,
`read*StorageValue`, `clamp`, `range`, `upperFirst`, …). These carry no docgen entry,
so ADR 0002 ("backfill non-docgen surface via hand-written supplements") appears to
apply. It does not.

**Decision.** Wrap these utilities by **widening the generator's barrel enumeration**,
not by hand-writing them into a supplement. The generator already produces the
`mantine.hooks` namespace by enumerating the `@mantine/hooks` barrel and filtering to
`use*`, so it is **not** a docgen-driven namespace. These utilities are *present and
enumerable in the barrel we already crawl*; only our own `use*` predicate omits them. So
the consistent fix is to relax the filter to everything-minus-excludes and reuse the
existing `emit-hook-def` (a raw-passthrough alias, identical in shape), routing non-`use*`
names to a `util-docstring`/`util-docs.edn` pair for their docs.

This draws the boundary ADR 0002 left implicit:

- **docgen-omitted** surface (providers, `Box`, compound parts) → **supplement**.
- **barrel-enumerable, non-docgen** surface (the hooks-barrel utilities) → **widen the
  generator**.

## Considered and rejected

- **Backfill via a `codegen/supplements/hooks.cljc`** (the ADR 0002 path). Rejected: it
  would hand-write, as literal source text, def-aliases for names we can read straight off
  the barrel. That is the "hand-maintained crawler-substitute" ADR 0002's own rejected
  alternative warned against. It would also require *adding* a supplement-hoisting path to
  `emit-hooks-ns`, which today has none. Enumeration is the cheaper, drift-safe mechanism.

## Consequences

- **Scope is everything-minus-excludes.** All documented utilities are wrapped. The sole
  exclude is `normalizeRadialValue`: it is undocumented, so the same "we expose what the
  docs describe" rule that governs the whole pipeline excludes it, not a value judgment.
- **Docstrings are hand-authored** in `codegen/input/util-docs.edn` (`{:desc, :page}` per
  entry). Upstream has no machine-readable source: the functions-reference page is
  hand-written MDX and the functions carry no JSDoc. The doc *URL* is derived from the JS
  name (lowercase, separators stripped: `.../guides/functions-reference/#randomid`,
  `.../hooks/use-hotkeys/#gethotkeyhandler`), so only the description must be written by hand.
- **A barrel utility included by the filter but missing a `util-docs.edn` entry fails the
  build.** This is the same enumerate-and-guard discipline as the collision and drift
  checks, and it keeps everything-minus-excludes safe across version bumps.
- **Raw passthrough, both directions**, consistent with the sibling hooks (ADR 0002). The
  utilities pair with hooks that take the same options objects, so converting a utility's
  input while its sibling hook stays raw would be a partial veneer at adjacent call sites.
