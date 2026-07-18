# Mantine UI Wrapper

The ubiquitous language for a code-generation pipeline that wraps the `@mantine/*`
React component libraries as ClojureScript namespaces for use from Fulcro.

## Language

**Wrapper**:
A generated `.cljc` namespace exposing one `@mantine/*` package's surface as
ClojureScript factory functions. The whole project produces wrappers.

**Mantine anchor**:
The single Mantine release the whole wrapper is built against — one version, held
canonically as the exact `@mantine/*` pins in `package.json`. Every other place the
version appears is a *rendering* of the anchor in that place's own form: the exact
pin, the `^`-prefixed consumer *floor* shipped in `deps.cljs`, the `9.4.1` prefix of
the `9.4.1.N` artifact, and the literal stamped into every generated banner. Renderings
must agree with the anchor; the installed version in `node_modules` must equal it too.
_Avoid_: Mantine version (ambiguous — pin, floor, and artifact prefix are distinct renderings).

**Factory**:
A thin `React.createElement` closure over a Mantine component, with clj→js prop
conversion and Fulcro-style child handling. The unit a wrapper is made of.
_Avoid_: component fn, element fn.

**Docgen-driven**:
The property that a wrapper's contents come from Mantine's `docgen.json`. The
pipeline generates exactly what docgen describes — and nothing docgen omits.

**Coverage**:
The set of a package's real exports that the wrapper actually exposes. Gaps in
coverage are omissions, not decisions — docgen simply didn't describe them.

**Compound part**:
A dot-notation subcomponent (`Menu.Dropdown`, `AppShell.Main`) that lives as a
static property on its parent component object rather than a top-level export.
docgen describes these inconsistently, so some are covered and sibling parts are not.
_Avoid_: subcomponent, dotted component, compound subcomponent.

**Companion hook**:
A `use*` export documented on a *sibling* hook's docs page rather than its own
(`useFullscreenElement` on the use-fullscreen page, `useSessionStorage` on
use-local-storage). It carries no standalone description, so its docstring is
mapped to that shared page via `codegen/input/hook-docs-page.edn` instead of the
default per-hook slug. The hooks analogue of a compound part.
_Avoid_: alias hook, sub-hook.

**Barrel utility**:
A non-hook plain function exported from the `@mantine/hooks` barrel (`randomId`,
`mergeRefs`, `getHotkeyHandler`, the `*Mask` family, `read*StorageValue`, …),
wrapped as a raw-passthrough def-alias in `mantine.hooks` alongside the hooks.
docgen never describes it and it carries no JSDoc, so its docstring is hand-sourced
from `codegen/input/util-docs.edn` — either the functions-reference guide or a
sibling hook's docs page. The plain-function analogue of a Companion hook.
_Avoid_: helper, util fn.

**Supplement**:
A hand-written `codegen/supplements/<pkg>.cljc` whose forms the generator hoists,
verbatim, into the end of the matching generated namespace. The one path for
surface that docgen cannot describe (providers, polymorphic primitives, plain
functions, package-local hooks, and missing compound parts).
_Avoid_: override, patch, addon.

**Backfill**:
Covering a docgen omission by hand-writing it in a supplement — as opposed to
generating it. The deliberate answer to incomplete docgen.

**Collision guard**:
A generate-time assertion that fails the build when a supplement def name matches
a name docgen now generates — turning a silent redefinition into a loud "docgen
now covers this, delete the supplement entry."

**Drift audit**:
A generate-time assertion that every real compound part of every wrapped component
is covered by either docgen or a supplement — turning a silently-unwrapped part
into a loud "wrap it or explicitly exclude it."

**Corpus collision guard**:
An extract-time assertion that no PascalCase component key appears in more than one
component corpus (`:core` / `:dates` / `:charts` / `:others`) — turning the old silent
last-wins `apply merge` over the corpora into a loud throw. Component inputs are kept
corpus-keyed (not flattened) precisely so this guard can fire; a future review must not
collapse the map back into a silent merge.
