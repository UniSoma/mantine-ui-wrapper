# Mantine UI Wrapper

The ubiquitous language for a code-generation pipeline that wraps the `@mantine/*`
React component libraries as ClojureScript namespaces for use from Fulcro.

## Language

**Wrapper**:
A generated `.cljc` namespace exposing one `@mantine/*` package's surface as
ClojureScript factory functions. The whole project produces wrappers.

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
