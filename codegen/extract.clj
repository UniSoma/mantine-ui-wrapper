;; Per-version-bump input refresh (ADR 0004 pattern): the fragile upstream-MDX
;; parsers live in a PURE, requirable core (parse-inputs) with a thin I/O driver.
;;
;;   parse-inputs   : PURE — corpus-keyed texts -> {:hook-docs :component-docs};
;;                    owns parsing, the component corpus merge, the collision guard,
;;                    and sorted-map determinism. THE test surface.
;;   write-inputs!  : thin — slurp clone texts, assert clone == anchor, copy
;;                    docgen.json, call parse-inputs, spit the two EDN maps + witness.
;;   -main          : reads the clone-dir arg (usage-throw if missing).
;;
;; Run AFTER the clone has run docgen:
;;
;;   git clone --depth 1 --branch 9.4.1 https://github.com/mantinedev/mantine <dir>
;;   cd <dir> && yarn install && yarn tsx scripts/docgen
;;   bb extract <dir>
;;
;; Writes (all committed):
;;   codegen/input/docgen.json         — verbatim copy of the docgen output
;;   codegen/input/hook-docs.edn       — {"useX" "description"} from mdx-hooks-data.ts
;;   codegen/input/component-docs.edn  — {"Button" {:description ... :slug ... :polymorphic ...}}
;;                                       (extension packages are keyed by docs-entry name;
;;                                        join via :props, which lists the docgen keys)
;;   codegen/input/mantine-version.edn — {:mantine-version ...} provenance witness (ADR 0005)
(ns extract
  (:require [anchor]
            [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.string :as str]))

;; --- hooks: hDocs('useX', 'description') calls + inline-object exceptions ------------

(defn extract-hook-docs [text]
  (let [hdocs (re-seq #"(?s)(use\w+): hDocs\(\s*'use\w+',\s*'([^']*)'\s*\)" text)
        ;; inline-object entries (e.g. useElementSize) — no nested braces in this file
        inline (for [[_ nm block] (re-seq #"(?s)(use\w+): \{([^{}]*)\}" text)
                     :let [d (second (re-find #"description: '([^']*)'" block))]
                     :when d]
                 [nm d])]
    (into (sorted-map)
          (concat (for [[_ nm d] hdocs]
                    [nm (str/replace d #"\s+" " ")])
                  inline))))

;; --- components: inline object literals keyed by exact PascalCase docgen name --------
;; Same parse Mantine's own scripts/llm/compile-mcp-data.ts uses (verified regex-safe).

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

;; --- pure core -----------------------------------------------------------------------

(defn parse-inputs
  "PURE. Given the raw MDX texts — {:hooks-text \"...\" :component-texts {:core ... :dates
  ... :charts ... :others ...}} — returns {:hook-docs {\"useX\" \"...\"} :component-docs
  {\"Button\" {...}}}. Parses each corpus, unions the component maps, and guards against a
  cross-corpus PascalCase key collision (ADR-0004: a wrong/ambiguous artifact throws, not
  a silent last-wins merge). Determinism comes from the sorted-maps."
  [{:keys [hooks-text component-texts]}]
  (let [per-corpus (into {} (for [[corpus text] component-texts]
                              [corpus (extract-component-docs text)]))
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
    {:hook-docs (extract-hook-docs hooks-text)
     :component-docs (into (sorted-map) (apply merge (vals per-corpus)))}))

;; --- thin I/O driver -----------------------------------------------------------------

(def ^:private component-corpora
  "Logical corpus name -> the MDX data file it is parsed from."
  {:core "mdx-core-data.ts"
   :dates "mdx-dates-data.ts"
   :charts "mdx-charts-data.ts"
   :others "mdx-others-data.ts"})

(defn- clone-mantine-version
  "The Mantine version the fed clone actually is — @mantine/core's package.json version,
  falling back to the clone-dir root package.json."
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
          (parse-inputs {:hooks-text (slurp (str (fs/path mdx-dir "mdx-hooks-data.ts")))
                         :component-texts (into {} (for [[corpus f] component-corpora]
                                                     [corpus (slurp (str (fs/path mdx-dir f)))]))})]
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
              (println (str ";; GENERATED by `bb extract` from mdx-{core,dates,charts}-data.ts (Mantine " anchor-v ") — do not edit."))
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
