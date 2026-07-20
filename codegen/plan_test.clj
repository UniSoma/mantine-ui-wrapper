;; Plan-level fixture tests (ADR 0004): exercise plan/build and plan/emit-ns on
;; hand-built `sources` maps — no Node, no writes. The plan IS the test surface.
;;
;; Run with: bb plan-test
(ns plan-test
  (:require [anchor]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing run-tests]]
            [plan]
            [release-check]))

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
   :util-docs {"randomId" {:desc "Generates a random id" :page "guides/functions-reference"}
               "getHotkeyHandler" {:desc "Builds an onKeyDown handler" :page "hooks/use-hotkeys"}}
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

;; Add a barrel export to both the observed :exports and the explicit :hooks scope
;; set — the two must agree for a name to flow through hooks-ns-plan.
(defn- add-barrel [sources nm]
  (-> sources
      (update-in [:exports "@mantine/hooks"] conj nm)
      (update-in [:scope :hooks] conj nm)))

;; ---------------------------------------------------------------- anchor

(deftest anchor-version-from-uniform-pins
  (let [pins {"@mantine/core" "9.4.1" "@mantine/hooks" "9.4.1" "@mantine/charts" "9.4.1"}]
    (is (= "9.4.1" (anchor/anchor-version pins)))))

(deftest anchor-version-disagreeing-pins-throws
  (let [pins {"@mantine/core" "9.4.1" "@mantine/hooks" "9.4.2"}]
    (is (thrown-with-msg? Exception #"pins disagree"
                          (anchor/anchor-version pins)))))

;; ---------------------------------------------------------------- release-check

(def rc-ok
  {:anchor "9.4.1"
   :pins {"@mantine/core" "9.4.1" "@mantine/hooks" "9.4.1"}
   :deps-ranges {"@mantine/core" "^9.4.1" "@mantine/hooks" "^9.4.1"}
   :build-version "9.4.1.0-SNAPSHOT"
   :witness "9.4.1"})

(deftest release-check-clean-input-has-no-violations
  (is (empty? (release-check/violations rc-ok))))

(deftest release-check-flags-stale-deps-range
  (let [probs (release-check/violations (assoc-in rc-ok [:deps-ranges "@mantine/core"] "^9.4.0"))]
    (is (some #(str/includes? % "@mantine/core") probs))
    (is (some #(str/includes? % "^9.4.0") probs))))

(deftest release-check-flags-stale-build-prefix
  (let [probs (release-check/violations (assoc rc-ok :build-version "9.4.0.2-SNAPSHOT"))]
    (is (some #(str/includes? % "build.clj") probs))
    (is (some #(str/includes? % "9.4.0") probs))))

(deftest release-check-witness-matching-anchor-has-no-violation
  (is (not-any? #(str/includes? % "witness") (release-check/violations rc-ok))))

(deftest release-check-flags-stale-witness
  (let [probs (release-check/violations (assoc rc-ok :witness "9.4.0"))]
    (is (some #(str/includes? % "witness") probs))
    (is (some #(str/includes? % "9.4.0") probs))))

(deftest prose-renderings-extracts-embedded-coordinates-only
  ;; Hermetic mechanics test: an arbitrary version (NOT the anchor) proves the extractor
  ;; is anchor-agnostic — it pulls embedded coordinates out of text and nothing else.
  (let [text (str "npm install @mantine/core@^1.2.3 @mantine/dates@^1.2.3\n"
                  "{:mvn/version} scheme 1.2.3.0 → 1.2.3.1 examples stay illustrative\n"
                  "io.github.unisoma/mantine-ui-wrapper {:mvn/version \"1.2.3.0-SNAPSHOT\"}")
        found (release-check/prose-renderings "README.md" text)]
    ;; two npm floors + one mvn coord; the bare scheme examples are NOT matched
    (is (= 3 (count found)))
    (is (= #{:npm-floor :mvn-coord} (set (map :kind found))))
    (is (every? #(= "1.2.3" (:version %)) found))))

(deftest release-check-flags-stale-prose-rendering
  (let [probs (release-check/violations
               (assoc rc-ok :prose [{:file "README.md" :kind :npm-floor :version "9.4.0"}]))]
    (is (some #(str/includes? % "README.md") probs))
    (is (some #(str/includes? % "9.4.0") probs))))

(deftest release-check-clean-prose-has-no-violation
  (is (empty? (release-check/violations
               (assoc rc-ok :prose [{:file "README.md" :kind :npm-floor :version "9.4.1"}
                                    {:file "docs/release.md" :kind :mvn-coord :version "9.4.1"}])))))

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
    (testing "a non-use* barrel export is now a generated :util def, not a skip"
      (is (nil? (skip "randomId")))
      (let [rid (def-plan (ns-plan plan "mantine.hooks") "random-id")]
        (is (= :util (:kind rid)))
        (is (= "randomId" (:js-name rid)))))))

(deftest scoped-name-absent-from-barrel-is-skipped
  (let [plan (plan/build (update-in base-sources [:scope :hooks] conj "useNotExported"))
        skip (fn [nm] (some #(when (= nm (:js-name %)) %) (:skipped plan)))]
    (is (= :barrel (:kind (skip "useNotExported"))))
    (is (= "not an export of @mantine/hooks" (:reason (skip "useNotExported"))))))

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
    (testing "hooks ns requires: cljs refer (hooks + utils) + clj-only factory"
      (is (= [["@mantine/hooks" :refer '[randomId useDisclosure useMousePosition]]]
             (get-in hooks [:requires :cljs])))
      (is (= '[[mantine.impl.factory :as f]] (get-in hooks [:requires :clj]))))))

(deftest util-docstrings-and-routing
  (let [plan (plan/build base-sources)
        hooks (ns-plan plan "mantine.hooks")]
    (testing "a functions-reference util links to the guide with a stripped anchor"
      (let [ds (:docstring (def-plan hooks "random-id"))]
        (is (str/includes? ds "randomId — Generates a random id"))
        (is (str/includes? ds "https://mantine.dev/guides/functions-reference/#randomid"))))
    (testing "a util documented on a sibling hook page links to that page"
      (let [plan (plan/build (add-barrel base-sources "getHotkeyHandler"))
            ds (:docstring (def-plan (ns-plan plan "mantine.hooks") "get-hotkey-handler"))]
        (is (str/includes? ds "https://mantine.dev/hooks/use-hotkeys/#gethotkeyhandler"))))))

(deftest util-missing-docs-fails-build
  (let [sources (add-barrel base-sources "mergeRefs")]
    (is (thrown-with-msg? Exception #"mergeRefs has no codegen/input/util-docs.edn entry"
                          (plan/build sources)))))

(deftest range-util-is-refer-clojure-excluded
  (let [sources (-> (add-barrel base-sources "range")
                    (assoc-in [:util-docs "range"] {:desc "Range" :page "guides/functions-reference"}))
        hooks (ns-plan (plan/build sources) "mantine.hooks")]
    (is (= :util (:kind (def-plan hooks "range"))))
    (is (some #{"range"} (:refer-clojure-exclude hooks)))))

(deftest single-word-util-reached-via-alias
  ;; clamp kebabs to itself; :refer-ing AND def-ing "clamp" would shadow the refer,
  ;; so it is reached via the hooks-js alias and dropped from the :refer list.
  (let [sources (-> (add-barrel base-sources "clamp")
                    (assoc-in [:util-docs "clamp"] {:desc "Clamp" :page "guides/functions-reference"}))
        plan (plan/build sources)
        hooks (ns-plan plan "mantine.hooks")
        [[_lib & clause]] (get-in hooks [:requires :cljs])
        opts (apply hash-map clause)]
    (testing "clamp is aliased, not referred"
      (is (= 'hooks-js (:as opts)))
      (is (not (some #{'clamp} (:refer opts))))
      (is (= "hooks-js/clamp" (:cljs-ref (def-plan hooks "clamp")))))
    (testing "referred hooks keep the bare-symbol RHS (no alias when nothing collides)"
      (let [plain (ns-plan (plan/build base-sources) "mantine.hooks")
            [[_ & c]] (get-in plain [:requires :cljs])]
        (is (not (some #{:as} c)))))
    (testing "emitted text uses the alias-qualified RHS"
      (is (str/includes? (:text (plan/emit-ns hooks))
                         "#?(:cljs hooks-js/clamp\n     :clj (f/not-implemented \"mantine.hooks/clamp\")))")))))

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
                        "   #?@(:clj [[mantine.impl.factory :as f]])\n"
                        "   #?@(:cljs [[\"@mantine/form\" :as mf]])))\n"
                        "\n"
                        "(def use-form\n  \"useForm.\"\n  #?(:cljs mf/useForm\n     :clj (f/not-implemented \"mantine.form/use-form\")))\n")
        sources (-> base-sources
                    (assoc-in [:scope :supplement-only-packages] #{"@mantine/form"})
                    (assoc :supplements {"form" supplement})
                    (update :exports assoc "@mantine/form" ["useForm"]))
        plan (plan/build sources)
        form (ns-plan plan "mantine.form")]
    (testing "zero generated defs; body is entirely the hoisted supplement"
      (is (= [] (:defs form)))
      (is (str/starts-with? (get-in form [:supplement :body]) "(def use-form")))
    (testing "requires come entirely from the supplement — no empty base refer, no injected factory"
      (is (= [["@mantine/form" :as 'mf]] (get-in form [:requires :cljs])))
      (is (= '[[mantine.impl.factory :as f]] (get-in form [:requires :clj])))
      (is (= [] (get-in form [:requires :common]))))))

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
    (testing "prop lines squash whitespace and emit a Markdown bullet with type/required/default"
      (is (str/includes? text "- **color** `string` **(required)** _(default `blue`)_ — Button color")))
    (testing "the Props header is followed by a blank line so cljdoc parses the bullet list"
      (is (str/includes? text "Props (docgen 9.9.9):\n\n- **color**")))))

(deftest prop-description-is-markdown-safe
  (testing "<code>…</code> becomes a backtick span; a stray literal backtick is escaped, not left to open a span (ADR 0007)"
    (let [sources (assoc-in base-sources
                            [:docgen "Button" "props" "color" "description"]
                            "Pass <code>#js {}</code> for a stray ` backtick")
          plan (plan/build sources)
          core (ns-plan plan "mantine.core")
          {:keys [text]} (plan/emit-ns core)]
      ;; emit-ns's `esc` doubles the backslash, so the escaped stray backtick
      ;; (`\`` in the plan) reads as `\\`` in the emitted source literal.
      (is (str/includes? text "Pass `#js {}` for a stray \\\\` backtick")))))

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
    (testing "a util emits the same raw-passthrough alias shape"
      (is (str/includes? text "(def random-id\n"))
      (is (str/includes? text "#?(:cljs randomId\n     :clj (f/not-implemented \"mantine.hooks/random-id\")))")))
    (testing "clj-only require block for the hooks ns"
      (is (str/includes? text "#?@(:clj [[mantine.impl.factory :as f]])")))))

;; ----------------------------------------------------------------

(let [{:keys [fail error]} (run-tests 'plan-test)]
  (when (pos? (+ fail error))
    (System/exit 1)))
