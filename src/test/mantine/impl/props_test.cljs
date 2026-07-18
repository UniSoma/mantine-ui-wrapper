(ns mantine.impl.props-test
  "Behavioral lock on the prop converter's compatibility invariants (mnt-01kxr60b7xe8).
  These make consumer migration a namespace rename, so they must not silently regress.
  See the mantine.impl.props docstring + gaps.md for the semantics each test pins."
  (:require [cljs.test :refer-macros [deftest is testing]]
            [goog.object :as gobj]
            [mantine.impl.props :as p]))

;; ---------------------------------------------------------------------------
;; Keys: camelCase passthrough + kebab->camel, with data-*/aria-*/--* exempt.
;; ---------------------------------------------------------------------------

(deftest camelcase-props-pass-through-as-identity
  (testing "dash-free keywords are untouched (existing camelCase call sites work)"
    (let [f (fn [])
          o (p/convert {:onClick f :withBorder true :leftSection "x"})]
      (is (identical? f (gobj/get o "onClick")) "fn value survives by identity")
      (is (true? (gobj/get o "withBorder")))
      (is (= "x" (gobj/get o "leftSection")))))
  (testing "kebab-case keywords are camelized"
    (let [o (p/convert {:label-position "left"})]
      (is (= "left" (gobj/get o "labelPosition")))
      (is (nil? (gobj/get o "label-position"))))))

(deftest data-aria-css-var-keys-bypass-camelization
  (let [o (p/convert {:data-foo-bar "d" :aria-label-thing "a" :--my-var "v"})]
    (is (= "d" (gobj/get o "data-foo-bar")) "data-* verbatim")
    (is (= "a" (gobj/get o "aria-label-thing")) "aria-* verbatim")
    (is (= "v" (gobj/get o "--my-var")) "--* verbatim")
    (is (nil? (gobj/get o "dataFooBar")))))

;; ---------------------------------------------------------------------------
;; Class: :class alias, vector space-join, :class + :className merge.
;; ---------------------------------------------------------------------------

(deftest class-aliases-classname
  (let [o (p/convert {:class "foo"})]
    (is (= "foo" (gobj/get o "className")))
    (is (nil? (gobj/get o "class")))))

(deftest class-vectors-space-join-dropping-nil-and-false
  (let [o (p/convert {:className ["a" nil "b" false "c"]})]
    (is (= "a b c" (gobj/get o "className")))))

(deftest class-and-classname-merge-when-both-present
  (let [o (p/convert {:class "a" :className "b"})]
    (is (= "a b" (gobj/get o "className")))))

;; ---------------------------------------------------------------------------
;; Children: prune nil, flatten nested seqs, pass through everything else.
;; ---------------------------------------------------------------------------

(deftest children-prune-nil
  (is (= ["a" "b"] (vec (p/convert-children ["a" nil "b"])))))

(deftest children-flatten-nested-seqs
  (testing "lazy seq from `for` is flattened alongside scalars"
    (is (= ["a" 0 1 "b"]
           (vec (p/convert-children ["a" (for [i (range 2)] i) "b"])))))
  (testing "deep nesting is flattened recursively"
    (is (= ["x"] (vec (p/convert-children [[["x"]]]))))))

(deftest children-pass-through-scalars-elements-and-render-fns
  (let [f (fn [])
        out (vec (p/convert-children ["s" 42 f]))]
    (is (= "s" (nth out 0)))
    (is (= 42 (nth out 1)))
    (is (identical? f (nth out 2)) "render-prop fn survives by identity")))

;; ---------------------------------------------------------------------------
;; Deep-convert set: :style / :styles / :classNames / :vars.
;; ---------------------------------------------------------------------------

(deftest style-keys-camelized-css-vars-verbatim-values-passthrough
  (let [o (p/convert {:style {:font-weight 900 :--my-var "on"}})
        s (gobj/get o "style")]
    (is (= 900 (gobj/get s "fontWeight")) "value stays numeric (React appends px)")
    (is (= "on" (gobj/get s "--my-var")) "--* key verbatim")
    (is (nil? (gobj/get s "font-weight")))))

(deftest styles-selector-map-camelizes-outer-and-inner
  (let [o (p/convert {:styles {:root {:font-weight 900}}})]
    (is (= 900 (gobj/getValueByKeys o "styles" "root" "fontWeight")))))

(deftest styles-function-values-pass-through
  (testing "function-form Styles API value survives unwrapped"
    (let [f (fn [_] #js {})
          o (p/convert {:styles f})]
      (is (identical? f (gobj/get o "styles"))))))

(deftest vars-selector-map-camelizes
  (let [o (p/convert {:vars {:root {:some-var "z"}}})]
    (is (= "z" (gobj/getValueByKeys o "vars" "root" "someVar")))))

(deftest classnames-map-camelizes-outer-and-joins-members
  (testing ":classNames selector map: outer keys camelized, members class-joined"
    (let [o (p/convert {:classNames {:root ["a" nil "b"]}})]
      (is (= "a b" (gobj/getValueByKeys o "classNames" "root")))))
  (testing ":class-names kebab alias resolves to classNames"
    (let [o (p/convert {:class-names {:input "x"}})]
      (is (= "x" (gobj/getValueByKeys o "classNames" "input"))))))

;; ---------------------------------------------------------------------------
;; Deep-by-default (ADR 0006): nested maps and vectors-of-maps convert at every
;; depth; :inner-props denylisted; `raw` wrapper opts any value out.
;; ---------------------------------------------------------------------------

(deftest nested-maps-convert-recursively
  (let [f (fn [])
        o (p/convert {:confirm-props {:color "red" :left-section "!" :on-click f}})]
    (is (= "red" (gobj/getValueByKeys o "confirmProps" "color")))
    (is (= "!" (gobj/getValueByKeys o "confirmProps" "leftSection")) "kebab->camel at depth")
    (is (identical? f (gobj/getValueByKeys o "confirmProps" "onClick")) "fn survives at depth")
    (is (nil? (gobj/getValueByKeys o "confirmProps" "left-section")))))

(deftest nested-leaf-handling-applies-at-depth
  (let [o (p/convert {:confirm-props {:class ["a" nil "b"]
                                      :style {:font-weight 900}}})]
    (is (= "a b" (gobj/getValueByKeys o "confirmProps" "className")) ":class leaf at depth")
    (is (= 900 (gobj/getValueByKeys o "confirmProps" "style" "fontWeight")) ":style leaf at depth")))

(deftest vectors-of-maps-convert-recursively
  (let [o (p/convert {:actions [{:id "a" :left-section "x"} "divider"]})
        arr (gobj/get o "actions")]
    (is (array? arr) "vector value becomes a JS array")
    (is (= "x" (gobj/get (aget arr 0) "leftSection")) "map members convert (kebab->camel at depth)")
    (is (= "divider" (aget arr 1)) "non-map members pass through")))

(deftest inner-props-key-camelizes-but-value-passes-raw
  (let [f (fn [])
        payload {:app/id 7 :on-done f}
        o (p/convert {:inner-props payload})]
    (is (identical? payload (gobj/get o "innerProps")) "value is the untouched CLJS map")
    (is (= 7 (:app/id (gobj/get o "innerProps"))) "qualified keyword lookup still works")))

(deftest raw-wrapper-skips-conversion-at-any-depth
  (let [payload {:qualified/key 1}
        o (p/convert {:payload (p/raw payload)
                      :nested {:inner (p/raw payload)}})]
    (is (identical? payload (gobj/get o "payload")) "top-level raw value untouched")
    (is (identical? payload (gobj/getValueByKeys o "nested" "inner")) "raw honored below top level")))

(deftest raw-wrapper-survives-merge-and-select-keys
  (let [payload {:qualified/key 1}
        assembled (-> (merge {:payload (p/raw payload)} {:other 2})
                      (select-keys [:payload]))
        o (p/convert assembled)]
    (is (identical? payload (gobj/get o "payload"))
        "tag survives a map pipeline (wrapper value, not metadata)")))

(deftest escape-hatch-merges-last-inside-nested-maps
  (let [o (p/convert {:confirm-props {:color "red" :& #js {:color "blue"}}})]
    (is (= "blue" (gobj/getValueByKeys o "confirmProps" "color")) ":& wins at depth too")))

;; ---------------------------------------------------------------------------
;; Escape hatch :& — merged LAST, hyphens preserved, wins over normal conversion.
;; ---------------------------------------------------------------------------

(deftest escape-hatch-raw-js-merges-in
  (let [o (p/convert {:foo 1 :& #js {:bar 2}})]
    (is (= 1 (gobj/get o "foo")))
    (is (= 2 (gobj/get o "bar")))))

(deftest escape-hatch-clj-map-clj->js-preserves-hyphens
  (let [o (p/convert {:& {:my-key "v"}})]
    (is (= "v" (gobj/get o "my-key")) "hyphenated key NOT camelized (plain clj->js)")
    (is (nil? (gobj/get o "myKey")))))

(deftest escape-hatch-merges-last-and-overrides
  (let [o (p/convert {:color "red" :& #js {:color "blue"}})]
    (is (= "blue" (gobj/get o "color")) "escape hatch wins over normal conversion")))

;; ---------------------------------------------------------------------------
;; nil handling.
;; ---------------------------------------------------------------------------

(deftest nil-props-yield-empty-object
  (let [o (p/convert nil)]
    (is (some? o))
    (is (empty? (js->clj o)))))
