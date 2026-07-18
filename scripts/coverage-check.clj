;; Coverage guard: independently recomputes the scoped surface per package from the
;; committed codegen inputs (docgen.json + scope.edn + installed @mantine exports) and
;; asserts every intended def landed in the generated source. Catches a scope/resolution
;; bug that silently drops components — the recount here is deliberately separate from
;; plan/build's classification, so a regression in the generator diverges from this check.
;;
;; Run with: bb coverage
(ns coverage-check
  (:require [babashka.process :refer [shell]]
            [cheshire.core :as json]
            [clojure.string :as str]
            [plan]))

;; LOAD-BEARING BOUNDARY (ADR 0004): this check shares plan/read-sources — the
;; OBSERVED facts (docgen, scope, exports) — so any divergence from the generator
;; is provably a classification bug, not an artifact of two separate Node
;; enumerations. It MUST re-derive the scoped surface itself and MUST NOT consume
;; plan/build's :namespaces — DRYing the classification through the plan would
;; silently destroy the guard.
(def sources (plan/read-sources))

(def docgen (:docgen sources))
(def scope (:scope sources))

(def exports-index
  (reduce (fn [m pkg]
            (reduce (fn [m nm] (update m nm (fnil conj []) pkg)) m (get (:exports sources) pkg)))
          {}
          (sort (keys (:exports sources)))))

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

;; {pkg -> #{export-name ...}} — the real top-level export surface of each package.
(def pkg-export-set
  (reduce (fn [m [nm pkgs]] (reduce #(update %1 %2 (fnil conj #{}) nm) m pkgs))
          {} exports-index))

;; Static keys on a component that are Mantine machinery, not compound subcomponents.
(def compound-machinery #{"extend" "withProps" "displayName" "classes" "varsResolver"})

(defn component-statics
  "{component -> [Capitalized static keys]} for `comps` in `pkg` (one node call)."
  [pkg comps]
  (-> (shell {:out :string} "node" "-e"
             (str "const m=require('" pkg "');const out={};"
                  "for(const c of " (json/generate-string comps) "){"
                  "const v=m[c];if(v==null||(typeof v!=='object'&&typeof v!=='function'))continue;"
                  "out[c]=Object.keys(v).filter(k=>/^[A-Z]/.test(k));}"
                  "console.log(JSON.stringify(out));"))
      :out
      json/parse-string))

;; {pkg-suffix -> #{kebab-name ...}} the scope intends to land in that package.
(def expected-by-suffix
  (->> (dimension-names (:components scope) wrapped-component-universe)
       (keep (fn [nm] (when-let [pkg (resolve-package nm)] [(pkg-suffix pkg) (kebab nm)])))
       (reduce (fn [m [suffix kb]] (update m suffix (fnil conj #{}) kb)) {})))

;; {pkg-suffix -> #{JS-component-name ...}} — the wrapped docgen components per package,
;; the roots whose Capitalized static parts the compound-part check enumerates.
(def js-names-by-suffix
  (->> (dimension-names (:components scope) wrapped-component-universe)
       (keep (fn [nm] (when-let [pkg (resolve-package nm)] [(pkg-suffix pkg) nm])))
       (reduce (fn [m [suffix nm]] (update m suffix (fnil conj #{}) nm)) {})))

(def hook-exports (set (get (:exports sources) "@mantine/hooks")))
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

(defn check-compound-parts
  "Every Capitalized static part of a wrapped component (minus machinery) that is a
  real package export must land as a def — via docgen or a supplement. A miss is a
  silently-unwrapped compound part (e.g. Menu.Dropdown); fail loud so it gets wrapped
  in a supplement or explicitly removed from scope."
  [suffix comps]
  (let [pkg (str "@mantine/" suffix)
        exports (get pkg-export-set pkg #{})
        present (def-lines (str "src/main/mantine/" suffix ".cljc"))
        statics (component-statics pkg (vec (sort comps)))
        missing (sort (for [c (sort comps)
                            s (get statics c)
                            :when (not (compound-machinery s))
                            :let [exp (str c s)]
                            :when (contains? exports exp)   ; real compound part, not React ctx internals
                            :let [kb (kebab exp)]
                            :when (not (present kb))]
                        kb))]
    (when (seq missing)
      (println (format "  %-14s UNCOVERED compound parts: %s" suffix (str/join ", " missing))))
    (empty? missing)))

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
                        (count expected-hooks))
      _ (println "\nCompound-part coverage (Capitalized static subcomponents vs generated defs):")
      compound-ok (doall (for [[suffix comps] (sort-by key js-names-by-suffix)]
                           (check-compound-parts suffix comps)))]
  (if (every? true? (concat component-ok [hooks-ok] compound-ok))
    (do (println (format "COVERAGE OK — %d scoped entries all present; all compound parts wrapped." total-expected))
        (System/exit 0))
    (do (println "COVERAGE FAILED — scoped entries or compound parts missing from generated source.")
        (System/exit 1))))
