;; release-check (ADR 0005): the src/main/deps.cljs @mantine/* ranges, the build.clj
;; version prefix, and the package.json pins are all *renderings* of the anchor. This
;; validates their agreement against the one canonical anchor; it READS them as data
;; and NEVER regenerates the hand-authored, shipped artifacts.
;;
;; The consumer-facing prose in README.md and docs/release.md also embeds the anchor in
;; live, copy-pasteable coordinates — the `@mantine/*@^X.Y.Z` install command and the
;; `mantine-ui-wrapper {:mvn/version "X.Y.Z..."}` dep. Those go silently wrong on a bump,
;; so we flag them too. Only tokens that literally embed a package/artifact pin are
;; matched; illustrative bare version-scheme examples (`9.4.1.0 → 9.4.1.1`) are left alone.
;;
;; PURE checker (violations) + thin I/O runner (-main). build.clj is a read TARGET —
;; it lives under the deps.edn :build alias which cannot see codegen/, so its version
;; string is regex'd out of its text, never required.
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
  {:file :kind :version} — :npm-floor for each `@mantine/pkg@^X.Y.Z` install token (the
  ^-stripped version) and :mvn-coord for each `mantine-ui-wrapper {:mvn/version \"X.Y.Z...`
  dep (its first three segments). Bare version-scheme examples are deliberately not matched."
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
  package.json pin must equal the anchor, and every anchor-embedding prose coordinate
  (README.md / docs/release.md) must equal the anchor."
  [{:keys [anchor deps-ranges build-version pins witness prose]}]
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
     (str file " " (name kind) " embeds Mantine " version ", expected " anchor))))

(defn -main
  "Read the real artifacts, assert the package.json pins are uniform (via the anchor
  module), then check deps.cljs + build.clj against the anchor. Non-zero exit on any
  violation."
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
                           :prose prose})]
    (if (seq probs)
      (do (println "RELEASE-CHECK FAILED — anchor" anchor)
          (doseq [p probs] (println "  •" p))
          (System/exit 1))
      (println "RELEASE-CHECK OK — anchor" anchor
               "matches deps.cljs ranges, build.clj prefix, package.json pins, and README/release prose."))))
