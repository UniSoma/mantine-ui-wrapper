;; The Wrapper plan deep module (ADR 0004).
;;
;; Seam: committed inputs + observed exports  ->  build  ->  plan (pure data)  ->  emit-ns
;;
;;   read-sources  : all I/O (slurp committed EDN/JSON + Node export enumeration)
;;   build         : THE deep module — pure fn of `sources`; ALL domain decisions
;;   emit-ns       : thin — one ns-plan -> {:file :text}; owns escaping + templating only
;;   write-plan!   : thin — the `bb generate` driver (emit-ns + spit + summary)
(ns plan
  (:require [anchor]
            [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [edamame.core :as e]))

;; ---------------------------------------------------------------- shapes
;;
;; sources — the only thing `build` reads. Separates observation from decision.
;; {:docgen          {}          ; parsed codegen/input/docgen.json
;;  :component-docs  {}          ; parsed codegen/input/component-docs.edn
;;  :hook-docs       {}          ; parsed codegen/input/hook-docs.edn
;;  :hook-docs-page  {}          ; parsed codegen/input/hook-docs-page.edn (Companion hooks)
;;  :util-docs       {}          ; parsed codegen/input/util-docs.edn (non-hook barrel utils)
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
;;  :skipped    [{:kind :component|:hook|:barrel :js-name "X" :reason "..."} ...]
;;  :notes      [{:kind :controlled-input-rot :js-name "X"}          ; stale controlled-inputs.edn entry
;;               {:kind :multi-package :js-name "X" :packages [...] :chosen "@mantine/core"} ...]}
;;
;; ns-plan
;; {:pkg      "@mantine/core"                 ; "@mantine/hooks" for the hooks ns
;;  :ns-name  "mantine.core"
;;  :file     "src/main/mantine/core.cljc"
;;  :mantine-version "9.4.1"                  ; stamped into the DO-NOT-EDIT header
;;  :docstring "Mantine @mantine/core 9.4.1 wrappers (generated...)."  ; UN-escaped
;;  :refer-clojure-exclude ["range" ...]      ; sorted, pre-computed
;;  :requires {:cljs [...] :clj [...] :common [...]}  ; fully merged w/ supplement requires
;;  :defs     [def-plan ...]                  ; sorted by :js-name (matches emitter byte-for-byte)
;;  :supplement {:suffix "core" :body "<verbatim, satisfied-declares dropped>"}} ; or nil
;;
;; def-plan
;; {:kind :component            ; :component | :hook | :util (non-hook @mantine/hooks
;;                              ;  barrel utility — raw-passthrough alias, same emit
;;                              ;  shape as :hook; docstring from util-docs.edn)
;;  :js-name "Button"
;;  :symbol  "button"           ; kebab — PRE-COMPUTED (naming is a domain decision)
;;  :docstring "Button — ..."   ; plain human text, UN-escaped; emit-ns escapes
;;  :controlled? false}         ; components only

;; ---------------------------------------------------------------- read-sources (all I/O)

(defn- assert-installed-versions!
  "Reality-check (ADR 0005): every installed node_modules/@mantine/*/package.json
  version must equal the anchor, catching a stray local npm install."
  [mantine-packages version]
  (let [mismatches (for [pkg mantine-packages
                         :let [installed (get (json/parse-string
                                               (slurp (str "node_modules/" pkg "/package.json")))
                                              "version")]
                         :when (not= installed version)]
                     (str pkg " " installed))]
    (when (seq mismatches)
      (throw (ex-info (str "installed @mantine/* versions disagree with the anchor " version ": "
                           (str/join ", " mismatches))
                      {:anchor version :mismatches (vec mismatches)})))))

(defn- package-exports [pkg]
  (-> (shell {:out :string} "node" "-e"
             (str "console.log(JSON.stringify(Object.keys(require('" pkg "'))))"))
      :out
      json/parse-string))

(defn read-sources
  "All I/O. Slurp the committed codegen inputs and enumerate installed @mantine/*
  exports via Node. Returns a `sources` map. The sole live adapter over the
  observation seam — fixtures hand-build the same map with literal :exports."
  []
  (let [mantine-packages (->> (get (json/parse-string (slurp "package.json")) "devDependencies")
                              keys
                              (filter #(str/starts-with? % "@mantine/"))
                              sort)
        _ (assert-installed-versions! mantine-packages (anchor/anchor-version))]
    {:docgen (json/parse-string (slurp "codegen/input/docgen.json"))
     :component-docs (edn/read-string (slurp "codegen/input/component-docs.edn"))
     :hook-docs (edn/read-string (slurp "codegen/input/hook-docs.edn"))
     :hook-docs-page (edn/read-string (slurp "codegen/input/hook-docs-page.edn"))
     :util-docs (edn/read-string (slurp "codegen/input/util-docs.edn"))
     :controlled (edn/read-string (slurp "codegen/controlled-inputs.edn"))
     :scope (edn/read-string (slurp "codegen/scope.edn"))
     :supplements (into {} (for [f (fs/list-dir "codegen/supplements")]
                             [(str/replace (fs/file-name f) #"\.cljc$" "")
                              (slurp (fs/file f))]))
     :exports (into {} (for [pkg mantine-packages] [pkg (package-exports pkg)]))
     :mantine-version (anchor/anchor-version)}))

;; ---------------------------------------------------------------- build helpers (pure)

(defn- kebab [s]
  (str/lower-case (str/replace s #"([a-z0-9])([A-Z])" "$1-$2")))

(def ^:private clojure-core-names
  (set (map str (keys (ns-publics 'clojure.core)))))

(defn- pkg-suffix [pkg] (subs pkg (count "@mantine/")))
(defn- pkg->ns [pkg] (str "mantine." (pkg-suffix pkg)))
(defn- pkg->file [pkg] (str "src/main/mantine/" (pkg-suffix pkg) ".cljc"))

(defn- exports-index
  "{export-name -> [package ...]} over every observed @mantine/* package."
  [exports]
  (reduce (fn [m pkg]
            (reduce (fn [m nm] (update m nm (fnil conj []) pkg)) m (get exports pkg)))
          {}
          (sort (keys exports))))

(defn- dimension-names
  "Resolve a scope dimension to a concrete name seq. Either an explicit name set,
  or {:all true :exclude #{...}} = every name in `universe` minus the excludes."
  [spec universe]
  (if (set? spec)
    spec
    (do (assert (:all spec)
                "scope dimension must be a set or {:all true :exclude #{...}}")
        (remove (:exclude spec #{}) universe))))

;; ---------------------------------------------------------------- docstrings (pure)

(defn- squash-ws [s] (str/trim (str/replace (str s) #"\s+" " ")))

(defn- docs-entry
  "Docs-app entry for a docgen component name: direct key match, else the family
  entry whose :props lists the name (compound sub-components inherit the parent's)."
  [component-docs nm]
  (or (get component-docs nm)
      (some (fn [[k v]]
              (when (and (not= k nm) (some #{nm} (:props v)))
                (assoc v :inherited-from k)))
            component-docs)))

(defn- md-description
  "Sanitize a docgen description for cljdoc's Markdown renderer (ADR 0007).
  Escape-then-convert: FIRST escape any literal backtick already in the raw text
  so it can never open a code span, THEN turn Mantine's <code>…</code> into real,
  balanced backtick code spans. The only active backticks in the output are the
  ones we emit from balanced <code> pairs."
  [d]
  (-> (squash-ws d)
      (str/replace "`" "\\`")
      (str/replace #"<code>(.*?)</code>" "`$1`")))

(defn- prop-lines [props]
  (for [[pname p] (sort-by key props)]
    (str "- **" pname "** `" (squash-ws (get-in p ["type" "name"])) "`"
         (when (get p "required") " **(required)**")
         (when-some [d (get p "defaultValue")] (str " _(default `" (squash-ws d) "`)_"))
         (let [d (get p "description")]
           (when (seq d) (str " — " (md-description d)))))))

(defn- component-docstring [{:keys [docgen component-docs mantine-version]} nm]
  (let [entry (docs-entry component-docs nm)
        props (get-in docgen [nm "props"])]
    (->> (concat
          [(str nm
                (when-let [d (:description entry)]
                  (str " — " d (when-let [p (:inherited-from entry)] (str " (" p " family)")))))]
          (when-let [slug (:slug entry)] ["" (str "https://mantine.dev" slug)])
          (when (:polymorphic entry)
            ["" "Polymorphic: accepts :component / :render-root."])
          (when (seq props)
            (into ["" (str "Props (docgen " mantine-version "):") ""] (prop-lines props)))
          ["" "Optional leading props map; remaining args are children."])
         (str/join "\n"))))

(defn- hook-docstring [{:keys [hook-docs hook-docs-page]} nm]
  (let [slug (get hook-docs-page nm (kebab nm))]
    (->> (concat
          [(str nm (when-let [d (get hook-docs nm)] (str " — " d)))
           ""
           (str "https://mantine.dev/hooks/" slug)
           ""
           "Raw passthrough of the JS hook: pass JS-shaped args (#js {...}); returns"
           "the raw JS value (tuples destructure positionally, object returns are read"
           "via interop — use ^js under :advanced)."])
         (str/join "\n"))))

(defn- util-docstring
  "Docstring for a non-hook barrel utility. Fails the build (drift guard) when the
  utility has no codegen/input/util-docs.edn entry — every enumerated barrel util
  must be documented. The anchored URL is derived from the JS name (lowercased,
  separators stripped) under the entry's :page."
  [{:keys [util-docs]} nm]
  (let [{:keys [desc page]} (or (get util-docs nm)
                                (throw (ex-info (str "barrel utility " nm
                                                     " has no codegen/input/util-docs.edn entry"
                                                     " — document it or exclude it in codegen/scope.edn")
                                                {:js-name nm})))
        anchor (str/lower-case (str/replace nm #"[^A-Za-z0-9]" ""))]
    (->> [(str nm " — " desc)
          ""
          (str "https://mantine.dev/" page "/#" anchor)
          ""
          "Raw passthrough of the JS function: pass JS-shaped args (#js {...}); returns"
          "the raw JS value (read object returns via interop — use ^js under :advanced)."]
         (str/join "\n"))))

;; ---------------------------------------------------------------- supplements (pure parse)

(defn- drop-satisfied-declares
  "Remove top-level (declare ...) lines whose every symbol is among satisfied-names —
  they exist only to make the standalone supplement compile and are redundant once
  hoisted after the generated defs. Collapses the blank line left behind."
  [lines body-start-row body-forms satisfied-names]
  (let [rows-to-drop (set (mapcat (fn [form]
                                    (let [{:keys [row end-row]} (meta form)]
                                      (when (and (seq? form)
                                                 (= 'declare (first form))
                                                 (every? #(satisfied-names (str %)) (rest form)))
                                        (range row (inc end-row)))))
                                  body-forms))]
    (->> (map-indexed (fn [i line] [(+ body-start-row i) line]) lines)
         (remove (fn [[row line]]
                   (or (rows-to-drop row)
                       ;; also the blank line a dropped form leaves behind
                       (and (str/blank? line) (rows-to-drop (dec row))))))
         (map second))))

(defn- parse-supplement
  "Parse the verbatim text of codegen/supplements/<suffix>.cljc. Returns
  {:cljs-requires [...] :clj-requires [...] :requires [...] :def-names [...]
   :body \"<verbatim text>\"}.
  Declares fully satisfied by `satisfied-names` are dropped from the body."
  [suffix text satisfied-names]
  (let [path (str "codegen/supplements/" suffix ".cljc")
        forms (e/parse-string-all text {:read-cond :preserve :all true})
        [ns-form & body-forms] forms
        _ (assert (and (seq? ns-form) (= 'ns (first ns-form)))
                  (str path ": first form must be the ns form"))
        require-clause (some #(when (and (seq? %) (= :require (first %))) %) ns-form)
        {:keys [cljs clj unconditional]}
        (reduce (fn [acc entry]
                  (if (reader-conditional? entry)
                    (let [{:keys [form splicing?]} entry
                          lang (first form)
                          _ (assert (#{:cljs :clj} lang)
                                    (str path ": only single-branch :cljs/:clj reader conditionals are supported in :require"))
                          entries (if splicing? (second form) [(second form)])]
                      (update acc lang into entries))
                    (update acc :unconditional conj entry)))
                {:cljs [] :clj [] :unconditional []}
                (rest require-clause))
        def-names (keep (fn [form]
                          (when (and (seq? form) ('#{def defn defn-} (first form)))
                            (str (second form))))
                        body-forms)
        ;; body = verbatim text from the first form after the ns form
        body-start-row (:row (meta (first body-forms)))
        body (when body-start-row
               (-> (drop (dec body-start-row) (str/split-lines text))
                   (drop-satisfied-declares body-start-row body-forms satisfied-names)
                   (->> (str/join "\n"))))]
    {:cljs-requires cljs
     :clj-requires clj
     :requires unconditional
     :def-names (vec def-names)
     :body body}))

;; ---------------------------------------------------------------- require merging (pure)

(defn- require-key [entry] (if (vector? entry) (first entry) entry))

(defn- merge-require-entry [a b]
  (let [a (if (vector? a) a [a])
        b (if (vector? b) b [b])
        opts-a (apply hash-map (rest a))
        opts-b (apply hash-map (rest b))
        refers (vec (distinct (concat (:refer opts-a) (:refer opts-b))))
        merged (cond-> (merge opts-a opts-b)
                 (seq refers) (assoc :refer refers))]
    (into [(first a)] (mapcat identity (sort-by key merged)))))

(defn- merge-requires [base extra]
  (reduce (fn [acc entry]
            (if-let [i (first (keep-indexed
                               (fn [i e] (when (= (require-key e) (require-key entry)) i))
                               acc))]
              (update acc i merge-require-entry entry)
              (conj acc entry)))
          (vec base)
          extra))

;; ---------------------------------------------------------------- classification (pure)

(defn- check-collisions! [ns-name names]
  (doseq [[kb group] (group-by kebab names)]
    (when (> (count group) 1)
      (throw (ex-info (str ns-name ": kebab collision " kb " <- " (str/join ", " group))
                      {:ns ns-name :kebab kb :names group})))))

(defn- resolve-component
  "Resolve a scoped component name -> its package, via docgen presence + observed
  package exports. Returns {:resolved [nm pkg]} (+ optional :note) or {:skipped ...}."
  [docgen idx nm]
  (let [pkgs (remove #{"@mantine/hooks"} (get idx nm []))]
    (cond
      (nil? (get docgen nm))
      {:skipped {:kind :component :js-name nm :reason "not present in docgen.json"}}

      (empty? pkgs)
      {:skipped {:kind :component :js-name nm :reason "no installed @mantine package exports it"}}

      :else
      (let [pkg (if (some #{"@mantine/core"} pkgs) "@mantine/core" (first (sort pkgs)))]
        (cond-> {:resolved [nm pkg]}
          (> (count pkgs) 1)
          (assoc :note {:kind :multi-package :js-name nm :packages (vec pkgs) :chosen pkg}))))))

;; ---------------------------------------------------------------- ns-plans (pure)

(defn- package-ns-plan
  "One ns-plan for `pkg` with its resolved component names (+ optional supplement).
  `component-names` may be empty (supplement-only package: zero defs, the ns body
  is entirely its hoisted supplement)."
  [{:keys [controlled supplements mantine-version] :as sources} pkg component-names]
  (let [ns-name (pkg->ns pkg)
        suffix (pkg-suffix pkg)
        kebabs (map kebab component-names)
        supplement (when-let [text (get supplements suffix)]
                     (parse-supplement suffix text (set kebabs)))
        _ (check-collisions! ns-name component-names)
        _ (when-let [clash (seq (filter (set kebabs) (:def-names supplement)))]
            (throw (ex-info (str ns-name ": supplement def(s) " (str/join ", " clash)
                                 " collide with generated defs — docgen now covers this; delete the supplement entry.")
                            {:ns ns-name :collisions (vec clash)})))
        def-names (concat kebabs (:def-names supplement))]
    {:pkg pkg
     :ns-name ns-name
     :file (pkg->file pkg)
     :mantine-version mantine-version
     :docstring (str "Mantine " pkg " " mantine-version " wrappers (generated"
                     (when supplement ", supplement hoisted from codegen/supplements/")
                     ").")
     :refer-clojure-exclude (vec (sort (filter clojure-core-names def-names)))
     :requires {:cljs (merge-requires
                       (if (seq component-names)
                         [(into [pkg :refer (mapv symbol (sort component-names))])]
                         [])
                       (:cljs-requires supplement))
                :clj (:clj-requires supplement)
                ;; component defs use f on both branches; a supplement-only ns
                ;; declares its own requires (factory :clj-only if that's where it's used)
                :common (merge-requires
                         (if (seq component-names) '[[mantine.impl.factory :as f]] [])
                         (:requires supplement))}
     :defs (vec (for [nm (sort component-names)]
                  {:kind :component
                   :js-name nm
                   :symbol (kebab nm)
                   :docstring (component-docstring sources nm)
                   :controlled? (contains? controlled nm)}))
     :supplement (when supplement {:suffix suffix :body (:body supplement)})}))

(defn- hooks-ns-plan
  "Plan for the mantine.hooks ns. `barrel-names` mixes use* hooks and non-hook
  plain utilities (randomId, clamp, mergeRefs, ...); each def is routed by name —
  use* -> :hook (hook-docstring), else -> :util (util-docstring, drift-guarded)."
  [{:keys [mantine-version] :as sources} barrel-names]
  (let [ns-name "mantine.hooks"
        _ (check-collisions! ns-name barrel-names)
        names (sort barrel-names)
        ;; a single-word util kebabs to its own JS name (clamp, range): :refer-ing it
        ;; while def-ing the same symbol would shadow the refer, so reach those via a
        ;; module alias (hooks-js/clamp) and keep them out of the :refer list.
        alias-sym 'hooks-js
        self-ref? (fn [nm] (= (kebab nm) nm))
        referred (remove self-ref? names)
        aliased? (boolean (some self-ref? names))]
    {:pkg "@mantine/hooks"
     :ns-name ns-name
     :file "src/main/mantine/hooks.cljc"
     :mantine-version mantine-version
     :docstring (str "Mantine @mantine/hooks " mantine-version " wrappers (generated).\n\n"
                     "Thin def-aliases over the JS hooks and plain utilities — raw passthrough\n"
                     "in both directions. Rules of hooks are NOT enforced here; they are\n"
                     "delegated to React (call hooks only inside function components / other\n"
                     "hooks). Object returns need ^js or js-interop access under :advanced\n"
                     "compilation.")
     :refer-clojure-exclude (vec (sort (filter clojure-core-names (map kebab barrel-names))))
     :requires {:cljs [(into (if aliased?
                               ["@mantine/hooks" :as alias-sym]
                               ["@mantine/hooks"])
                             [:refer (mapv symbol referred)])]
                ;; defs are bare aliases on :cljs; f is only used by
                ;; the :clj not-implemented branch
                :clj '[[mantine.impl.factory :as f]]}
     :defs (vec (for [nm names]
                  (let [hook? (str/starts-with? nm "use")]
                    (cond-> {:kind (if hook? :hook :util)
                             :js-name nm
                             :symbol (kebab nm)
                             :docstring ((if hook? hook-docstring util-docstring) sources nm)}
                      (self-ref? nm) (assoc :cljs-ref (str alias-sym "/" nm))))))
     :supplement nil}))

;; ---------------------------------------------------------------- build

(defn build
  "THE deep module. Pure fn of `sources` -> `plan`. Owns every domain decision:
  scope resolution, package assignment (core-precedence + multi-package note),
  component/hook/supplement-only classification, kebab naming + refer-clojure
  excludes, docstring composition (incl. Companion-hook page mapping), supplement
  parsing + declare-dropping, and require merging.

  Collision guard throws ex-info (kebab collision within a namespace; a supplement
  def colliding with a generated def). Unresolvable names -> :skipped data;
  controlled-input rot -> :notes data. Never shells out; never writes.

  The compound-part Drift audit is intentionally NOT here — it needs component
  statics the emitter never uses; it stays in the observation/Coverage layer."
  [{:keys [docgen scope controlled exports] :as sources}]
  (let [idx (exports-index exports)
        ;; every docgen entry exported by a wrapped @mantine/* package other than
        ;; @mantine/hooks — the universe the :components dimension draws from when {:all true}
        component-universe (filter (fn [nm]
                                     (and (get docgen nm)
                                          (some #(not= % "@mantine/hooks") (get idx nm []))))
                                   (keys idx))
        resolutions (map #(resolve-component docgen idx %)
                         (sort (dimension-names (:components scope) component-universe)))
        resolved (keep :resolved resolutions)
        by-pkg (group-by second resolved)
        ;; the barrel is enumerated as everything-minus-excludes: every export
        ;; (use* hooks AND non-hook utilities) flows through hooks-ns-plan, which
        ;; routes each by name. Undocumented exports must be excluded in scope.edn.
        hook-exports (set (get exports "@mantine/hooks"))
        barrel-names (sort (dimension-names (:hooks scope) hook-exports))
        barrel (filter hook-exports barrel-names)
        barrel-skips (for [nm barrel-names
                           :when (not (hook-exports nm))]
                       {:kind :barrel :js-name nm :reason "not an export of @mantine/hooks"})
        rot (for [nm (sort controlled)
                  :when (not-any? #(= nm (first %)) resolved)]
              {:kind :controlled-input-rot :js-name nm})
        namespaces (-> (vec (for [[pkg entries] (sort-by key by-pkg)]
                              (package-ns-plan sources pkg (mapv first entries))))
                       ;; declared supplement-only packages: no docgen components, ns body
                       ;; is entirely the hoisted supplement.
                       (into (for [pkg (sort (:supplement-only-packages scope #{}))]
                               (package-ns-plan sources pkg [])))
                       (conj (hooks-ns-plan sources barrel)))]
    {:mantine-version (:mantine-version sources)
     :namespaces (vec (sort-by :ns-name namespaces))
     :skipped (vec (concat (keep :skipped resolutions) barrel-skips))
     :notes (vec (concat (keep :note resolutions) rot))}))

;; ---------------------------------------------------------------- emit-ns (thin)

(defn- esc
  "Escape a docstring for emission as a (multi-line) string literal."
  [s]
  (-> s (str/replace "\\" "\\\\") (str/replace "\"" "\\\"")))

(defn- emit-ns-form [{:keys [ns-name docstring refer-clojure-exclude requires]}]
  (let [{:keys [cljs clj common]} requires
        require-lines
        (concat
         [(str "#?@(:cljs [" (str/join "\n              " (map pr-str cljs)) "])")]
         (when (seq clj)
           [(str "#?@(:clj [" (str/join "\n             " (map pr-str clj)) "])")])
         (map pr-str common))]
    (str "(ns " ns-name "\n"
         "  \"" (esc docstring) "\"\n"
         (when (seq refer-clojure-exclude)
           (str "  (:refer-clojure :exclude [" (str/join " " refer-clojure-exclude) "])\n"))
         "  (:require\n   "
         (str/join "\n   " require-lines)
         "))\n")))

(defn- emit-def [ns-name {:keys [kind js-name docstring controlled? cljs-ref] sym :symbol}]
  (case kind
    :component
    (str "(def " sym "\n"
         "  \"" (esc docstring) "\"\n"
         "  #?(:cljs (f/factory " (if controlled? (str "(f/controlled " js-name ")") js-name) ")\n"
         "     :clj (f/not-implemented \"" ns-name "/" sym "\")))\n")

    ;; hooks and plain utilities share the raw-passthrough alias shape; a
    ;; single-word util reaches its JS export via :cljs-ref (hooks-js/clamp) to
    ;; avoid shadowing its own :refer.
    (:hook :util)
    (str "(def " sym "\n"
         "  \"" (esc docstring) "\"\n"
         "  #?(:cljs " (or cljs-ref js-name) "\n"
         "     :clj (f/not-implemented \"" ns-name "/" sym "\")))\n")))

(defn- header [mantine-version]
  (str ";; GENERATED by `bb generate` from the committed codegen inputs (Mantine "
       mantine-version ") — DO NOT EDIT.\n"))

(defn emit-ns
  "Thin. One ns-plan -> {:file <path> :text <cljc source>}. Pure templating +
  escaping over pre-computed fields — no kebab, no docstring lookup, no
  controlled-inputs, no merge-requires, no derive-exclude. Exactly two text rules:
  escape docstrings here; splice the supplement body VERBATIM (it is already valid
  .cljc — re-escaping would corrupt it)."
  [{:keys [ns-name file mantine-version defs supplement] :as ns-plan}]
  {:file file
   :text (str (header mantine-version)
              (emit-ns-form ns-plan)
              "\n"
              (str/join "\n" (map #(emit-def ns-name %) defs))
              (when-let [body (:body supplement)]
                (str "\n;; ---- hoisted from codegen/supplements/" (:suffix supplement) ".cljc ----\n\n"
                     body)))})

;; ---------------------------------------------------------------- write-plan! (thin)

(defn write-plan!
  "Thin. The `bb generate` driver: emit-ns every namespace, create dirs, spit,
  then print the summary plus :skipped / :notes. Returns a summary map."
  [{:keys [namespaces skipped notes]}]
  (doseq [{:keys [kind js-name reason]} skipped]
    (println "SKIP" (name kind) js-name "—" reason))
  (doseq [{:keys [kind js-name] :as note} notes]
    (case kind
      :multi-package
      (println "WARN component" js-name "exported by" (str/join ", " (:packages note))
               "— using" (:chosen note))
      :controlled-input-rot
      (println "NOTE controlled-input" js-name "not among resolved components (out of current scope)")))
  (doseq [ns-plan namespaces
          :let [{:keys [file text]} (emit-ns ns-plan)]]
    (fs/create-dirs (fs/parent file))
    (spit file text)
    (println "WROTE" file (str "(" (count (str/split-lines text)) " lines)")))
  (let [all-defs (mapcat :defs namespaces)
        n-components (count (filter #(= :component (:kind %)) all-defs))
        n-hooks (count (filter #(= :hook (:kind %)) all-defs))]
    (println "Generated" (count namespaces) "namespaces from" n-components "components +" n-hooks "hooks.")
    {:namespaces (count namespaces)
     :components n-components
     :hooks n-hooks
     :skipped (count skipped)
     :notes (count notes)}))
