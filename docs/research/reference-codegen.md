> Research for ticket mnt-01kxe8gz8v4w (semantic-ui-wrapper codegen)

# How fulcrologic/semantic-ui-wrapper generates its wrappers

Investigated against a fresh clone of https://github.com/fulcrologic/semantic-ui-wrapper (default branch, `semantic-ui-react ^2.1.4`). All file references below are relative to that repo unless noted.

## TL;DR

- The generator is a single **Clojure** file, `src/dev/user.clj` (~270 lines), run interactively from a REPL. There is no build-time hook — regeneration is a manual, occasional step.
- The **source of truth is not** Semantic UI React's runtime `propTypes`/`handledProps`/`_meta`. It is a directory of **pre-built JSON files** (`docs/src/componentInfo/*.json`) produced inside a checkout of Semantic-UI-React by running `yarn build:docs`. The Clojure generator only reads those JSON files. This is the direct analogue of our `docgen.json`.
- Each component becomes one `.cljc` file: a `(def ui-xxx "<docstring>" #?(:clj <stub> :cljs (h/factory-apply Component)))`. The docstring embeds the human-readable prop list. There is **no machine-readable prop metadata** carried into the runtime — props are only documentation.
- Runtime prop conversion is deliberately **minimal**: `(clj->js props)` and nothing more. No kebab->camel, no `:style` special-casing, no className merging, no event-handler wrapping. Callers must already write camelCase keys (`:labelPosition`, `:onClick`).

---

## 1. Codegen source of truth

The generator reads a folder of JSON files, one per component. From `README.adoc` ("Regenerating Factories", lines ~144-169) the workflow is:

```
git clone https://github.com/Semantic-Org/Semantic-UI-React
cd Semantic-UI-React
yarn install
yarn build:docs          # emits docs/src/componentInfo/*.json
# then, in the wrapper repo's REPL:
(gen-factories "path/to/Semantic-UI-React/docs/src/componentInfo")
```

So the *upstream React project's own doc-build task* (react-docgen-based, but post-processed into SUIR's own `componentInfo` shape) is the source of truth. The wrapper never introspects the running JS component and never parses TypeScript.

The exact JSON fields the generator consumes (from `read-info` + `gen-factory-map` + `gen-docstring` in `src/dev/user.clj`):

- Top level per file: `displayName`, `type`, `repoPath`, `docblock.description`, `props`.
- Per prop (`props` is an array): `name`, `type` (already flattened to a **string** like `"bool"`, `"enum"`, `"func"`, `"elementType"`, `"arrayOf"`, or a union rendered like `"bool|enum"`), `description`, and `value` (an array of allowed enum values, or richer nested metadata for `shape`/`arrayOf`).

Key detail: `type` arrives as a ready-to-print string and `value` as the enum list. The generator does no type analysis of its own — it just formats these fields. (For complex props the raw `value` leaks through verbatim, e.g. `ui_tab.cljc` shows `panes (arrayOf): ... ([:name "shape"], [:value {...}])` — the EDN-ified JSON substructure printed straight into the docstring.)

`repoPath` (e.g. `src/elements/Button/Button.js`) is what drives the output namespace/path tree (see §3).

## 2. The generator itself

- **Location:** `src/dev/user.clj`, `ns user`. Loaded via the `:dev` alias in `deps.edn` (`:extra-paths ["src/workspaces" "src/dev"]`).
- **Language / runtime:** plain Clojure (JVM). Deps: only `clojure.data.json`, `clojure.java.io`, `clojure.string`.
- **How it's run:** manually from a REPL — `(gen-factories "<componentInfo dir>")`. The `(comment ...)` block at the bottom (lines 257-271) documents the exact call and even has the original authors' machine paths hard-coded as examples.
- **What it emits:**
  - One `.cljc` file per component under `src/main/com/fulcrologic/semantic_ui/...` (path mirrors the upstream `repoPath` tree).
  - One aggregate `src/main/com/fulcrologic/semantic_ui/factories.cljc` containing every `ui-*` def (convenience "import everything" ns; bloats advanced builds, per the README).
  - One `src/main/com/fulcrologic/semantic_ui/icons.cljc` — special-cased: when `displayName == "Icon"`, it reads the `name` prop's `value` enum list and emits a `(def xxx-icon "css class")` for every icon name.

Core generator functions in `src/dev/user.clj`:

| fn | role |
|----|------|
| `gen-factories` (173) | entry point: lists the componentInfo dir, reads each JSON, maps to a factory descriptor, sorts, spits all files. |
| `read-info` (150) | `json/read` with `:key-fn keyword`. |
| `gen-factory-map` (131) | builds `{:class :factory-name :filename :ns :docstring :props ...}` from one component's JSON. |
| `hyphenated` (67) | `camelCase` -> `kebab-case` for the factory symbol name only. |
| `gen-docstring` (73) | assembles the description + `Props:` bullet list from `props`. |
| `factory-helper-function` (99) | renders the `(def ui-xxx "docstring" #?(:clj ... :cljs ...))` form. |
| `factory-ns-cljs` (124) | renders the per-component namespace + require. |
| `icons-ns` (154) | renders the icons namespace. |

There is also a **one-time migration** section (lines 185-255: `migrate-component-files!`, `fix-js-requires!`) — regex rewrites that retrofitted CLJ SSR stubs and reader-conditional-wrapped JS requires onto already-generated files. Not part of normal codegen; safe to ignore for our purposes, but instructive that they treated generated files as re-editable text.

## 3. Shape of a generated namespace

Per-component file (`src/main/.../elements/button/ui_button.cljc`), produced by `factory-ns-cljs`:

```clojure
(ns com.fulcrologic.semantic-ui.elements.button.ui-button
  (:require
    [com.fulcrologic.semantic-ui.factory-helpers :as h]
    #?(:cljs ["semantic-ui-react$Button" :as Button])))

(def ui-button
  "A Button indicates a possible user action.

  Props:
    - active (bool): A button can show it is currently the active user selection.
    - as (elementType): An element type to render as (string or function).
    - color (enum): A button can have different colors (red, orange, yellow, ...)
    - labelPosition (enum): ... (right, left)
    - onClick (func): Called after user's click.
    ..."
  #?(:clj  (h/make-stub-factory "Button")
     :cljs (h/factory-apply Button)))
```

Structure decisions:
- **Factory = a `def`, not a `defn`.** `ui-button` is bound to a function value returned by `h/factory-apply`. Call convention is `(ui-button props & children)`.
- **Naming:** `ui-` + kebab-cased `displayName`. Nested SUIR components (`Button.Group`) arrive as flat display names (`ButtonGroup`) -> `ui-button-group`. Namespace path segments come from `repoPath` (lower-cased, middle segments), so a component lands at a path mirroring SUIR's `elements/button/...`.
- **Docstring** = description + a sorted `Props:` bullet list; each bullet is `name (type): description (enum-values)`. Enum/`value` list truncated at 100 entries with ` ...`. This is purely documentation — nothing reads it at runtime.
- **No per-component prop schema, no spec, no defaults** are carried into runtime. The only structured knowledge that survives codegen is the small hard-coded component-category sets in the generator (§ below).
- **Reader conditionals everywhere:** `:cljs` gets the real React factory; `:clj` gets a `dom-server` stub (for SSR/testing). The JS require is wrapped in `#?(:cljs ...)` (a comment at line 117 notes shadow-cljs 2.18.0 needed the **string** require form `["semantic-ui-react$Button" :as Button]`, not the symbol form).

Special component handling is via **hard-coded name sets** in the generator, not from metadata:
- `input-factory-classes` (86) -> use `wrapped-factory-apply` (form elements needing `:value` support): Input, Checkbox, FormInput, DropdownSearchInput, Search, TextArea.
- CLJ-stub variants (21-65): `form-input-components`, `select-components`, `textarea-components`, `checkbox-components`, `modal-components` pick which `make-*-stub-factory` the `:clj` branch gets.

## 4. Runtime CLJS-props -> React-props conversion

This is the important and slightly surprising finding. The shared conversion logic lives in `src/main/com/fulcrologic/semantic_ui/factory_helpers.cljc` and it is *tiny*:

```clojure
(defn factory-apply [class]
  (fn [props & children]
    #?(:clj  (apply dom/create-element class props children)
       :cljs (apply react/createElement class (clj->js props) children))))

(defn wrapped-factory-apply [class]      ; for form inputs needing controlled :value
  #?(:clj  (factory-apply class)
     :cljs (let [factory (dom/wrap-form-element class)]
             (fn [props & children]
               (apply factory (clj->js props) children)))))
```

So the entire prop transformation on the `:cljs` path is **`(clj->js props)`** — Fulcro/React see whatever keys you passed, verbatim. Concretely:

- **kebab->camel: NOT done.** The wrapper expects the *caller* to write camelCase keyword keys. The README's porting example uses `:labelPosition "right"`, `:onClick`, etc. This is why the generator preserves camelCase prop names in docstrings verbatim (it only kebab-cases the *factory symbol*, never the prop keys).
- **`:style`: no special handling.** `clj->js` turns a `{:style {:color "red"}}` map into a JS object; React accepts that. No px-adding, no CSS-var handling.
- **className: no merging.** Whatever `:className` string you pass is passed through. (Semantic UI's own component does class composition internally from its semantic props like `:primary`, `:color`.)
- **Event handlers / refs / children:** untouched. `children` are passed as trailing varargs straight to `createElement` (not converted — so children must already be React elements / strings, which the nested `ui-*` factories produce). No render-prop or function-child special handling.
- `wrap-form-element` (Fulcro built-in) is the one behavioral add-on: it makes text inputs behave as controlled components so cursor position isn't lost — only applied to the hard-coded input set.

Takeaway: SUIW pushes *all* naming responsibility onto the caller and does essentially zero prop massaging. A Mantine wrapper will almost certainly want *more* here (at least a documented convention; possibly kebab->camel and `:style`/`:className` conveniences).

## 5. How the JS component is referenced (interop)

shadow-cljs npm interop via the `pkg$Export` require sugar:

```clojure
#?(:cljs ["semantic-ui-react$Button" :as Button])   ; per-component file
```

`"semantic-ui-react$Button"` = "import the `Button` named export from the `semantic-ui-react` npm package." The aggregate `factories.cljc` instead does one `[semantic-ui-react :as suir]` and refers to `suir/Button`. The npm dep is declared in `package.json` (`semantic-ui-react`), resolved by shadow-cljs; `shadow-cljs.edn` defines only a `:workspaces` browser build for the demo — the library itself ships as source and consumers bring their own `semantic-ui-react`. Per-component requires are what let consumers pull in only the components they use (tree-shaking of the CLJS side).

## 6. What to reuse vs. what won't transfer for a Mantine wrapper

### Reusable / worth adapting
- **The whole architecture:** JSON metadata file -> per-component `.cljc` with a `(def factory ...)` + docstring, plus an aggregate namespace. Our `docgen.json` (react-docgen-typescript output for Mantine) maps cleanly onto the `componentInfo` role. We read metadata, we do NOT introspect the runtime.
- **Symbol/namespace derivation from a `repoPath`-like field** and `displayName` -> kebab factory name (`hyphenated`, lines 67-71). Directly portable.
- **Docstring assembly** (`gen-docstring`): description + sorted `Props:` bullets with `name (type): description (enum values)`. Mantine docgen gives us richer per-prop `type`/`description`/`defaultValue` — we can produce strictly better docstrings.
- **Reader-conditional split** (`:cljs` real factory / `:clj` SSR stub) — good pattern if we care about CLJ/SSR/testing.
- **shadow-cljs `pkg$Export` per-component require** for tree-shakeable, granular imports. Mantine's `@mantine/core` is likewise a named-export ESM package, so `["@mantine/core$Button" :as Button]` should work the same way.
- **Special-case-by-name sets** (form inputs needing controlled-value wrapping): Mantine has the same category (TextInput, Textarea, Select, Checkbox, NumberInput...). We'll want an analogous small curated set rather than deriving it from metadata.
- **Manual REPL / script entry point** is simple and adequate; but see below — we should make ours a headless `bb`/`clj -X` task rather than a REPL `comment` block.

### Semantic-UI-specific — will NOT transfer
- **The `componentInfo` build step** (`yarn build:docs`) is SUIR-specific tooling. Our equivalent is running react-docgen-typescript over Mantine's `.d.ts`/TSX to produce `docgen.json`. The JSON *shape* differs (Mantine/TS types are structured objects, not the pre-flattened `"bool|enum"` strings SUIR emits) — our `gen-docstring`/type-rendering must parse TS type shapes ourselves. This is the single biggest divergence.
- **Icons namespace** (`icons.cljc`): SUIR ships icons as string enum values on the `Icon` component's `name` prop. Mantine icons come from a separate package (`@tabler/icons-react`) with a totally different model — the icons codegen path does not carry over.
- **`clj->js`-only, no-normalization prop convention**: transferable as code, but it's a *choice* we should reconsider. SUIR relies on semantic boolean/enum props (`:primary`, `:color "red"`) and camelCase input from callers; Mantine's API is more prop-object/style-system oriented. Decide our prop convention deliberately (camelCase passthrough vs. kebab->camel + `:style`/`:className` conveniences) rather than inheriting SUIW's minimalism by default.
- **Fulcro coupling:** `factory-helpers` depends on `com.fulcrologic.fulcro.dom` / `dom-server` and Fulcro's `wrap-form-element`. Our wrapper is framework-agnostic, so we must supply our own `create-element` wrapper (e.g. straight `react/createElement`, or Reagent/UIx/Helix-adapter-based) and our own controlled-input handling instead of Fulcro's.
- **`make-*-stub-factory` SSR stubs** assume Fulcro dom-server; keep the *idea* (a `:clj` branch) but reimplement without Fulcro.

### Concrete recommendations for our Mantine codegen
1. Treat `docgen.json` exactly as SUIW treats `componentInfo/` — the sole input; don't introspect runtime components.
2. Emit one `.cljc` per component (`(def ui-xxx "docstring" factory)`) plus an aggregate ns, mirroring Mantine's package/category structure for namespaces; derive factory names by kebab-casing `displayName`.
3. Build docstrings from docgen `description` + a sorted prop list including `type`, `description`, and `defaultValue` (richer than SUIW). Truncate long enum/union lists.
4. Write ONE shared `factory-helpers` conversion fn (the SUIW hot-spot) and decide its policy up front: at minimum `clj->js`; strongly consider kebab->camel, `:style` map handling, and `:className`/`:class` merging since Mantine users will expect them. This fn is where we should diverge from SUIW, not copy it.
5. Use shadow-cljs `["@mantine/core$Button" :as Button]` per-component requires for granular, tree-shakeable imports.
6. Curate a small by-name set for controlled form inputs and any modal/portal SSR quirks, as SUIW does — metadata won't tell us these.
7. Make the generator a headless task (`clj -X` / `bb`), not a REPL `comment`, so it's CI-runnable and pinned to a Mantine version (the README explicitly warns wrappers must be regenerated per upstream version because SUIR uses break-versioning; Mantine will be similar).
