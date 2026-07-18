#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns mantine.supplements.modals
  "Hand-written supplement HOISTED by the generator into the generated
  mantine.modals ns. Committed generator INPUT — compilable for editor/
  clj-kondo support, but never shipped as-is: its :require entries are merged into
  the generated ns and its top-level forms are appended after the codegen'd defs."
  (:require
   ;; f is :clj-branch-only HERE, but the generated ns also uses it on :cljs
   ;; (f/factory for its component defs) — so the require stays unconditional.
   #_{:clj-kondo/ignore [:unused-namespace]}
   [mantine.impl.factory :as f]
   #?@(:cljs [["@mantine/modals" :refer [openModal openConfirmModal openContextModal
                                         closeModal closeAllModals updateModal
                                         updateContextModal useModals]]
              [mantine.impl.props :as p]])))

(declare modals-provider)

(def provider
  "Alias for `modals-provider` — the ModalsProvider that must be mounted once
  (inside MantineProvider) for the imperative modal fns to display anything."
  modals-provider)

(defn open
  "Open a modal. The options map goes through the standard props converter
  (:title, :children, :size, :centered, ...); nested *Props supply JS-shaped
  values (shallow passthrough). Returns the modal id (raw string)."
  [data]
  #?(:cljs (openModal (p/convert data))
     :clj ((f/not-implemented "mantine.modals/open") data)))

(defn open-confirm-modal
  "Open a confirmation modal. Options map converted like `open`; :labels and
  :on-confirm / :on-cancel supply the confirm/cancel wiring. Returns the modal id."
  [data]
  #?(:cljs (openConfirmModal (p/convert data))
     :clj ((f/not-implemented "mantine.modals/open-confirm-modal") data)))

(defn open-context-modal
  "Open a context modal registered on the provider. Options map converted like
  `open`; :modal is the registry key (passed verbatim) and :inner-props is passed
  RAW to the registered component. Returns the modal id."
  [data]
  #?(:cljs (openContextModal (p/convert data))
     :clj ((f/not-implemented "mantine.modals/open-context-modal") data)))

(defn close
  "Close the modal with the given id (raw string in and out)."
  [id]
  #?(:cljs (closeModal id)
     :clj ((f/not-implemented "mantine.modals/close") id)))

(defn close-all
  "Close all open modals."
  []
  #?(:cljs (closeAllModals)
     :clj ((f/not-implemented "mantine.modals/close-all"))))

(defn update-modal
  "Update an open modal; matched by :id in the options map (converted like `open`)."
  [data]
  #?(:cljs (updateModal (p/convert data))
     :clj ((f/not-implemented "mantine.modals/update-modal") data)))

(defn update-context-modal
  "Update an open context modal; matched by :id in the options map (converted like
  `open`, with :inner-props passed RAW)."
  [data]
  #?(:cljs (updateContextModal (p/convert data))
     :clj ((f/not-implemented "mantine.modals/update-context-modal") data)))

(def use-modals
  "Reactive hook over the default modals store. Raw passthrough: returns the raw
  JS ModalsContext value (read via interop: .-modals, .-openModal, ...)."
  #?(:cljs useModals
     :clj (f/not-implemented "mantine.modals/use-modals")))
