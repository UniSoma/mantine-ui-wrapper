;; Coverage guard: independently recomputes the scoped surface per package from the
;; committed codegen inputs (docgen.json + scope.edn + installed @mantine exports) and
;; asserts every intended def landed in the generated source. Catches a scope/resolution
;; bug that silently drops components — the recount here is deliberately separate from
;; codegen/generate.clj, so a regression in the generator diverges from this check.
;;
;; Run with: bb coverage
(ns coverage-check
  (:require [babashka.process :refer [shell]]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def docgen (json/parse-string (slurp "codegen/input/docgen.json")))
(def scope (edn/read-string (slurp "codegen/scope.edn")))

(def mantine-packages
  (->> (get (json/parse-string (slurp "package.json")) "devDependencies")
       keys
       (filter #(str/starts-with? % "@mantine/"))
       sort))

(defn package-exports [pkg]
  (-> (shell {:out :string} "node" "-e"
             (str "console.log(JSON.stringify(Object.keys(require('" pkg "'))))"))
      :out
      json/parse-string))

(def exports-index
  (reduce (fn [m pkg]
            (reduce (fn [m nm] (update m nm (fnil conj []) pkg)) m (package-exports pkg)))
          {}
          mantine-packages))

(def wrapped-component-universe
  (filter (fn [nm]
            (and (get docgen nm)
                 (some #(not= % "@mantine/hooks") (get exports-index nm []))))
          (keys exports-index)))

(defn dimension-names [spec universe]
  (if (set? spec) spec (remove (:exclude spec #{}) universe)))

(defn kebab [s]
  (str/lower-case (str/replace s #"([a-z0-9])([A-Z])" "$1-$2")))

(defn resolve-package [nm]
  (let [pkgs (remove #{"@mantine/hooks"} (get exports-index nm []))]
    (when (and (get docgen nm) (seq pkgs))
      (if (some #{"@mantine/core"} pkgs) "@mantine/core" (first (sort pkgs))))))

(defn pkg-suffix [pkg] (subs pkg (count "@mantine/")))

;; {pkg-suffix -> #{kebab-name ...}} the scope intends to land in that package.
(def expected-by-suffix
  (->> (dimension-names (:components scope) wrapped-component-universe)
       (keep (fn [nm] (when-let [pkg (resolve-package nm)] [(pkg-suffix pkg) (kebab nm)])))
       (reduce (fn [m [suffix kb]] (update m suffix (fnil conj #{}) kb)) {})))

(def hook-exports (set (package-exports "@mantine/hooks")))
(def expected-hooks
  (->> (dimension-names (:hooks scope) (filter #(str/starts-with? % "use") hook-exports))
       (filter #(and (str/starts-with? % "use") (hook-exports %)))
       (map kebab)
       set))

(defn def-lines
  "kebab names emitted as top-level defs in a generated file (exact-line match, so
  `button` never matches `button-group`)."
  [file]
  (->> (str/split-lines (slurp file))
       (keep #(second (re-matches #"\(def ([^\s]+)" %)))
       set))

(defn check-package [suffix expected]
  (let [file (str "src/main/mantine/" suffix ".cljc")
        present (def-lines file)
        missing (sort (remove present expected))]
    (println (format "  %-14s expected %3d  present-defs %3d  missing %d"
                     suffix (count expected) (count present) (count missing)))
    (when (seq missing)
      (println "    MISSING:" (str/join ", " missing)))
    (empty? missing)))

(println "Per-package def-count coverage (scoped docgen entries vs generated defs):")
(let [component-ok (doall (for [[suffix expected] (sort-by key expected-by-suffix)]
                            (check-package suffix expected)))
      hooks-ok (check-package "hooks" expected-hooks)
      total-expected (+ (reduce + (map count (vals expected-by-suffix)))
                        (count expected-hooks))]
  (if (every? true? (conj component-ok hooks-ok))
    (do (println (format "COVERAGE OK — %d scoped entries all present." total-expected))
        (System/exit 0))
    (do (println "COVERAGE FAILED — scoped entries missing from generated source.")
        (System/exit 1))))
