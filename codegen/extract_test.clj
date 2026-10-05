;; Extract-level fixture tests (ADR 0004) for the pure extract parsers and
;; parse-inputs, with no clone and no writes. The two regex parsers scrape upstream
;; MDX .ts, which makes them the highest-drift code in the repo; parse-inputs is the
;; test surface. Fixtures are minimal hand-written snippets, not real MDX excerpts.
;;
;; Run with: bb extract-test
(ns extract-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [extract]))

(deftest hook-hdocs-form
  (is (= {"useDisclosure" "Manages boolean state"}
         (extract/extract-hook-docs
          "useDisclosure: hDocs('useDisclosure', 'Manages boolean state'),"))))

(deftest hook-inline-object-form
  (testing "an inline-object hook entry contributes its description field"
    (is (= {"useElementSize" "Measures an element"}
           (extract/extract-hook-docs
            "useElementSize: {\n  description: 'Measures an element',\n  category: 'ui',\n},")))))

(deftest hook-description-whitespace-collapsed
  (is (= {"useFoo" "does a thing"}
         (extract/extract-hook-docs
          "useFoo: hDocs('useFoo', 'does   a\n  thing'),"))))

(deftest component-happy-path
  (is (= {"Button" {:description "A button"
                    :package "@mantine/core"
                    :slug "/core/button"
                    :props ["Button" "ButtonGroup"]}}
         (extract/extract-component-docs
          "Button: {\n  description: 'A button',\n  package: '@mantine/core',\n  slug: '/core/button',\n  props: ['Button', 'ButtonGroup'],\n},"))))

(deftest component-polymorphic-flag
  (is (= {:polymorphic true}
         (select-keys (get (extract/extract-component-docs
                            "Box: {\n  description: 'poly',\n  package: '@mantine/core',\n  slug: '/core/box',\n  polymorphic: true,\n  props: ['Box'],\n},")
                           "Box")
                      [:polymorphic]))))

(deftest component-landing-page-skipped
  (testing "an entry with no package/props (landing pages) is dropped"
    (is (= {} (extract/extract-component-docs
               "GettingStarted: {\n  description: 'just a page',\n},")))))

(deftest component-nested-brace-block
  (testing "a one-level nested brace inside the block does not truncate the parse"
    (is (= {"Grid" {:description "grid"
                    :package "@mantine/core"
                    :slug "/core/grid"
                    :props ["Grid"]}}
           (extract/extract-component-docs
            "Grid: {\n  description: 'grid',\n  package: '@mantine/core',\n  slug: '/core/grid',\n  vars: { root: 1 },\n  props: ['Grid'],\n},")))))

(def button-core
  "Button: {\n  description: 'A button',\n  package: '@mantine/core',\n  slug: '/core/button',\n  props: ['Button'],\n},")

(def calendar-dates
  "Calendar: {\n  description: 'A calendar',\n  package: '@mantine/dates',\n  slug: '/dates/calendar',\n  props: ['Calendar'],\n},")

(def empty-parsed-files
  "Every docs-data file extract parses, each with empty text."
  {"mdx-core-data.ts" ""
   "mdx-dates-data.ts" ""
   "mdx-charts-data.ts" ""
   "mdx-schedule-data.ts" ""
   "mdx-others-data.ts" ""
   "mdx-hooks-data.ts" ""})

(deftest parse-inputs-unions-corpora
  (let [{:keys [hook-docs component-docs]}
        (extract/parse-inputs
         (assoc empty-parsed-files
                "mdx-hooks-data.ts" "useDisclosure: hDocs('useDisclosure', 'Manages boolean state'),"
                "mdx-core-data.ts" button-core
                "mdx-dates-data.ts" calendar-dates))]
    (is (= {"useDisclosure" "Manages boolean state"} hook-docs))
    (is (= ["Button" "Calendar"] (keys component-docs)))
    (is (= "@mantine/dates" (get-in component-docs ["Calendar" :package])))))

(deftest parse-inputs-cross-corpus-collision-throws
  (let [dup-in-others
        "Button: {\n  description: 'dupe',\n  package: '@mantine/core',\n  slug: '/x/button',\n  props: ['Button'],\n},"]
    (is (thrown-with-msg?
         Exception #"corpus collision.*Button"
         (extract/parse-inputs
          (assoc empty-parsed-files
                 "mdx-core-data.ts" button-core
                 "mdx-others-data.ts" dup-in-others))))))

(deftest parse-inputs-unknown-file-throws
  (let [e (try (extract/parse-inputs
                (assoc empty-parsed-files
                       "mdx-new-package-data.ts" button-core
                       "mdx-another-data.ts" ""))
               nil
               (catch clojure.lang.ExceptionInfo e e))]
    (is (re-find #"mdx-another-data\.ts, mdx-new-package-data\.ts" (ex-message e)))
    (is (= {:unknown ["mdx-another-data.ts" "mdx-new-package-data.ts"]} (ex-data e)))))

(deftest parse-inputs-ignored-files-skipped
  (testing "ignored docs-data files neither throw nor add components"
    (is (= ["Button"]
           (keys (:component-docs
                  (extract/parse-inputs
                   (assoc empty-parsed-files
                          "mdx-core-data.ts" button-core
                          "mdx-code-highlight-data.ts" calendar-dates
                          "mdx-form-data.ts" ""
                          "mdx-guides-data.ts" ""
                          "mdx-meta-data.ts" ""
                          "mdx-styles-data.ts" ""
                          "mdx-theming-data.ts" ""))))))))

(deftest parse-inputs-missing-file-throws
  (let [e (try (extract/parse-inputs
                (dissoc empty-parsed-files "mdx-others-data.ts" "mdx-hooks-data.ts"))
               nil
               (catch clojure.lang.ExceptionInfo e e))]
    (is (re-find #"mdx-hooks-data\.ts, mdx-others-data\.ts" (ex-message e)))
    (is (= {:missing ["mdx-hooks-data.ts" "mdx-others-data.ts"]} (ex-data e)))))

(deftest parse-inputs-renamed-file-reports-both
  (testing "a parsed file renamed upstream shows as unknown and missing in one error"
    (let [e (try (extract/parse-inputs
                  (-> empty-parsed-files
                      (dissoc "mdx-others-data.ts")
                      (assoc "mdx-extensions-data.ts" "")))
                 nil
                 (catch clojure.lang.ExceptionInfo e e))]
      (is (re-find #"unknown.*mdx-extensions-data\.ts.*missing.*mdx-others-data\.ts" (ex-message e)))
      (is (= {:unknown ["mdx-extensions-data.ts"] :missing ["mdx-others-data.ts"]} (ex-data e))))))

(deftest parsed-and-ignored-files-disjoint
  (testing "no docs-data file is both parsed and ignored"
    (is (empty? (filter @#'extract/ignored-files
                        (conj (vals @#'extract/component-corpora) @#'extract/hooks-file))))))

(let [{:keys [fail error]} (run-tests 'extract-test)]
  (when (pos? (+ fail error))
    (System/exit 1)))
