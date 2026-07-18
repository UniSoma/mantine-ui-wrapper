;; release-check (ADR 0005): the src/main/deps.cljs @mantine/* ranges, the build.clj
;; version prefix, and the package.json pins are all *renderings* of the anchor. This
;; validates their agreement against the one canonical anchor; it READS them as data
;; and NEVER regenerates the hand-authored, shipped artifacts.
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

(defn violations
  "Pure checker: seq of human-readable problem strings (empty = all agree). Every
  @mantine/* deps.cljs range must equal \"^\"+anchor, the build.clj version prefix must
  equal the anchor, the committed provenance witness must equal the anchor, and every
  package.json pin must equal the anchor."
  [{:keys [anchor deps-ranges build-version pins witness]}]
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
           ", expected " anchor " — re-run `bb extract`")])))

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
        probs (violations {:anchor anchor :deps-ranges deps-ranges
                           :build-version build-version :pins pins :witness witness})]
    (if (seq probs)
      (do (println "RELEASE-CHECK FAILED — anchor" anchor)
          (doseq [p probs] (println "  •" p))
          (System/exit 1))
      (println "RELEASE-CHECK OK — anchor" anchor
               "matches deps.cljs ranges, build.clj prefix, and package.json pins."))))
