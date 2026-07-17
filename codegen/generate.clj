;; Generator: pure, headless, CI-runnable transform of the COMMITTED inputs
;; (codegen/input/*, codegen/scope.edn, codegen/controlled-inputs.edn,
;; codegen/supplements/*.cljc) + installed node_modules (export enumeration)
;; into one .cljc namespace per wrapped package under src/main/mantine/.
;;
;; Run with: bb generate
(ns generate
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [edamame.core :as e]))

(def mantine-version "9.4.1")

;; ---------------------------------------------------------------- inputs

(def docgen (json/parse-string (slurp "codegen/input/docgen.json")))
(def component-docs (edn/read-string (slurp "codegen/input/component-docs.edn")))
(def hook-docs (edn/read-string (slurp "codegen/input/hook-docs.edn")))
(def hook-docs-page (edn/read-string (slurp "codegen/input/hook-docs-page.edn")))
(def scope (edn/read-string (slurp "codegen/scope.edn")))
(def controlled-inputs (edn/read-string (slurp "codegen/controlled-inputs.edn")))

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

;; {export-name -> [package ...]} over every installed @mantine/* package
(def exports-index
  (reduce (fn [m pkg]
            (reduce (fn [m nm] (update m nm (fnil conj []) pkg)) m (package-exports pkg)))
          {}
          mantine-packages))

;; Every docgen entry exported by a wrapped @mantine/* package other than
;; @mantine/hooks — the universe the :components dimension draws from when {:all true}.
(def wrapped-component-universe
  (filter (fn [nm]
            (and (get docgen nm)
                 (some #(not= % "@mantine/hooks") (get exports-index nm []))))
          (keys exports-index)))

(defn dimension-names
  "Resolve a scope dimension to a concrete name seq. Either an explicit name set,
  or {:all true :exclude #{...}} = every name in `universe` minus the excludes."
  [spec universe]
  (if (set? spec)
    spec
    (do (assert (:all spec)
                "scope dimension must be a set or {:all true :exclude #{...}}")
        (remove (:exclude spec #{}) universe))))

;; ---------------------------------------------------------------- naming

(defn kebab [s]
  (str/lower-case (str/replace s #"([a-z0-9])([A-Z])" "$1-$2")))

(def clojure-core-names
  (set (map str (keys (ns-publics 'clojure.core)))))

(defn pkg-suffix [pkg] (subs pkg (count "@mantine/")))
(defn pkg->ns [pkg] (str "mantine." (pkg-suffix pkg)))
(defn pkg->file [pkg] (str "src/main/mantine/" (pkg-suffix pkg) ".cljc"))

;; ---------------------------------------------------------------- docstrings

(defn esc
  "Escape a docstring for emission as a (multi-line) string literal."
  [s]
  (-> s (str/replace "\\" "\\\\") (str/replace "\"" "\\\"")))

(defn squash-ws [s] (str/trim (str/replace (str s) #"\s+" " ")))

(defn docs-entry
  "Docs-app entry for a docgen component name: direct key match, else the family
  entry whose :props lists the name (compound sub-components inherit the parent's)."
  [nm]
  (or (get component-docs nm)
      (some (fn [[k v]]
              (when (and (not= k nm) (some #{nm} (:props v)))
                (assoc v :inherited-from k)))
            component-docs)))

(defn prop-lines [props]
  (for [[pname p] (sort-by key props)]
    (str "  " pname " (" (squash-ws (get-in p ["type" "name"])) ")"
         (when (get p "required") " REQUIRED")
         (when-some [d (get p "defaultValue")] (str " [default " (squash-ws d) "]"))
         (let [d (get p "description")]
           (when (seq d) (str " — " (squash-ws d)))))))

(defn component-docstring [nm]
  (let [entry (docs-entry nm)
        props (get-in docgen [nm "props"])]
    (->> (concat
          [(str nm
                (when-let [d (:description entry)]
                  (str " — " d (when-let [p (:inherited-from entry)] (str " (" p " family)")))))]
          (when-let [slug (:slug entry)] ["" (str "https://mantine.dev" slug)])
          (when (:polymorphic entry)
            ["" "Polymorphic: accepts :component / :render-root."])
          (when (seq props)
            (into ["" (str "Props (docgen " mantine-version "):")] (prop-lines props)))
          ["" "Optional leading props map; remaining args are children."])
         (str/join "\n"))))

(defn hook-docstring [nm]
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

;; ---------------------------------------------------------------- supplements

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

(defn parse-supplement
  "Parse codegen/supplements/<pkg-suffix>.cljc. Returns nil when absent, else
  {:cljs-requires [...] :requires [...] :def-names [...] :body \"<verbatim text>\"}.
  Declares fully satisfied by `satisfied-names` are dropped from the body."
  [suffix satisfied-names]
  (let [path (str "codegen/supplements/" suffix ".cljc")]
    (when (fs/exists? path)
      (let [text (slurp path)
            forms (e/parse-string-all text {:read-cond :preserve :all true})
            [ns-form & body-forms] forms
            _ (assert (and (seq? ns-form) (= 'ns (first ns-form)))
                      (str path ": first form must be the ns form"))
            require-clause (some #(when (and (seq? %) (= :require (first %))) %) ns-form)
            {:keys [cljs unconditional]}
            (reduce (fn [acc entry]
                      (if (reader-conditional? entry)
                        (let [{:keys [form splicing?]} entry
                              _ (assert (= :cljs (first form))
                                        (str path ": only :cljs reader conditionals are supported in :require"))
                              entries (if splicing? (second form) [(second form)])]
                          (update acc :cljs into entries))
                        (update acc :unconditional conj entry)))
                    {:cljs [] :unconditional []}
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
         :requires unconditional
         :def-names (vec def-names)
         :body body}))))

;; ---------------------------------------------------------------- require merging

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

(defn merge-requires [base extra]
  (reduce (fn [acc entry]
            (if-let [i (first (keep-indexed
                               (fn [i e] (when (= (require-key e) (require-key entry)) i))
                               acc))]
              (update acc i merge-require-entry entry)
              (conj acc entry)))
          (vec base)
          extra))

;; ---------------------------------------------------------------- emission

(defn emit-ns-form [{:keys [ns-name docstring exclude cljs-requires clj-requires requires]}]
  (let [require-lines
        (concat
         [(str "#?@(:cljs [" (str/join "\n              " (map pr-str cljs-requires)) "])")]
         (when (seq clj-requires)
           [(str "#?@(:clj [" (str/join "\n             " (map pr-str clj-requires)) "])")])
         (map pr-str requires))]
    (str "(ns " ns-name "\n"
         "  \"" (esc docstring) "\"\n"
         (when (seq exclude)
           (str "  (:refer-clojure :exclude [" (str/join " " (sort exclude)) "])\n"))
         "  (:require\n   "
         (str/join "\n   " require-lines)
         "))\n")))

(defn emit-component-def [{:keys [ns-name js-name controlled?]}]
  (let [kb (kebab js-name)]
    (str "(def " kb "\n"
         "  \"" (esc (component-docstring js-name)) "\"\n"
         "  #?(:cljs (f/factory " (if controlled? (str "(f/controlled " js-name ")") js-name) ")\n"
         "     :clj (f/not-implemented \"" ns-name "/" kb "\")))\n")))

(defn emit-hook-def [{:keys [ns-name js-name]}]
  (let [kb (kebab js-name)]
    (str "(def " kb "\n"
         "  \"" (esc (hook-docstring js-name)) "\"\n"
         "  #?(:cljs " js-name "\n"
         "     :clj (f/not-implemented \"" ns-name "/" kb "\")))\n")))

(def header
  (str ";; GENERATED by `bb generate` from the committed codegen inputs (Mantine "
       mantine-version ") — DO NOT EDIT.\n"))

(defn check-collisions! [ns-name names]
  (doseq [[kb group] (group-by kebab names)]
    (when (> (count group) 1)
      (throw (ex-info (str ns-name ": kebab collision " kb " <- " (str/join ", " group))
                      {:ns ns-name :kebab kb :names group})))))

(defn derive-exclude [def-names]
  (filter clojure-core-names def-names))

(defn emit-package-ns
  "One generated ns for `pkg` with its resolved component names (+ optional supplement)."
  [pkg component-names]
  (let [ns-name (pkg->ns pkg)
        supplement (parse-supplement (pkg-suffix pkg) (set (map kebab component-names)))
        _ (check-collisions! ns-name component-names)
        _ (when-let [clash (seq (filter (set (map kebab component-names))
                                        (:def-names supplement)))]
            (throw (ex-info (str ns-name ": supplement def(s) " (str/join ", " clash)
                                 " collide with generated defs — docgen now covers this; delete the supplement entry.")
                            {:ns ns-name :collisions (vec clash)})))
        def-names (concat (map kebab component-names) (:def-names supplement))
        docstring (str "Mantine " pkg " " mantine-version " wrappers (generated"
                       (when supplement ", supplement hoisted from codegen/supplements/")
                       ").")
        cljs-requires (merge-requires
                       [(into [pkg :refer (mapv symbol (sort component-names))])]
                       (:cljs-requires supplement))
        requires (merge-requires
                  '[[mantine.impl.factory :as f]]
                  (:requires supplement))]
    {:file (pkg->file pkg)
     :text (str header
                (emit-ns-form {:ns-name ns-name
                               :docstring docstring
                               :exclude (derive-exclude def-names)
                               :cljs-requires cljs-requires
                               :requires requires})
                "\n"
                (str/join "\n" (for [nm (sort component-names)]
                                 (emit-component-def {:ns-name ns-name
                                                      :js-name nm
                                                      :controlled? (contains? controlled-inputs nm)})))
                (when-let [body (:body supplement)]
                  (str "\n;; ---- hoisted from codegen/supplements/" (pkg-suffix pkg) ".cljc ----\n\n"
                       body)))}))

(defn emit-hooks-ns [hook-names]
  (let [ns-name "mantine.hooks"
        _ (check-collisions! ns-name hook-names)
        docstring (str "Mantine @mantine/hooks " mantine-version " wrappers (generated).\n\n"
                       "Thin def-aliases over the JS hooks — raw passthrough in both directions.\n"
                       "Rules of hooks are NOT enforced here; they are delegated to React (call\n"
                       "only inside function components / other hooks). Object returns need ^js\n"
                       "or js-interop access under :advanced compilation.")]
    {:file "src/main/mantine/hooks.cljc"
     :text (str header
                (emit-ns-form {:ns-name ns-name
                               :docstring docstring
                               :exclude (derive-exclude (map kebab hook-names))
                               :cljs-requires [(into ["@mantine/hooks" :refer (mapv symbol (sort hook-names))])]
                               ;; hook defs are bare aliases on :cljs; f is only used by
                               ;; the :clj not-implemented branch
                               :clj-requires '[[mantine.impl.factory :as f]]})
                "\n"
                (str/join "\n" (for [nm (sort hook-names)]
                                 (emit-hook-def {:ns-name ns-name :js-name nm}))))}))

;; ---------------------------------------------------------------- main

(defn resolve-component
  "Resolve a scoped component name -> its package, via docgen presence + installed
  package exports. Unresolvable names are logged and skipped (never silently)."
  [nm]
  (let [pkgs (remove #{"@mantine/hooks"} (get exports-index nm []))]
    (cond
      (nil? (get docgen nm))
      (println "SKIP component" nm "— not present in docgen.json")

      (empty? pkgs)
      (println "SKIP component" nm "— no installed @mantine package exports it")

      :else
      (let [pkg (if (some #{"@mantine/core"} pkgs) "@mantine/core" (first (sort pkgs)))]
        (when (> (count pkgs) 1)
          (println "WARN component" nm "exported by" (str/join ", " pkgs) "— using" pkg))
        [nm pkg]))))

(let [components (dimension-names (:components scope) wrapped-component-universe)
      resolved (keep resolve-component (sort components))
      by-pkg (group-by second resolved)
      hook-exports (set (package-exports "@mantine/hooks"))
      hook-universe (filter #(str/starts-with? % "use") hook-exports)
      hooks (vec (for [nm (sort (dimension-names (:hooks scope) hook-universe))
                       :when (or (and (str/starts-with? nm "use") (hook-exports nm))
                                 (do (println "SKIP hook" nm "— not a use* export of @mantine/hooks") false))]
                   nm))
      supplement-only-pkgs (:supplement-only-packages scope #{})
      outputs (-> (vec (for [[pkg entries] (sort-by key by-pkg)]
                         (emit-package-ns pkg (mapv first entries))))
                  ;; declared supplement-only packages: no docgen components, ns body
                  ;; is entirely the hoisted supplement.
                  (into (for [pkg (sort supplement-only-pkgs)]
                          (emit-package-ns pkg [])))
                  (conj (emit-hooks-ns hooks)))]
  ;; controlled-inputs rot check
  (doseq [nm controlled-inputs
          :when (not-any? #(= nm (first %)) resolved)]
    (println "NOTE controlled-input" nm "not among resolved components (out of current scope)"))
  (doseq [{:keys [file text]} outputs]
    (fs/create-dirs (fs/parent file))
    (spit file text)
    (println "WROTE" file (str "(" (count (str/split-lines text)) " lines)")))
  (println "Generated" (count outputs) "namespaces from" (count resolved) "components +" (count hooks) "hooks."))
