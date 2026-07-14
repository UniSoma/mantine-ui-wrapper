(ns mantine.supplements.spotlight
  "Hand-written supplement HOISTED by the generator into the generated
  mantine.spotlight ns. Committed generator INPUT — compilable for editor/
  clj-kondo support, but never shipped as-is: its :require entries are merged into
  the generated ns and its top-level forms are appended after the codegen'd defs."
  (:require
   [mantine.impl.factory :as f]
   #?@(:cljs [["@mantine/spotlight" :refer [openSpotlight closeSpotlight
                                            toggleSpotlight useSpotlight]]])))

(defn open
  "Open the spotlight command palette (single default instance)."
  []
  #?(:cljs (openSpotlight)
     :clj ((f/not-implemented "mantine.spotlight/open"))))

(defn close
  "Close the spotlight command palette."
  []
  #?(:cljs (closeSpotlight)
     :clj ((f/not-implemented "mantine.spotlight/close"))))

(defn toggle
  "Toggle the spotlight command palette open/closed."
  []
  #?(:cljs (toggleSpotlight)
     :clj ((f/not-implemented "mantine.spotlight/toggle"))))

(def use-spotlight
  "Reactive hook over the default spotlight store. Raw passthrough: returns the raw
  JS spotlight store value (read via interop: .-opened, .-open, ...)."
  #?(:cljs useSpotlight
     :clj (f/not-implemented "mantine.spotlight/use-spotlight")))
