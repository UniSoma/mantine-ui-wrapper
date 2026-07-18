;; Extract-level fixture tests (ADR 0004): exercise the PURE extract parsers and
;; parse-inputs on hand-written MDX snippets — no clone, no writes. The two regex
;; parsers are the highest-drift code in the repo (they scrape upstream MDX .ts);
;; parse-inputs IS the test surface. Fixtures are minimal hand-written snippets,
;; NOT real MDX excerpts.
;;
;; Run with: bb extract-test
(ns extract-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [extract]))

;; ---------------------------------------------------------------- hooks

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

;; ---------------------------------------------------------------- components

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

;; ---------------------------------------------------------------- parse-inputs

(def button-core
  "Button: {\n  description: 'A button',\n  package: '@mantine/core',\n  slug: '/core/button',\n  props: ['Button'],\n},")

(def calendar-dates
  "Calendar: {\n  description: 'A calendar',\n  package: '@mantine/dates',\n  slug: '/dates/calendar',\n  props: ['Calendar'],\n},")

(deftest parse-inputs-unions-corpora
  (let [{:keys [hook-docs component-docs]}
        (extract/parse-inputs
         {:hooks-text "useDisclosure: hDocs('useDisclosure', 'Manages boolean state'),"
          :component-texts {:core button-core :dates calendar-dates :charts "" :others ""}})]
    (is (= {"useDisclosure" "Manages boolean state"} hook-docs))
    (is (= ["Button" "Calendar"] (keys component-docs)))
    (is (= "@mantine/dates" (get-in component-docs ["Calendar" :package])))))

(deftest parse-inputs-cross-corpus-collision-throws
  (let [dup-in-others
        "Button: {\n  description: 'dupe',\n  package: '@mantine/core',\n  slug: '/x/button',\n  props: ['Button'],\n},"]
    (is (thrown-with-msg?
         Exception #"corpus collision.*Button"
         (extract/parse-inputs
          {:hooks-text ""
           :component-texts {:core button-core :dates "" :charts "" :others dup-in-others}})))))

;; ----------------------------------------------------------------

(let [{:keys [fail error]} (run-tests 'extract-test)]
  (when (pos? (+ fail error))
    (System/exit 1)))
