#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns mantine.supplements.form
  "Hand-written supplement HOISTED by the generator into the generated
  mantine.form ns. Committed generator INPUT — compilable for editor/
  clj-kondo support, but never shipped as-is: its :require entries are merged into
  the generated ns and its top-level forms are appended after the codegen'd defs.

  @mantine/form has no docgen entries, so mantine.form is a supplement-only package
  (see codegen/scope.edn :supplement-only-packages). Everything here is a RAW
  passthrough of the JS export: no js<->clj conversion accessors — consumers layer
  interop themselves. The package is aliased (not :refer'd) so the `matches`
  validator doesn't clash with the `matches` def below."
  (:require
   #?@(:clj [[mantine.impl.factory :as f]])
   #?@(:cljs [["@mantine/form" :as mf]])))

(def use-form
  "useForm — the form hook. Raw passthrough: pass JS-shaped options (#js {...});
  returns the raw JS form object (read/call its members via interop under :advanced,
  e.g. (.getInputProps form \"name\"), (.-values form), (.onSubmit form handler))."
  #?(:cljs mf/useForm
     :clj (f/not-implemented "mantine.form/use-form")))

;; ---- validators (pure functions, raw passthrough) ----

(def has-length
  "hasLength — validator: value length within the given bounds. Raw passthrough."
  #?(:cljs mf/hasLength
     :clj (f/not-implemented "mantine.form/has-length")))

(def is-email
  "isEmail — validator: value is a valid email. Raw passthrough."
  #?(:cljs mf/isEmail
     :clj (f/not-implemented "mantine.form/is-email")))

(def is-in-range
  "isInRange — validator: numeric value within the given range. Raw passthrough."
  #?(:cljs mf/isInRange
     :clj (f/not-implemented "mantine.form/is-in-range")))

(def is-json-string
  "isJSONString — validator: value parses as JSON. Raw passthrough."
  #?(:cljs mf/isJSONString
     :clj (f/not-implemented "mantine.form/is-json-string")))

(def is-not-empty
  "isNotEmpty — validator: value is not empty. Raw passthrough."
  #?(:cljs mf/isNotEmpty
     :clj (f/not-implemented "mantine.form/is-not-empty")))

(def is-not-empty-html
  "isNotEmptyHTML — validator: value is not empty HTML. Raw passthrough."
  #?(:cljs mf/isNotEmptyHTML
     :clj (f/not-implemented "mantine.form/is-not-empty-html")))

(def is-one-of
  "isOneOf — validator: value is one of the given options. Raw passthrough."
  #?(:cljs mf/isOneOf
     :clj (f/not-implemented "mantine.form/is-one-of")))

(def is-url
  "isUrl — validator: value is a valid URL. Raw passthrough."
  #?(:cljs mf/isUrl
     :clj (f/not-implemented "mantine.form/is-url")))

(def matches
  "matches — validator: value matches the given regexp. Raw passthrough."
  #?(:cljs mf/matches
     :clj (f/not-implemented "mantine.form/matches")))

(def matches-field
  "matchesField — validator: value equals another field's value. Raw passthrough."
  #?(:cljs mf/matchesField
     :clj (f/not-implemented "mantine.form/matches-field")))
