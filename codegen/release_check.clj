;; release-check (ADR 0005): the src/main/deps.cljs @mantine/* ranges, the build.clj
;; version prefix, the package.json pins and the extract provenance witness are all
;; *renderings* of the anchor. This checks each against the anchor. It reads them as
;; data and never regenerates the hand-authored, shipped artifacts.
;;
;; The consumer-facing prose in README.md and docs/release.md also embeds the anchor in
;; copy-pasteable coordinates: the `@mantine/*@^X.Y.Z` install command and the
;; `mantine-ui-wrapper {:mvn/version "X.Y.Z..."}` dep. Those go silently wrong on a bump,
;; so they are checked too. Only tokens that embed a package or artifact pin match;
;; bare version-scheme examples (`9.4.1.0 → 9.4.1.1`) do not.
;;
;; CHANGELOG.md must contain the anchor's release-notes link. Unlike the checks above,
;; the link is not a rendering: it records the bump, and older anchors' links stay in
;; the file. It is checked here because a bump without a changelog entry is a bump-time
;; slip like the stale witness, and the check still passes after a release cut.
;;
;; Pure checker (violations), thin I/O runner (-main). build.clj lives under the
;; deps.edn :build alias, which cannot see codegen/, so its version string is regex'd
;; out of its text instead of required.
(ns release-check
  (:require [anchor]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(defn- version-prefix
  "First three dot-segments of a build.clj version like \"9.4.1.0-SNAPSHOT\"."
  [v]
  (str/join "." (take 3 (str/split v #"\."))))

(defn prose-renderings
  "Pure: extract anchor-embedding coordinates from one prose file's text. Returns a seq of
  {:file :kind :version}: :npm-floor for each `@mantine/pkg@^X.Y.Z` install token (the
  ^-stripped version), :mvn-coord for each `mantine-ui-wrapper {:mvn/version \"X.Y.Z...`
  dep (its first three segments). Bare version-scheme examples do not match."
  [file text]
  (concat
   (for [[_ v] (re-seq #"@mantine/[\w.-]+@\^(\d+\.\d+\.\d+)" text)]
     {:file file :kind :npm-floor :version v})
   (for [[_ v] (re-seq #"mantine-ui-wrapper \{:mvn/version \"(\d+\.\d+\.\d+)" text)]
     {:file file :kind :mvn-coord :version v})))

(defn violations
  "Pure checker: seq of human-readable problem strings (empty = all agree). Every
  @mantine/* deps.cljs range must equal \"^\"+anchor, the build.clj version prefix must
  equal the anchor, the committed provenance witness must equal the anchor, every
  package.json pin must equal the anchor, every anchor-embedding prose coordinate
  (README.md / docs/release.md) must equal the anchor, and the CHANGELOG.md text must
  contain the anchor's release-notes link."
  [{:keys [anchor deps-ranges build-version pins witness prose changelog]}]
  (concat
   (for [[pkg v] (sort pins)
         :when (not= v anchor)]
     (str "package.json pin " pkg " is " v ", expected " anchor))
   (for [[pkg range] (sort deps-ranges)
         :let [expected (str "^" anchor)]
         :when (not= range expected)]
     (str "deps.cljs range " pkg " is " range ", expected " expected))
   (let [prefix (version-prefix build-version)]
     (when (not= prefix anchor)
       [(str "build.clj version prefix is " prefix " (from " build-version "), expected " anchor)]))
   (when (not= witness anchor)
     [(str "provenance witness (codegen/input/mantine-version.edn) is " witness
           ", expected " anchor " — re-run `bb extract`")])
   (for [{:keys [file kind version]} prose
         :when (not= version anchor)]
     (str file " " (name kind) " embeds Mantine " version ", expected " anchor))
   (let [link (str "https://mantine.dev/changelog/" (str/replace anchor "." "-") "/")]
     (when-not (str/includes? changelog link)
       [(str "CHANGELOG.md has no release-notes link " link " for anchor " anchor)]))))

(defn -main
  "Read the real artifacts, assert the package.json pins are uniform (via the anchor
  module), then check every rendering and the changelog link in `violations` against
  the anchor. Exits non-zero on any violation."
  [& _]
  (let [pins (anchor/pins)
        anchor (anchor/anchor-version pins)
        deps-ranges (->> (:npm-deps (edn/read-string (slurp "src/main/deps.cljs")))
                         (filter (fn [[k _]] (str/starts-with? k "@mantine/")))
                         (into {}))
        build-version (second (re-find #"\(def version \"([^\"]+)\"" (slurp "build.clj")))
        witness (:mantine-version (edn/read-string (slurp "codegen/input/mantine-version.edn")))
        prose (mapcat (fn [f] (prose-renderings f (slurp f)))
                      ["README.md" "docs/release.md"])
        probs (violations {:anchor anchor :deps-ranges deps-ranges
                           :build-version build-version :pins pins :witness witness
                           :prose prose :changelog (slurp "CHANGELOG.md")})]
    (if (seq probs)
      (do (println "RELEASE-CHECK FAILED — anchor" anchor)
          (doseq [p probs] (println "  •" p))
          (System/exit 1))
      (println "RELEASE-CHECK OK — anchor" anchor
               "matches deps.cljs ranges, build.clj prefix, package.json pins, provenance witness,"
               "README/release prose, and the CHANGELOG.md release-notes link."))))
