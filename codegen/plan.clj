;; INTERFACE STUB — the Wrapper plan deep module (ADR 0004). Not yet wired into
;; `bb generate`; this file documents the seam and shapes so the implementation and
;; its fixtures can be built against a settled interface. Bodies are placeholders.
;;
;; Seam: committed inputs + observed exports  ->  build  ->  plan (pure data)  ->  emit-ns
;;
;;   read-sources  : all I/O (slurp committed EDN/JSON + Node export enumeration)
;;   build         : THE deep module — pure fn of `sources`; ALL domain decisions
;;   emit-ns       : thin — one ns-plan -> {:file :text}; owns escaping + templating only
;;   write-plan!   : thin — the `bb generate` driver (emit-ns + spit + summary)
(ns plan)

;; ---------------------------------------------------------------- shapes
;;
;; sources — the only thing `build` reads. Separates observation from decision.
;; {:docgen          {}          ; parsed codegen/input/docgen.json
;;  :component-docs  {}          ; parsed codegen/input/component-docs.edn
;;  :hook-docs       {}          ; parsed codegen/input/hook-docs.edn
;;  :hook-docs-page  {}          ; parsed codegen/input/hook-docs-page.edn (Companion hooks)
;;  :util-docs       {}          ; parsed codegen/input/util-docs.edn (Barrel utilities)
;;  :controlled      #{}         ; parsed codegen/controlled-inputs.edn
;;  :scope           {:components ... :hooks ... :supplement-only-packages ...}
;;  :supplements     {"core" "<verbatim .cljc text>" ...}  ; suffix -> file text (unparsed)
;;  :exports         {"@mantine/core" ["Button" ...]       ; OBSERVED (Node) — injected fact
;;                    "@mantine/hooks" ["useDisclosure" ...]}
;;  :mantine-version "9.4.1"}
;;
;; plan — the domain artifact. Entirely data: no text, no I/O, JVM-serializable.
;; {:mantine-version "9.4.1"
;;  :namespaces [ns-plan ...]    ; sorted by :ns-name
;;  :skipped    [{:kind :component|:hook|:util :js-name "X" :reason "..."} ...]
;;  :notes      [{:kind :controlled-input-rot :js-name "X"}          ; stale controlled-inputs.edn entry
;;               {:kind :multi-package :js-name "X" :packages [...] :chosen "@mantine/core"} ...]}
;;
;; ns-plan
;; {:pkg      "@mantine/core"                 ; "@mantine/hooks" for the hooks ns
;;  :ns-name  "mantine.core"
;;  :file     "src/main/mantine/core.cljc"
;;  :docstring "Mantine @mantine/core 9.4.1 wrappers (generated...)."  ; UN-escaped
;;  :refer-clojure-exclude ["range" ...]      ; sorted, pre-computed
;;  :requires {:cljs [...] :clj [...] :common [...]}  ; fully merged w/ supplement requires
;;  :defs     [def-plan ...]                  ; sorted by :js-name (matches emitter byte-for-byte)
;;  :supplement {:suffix "core" :body "<verbatim, satisfied-declares dropped>"}} ; or nil
;;
;; def-plan
;; {:kind :component            ; :component | :hook | :util
;;  :js-name "Button"
;;  :symbol  "button"           ; kebab — PRE-COMPUTED (naming is a domain decision)
;;  :docstring "Button — ..."   ; plain human text, UN-escaped; emit-ns escapes
;;  :controlled? false}         ; components only

;; ---------------------------------------------------------------- interface

(defn read-sources
  "All I/O. Slurp the committed codegen inputs and enumerate installed @mantine/*
  exports via Node. Returns a `sources` map. The sole live adapter over the
  observation seam — fixtures hand-build the same map with literal :exports."
  []
  (throw (ex-info "stub" {})))

(defn build
  "THE deep module. Pure fn of `sources` -> `plan`. Owns every domain decision:
  scope resolution, package assignment (core-precedence + multi-package warn),
  component/hook/util/supplement-only classification, kebab naming + refer-clojure
  excludes, docstring composition (incl. Companion-hook page mapping), supplement
  parsing + declare-dropping, and require merging.

  Collision guard throws ex-info (kebab collision within a namespace; a supplement
  def colliding with a generated def). Unresolvable names -> :skipped data;
  controlled-input rot -> :notes data. Never shells out; never writes.

  The compound-part Drift audit is intentionally NOT here — it needs component
  statics the emitter never uses; it stays in the observation/Coverage layer."
  [_sources]
  (throw (ex-info "stub" {})))

(defn emit-ns
  "Thin. One ns-plan -> {:file <path> :text <cljc source>}. Pure templating +
  escaping over pre-computed fields — no kebab, no docstring lookup, no
  controlled-inputs, no merge-requires, no derive-exclude. Escapes docstrings here."
  [_ns-plan]
  (throw (ex-info "stub" {})))

(defn write-plan!
  "Thin. The `bb generate` driver: emit-ns every namespace, create dirs, spit,
  then print the summary plus :skipped / :notes. Returns a summary map."
  [_plan]
  (throw (ex-info "stub" {})))

;; ---------------------------------------------------------------- usage (design intent)
;;
;; generate (common path — two lines of real work):
;;   (write-plan! (build (read-sources)))
;;
;; drift (same build, no writes — diff emitted text vs committed):
;;   (let [plan (build (read-sources))]
;;     (doseq [{:keys [file] :as np} (:namespaces plan)]
;;       (assert (= (slurp file) (:text (emit-ns np))) (str "DRIFT " file))))
;;
;; Coverage audit — shares OBSERVATION only, never `build`'s resolution, so the
;; guard stays an independent recount (see ADR 0004; do NOT DRY through the plan):
;;   (let [{:keys [docgen exports scope]} (read-sources)]
;;     (audit-coverage docgen exports scope))
