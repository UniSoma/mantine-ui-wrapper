(ns mantine.supplements.form
  "Hand-written supplement HOISTED by the generator into the generated
  mantine.form ns. Committed generator INPUT — compilable for editor/
  clj-kondo support, but never shipped as-is: its :require entries are merged into
  the generated ns and its top-level forms are appended after the codegen'd defs.

  @mantine/form has no docgen entries, so mantine.form is a supplement-only package
  (see codegen/scope.edn :supplement-only-packages). Everything here is a RAW
  passthrough of the JS export: no js<->clj conversion accessors — consumers layer
  interop themselves."
  (:require
   [mantine.impl.factory :as f]
   #?@(:cljs [["@mantine/form" :refer [useForm hasLength isEmail isInRange
                                       isJSONString isNotEmpty isNotEmptyHTML
                                       isOneOf isUrl matches matchesField]]])))

(def use-form
  "useForm — the form hook. Raw passthrough: pass JS-shaped options (#js {...});
  returns the raw JS form object (read/call its members via interop under :advanced,
  e.g. (.getInputProps form \"name\"), (.-values form), (.onSubmit form handler))."
  #?(:cljs useForm
     :clj (f/not-implemented "mantine.form/use-form")))

;; ---- validators (pure functions, raw passthrough) ----

(def has-length
  "hasLength — validator: value length within the given bounds. Raw passthrough."
  #?(:cljs hasLength
     :clj (f/not-implemented "mantine.form/has-length")))

(def is-email
  "isEmail — validator: value is a valid email. Raw passthrough."
  #?(:cljs isEmail
     :clj (f/not-implemented "mantine.form/is-email")))

(def is-in-range
  "isInRange — validator: numeric value within the given range. Raw passthrough."
  #?(:cljs isInRange
     :clj (f/not-implemented "mantine.form/is-in-range")))

(def is-json-string
  "isJSONString — validator: value parses as JSON. Raw passthrough."
  #?(:cljs isJSONString
     :clj (f/not-implemented "mantine.form/is-json-string")))

(def is-not-empty
  "isNotEmpty — validator: value is not empty. Raw passthrough."
  #?(:cljs isNotEmpty
     :clj (f/not-implemented "mantine.form/is-not-empty")))

(def is-not-empty-html
  "isNotEmptyHTML — validator: value is not empty HTML. Raw passthrough."
  #?(:cljs isNotEmptyHTML
     :clj (f/not-implemented "mantine.form/is-not-empty-html")))

(def is-one-of
  "isOneOf — validator: value is one of the given options. Raw passthrough."
  #?(:cljs isOneOf
     :clj (f/not-implemented "mantine.form/is-one-of")))

(def is-url
  "isUrl — validator: value is a valid URL. Raw passthrough."
  #?(:cljs isUrl
     :clj (f/not-implemented "mantine.form/is-url")))

(def matches
  "matches — validator: value matches the given regexp. Raw passthrough."
  #?(:cljs matches
     :clj (f/not-implemented "mantine.form/matches")))

(def matches-field
  "matchesField — validator: value equals another field's value. Raw passthrough."
  #?(:cljs matchesField
     :clj (f/not-implemented "mantine.form/matches-field")))
