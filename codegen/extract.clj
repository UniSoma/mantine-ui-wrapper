;; Input refresh for an anchor bump (ADR 0004 pattern): the fragile upstream-MDX
;; parsers live in a pure, requirable core (parse-inputs) with a thin I/O driver.
;;
;;   parse-inputs   pure: docs-data texts keyed by file name -> {:hook-docs :component-docs};
;;                  owns which file is parsed or ignored, the unknown- and missing-file
;;                  throws, parsing, the component corpus merge, the corpus collision
;;                  guard, and sorted-map determinism. The test surface.
;;   write-inputs!  thin: slurp every mdx-*-data.ts, assert clone == anchor, copy
;;                  docgen.json, call parse-inputs, spit the two EDN maps + witness.
;;   -main          reads the clone-dir arg (usage-throw if missing).
;;
;; Run after the clone has run docgen:
;;
;;   git clone --depth 1 --branch <anchor> https://github.com/mantinedev/mantine <dir>
;;   cd <dir> && npx corepack yarn install && npx corepack yarn tsx scripts/docgen
;;   bb extract <dir>            (or `bb clone-anchor`, which does all of the above)
;;
;; Writes (all committed):
;;   codegen/input/docgen.json          verbatim copy of the docgen output
;;   codegen/input/hook-docs.edn        {"useX" "description"} from mdx-hooks-data.ts
;;   codegen/input/component-docs.edn   {"Button" {:description ... :slug ... :polymorphic ...}}
;;                                      (extension packages are keyed by docs-entry name;
;;                                       join via :props, which lists the docgen keys)
;;   codegen/input/mantine-version.edn  {:mantine-version ...} provenance witness (ADR 0005)
(ns extract
  (:require [anchor]
            [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.string :as str]))

;; mdx-hooks-data.ts builds most entries with hDocs('useX', 'description') calls; a
;; few are inline objects instead.
(defn extract-hook-docs [text]
  (let [hdocs (re-seq #"(?s)(use\w+): hDocs\(\s*'use\w+',\s*'([^']*)'\s*\)" text)
        ;; inline-object entries (e.g. useElementSize); none nests braces
        inline (for [[_ nm block] (re-seq #"(?s)(use\w+): \{([^{}]*)\}" text)
                     :let [d (second (re-find #"description: '([^']*)'" block))]
                     :when d]
                 [nm d])]
    (into (sorted-map)
          (concat (for [[_ nm d] hdocs]
                    [nm (str/replace d #"\s+" " ")])
                  inline))))

;; The component data files are inline object literals. A regex parses them because
;; no entry nests braces more than one level deep.

(defn extract-component-docs [text]
  (into (sorted-map)
        (for [[_ nm block] (re-seq #"(?s)(\w+): \{([^{}]*(?:\{[^{}]*\}[^{}]*)*)\}" text)
              :let [field (fn [k] (second (re-find (re-pattern (str k ": '([^']*)'")) block)))
                    description (field "description")
                    package (second (re-find #"package: '(@mantine/[^']+)'" block))
                    props (some->> (re-find #"(?s)props: \[([^\]]*)\]" block)
                                   second
                                   (re-seq #"'([^']+)'")
                                   (mapv second))]
              ;; landing pages (CorePackage, GettingStarted*) carry no package/props
              :when (and package (seq props))]
          [nm (cond-> {:description description
                       :package package
                       :slug (field "slug")
                       :props props}
                (re-find #"polymorphic: true" block) (assoc :polymorphic true))])))

(def ^:private component-corpora
  "Logical corpus name -> the MDX data file it is parsed from."
  {:core "mdx-core-data.ts"
   :dates "mdx-dates-data.ts"
   :charts "mdx-charts-data.ts"
   :schedule "mdx-schedule-data.ts"
   :others "mdx-others-data.ts"})

(def ^:private hooks-file "mdx-hooks-data.ts")

(def ^:private ignored-files
  "Docs-data files that carry no wrapped component or hook docs."
  #{"mdx-guides-data.ts"
    "mdx-meta-data.ts"
    "mdx-styles-data.ts"
    "mdx-theming-data.ts"
    "mdx-form-data.ts"
    "mdx-code-highlight-data.ts"})

(defn parse-inputs
  "Pure. Parse the raw MDX texts, {\"mdx-core-data.ts\" \"...\" ...} keyed by file name,
  into {:hook-docs {\"useX\" \"...\"} :component-docs {\"Button\" {...}}}. Throws
  ex-info when a file is neither parsed nor in ignored-files, or a parsed file is
  absent, so a docs move upstream cannot silently drop entries. Unions the
  per-corpus component maps; throws ex-info when a PascalCase key appears in more than
  one corpus instead of letting the merge keep the last one (ADR 0004: a wrong or
  ambiguous artifact throws). Output maps are sorted, so the result is deterministic."
  [texts]
  (let [parsed-files (conj (set (vals component-corpora)) hooks-file)
        unknown (sort (remove (into parsed-files ignored-files) (keys texts)))]
    (when (seq unknown)
      (throw (ex-info (str "unknown docs-data file(s): " (str/join ", " unknown)
                           " — add each to component-corpora or ignored-files in codegen/extract.clj.")
                      {:unknown (vec unknown)})))
    (let [missing (sort (remove (set (keys texts)) parsed-files))]
      (when (seq missing)
        (throw (ex-info (str "missing docs-data file(s): " (str/join ", " missing)
                             " — the clone no longer has them; find where their entries moved and"
                             " update component-corpora or hooks-file in codegen/extract.clj.")
                        {:missing (vec missing)})))))
  (let [per-corpus (into {} (for [[corpus f] component-corpora]
                              [corpus (extract-component-docs (get texts f))]))
        collisions (->> (for [[corpus m] per-corpus, k (keys m)] [k corpus])
                        (group-by first)
                        (filter (fn [[_ pairs]] (> (count pairs) 1)))
                        (into (sorted-map)))]
    (when (seq collisions)
      (throw (ex-info (str "component corpus collision — PascalCase key(s) appear in more than one "
                           "corpus: "
                           (str/join "; " (for [[k pairs] collisions]
                                            (str k " in " (str/join ", " (sort (map second pairs)))))))
                      {:collisions (into {} (for [[k pairs] collisions]
                                              [k (vec (sort (map second pairs)))]))})))
    {:hook-docs (extract-hook-docs (get texts hooks-file))
     :component-docs (into (sorted-map) (apply merge (vals per-corpus)))}))

(defn- clone-mantine-version
  "The Mantine version of the fed clone: @mantine/core's package.json version, falling
  back to the clone-dir root package.json."
  [clone-dir]
  (let [core-pkg (fs/path clone-dir "packages/@mantine/core/package.json")
        pkg (if (fs/exists? core-pkg) core-pkg (fs/path clone-dir "package.json"))]
    (get (json/parse-string (slurp (str pkg))) "version")))

(defn write-inputs!
  "Thin driver: slurp the clone texts, assert the clone == the anchor, copy docgen.json,
  call parse-inputs, and spit the two EDN maps + the version witness with their banners."
  [clone-dir]
  (let [mdx-dir (fs/path clone-dir "apps/mantine.dev/src/mdx/data")
        out (fs/path "codegen" "input")
        anchor-v (anchor/anchor-version)
        clone-v (clone-mantine-version clone-dir)]
    (when (not= clone-v anchor-v)
      (throw (ex-info (str "clone Mantine version " clone-v " != anchor " anchor-v
                           " — extract the clone pinned to the anchor, or bump package.json first.")
                      {:clone clone-v :anchor anchor-v})))
    (let [{:keys [hook-docs component-docs]}
          (parse-inputs (into {} (for [f (fs/glob mdx-dir "mdx-*-data.ts")]
                                   [(str (fs/file-name f)) (slurp (str f))])))]
      (fs/create-dirs out)
      (fs/copy (fs/path clone-dir "apps/mantine.dev/src/.docgen/docgen.json")
               (fs/path out "docgen.json")
               {:replace-existing true})
      (spit (str (fs/path out "hook-docs.edn"))
            (with-out-str
              (println (str ";; GENERATED by `bb extract` from mdx-hooks-data.ts (Mantine " anchor-v ") — do not edit."))
              (prn hook-docs)))
      (println "hook-docs.edn:" (count hook-docs) "hooks")
      (spit (str (fs/path out "component-docs.edn"))
            (with-out-str
              (println (str ";; GENERATED by `bb extract` from "
                            (str/join ", " (sort (vals component-corpora)))
                            " (Mantine " anchor-v ") — do not edit."))
              (prn component-docs)))
      (println "component-docs.edn:" (count component-docs) "components")
      (spit (str (fs/path out "mantine-version.edn"))
            (with-out-str
              (println (str ";; GENERATED by `bb extract` — the Mantine anchor the committed inputs were captured at. Do not edit."))
              (prn {:mantine-version anchor-v})))
      (println "mantine-version.edn:" anchor-v)
      (println "docgen.json copied."))))

(defn -main [& args]
  (write-inputs! (or (first args)
                     (throw (ex-info "usage: bb extract <mantine-clone-dir>" {})))))
