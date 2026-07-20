# Generated docstrings are Markdown-safe for cljdoc

Generated component docstrings (`codegen/plan.clj`, `component-docstring` /
`prop-lines`) are consumed by **two** audiences with different rendering rules:
`clojure.repl/doc` and editor hovers show them as **raw text**, while cljdoc
renders them as **Markdown**. The prop list was authored for the first audience —
one prop per source line, 2-space indent — which cljdoc's Markdown renderer
collapses: single newlines become spaces, so every prop ran together into one
wall of text. Two angle-bracket hazards compounded it: descriptions carry literal
`<code>…</code>` from Mantine's docgen (2428 occurrences), and prop **type** names
carry TS-generic brackets (`Omit<PortalProps, "children">`, `Partial<Record<…>>`)
— both look like HTML tags to cljdoc's sanitizer and get mangled or dropped.

## Decision

Emit each prop as a Markdown **bullet list item**, so the fix reads well in both
audiences:

```
- **{name}** `{type}`{ **(required)**}{ _(default `{d}`)_} — {description}
```

- The **type is wrapped in backticks** (inline code), so its `|`, `"`, and
  TS-generic `<…>` are all literal — the type-name hazard is neutralized
  structurally, not per-token.
- Descriptions are emitted as **live Markdown** (not fenced, not escaped),
  through the existing `squash-ws` collapse. An audit of all 4,530 non-empty
  descriptions found zero prose hazards for `_ * < > |` (they only ever appear
  inside `<code>` spans or as `->`/`=>` operators), the two `[text](url)`
  sequences render as intended links, and multi-line descriptions with interior
  `- ` lines are flattened by `squash-ws` so they never become stray lists.
- **Backtick invariant.** A description is sanitized in a fixed order: first
  **escape any literal backtick already present in the raw text**, then convert
  `<code>…</code>` → backticks. The only *active* backticks in the output are
  therefore the ones we emit from balanced `<code>` pairs — code spans can never
  be left open. This kills the one description that actually broke rendering
  (`DateTimePicker.defaultTimeValue`, an upstream stray-backtick typo) and any
  future one of its kind.

The change is produced **through codegen**, never hand-edited into generated
files; `docgen.json` itself stays a verbatim upstream copy (`codegen/extract.clj`)
and is not patched.

## Considered and rejected

- **Fenced code block** around the whole Props section. Neutralizes every
  angle-bracket hazard in one move and preserves column alignment, reading
  identically in REPL and cljdoc. Rejected in favour of the bullet list for its
  native-docs typography on cljdoc — at the cost of accepting the (audited,
  effectively nil) live-Markdown risk in descriptions and the explicit backtick
  invariant above.
- **Patching the typo in `docgen.json`.** Rejected: `docgen.json` is a verbatim
  copy of Mantine's docgen output, re-extracted on every anchor bump, so a manual
  patch violates that invariant and is clobbered by the next `bb extract`. The
  escape-then-convert rule fixes the symptom in codegen where it belongs.

## Consequences

- Descriptions are trusted as Markdown. Should Mantine ever ship prose that
  genuinely needs emphasis/link escaping beyond backticks, it would render oddly
  — accepted, because the audit shows the current surface is clean and the
  backtick invariant covers the one real failure mode.
