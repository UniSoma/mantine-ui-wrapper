(ns mantine.interop-test
  "Behavioral lock on mantine.interop/raw-component and the factory tag it reads
  (mnt-01ky00nnhfcv). raw-component must reach the underlying Mantine component
  through any wrapper — including the controlled input shim — and console.error +
  return nil on a non-wrapper argument."
  (:require [cljs.test :refer-macros [deftest is testing]]
            [mantine.impl.factory :as f]
            [mantine.interop :as mi]))

(deftest raw-component-returns-the-tagged-component
  (let [component #js {:name "Fake"}
        wrapper (f/factory component)]
    (is (identical? component (mi/raw-component wrapper))
        "factory tags the wrapper with its underlying component")))

(deftest raw-component-reads-through-the-controlled-shim
  (let [component #js {:name "Input"}
        wrapper (f/factory (f/controlled component))]
    (is (identical? component (mi/raw-component wrapper))
        "returns the real Mantine component, not the controlled shim")))

(deftest raw-component-on-non-wrapper-errors-and-returns-nil
  (testing "a plain fn, map, nil, and string are all misuse"
    (let [calls (atom [])
          orig js/console.error]
      (set! js/console.error (fn [& args] (swap! calls conj (vec args))))
      (try
        (is (nil? (mi/raw-component (fn [] :not-a-wrapper))))
        (is (nil? (mi/raw-component {:a 1})))
        (is (nil? (mi/raw-component nil)))
        (is (nil? (mi/raw-component "x")))
        (is (= 4 (count @calls)) "console.error fires once per misused value")
        (finally (set! js/console.error orig))))))
