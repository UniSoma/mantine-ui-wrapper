(ns mantine.supplements.spotlight
  "Hand-written supplement HOISTED by the generator into the generated
  mantine.spotlight ns. Committed generator INPUT — compilable for editor/
  clj-kondo support, but never shipped as-is: its :require entries are merged into
  the generated ns and its top-level forms are appended after the codegen'd defs."
  (:require
   [mantine.impl.factory :as f]
   #?@(:cljs [["@mantine/spotlight" :refer [openSpotlight closeSpotlight
                                            toggleSpotlight useSpotlight
                                            SpotlightActionsList SpotlightEmpty
                                            SpotlightFooter]]])))

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

;; ---- compound parts (dot-notation subcomponents docgen omits) ----

(def spotlight-actions-list
  "Spotlight.ActionsList — compound part of Spotlight (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory SpotlightActionsList)
     :clj (f/not-implemented "mantine.spotlight/spotlight-actions-list")))

(def spotlight-empty
  "Spotlight.Empty — compound part of Spotlight (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory SpotlightEmpty)
     :clj (f/not-implemented "mantine.spotlight/spotlight-empty")))

(def spotlight-footer
  "Spotlight.Footer — compound part of Spotlight (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory SpotlightFooter)
     :clj (f/not-implemented "mantine.spotlight/spotlight-footer")))
