(ns mantine.interop
  "Handwritten interop escape hatches (not generated, no supplement) for reaching
  past the factories to the underlying Mantine components when a factory does not
  cover some interop, e.g. passing a component to a slot prop like `:component`."
  (:require [mantine.impl.factory :as f]
            #?(:cljs [mantine.impl.props :as p])))

(def no-convert
  "Tag a value so the props converter passes it through untouched (kept as-is
  CLJS) instead of deep-converting it. Honored at any depth. The tag is a VALUE,
  not metadata, so it survives merge/select-keys/map rebuilds. The general opt-out
  for raw-CLJS payloads; :inner-props gets this treatment automatically."
  #?(:cljs p/no-convert
     :clj (f/not-implemented "mantine.interop/no-convert")))

(def raw-component
  "Given a factory from any generated namespace (e.g. `mc/button`,
  `mantine.dates/date-picker`), returns the underlying raw Mantine React component.
  Reads through the controlled-input shim, so a curated input (e.g. `mc/text-input`)
  returns the true Mantine component, never our shim.

  Main use case: passing the raw component to slots such as the `:component` prop, and
  any interop a factory does not cover. Cannot resolve components a wrapper does not
  cover; backfill those in the package's supplement.

  On misuse (the argument is not a factory) `js/console.error`s the offending value
  and returns nil."
  #?(:cljs (fn [wrapper]
             (if-some [component (f/component-of wrapper)]
               component
               (do (js/console.error
                    "mantine.interop/raw-component: not a Mantine wrapper —" wrapper)
                   nil)))
     :clj (f/not-implemented "mantine.interop/raw-component")))
