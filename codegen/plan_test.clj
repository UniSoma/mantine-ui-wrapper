;; Plan-level fixture tests (ADR 0004): exercise plan/build and plan/emit-ns on
;; hand-built `sources` maps — no Node, no writes. The plan IS the test surface.
;;
;; Run with: bb plan-test
(ns plan-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing run-tests]]
            [plan]))

(def base-sources
  {:docgen {"Button" {"props" {"color" {"type" {"name" "string"}
                                        "required" true
                                        "defaultValue" "blue"
                                        "description" "Button   color"}}}
            "Alert" {"props" {}}
            "Slider" {"props" {}}
            "Chart" {"props" {}}
            "List" {"props" {}}
            "Ghost" {"props" {}}}
   :component-docs {"Button" {:slug "/core/button"
                              :description "Renders a button"
                              :polymorphic true
                              :props ["Button" "ButtonGroup"]}}
   :hook-docs {"useDisclosure" "Manages boolean state"}
   :hook-docs-page {"useMousePosition" "use-mouse"}
   :controlled #{"Slider"}
   :scope {:components #{"Button" "Alert" "Slider" "Chart" "List" "Ghost" "NotInDocgen"}
           :hooks #{"useDisclosure" "useMousePosition" "randomId"}
           :supplement-only-packages #{}}
   :supplements {}
   :exports {"@mantine/core" ["Button" "Alert" "Slider" "List" "Chart"]
             "@mantine/charts" ["Chart"]
             "@mantine/hooks" ["useDisclosure" "useMousePosition" "randomId"]}
   :mantine-version "9.9.9"})

(defn- ns-plan [plan ns-name]
  (some #(when (= ns-name (:ns-name %)) %) (:namespaces plan)))

(defn- def-plan [np sym]
  (some #(when (= sym (:symbol %)) %) (:defs np)))

;; ---------------------------------------------------------------- classification

(deftest classification-and-resolution
  (let [plan (plan/build base-sources)
        core (ns-plan plan "mantine.core")]
    (testing "namespaces sorted by :ns-name"
      (is (= ["mantine.core" "mantine.hooks"] (mapv :ns-name (:namespaces plan)))))
    (testing "defs sorted by :js-name, kebab pre-computed"
      (is (= ["Alert" "Button" "Chart" "List" "Slider"] (mapv :js-name (:defs core))))
      (is (= "button" (:symbol (def-plan core "button")))))
    (testing "@mantine/core wins a multi-package export, recorded as a note"
      (is (= "@mantine/core" (:pkg core)))
      (is (nil? (ns-plan plan "mantine.charts")))
      (is (some #(and (= :multi-package (:kind %))
                      (= "Chart" (:js-name %))
                      (= "@mantine/core" (:chosen %)))
                (:notes plan))))
    (testing "controlled-inputs membership pre-computed"
      (is (true? (:controlled? (def-plan core "slider"))))
      (is (false? (:controlled? (def-plan core "button")))))
    (testing "refer-clojure excludes pre-computed and sorted"
      (is (= ["list"] (:refer-clojure-exclude core))))))

(deftest skips-are-data
  (let [plan (plan/build base-sources)
        skip (fn [nm] (some #(when (= nm (:js-name %)) %) (:skipped plan)))]
    (is (= "not present in docgen.json" (:reason (skip "NotInDocgen"))))
    (is (= "no installed @mantine package exports it" (:reason (skip "Ghost"))))
    (is (nil? (def-plan (ns-plan plan "mantine.core") "ghost")))
    (testing "non-use* name in the :hooks dimension is skipped"
      (is (= :hook (:kind (skip "randomId"))))
      (is (= "not a use* export of @mantine/hooks" (:reason (skip "randomId")))))))

(deftest controlled-input-rot-is-a-note
  (let [plan (plan/build (assoc base-sources :controlled #{"Slider" "Rotten"}))]
    (is (some #(and (= :controlled-input-rot (:kind %)) (= "Rotten" (:js-name %)))
              (:notes plan)))))

(deftest kebab-collision-throws
  (let [sources (-> base-sources
                    (assoc-in [:docgen "ALert"] {"props" {}})
                    (update-in [:scope :components] conj "ALert")
                    (update :exports assoc "@mantine/core" ["Button" "Alert" "ALert" "Slider" "List" "Chart"]))]
    (is (thrown-with-msg? Exception #"kebab collision alert"
                          (plan/build sources)))))

;; ---------------------------------------------------------------- hooks

(deftest hook-docstrings-and-page-mapping
  (let [plan (plan/build base-sources)
        hooks (ns-plan plan "mantine.hooks")]
    (testing "described hook links to its own kebab page"
      (is (str/includes? (:docstring (def-plan hooks "use-disclosure"))
                         "useDisclosure — Manages boolean state"))
      (is (str/includes? (:docstring (def-plan hooks "use-disclosure"))
                         "https://mantine.dev/hooks/use-disclosure")))
    (testing "Companion hook maps to its shared docs page via hook-docs-page"
      (is (str/includes? (:docstring (def-plan hooks "use-mouse-position"))
                         "https://mantine.dev/hooks/use-mouse")))
    (testing "hooks ns requires: cljs refer + clj-only factory"
      (is (= [["@mantine/hooks" :refer '[useDisclosure useMousePosition]]]
             (get-in hooks [:requires :cljs])))
      (is (= '[[mantine.impl.factory :as f]] (get-in hooks [:requires :clj]))))))

;; ---------------------------------------------------------------- supplements

(def core-supplement
  (str "(ns mantine.supplements.core\n"
       "  \"Supplement.\"\n"
       "  (:require\n"
       "   [mantine.impl.factory :as f]\n"
       "   #?@(:cljs [[\"@mantine/core\" :refer [Box]]\n"
       "              [mantine.impl.props :as p]])))\n"
       "\n"
       "(declare button)\n"
       "\n"
       "(def box\n"
       "  \"Box — say \\\"hi\\\".\"\n"
       "  #?(:cljs (f/factory Box)\n"
       "     :clj (f/not-implemented \"mantine.core/box\")))\n"
       "\n"
       "(defn frob\n"
       "  \"Frobs a button.\"\n"
       "  []\n"
       "  button)\n"))

(deftest supplement-parsing-and-hoisting
  (let [sources (assoc base-sources :supplements {"core" core-supplement})
        plan (plan/build sources)
        core (ns-plan plan "mantine.core")]
    (testing "satisfied declare dropped from the body (with its trailing blank line)"
      (is (not (str/includes? (get-in core [:supplement :body]) "(declare button)")))
      (is (str/starts-with? (get-in core [:supplement :body]) "(def box")))
    (testing "supplement requires merged into the ns requires"
      (is (= [["@mantine/core" :refer '[Alert Button Chart List Slider Box]]
              '[mantine.impl.props :as p]]
             (get-in core [:requires :cljs])))
      (is (= '[[mantine.impl.factory :as f]] (get-in core [:requires :common]))))
    (testing "supplement def names feed refer-clojure excludes"
      (is (= ["list"] (:refer-clojure-exclude core))))
    (testing "ns docstring flags the hoisted supplement"
      (is (str/includes? (:docstring core) "supplement hoisted")))))

(deftest supplement-def-collision-throws
  (let [supplement (str "(ns mantine.supplements.core\n  (:require [mantine.impl.factory :as f]))\n"
                        "\n(def button \"dup\" nil)\n")
        sources (assoc base-sources :supplements {"core" supplement})]
    (is (thrown-with-msg? Exception #"supplement def\(s\) button collide"
                          (plan/build sources)))))

(deftest supplement-only-package
  (let [supplement (str "(ns mantine.supplements.form\n"
                        "  (:require\n"
                        "   [mantine.impl.factory :as f]\n"
                        "   #?@(:cljs [[\"@mantine/form\" :refer [useForm]]])))\n"
                        "\n"
                        "(def use-form\n  \"useForm.\"\n  #?(:cljs useForm\n     :clj (f/not-implemented \"mantine.form/use-form\")))\n")
        sources (-> base-sources
                    (assoc-in [:scope :supplement-only-packages] #{"@mantine/form"})
                    (assoc :supplements {"form" supplement})
                    (update :exports assoc "@mantine/form" ["useForm"]))
        plan (plan/build sources)
        form (ns-plan plan "mantine.form")]
    (testing "zero generated defs; body is entirely the hoisted supplement"
      (is (= [] (:defs form)))
      (is (str/starts-with? (get-in form [:supplement :body]) "(def use-form")))
    (testing "the supplement's :refer survives the merge with the empty base refer"
      (is (= [["@mantine/form" :refer '[useForm]]] (get-in form [:requires :cljs]))))))

;; ---------------------------------------------------------------- emit-ns

(deftest emit-escapes-docstrings
  (let [sources (assoc-in base-sources
                          [:component-docs "Button" :description]
                          "A \"quoted\" \\ description")
        plan (plan/build sources)
        core (ns-plan plan "mantine.core")
        {:keys [file text]} (plan/emit-ns core)]
    (testing "docstring is plain in the plan, escaped only in the emitted text"
      (is (str/includes? (:docstring (def-plan core "button")) "A \"quoted\" \\ description"))
      (is (str/includes? text "A \\\"quoted\\\" \\\\ description")))
    (is (= "src/main/mantine/core.cljc" file))
    (testing "header stamps the sources' mantine-version"
      (is (str/starts-with? text ";; GENERATED by `bb generate` from the committed codegen inputs (Mantine 9.9.9) — DO NOT EDIT.\n")))
    (testing "controlled component emits the f/controlled wrapper"
      (is (str/includes? text "#?(:cljs (f/factory (f/controlled Slider))")))
    (testing "prop lines squash whitespace and carry REQUIRED/default"
      (is (str/includes? text "color (string) REQUIRED [default blue] — Button color")))))

(deftest emit-splices-supplement-verbatim
  (let [sources (assoc base-sources :supplements {"core" core-supplement})
        plan (plan/build sources)
        text (:text (plan/emit-ns (ns-plan plan "mantine.core")))]
    (testing "already-escaped supplement text is NOT re-escaped"
      (is (str/includes? text "say \\\"hi\\\"")))
    (is (str/includes? text ";; ---- hoisted from codegen/supplements/core.cljc ----"))))

(deftest emit-hook-def-shape
  (let [plan (plan/build base-sources)
        text (:text (plan/emit-ns (ns-plan plan "mantine.hooks")))]
    (is (str/includes? text "(def use-disclosure\n"))
    (is (str/includes? text "#?(:cljs useDisclosure\n     :clj (f/not-implemented \"mantine.hooks/use-disclosure\")))"))
    (testing "clj-only require block for the hooks ns"
      (is (str/includes? text "#?@(:clj [[mantine.impl.factory :as f]])")))))

;; ----------------------------------------------------------------

(let [{:keys [fail error]} (run-tests 'plan-test)]
  (when (pos? (+ fail error))
    (System/exit 1)))
