> Research for ticket mnt-01kxf3n77ee6 (component docstring/metadata enrichment from docs-app MDX data)

# Extracting component descriptions & metadata from Mantine's docs-app MDX data

**Question:** Can component codegen pull prose descriptions (and metadata that `docgen.json` drops) out of the docs-app MDX data files — the same way hook codegen already pulls descriptions from `mdx-hooks-data.ts`?

**Short answer:** Yes, and it is a near-automatic extension of the hooks approach. The component data files are as clean as the hooks file (plain single-quoted, single-line description strings), Mantine's own tooling already parses them with a simple regex, and every real component carries a one-line `description` keyed by the exact component name that matches `docgen.json`. There are two honest caveats: (a) compound sub-components (e.g. `ButtonGroup`, `AccordionItem`) get **no** description of their own, and (b) the real Styles API selectors / CSS variables live in a *different*, less regex-friendly source, not in these files.

All paths below are inside the pinned Mantine 9.4.1 checkout at `/tmp/mantine-inv`.

---

## 1. Structure & cleanliness of the component data files

Files (under `apps/mantine.dev/src/mdx/data/`):

| File | Lines | Top-level entries | Entries with `props` (real components) |
|------|------|-------------------|----------------------------------------|
| `mdx-core-data.ts` | 1409 | 117 | 111 |
| `mdx-dates-data.ts` | 191 | 16 | ~15 |
| `mdx-charts-data.ts` | 204 | 17 | ~16 |

Each file exports one object typed `Record<string, Frontmatter>`:

```ts
import { Frontmatter } from '@/types';

export const MDX_CORE_DATA: Record<string, Frontmatter> = { ... };
```

**Key difference from hooks:** the hooks file builds every entry through a helper, `hDocs(hook, description)` (`mdx-hooks-data.ts:3-14`), which synthesizes the kebab title/slug/source from the camelCase name. The **component files do NOT use a helper** — every entry is a hand-written inline object literal. That is actually *easier* to parse (no helper-call indirection), just a different shape.

Representative entries, verbatim:

```ts
// mdx-core-data.ts:21-32
Button: {
  title: 'Button',
  package: '@mantine/core',
  slug: '/core/button',
  description: 'Button component to render button or link',
  componentPrefix: 'Button',
  props: ['Button', 'ButtonGroup', 'ButtonGroupSection'],
  styles: ['Button', 'ButtonGroup', 'ButtonGroupSection'],
  source: '@mantine/core/src/components/Button/Button.tsx',
  docs: 'core/button.mdx',
  searchTags: 'action, cta, submit, button group, link button',
},

// mdx-core-data.ts:478-488
TextInput: {
  title: 'TextInput',
  package: '@mantine/core',
  slug: '/core/text-input',
  props: ['TextInput'],
  styles: ['TextInput'],
  description: 'Capture string input from user',
  source: '@mantine/core/src/components/TextInput/TextInput.tsx',
  docs: 'core/text-input.mdx',
  searchTags: 'text field, string input, form input, search box, input box',
},

// mdx-core-data.ts:699-709
Modal: {
  title: 'Modal',
  package: '@mantine/core',
  slug: '/core/modal',
  props: ['Modal'],
  styles: ['Modal'],
  description: 'An accessible overlay dialog',
  source: '@mantine/core/src/components/Modal/Modal.tsx',
  docs: 'core/modal.mdx',
  searchTags: 'dialog, popup, window, overlay, lightbox, useModalsStack, use-modals-stack',
},

// mdx-core-data.ts:456-466
Table: {
  title: 'Table',
  package: '@mantine/core',
  slug: '/core/table',
  props: ['Table'],
  styles: ['Table'],
  description: 'Render table with theme styles',
  source: '@mantine/core/src/components/Table/Table.tsx',
  docs: 'core/table.mdx',
  searchTags: 'grid, data table, rows, columns, spreadsheet',
},
```

**Fields carried** (per the `Frontmatter` interface at `apps/mantine.dev/src/types/MdxContent.ts:1-67`; `?` = optional):

`title`, `slug`, `description?`, `group?`, `category?`, `order?`, `search?`, `searchTags?`, `date?`, `release?`, `package?`, `props?` (string[]), `docs?`, `source?`, `license?`, `styles?` (string[]), `componentPrefix?`, `polymorphic?` (boolean), `hideInSearch?`, `hideSiblings?`, `hideHeader?`, `hideTableOfContents?`.

In practice the component entries populate: `title`, `package`, `slug`, `description`, `props`, `styles`, `source`, `docs`, `searchTags`, and — where relevant — `componentPrefix` and `polymorphic`. **Note:** although the type defines `group` and `category`, **no core/dates/charts entry actually sets either** (`grep -c 'category:|group:'` = 0 in all three files). So there is no category grouping to recover from these files.

---

## 2. Is there a component DESCRIPTION string? (the gap docgen.json leaves)

Yes — this is the headline finding. Every real component entry has a one-line prose `description`. Verbatim:

- `Button` → `'Button component to render button or link'`
- `TextInput` → `'Capture string input from user'`
- `Modal` → `'An accessible overlay dialog'`
- `Table` → `'Render table with theme styles'`
- `Badge` → `'Display badge, pill or tag'`
- `Accordion` → `'Divide content into collapsible sections'`
- (dates) `DatePicker` → `'Inline date, multiple dates and dates range picker'`
- (charts) `AreaChart` → `'Area chart component with stacked, percent and split variants'`

**Keying — matches `docgen.json` directly.** The object **key** is the exact PascalCase component name (`Button`, `TextInput`, `Modal`), which is the same key `docgen.json` uses for the top-level component. So the merge key is the data-file's object key.

Two keying subtleties worth flagging:

1. **Use the key, not `title`, as the join field.** For components `title === key` (both `Button`), so it doesn't matter. But this differs from hooks, where `hDocs` sets `title` to the *kebab* form (`use-click-outside`) while the key is camelCase (`useClickOutside`). If a shared parser keys on `title` it will break for hooks; keying on the **object key** is correct for both.
2. **Section/landing pages are mixed in.** Entries like `CorePackage` (`mdx-core-data.ts:4`), `GettingStartedCharts`, `GettingStartedDates` have `hideInSearch: true` and **no `props`** array. These are not components and must be filtered out (Mantine's own scripts filter by "has `package` + `slug` + `title`" and, for components, "`props.length > 0`").

---

## 3. Metadata that `docgen.json` DROPS — what's recoverable here vs not

Recoverable from the MDX data files:

- **Polymorphic (`component` prop) flag** — `polymorphic: true` (e.g. `Input` at line 74, `ActionIcon` at 87, `Badge` at 506). This tells you the component accepts the polymorphic `component`/`renderRoot` prop, which is exactly one of the props `docgen.json` strips. You recover the *fact that it's polymorphic*, not a typed signature for `component`.
- **Styles API group names** — the `styles: [...]` array lists the selector-group names (e.g. Button → `['Button','ButtonGroup','ButtonGroupSection']`). This is a reference/index, not the selectors themselves.
- **Compound-component prop set** — the `props: [...]` array enumerates every prop-interface `docgen.json` key that belongs to this component family (e.g. `Input` → `['Input','InputWrapper','InputLabel','InputDescription','InputError']`). This is the bridge from one data entry to its several `docgen.json` entries.
- **`componentPrefix`** — the shared prefix used to render compound sub-component names.
- Source path, docs path, search tags, package.

**NOT recoverable from these files:**

- The actual **Styles API selectors and CSS variables** (`--button-bg`, the `root`/`label`/`section` selectors, `data-*` modifiers). Those live in a **separate** source: `packages/@docs/styles-api/src/data/<Component>.styles-api.ts`. Example, `Button.styles-api.ts`:
  ```ts
  export const ButtonStylesApi: StylesApiData<ButtonFactory> = {
    selectors: { root: 'Root element', loader: 'Loader component, displayed only when `loading` prop is set', ... },
    vars: { root: { '--button-bg': 'Controls `background`', ... } },
    modifiers: [ { modifier: 'data-disabled', selector: 'root', condition: '`disabled` prop is set' }, ... ],
  };
  ```
  These files are richer but **less regex-friendly** — the description values are full of backticks and embedded markup. Extracting them cleanly needs a real TS parse, not a one-line regex. They are a *separate, harder project* from the description enrichment this ticket is about.
- The **style-system spacing props** and `styles`/`classNames`/`variant`/`className` prop signatures — the data files only reference group names; they don't re-describe those props.
- **Category/group** — defined in the type but unused (see §1).

---

## 4. Coverage & keying

- **~140+ real component entries** across the three files (core 111 with `props`, dates ~15, charts ~16). Every entry that has a `props` array is a documented component and carries a `description`.
- **Every top-level documented component has a data entry with a description.** The gap is not missing top-level components; it's granularity below the component level.
- **Compound sub-components are NOT top-level keys.** `ButtonGroup`, `ButtonGroupSection`, `AccordionItem`, `AccordionControl`, `Table.Tr`/`TableTr`, etc. do **not** exist as their own object keys (confirmed: `grep '^  ButtonGroup:|^  AccordionItem:'` → no matches). They appear only inside the parent's `props: [...]` / `styles: [...]` arrays. Consequence: `docgen.json` will have separate entries like `ButtonGroup`, but the MDX data gives you **one description for the whole family** (`Button`) and no per-sub-component prose. You can attribute the parent's description (or nothing) to sub-components; there is no dedicated sub-component sentence to pull.
- So the mapping is **one MDX entry → many `docgen.json` keys**, and the `props` array is the explicit list of those keys.

---

## 5. Extraction difficulty verdict

**Difficulty: LOW — essentially the same as the hooks extraction, with strong precedent.**

Which file(s) a headless plain-Clojure generator reads: `mdx-core-data.ts`, `mdx-dates-data.ts`, `mdx-charts-data.ts` (and the other `mdx-*-data.ts` siblings if you want form/others too). Read the raw `.ts` text; **no TS/JS runtime or AST parse is required** for the description + basic metadata.

**Precedent — Mantine already does exactly this with regex.** `scripts/llm/compile-mcp-data.ts` has a `parseMdxEntries()` function (lines 79-113) that globs `mdx-*-data.ts` and extracts `{id, name, description, package, slug, source, docs, propsRefs}` per component:

```ts
// scripts/llm/compile-mcp-data.ts
const exportMatch = fileContent.match(/export const MDX_\w+_DATA[^=]*=\s*{([\s\S]*?)};/);
const componentRegex = /(\w+):\s*{([^{}]*(?:{[^{}]*}[^{}]*)*)}/g;   // per-entry block
// ...per block:
const descriptionMatch = block.match(/description:\s*['"]([^'"]+)['"]/);
const packageMatch     = block.match(/package:\s*['"](@mantine\/[^'"]+)['"]/);
const slugMatch        = block.match(/slug:\s*['"]([^'"]+)['"]/);
const titleMatch       = block.match(/title:\s*['"]([^'"]+)['"]/);
// requires package + slug + title, else skip (filters out landing pages)
```

`scripts/llm/compile-mcp-data.ts:142-159` contains a second, identical copy of the same regex approach. So the block regex `/(\w+):\s*\{ ... \}/g` plus a per-field `description:\s*'([^']+)'` is a proven, sufficient parse.

**Are descriptions regex-safe? Yes — verified exhaustively.** Across all 149 `description:` lines in the three files:
- **Zero** use double-quote delimiters (all single-quoted).
- **Zero** contain a backtick or a double-quote character.
- **Zero** contain an escaped/embedded apostrophe (`grep` for `description: '...\...'` and for apostrophes inside the string → no matches).
- **Zero** are multi-line (no `description: '...` line lacking a closing quote).

So `description:\s*'([^']+)'` captures every one cleanly — same safety profile as the hooks descriptions.

**Gotchas to handle (all minor):**
1. **Filter non-components.** Skip entries with no `props`/no `package` (landing pages like `CorePackage`, `GettingStartedCharts`).
2. **`searchTags` can wrap to a second line** (e.g. `Drawer` at line 720-721). The block regex tolerates this since it captures to the matching brace; a naive line-by-line reader would not. Descriptions never wrap, so if you only want `description` this is moot.
3. **Don't reuse the same parser blindly on `mdx-hooks-data.ts`** — that file uses `hDocs(...)` calls, not literals, so it needs the helper-call regex the current hooks path already uses. Components use literals.
4. **Key on the object key, not `title`** (see §2).
5. No computed values or template literals appear in the fields you care about (`description`, `package`, `slug`, `polymorphic`, `props`, `styles`).

---

## 6. Autogenerated or hand-authored?

**Hand-authored.** No script writes these files:
- `grep` across `scripts/` for anything that `writeFile`s an `mdx-*-data.ts` → none. The two scripts that touch these files (`scripts/llm/compile-mcp-data.ts`, `scripts/llm/compile-llm-doc.ts`) only **read** them (via `glob('mdx-*-data.ts')`) to produce compiled LLM/MCP outputs.
- The other consumer is `apps/mantine.dev/src/mdx/mdx-data.ts`, which just spreads all `MDX_*_DATA` objects into one `MDX_DATA` map for the docs site.
- `git log` on `mdx-core-data.ts` shows only the release commit (`[release] Version: 9.4.1`) — consistent with a shallow clone, and there's no generator commit trail.

Practical implication: these descriptions are curated by Mantine maintainers and travel with each release. They're stable input, exactly like `docgen.json` — but must be re-pulled per Mantine version bump (as should already happen at the 9.4.1 pin).

---

## 7. Recommendation

**Adopt it — it's a near-automatic extension of the existing hooks-extraction decision, not a new capability.**

Mirror the hooks flow: at codegen prep, parse the component `mdx-*-data.ts` files into a committed `{component-name → {description, polymorphic?, props-refs, styles-groups, source}}` input, then merge `description` into the generated component docstring (filling the exact prose gap `docgen.json` leaves) and optionally surface `polymorphic` and the Styles API group names. The parse is a small, proven regex — Mantine ships the reference implementation in `scripts/llm/compile-mcp-data.ts`.

**Real (but bounded) complications to decide on, not blockers:**
1. **Sub-components inherit nothing.** `ButtonGroup`, `AccordionItem`, `Table.Tr`, etc. have `docgen.json` entries but no MDX description. Decide: attach the parent family's description, or leave sub-component docstrings prose-less. Use the parent's `props: [...]` array as the authoritative parent→sub-component map.
2. **Real Styles API selectors/CSS vars are out of scope of these files.** If docstrings should list actual selectors/`--css-vars`, that's a **second, harder extraction** from `packages/@docs/styles-api/src/data/*.styles-api.ts` (backtick-laden values → needs a proper parse). Recommend treating that as a separate follow-up ticket; don't couple it to the (easy) description work.
3. **No category/group data** to recover from these files (the fields exist in the type but are unset), so any category grouping would have to come from elsewhere (e.g. the navbar data), not here.

Net: descriptions + `polymorphic` + styles-group names are low-risk, high-value, and should be done now on the hooks pattern. Actual Styles API selector text is the only piece that warrants its own effort.
