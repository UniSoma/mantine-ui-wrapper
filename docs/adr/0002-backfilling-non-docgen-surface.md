# Backfilling non-docgen Mantine surface via guarded hand-written supplements

The wrapper is docgen-driven, but Mantine's `docgen.json` omits real surface: provider
components (`MantineProvider`), polymorphic primitives (`Box`), plain functions
(`createTheme`, `rem`), package-local hooks (`useMantineTheme`), whole packages with no
components (`@mantine/form`), and — inconsistently — dot-notation compound parts
(`Menu.Dropdown`, `AppShell.Main` are absent while `Combobox.Dropdown`,
`AppShell.Header` are present). Without a fix the wrapper cannot render a screen.

**Decision.** Backfill omitted surface by hand, in `codegen/supplements/*.cljc`, rather
than teaching the generator to auto-discover it. Two generate-time guards keep the
hand-written lists honest across version bumps:

- **Collision guard** (`bb generate`): fail when a supplement def name collides with a
  now-generated def — "docgen grew coverage, delete the supplement entry" — instead of
  the silent last-wins redefinition the merge would otherwise produce.
- **Compound-part coverage check** (`bb coverage`): enumerate each wrapped component's
  Capitalized static keys (minus machinery: `extend`/`withProps`/`displayName`/`classes`/
  `varsResolver`) and fail on any not covered by docgen or a supplement. This check, not a
  prose list, is the source of truth for the backfill.

Packages with zero docgen components (`@mantine/form`) are wrapped through a
**supplement-only package** path: the generated namespace's body is entirely its hoisted
supplement.

## Considered and rejected

- **Teach the generator to auto-discover omissions** (runtime introspection of exports and
  static properties). Rejected: the omitted set is small, enumerable, and slow-moving; a
  crawler is a whole new discovery mechanism with its own failure modes, and the guards
  give us drift-detection without it. The two guards deliberately make the hand-written
  path safe rather than replacing it.

## The conversion boundary (why `create-theme` converts but form does not)

A companion decision, recorded here because it will otherwise be re-litigated per package:
the wrapper converts the **input construction surface** and passes **runtime returns**
through raw.

- Props convert (clj map → React, kebab→camel) — the library's core, mechanical value.
- `create-theme` wraps `clj->js`, so theme keys are **camelCase-only** (unlike props,
  which also accept kebab). Full prop-style deep conversion was rejected: it would need a
  second, deeper converter that hard-codes Mantine's theme schema and silently corrupts the
  freeform parts where map keys are user data (`theme.colors` names, `theme.other`).
- Every hook — including `@mantine/form`'s `use-form` — is **raw passthrough**. No js↔clj
  accessors: the library does not editorialize on return-value shape it cannot know, and a
  partial veneer over ~6 of `@mantine/form`'s ~40 methods would be both incomplete and
  inconsistent. Interop conventions are a consumer-layer concern, exactly like the
  field-DSL the wrapper also declines to bake in.
