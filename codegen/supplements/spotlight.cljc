#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns mantine.supplements.spotlight
  "Hand-written supplement the generator HOISTS into the generated
  mantine.spotlight ns. A committed generator INPUT: it compiles for editor and
  clj-kondo support but never ships as-is. The generator merges its :require
  entries into the generated ns and appends its top-level forms after the
  generated defs."
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
