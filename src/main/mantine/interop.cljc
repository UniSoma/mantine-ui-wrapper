(ns mantine.interop
  "Handwritten interop escape hatches (not generated, no supplement). Helpers for
  reaching past the wrappers to the underlying Mantine primitives when a wrapper
  does not cover some interop — e.g. passing a component to a slot prop like
  `:component`."
  (:require [mantine.impl.factory :as f]))

(def raw-component
  "Given a wrapper var from any generated namespace (e.g. `mc/button`,
  `mantine.dates/date-picker`), returns the underlying raw Mantine React component.
  Reads through the controlled-input shim, so a curated input (e.g. `mc/text-input`)
  returns the true Mantine component, never our shim.

  Main use case: passing the raw component to slots such as the `:component` prop, and
  any interop a wrapper does not cover. Cannot resolve components a wrapper does not
  cover — supplement the wrapper for those.

  On misuse (the argument is not one of our wrappers) `js/console.error`s the offending
  value and returns nil."
  #?(:cljs (fn [wrapper]
             (if-some [component (f/component-of wrapper)]
               component
               (do (js/console.error
                    "mantine.interop/raw-component: not a Mantine wrapper —" wrapper)
                   nil)))
     :clj (f/not-implemented "mantine.interop/raw-component")))
