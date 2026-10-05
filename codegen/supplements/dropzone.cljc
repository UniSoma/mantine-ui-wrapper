#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns mantine.supplements.dropzone
  "Hand-written supplement the generator HOISTS into the generated
  mantine.dropzone ns. A committed generator INPUT: it compiles for editor and
  clj-kondo support but never ships as-is. The generator merges its :require
  entries into the generated ns and appends its top-level forms after the
  generated defs.

  Backfills the Dropzone compound parts docgen omits (Dropzone.Accept/Idle/Reject).
  The compound-part coverage check in scripts/coverage-check.clj is the source of
  truth for this list. It fails if any wrapped component grows a static part that
  is neither generated from docgen nor defined here."
  (:require
   [mantine.impl.factory :as f]
   #?@(:cljs [["@mantine/dropzone" :refer [DropzoneAccept DropzoneIdle DropzoneReject]]])))

(def dropzone-accept
  "Dropzone.Accept — compound part of Dropzone (docgen omits it); renders its
  children only while dragged files are accepted. Optional leading props map;
  remaining args are children."
  #?(:cljs (f/factory DropzoneAccept)
     :clj (f/not-implemented "mantine.dropzone/dropzone-accept")))

(def dropzone-idle
  "Dropzone.Idle — compound part of Dropzone (docgen omits it); renders its
  children only while no drag is in progress. Optional leading props map;
  remaining args are children."
  #?(:cljs (f/factory DropzoneIdle)
     :clj (f/not-implemented "mantine.dropzone/dropzone-idle")))

(def dropzone-reject
  "Dropzone.Reject — compound part of Dropzone (docgen omits it); renders its
  children only while dragged files are rejected. Optional leading props map;
  remaining args are children."
  #?(:cljs (f/factory DropzoneReject)
     :clj (f/not-implemented "mantine.dropzone/dropzone-reject")))
