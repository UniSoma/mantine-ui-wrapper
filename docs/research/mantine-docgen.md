> Research for ticket mnt-01kxe8gzbttp (Mantine docgen.json)

# Mantine docgen mechanism & `docgen.json` schema

Investigated against a shallow clone of `mantinedev/mantine` at repo root version **9.4.1** (all `@mantine/*` packages are `9.4.1` — root and packages agree, no drift).

## 1. The generator

- npm script: `"docs:docgen": "tsx scripts/docgen && npm run docs:count"` (root `package.json`). Also invoked by `setup`, `docs:build`, `docs:build:v7/v8`.
- Entry point: **`scripts/docgen/index.ts`**. It imports `generateDeclarations` from the external npm package **`mantine-docgen-script`** (pinned `^1.6.0` in root `package.json` devDeps; not a custom in-repo generator).
- `mantine-docgen-script` is a thin wrapper around **`react-docgen-typescript`** (`withCustomConfig`). So docgen = react-docgen-typescript + Mantine-specific filtering/type-rewriting + JSON serialization.

`scripts/docgen/index.ts` calls:

```ts
generateDeclarations({
  tsConfigPath: getPath('tsconfig.json'),
  outputPath: getPath('apps/mantine.dev/src/.docgen'),   // <-- output DIRECTORY
  componentsPaths: DOCGEN_PATHS,
  excludeProps: ['mie', 'mis', 'pie', 'pis'],
  typesReplacement: { /* ~15 Accordion/Input/Popover/etc. type aliases -> friendly names */ },
});
```

### Output path (VERIFIED — differs from the guess)

The user's guess `src/.docgen/docgen.json` is **wrong on location**. The real output is:

```
apps/mantine.dev/src/.docgen/docgen.json
```

`generateDeclarations` writes `path.join(outputPath, "docgen.json")` with `spaces: 2`. It is **gitignored / not committed** (no `.docgen` dir exists in the clone until `docs:docgen` runs). The sibling `count.json` (component/hook/page/demo counts) is written to the same dir by `scripts/docs/count.ts`.

### How generation was inspected

A full monorepo `yarn install` + docgen run is heavy/slow, so I took the **source-inference path** (permitted by the task): I `npm pack`-ed `mantine-docgen-script@1.6.0` and read its compiled implementation (`dist/esm/index.mjs`), its `.d.ts` types, and its **committed sample output** `tests/.docgen/docgen.json`. The transform is fully deterministic and readable, so the schema below is authoritative.

## 2. Inputs — what gets scanned (`scripts/docgen/docgen-paths.ts`)

`DOCGEN_PATHS` = two kinds of entries resolved by `get-declarations-paths.ts`:

- **`type: 'package'`** — scans a directory and picks every subfolder `X/` that contains `X/X.tsx` (`get-package-paths.ts`). Applied to:
  - `packages/@mantine/core/src/components` → **113** components matched
  - `packages/@mantine/dates/src/components` → **33** components matched
  - `packages/@mantine/charts/src` → **19** components matched
- **`type: 'file'`** — an explicit list (~90 files) of sub-components and other-package components: Input parts, Button/ActionIcon/Avatar groups, Popover/Menu/Tabs/Accordion/Combobox/AppShell parts, plus whole components from `@mantine/spotlight`, `@mantine/carousel`, `@mantine/dropzone`, `@mantine/code-highlight`, `@mantine/nprogress`, `@mantine/modals`, `@mantine/tiptap`, `@mantine/notifications`, `@mantine/schedule`.

## 3. `docgen.json` SCHEMA (precise)

**Top level:** a single JSON **object keyed by component display name** (NOT an array). Display name = react-docgen-typescript `displayName` with any leading `@mantine/<pkg>/` or `@mantinex/<pkg>/` prefix stripped (`get-display-name`). Entries whose name contains `.extend` (Mantine's factory helpers) are **skipped**.

**Per-component value:** after `prepareDeclaration`, the object is reduced to **just `{ "props": { ... } }`**. The generator explicitly `delete`s `tags`, `methods`, `filePath`, `displayName`, and — importantly — **`description`**. So the react-docgen `ComponentDoc` component-level description is dropped; there is **no component description, no export path, no methods** in the output.

**Per-prop value** (`props` is an object keyed by prop name, **sorted alphabetically**):

| field | type | notes |
|---|---|---|
| `name` | string | same as the key |
| `description` | string | JSDoc comment; backticks converted `` `x` `` → `<code>x</code>` (contains raw `@deprecated` when present) |
| `required` | boolean | |
| `defaultValue` | string \| null | react-docgen `defaultValue.value` if present, else `null` (never an object) |
| `type` | `{ "name": string }` | only the `name` sub-field is kept |

Prop `type.name` post-processing: enums are expanded to their `raw` union; `typesReplacement` (a ~60-entry built-in `DEFAULT_TYPE_REPLACEMENT` map merged with the caller's overrides) rewrites verbose TS types to friendly aliases (e.g. `DefaultMantineColor` → `MantineColor`, big `DetailedHTMLProps<...>` → `React.ComponentPropsWithoutRef<"div">`, Recharts blobs → `RechartsProps`); `radius` is forced to `MantineRadius | number`; trailing/leading ` | undefined` stripped; `"xs" | "sm" | ...` collapsed to `MantineSize`; wrapping parens removed.

### Prop filtering (what is EXCLUDED from every component)

`getPropsFilter` drops a prop when:
- name is in `DEFAULT_EXCLUDE_PROPS` — all the style/system props: `className`, `classNames`, `styles`, `unstyled`, `component`, `ref`, `style`, `sx`, `mod`, `variant`, `renderRoot`, `vars`, `attributes`, spacing/sizing shorthands (`m*`, `p*`, `w`, `h`, `bg*`, `c`, `fz`, `fw`, `ff`, `ta`, `bd`, `bdrs`, `pos`, `inset`, `top/left/...`, `hiddenFrom`, `visibleFrom`, `lightHidden`, `darkHidden`, `flex`, …) — plus the caller's `excludeProps` (`mie`, `mis`, `pie`, `pis`);
- name starts with `__` or `data-`;
- name is `variant` **and** type is `string`;
- the prop's declaration originates only from `node_modules` (inherited DOM/library props are excluded; only props declared in Mantine source survive).

### Real (trimmed) example entry

From the tool's own committed fixture `tests/.docgen/docgen.json` (genuine `mantine-docgen-script@1.6.0` output — real components produce identical structure):

```json
{
  "TestComponent": {
    "props": {
      "parse": {
        "defaultValue": "JSON.parse",
        "description": "Test component number prop,",
        "name": "parse",
        "required": false,
        "type": { "name": "typeof JSON.parse" }
      },
      "stringify": {
        "defaultValue": null,
        "description": "Test component string prop",
        "name": "stringify",
        "required": false,
        "type": { "name": "typeof JSON.stringify" }
      },
      "text": {
        "defaultValue": "\"Hello World\"",
        "description": "Test component text prop,",
        "name": "text",
        "required": true,
        "type": { "name": "string" }
      }
    }
  }
}
```

The package README's example shows the same shape for a Mantine-style component (`color` → `{ name: "MantineColor", required: true }`, backtick descriptions rendered as `<code>`).

## 4. Coverage & notable gaps

- **Included:** core (113), `@mantine/dates` (33) and `@mantine/charts` (19) components are **confirmed present** (all three are `type:'package'` scans), plus ~90 explicit sub-components / other-package components (spotlight, carousel, dropzone, code-highlight, nprogress, modals, tiptap, notifications, schedule). Roughly ~250 component entries total.
- **Missing by design:**
  - **Hooks** — `@mantine/hooks` is not scanned at all; docgen is components-only (hooks are documented via MDX, counted separately in `count.ts`).
  - **`.extend` factory helpers** — explicitly filtered out.
  - **Component-level descriptions**, **methods/imperative APIs**, `filePath`/export location, and `tags` — all deleted from output.
  - **All style-system / polymorphic props** (`component`, `className`, `styles`, spacing, responsive, etc.) — filtered out of every component.
  - **Inherited DOM/native props** (declared only in `node_modules`) — filtered out; only Mantine-authored props remain.
  - `@mantine/form`, `@mantine/store`, `@mantine/emotion`, `@mantine/vanilla-extract`, `@mantine/colors-generator` — not component packages, not in docgen.

## 5. Docs website consumption (CONFIRMED)

The docs site props tables are generated directly from this file:
- `apps/mantine.dev/src/components/PropsTable/PropsTable.tsx` does `import docgenData from '@/.docgen/docgen.json'` and indexes it by component name: `PROPS_DATA[component].props`, rendering Name / Type / Description columns (required → red `*`; `defaultValue` → "Default value: `<code>…`"; `@deprecated` in description flagged; Fuse.js search over `name`/`description`/`type.name`).
- Its local `Docgen` TS interface declares `description`/`displayName` too, but those are **not actually present** in the emitted JSON (dropped by the generator) — the interface is looser than the data.
- Other consumers of the `.docgen` dir: `CssFilesList` and `HomePageSponsors` (unrelated to props).

## 6. Version to pin

- `packages/@mantine/core/package.json` version = **`9.4.1`**, identical to repo root and every other `@mantine/*` package (carousel, charts, code-highlight, dates, dropzone, emotion, form, hooks, modals, notifications, nprogress, schedule, spotlight, store, tiptap, vanilla-extract, …). **No version drift** — pin to **9.4.1**.
- `mantine-docgen-script` (the generator) is a separate tool at `^1.6.0`.

## Key file references (in the mantine clone)

- `scripts/docgen/index.ts` — generator invocation + output path + type replacements
- `scripts/docgen/docgen-paths.ts` — component input list (core/dates/charts scans + explicit files)
- `scripts/docgen/get-package-paths.ts` — `X/X.tsx` folder-scan rule
- `scripts/docs/count.ts` — writes sibling `count.json`
- `apps/mantine.dev/src/components/PropsTable/PropsTable.tsx` — consumer
- generator impl (external): `mantine-docgen-script@1.6.0` `dist/esm/index.mjs` (`prepareDeclaration`, `getPropsFilter`, `DEFAULT_EXCLUDE_PROPS`, `DEFAULT_TYPE_REPLACEMENT`)
