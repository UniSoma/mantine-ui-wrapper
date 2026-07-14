> Prototype for ticket mnt-01kxe8gzvp9e (Decide: build tooling, npm interop & CSS delivery)

# Build-tooling / npm-interop / CSS-delivery — verified skeleton

A throwaway shadow-cljs project (built in scratchpad, **not** committed) that renders one
Mantine component to prove the locked decisions before the destination PoC grows the real
build. Verified against **Mantine 9.4.1**, shadow-cljs 2.28.23, React 19, Java 25, node 24.

## The skeleton that rendered

`package.json` — `@mantine/*` **not** listed here (see auto-install finding); only tooling:
```json
{ "devDependencies": { "react": "^19", "react-dom": "^19", "shadow-cljs": "^2.28" } }
```

`src/deps.cljs` — the consumer-facing npm-dep declaration shipped in the Clojars jar:
```clojure
{:npm-deps {"@mantine/core" "^9.4.1"}}   ; react/react-dom NOT shipped — app owns React
```

`shadow-cljs.edn`:
```clojure
{:source-paths ["src" "demo"]
 :dev-http {8090 {:root "public"}}
 :builds {:demo {:target :browser
                 :output-dir "public/js" :asset-path "/js"
                 :modules {:main {:init-fn mantine.demo/init}}}}}
```

`demo/mantine/demo.cljs` — the locked require-sugar convention:
```clojure
(ns mantine.demo
  (:require ["react" :as react]                          ; grab-bag ns -> :as
            ["react-dom/client" :as rdom]
            ["@mantine/core" :refer [MantineProvider Button]]))  ; named exports -> :refer
(defn app [] (react/createElement MantineProvider #js {}
               (react/createElement Button #js {} "Hello Mantine")))
(defn init [] (.render (rdom/createRoot (js/document.getElementById "app"))
                       (react/createElement app)))
```

`public/index.html` — CSS `<link>`ed straight from `node_modules` (core first, plain non-layer):
```html
<link rel="stylesheet" href="/node_modules/@mantine/core/styles.css">
```
(`public/node_modules` symlinked to the project `node_modules` so `:dev-http` serves it.)

## Verification results

- **`deps.cljs` auto-install CONFIRMED.** `@mantine/core` was absent from `package.json` and
  `node_modules`; `shadow-cljs release` ran `npm install --save --save-exact @mantine/core@^9.4.1`
  on its own before compiling.
- **`:advanced` release CLEAN.** `793 compiled, 0 warnings` — `:refer`-Mantine + `:as`-react +
  `createElement` survive advanced optimization.
- **DOM render CONFIRMED (jsdom, executing the real `:advanced` bundle).** React mounted a
  `<button class="mantine-Button-root m_87cf2631 mantine-UnstyledButton-root m_77c9d27d">Hello
  Mantine</button>`; MantineProvider injected its `<style data-mantine-styles>` blocks. Those
  hashed `.m_…` classes are exactly what the linked `styles.css` (2193 `--mantine-` vars) targets.
- **Pixel paint NOT verified here** — headless Chromium can't launch in this sandbox (missing
  `libatk-1.0.so.0` system lib). Left to the destination PoC in a real browser.

## Corrections this prototype forced

1. **No tree-shaking (corrects the codegen decision's rationale).** The `:advanced` bundle is
   ~1 MB — the *entire* `@mantine/core` barrel. `:refer` vs `:as` is bundle-neutral; shadow-cljs
   pulls the whole package regardless (thheller #412/#21). The wrapper exposing the full surface
   makes this an accepted cost, but the "tree-shakeable under `:advanced`" claim was false.
2. **Auto-install persists, exact + into `dependencies`.** shadow-cljs uses `--save --save-exact`,
   so it writes the resolved version as an **exact pin into `dependencies`** (not the `deps.cljs`
   caret range, not `devDependencies`). Consequence for our repo: pre-declare `@mantine/*` in our
   own `devDependencies` (exact `9.4.1`) so the auto-install never fires for us and never pollutes
   `dependencies`. For consumers this is benign (reproducible, gap-filled — their version wins).
