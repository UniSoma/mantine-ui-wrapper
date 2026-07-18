# The converter is deep-by-default, camelizing, guarded by a denylist and a `raw` escape

ADR 0006 revises the **value-handling** half of the props-conversion decision
(`mnt-01kxe8gzn6bt`). That decision chose *camelize keys + shallow-default values +
a curated deep-convert set* `#{style styles classNames vars}`. The consequence: any
nested config map silently broke. A `:confirm-props {:left-section icon}` passed
through untouched, so a CLJS `PersistentArrayMap` landed on the JS props object;
when Mantine spread it onto `<Button>` none of the Mantine keys existed as own JS
properties, so the styled button rendered as a bare default — **no error, just
wrong**. The same held for modal `:labels`, and any `*Props`-shaped nested value.
The escape was to hand-write `#js {…}` (see the demo, `demo/mantine/demo.cljs`),
dropping out of idiomatic CLJS and back into camelCase.

Ticket `mnt-01kxh6gf162j` originally scoped the fix as a *curated allowlist* of
nested keys that would opt into deep conversion. Grilling that design surfaced two
walls: (1) per-call opt-in only reaches hand-written supplement fns — the
data-driven `:actions` prop flows through the **generated** `spotlight` factory,
which calls `(p/convert props)` with no opts, so the allowlist could not reach it
without generator machinery; and (2) an allowlist must be *complete* to be uniform,
but the set of `*Props` keys is large and docgen cannot classify them (it strips
exactly this surface — see `mnt-01kxe8gzn6bt` notes).

## Decision

Convert **deeply by default**. The single converter (`mantine.impl.props/convert`)
recurses into nested maps and vectors-of-maps, applying the *same* rules at every
depth: kebab→camel keys (data-/aria-/`--` exempt) and the `:class` / `:style` /
`:styles` / `:classNames` / `:vars` leaf handling. Raw-passthrough safety, which the
old shallow default gave for free, is restored by two explicit mechanisms:

- **Denylist.** A small set of keys — currently just `:inner-props` — is never
  converted; the value passes through as a raw CLJS map. This is the context-modal
  slot: `open-context-modal`'s `:inner-props` is handed to a *CLJS* modal component
  that reads it as a CLJS map, so converting it to a JS object would destroy the
  CLJS→CLJS handoff (qualified keywords become dead string keys; sets/records
  mangle). Deny-by-name keeps the idiomatic call working with zero ceremony.
- **`raw` escape helper.** A public helper that tags any value to skip conversion,
  as the general opt-out for raw-CLJS payloads the denylist does not name. It is a
  **wrapper value** (`deftype`-style), not bare metadata, specifically so it
  survives `merge` / `select-keys` / map rebuilds — metadata is silently stripped by
  those, and a stripped tag reintroduces the silent-mangle bug at exactly the moment
  a real app assembles the payload through a pipeline.

The `:&` escape hatch (force a raw *JS* object, merged last) and the children path
from `mnt-01kxe8gzn6bt` are unchanged. Camelization is **kept** — see below.

**Why now:** the wrapper is pre-release with no users, so reversing a locked
decision costs only this repo and its demo. After release it would be a breaking
change to every consumer's prop maps. This is the window.

## Considered and rejected

- **Curated opt-in allowlist** (the original ticket plan; per-call `:deep` set threaded
  into `convert`). Rejected: cannot reach generated factories (spotlight `:actions`)
  without generator changes, and cannot be made uniform without enumerating a large,
  docgen-opaque set of `*Props` keys. Deep-by-default makes the whole question moot —
  collections and nesting work everywhere, including generated factories, with no
  generator change.
- **Blind `clj->js`** (the semantic-ui-wrapper design; confirmed by research to be
  `(apply react/createElement class (clj->js props) children)` with no curation).
  Rejected for the same reasons `mnt-01kxe8gzn6bt` rejected it: `clj->js` preserves
  hyphens so every kebab prop key goes dead (`:on-click` → `"on-click"`, never bound),
  and it deep-mangles opaque CLJS payloads with **no way to keep a value as a CLJS
  map** — its only escape is `#js`, which forces the value the *opposite* direction.
  Our `raw` helper is the CLJS-preserving escape SUIW structurally cannot offer.
- **Drop camelization** (write camelCase prop keys, matching Mantine's docs and our
  docstrings). Genuinely weighed — with no users it is cheap, and deep conversion
  would then only flip containers, never rename keys. Rejected: camelization is ~12
  memoized lines already written and tested, it keeps the call site uniformly kebab
  (`open-confirm-modal` beside `:close-on-confirm`) — the same idiom that justifies
  kebab var names — and deep-by-default makes it *safe everywhere it fires*, because
  the only values that would be wrongly camelized (raw CLJS payloads) are precisely
  the ones the denylist / `raw` helper exclude from conversion.
- **Bare metadata for the escape** (`(vary-meta x assoc ::raw true)`). Rejected in
  favour of a wrapper value: CLJS metadata is silently dropped by `merge` (second
  arg), `select-keys`, and any map rebuild — the failure is invisible and strikes
  when the payload is assembled through a pipeline, the worst possible time.

## Consequences

- **Raw-passthrough safety inverts.** The old shallow default was safe-by-default and
  needed opt-*in* to deep-convert; the new default is convert-by-default and needs
  opt-*out* to stay raw. Known structural slots are protected by name (`:inner-props`),
  but an *unanticipated* raw-CLJS slot mangles silently unless the author reaches for
  `raw`. Accepted: `:inner-props` is likely the only structural case — most "data"
  props (chart `:data`, select `:data`) *want* JS conversion because they feed JS
  libs; the only "keep it CLJS" case is data flowing back into the caller's own CLJS
  code, i.e. context-modal inner props.
- **Hot-path cost.** `convert` now walks nested structure on every render, versus the
  old top-level-only walk. Accepted for the ergonomics; the leaf vocabulary is bounded
  and camelize is memoized.
- **Supersedes** the value-handling of `mnt-01kxe8gzn6bt`. That decision's key-casing
  (hybrid kebab→camel), `:&` escape hatch, and children path stand unchanged; only the
  shallow-default + curated-deep-set clause is replaced.
