(ns ^:no-doc mantine.impl.props
  "CLJS map -> React/Mantine props conversion. ONE public converter (`convert`) that
  every generated factory and imperative fn funnels through, plus the children path
  (`convert-children`).

  Semantics (deep-by-default conversion, ADR 0006; key-casing/:&/children locked
  by mnt-01kxe8gzn6bt):
  - Keys: hybrid kebab->camel (:label-position -> labelPosition); data-*/aria-*/--* exempt.
  - Values: DEEP conversion by default — nested maps and sequential collections
    recurse, applying the same rules at every depth (so *Props config maps and
    vectors-of-maps like spotlight :actions work in plain CLJS). Everything else
    (React elements, fns, primitives, #js values) passes through untouched;
    keyword VALUES are NOT stringified (convert is not clj->js).
  - Raw passthrough opt-outs: keys in `raw-value-keys` (:inner-props) camelize but
    their VALUES stay untouched CLJS; `(raw v)` tags any value to skip conversion
    at any depth — a wrapper VALUE, so it survives merge/select-keys/map rebuilds.
  - :style / inner style maps: keys camelCased, --* verbatim, values passthrough.
  - class values (top-level :class/:className + classNames members): string or
    collection -> space-joined, nil/false dropped.
  - :class accepted as alias of :className; merged when both present.
  - Event handlers receive the RAW JS SyntheticEvent; refs/component/renderRoot/section
    props are pure passthrough.
  - Escape hatch: the reserved key :& — raw JS object or CLJS map through plain
    clj->js (hyphens preserved) — merged LAST, overrides normal conversion; applies
    at every depth, since nested maps go through `convert`."
  (:require [clojure.string :as str]
            [goog.object :as gobj]))

(def escape-hatch-key
  "Reserved merge key: its value (raw JS object, or CLJS map run through plain clj->js)
  is merged into the converted props LAST and wins over normal conversion."
  :&)

(deftype Raw [v])

(defn raw
  "Tag a value so `convert` emits it untouched (kept as-is, e.g. a raw CLJS map)
  instead of deep-converting it. Honored at any depth. A wrapper VALUE, not
  metadata — it survives merge/select-keys/map rebuilds. Hoisted into
  mantine.core/raw for consumers."
  [x]
  (->Raw x))

(defn- unraw [^Raw r] (.-v r))

(def ^:private raw-value-keys
  "Camelized keys whose values ALWAYS pass through as untouched CLJS. :inner-props
  is the context-modal slot: open-context-modal hands it to a CLJS modal component
  that reads it as a CLJS map, so converting it would destroy the CLJS->CLJS
  handoff (qualified keywords become dead string keys)."
  #{"innerProps"})

(defn- key->str [k]
  (if (keyword? k) (name k) (str k)))

(def ^:private camelize
  "\"label-position\" -> \"labelPosition\"; data-*/aria-*/--* pass verbatim.
  Memoized — the prop vocabulary is bounded."
  (memoize
   (fn [s]
     (if (or (not (str/includes? s "-"))
             (str/starts-with? s "data-")
             (str/starts-with? s "aria-")
             (str/starts-with? s "--"))
       s
       (let [[head & tail] (str/split s #"-")]
         (apply str head (map str/capitalize tail)))))))

(defn- class-leaf
  "Class value: string passthrough; sequential collection -> space-joined with
  nil/false dropped (keywords allowed for convenience)."
  [v]
  (if (sequential? v)
    (->> v
         (remove #(or (nil? %) (false? %)))
         (map key->str)
         (str/join " "))
    (if (keyword? v) (name v) v)))

(defn- style-leaf
  "Style map: keys camelCased (--* verbatim), values passthrough (numbers stay
  numeric — React appends px). Non-map values (raw JS objects) pass through."
  [v]
  (if (map? v)
    (let [o #js {}]
      (doseq [[k sv] v]
        (gobj/set o (camelize (key->str k)) sv))
      o)
    v))

(defn- selector-map
  "styles/vars object: outer selector keys camelCased, member values through style-leaf.
  Function values (function-form Styles API) pass through unwrapped."
  [v]
  (if (map? v)
    (let [o #js {}]
      (doseq [[k sv] v]
        (gobj/set o (camelize (key->str k)) (style-leaf sv)))
      o)
    v))

(defn- class-names-map
  "classNames object: outer selector keys camelCased, member values through class-leaf."
  [v]
  (if (map? v)
    (let [o #js {}]
      (doseq [[k cv] v]
        (gobj/set o (camelize (key->str k)) (class-leaf cv)))
      o)
    v))

(declare convert)

(defn- convert-value
  "Deep default for a plain prop value: Raw unwraps untouched, maps recurse through
  `convert`, sequential collections become JS arrays with members recursed;
  everything else passes through untouched."
  [v]
  (cond
    (instance? Raw v) (unraw v)
    (map? v) (convert v)
    (sequential? v) (let [a #js []]
                      (doseq [x v] (.push a (convert-value x)))
                      a)
    :else v))

(defn convert
  "Convert a CLJS props map to a JS props object per the semantics in the ns docstring.
  nil converts to an empty JS object."
  [props]
  (let [hatch (get props escape-hatch-key)
        o #js {}]
    (doseq [[k v] (dissoc props escape-hatch-key)]
      (let [ks (key->str k)
            ks (if (= ks "class") "className" ks)
            ck (camelize ks)]
        (cond
          (instance? Raw v) (gobj/set o ck (unraw v))
          (contains? raw-value-keys ck) (gobj/set o ck v)
          :else
          (case ck
            "className" (gobj/set o "className"
                                  (if-some [existing (gobj/get o "className")]
                                    (str existing " " (class-leaf v))
                                    (class-leaf v)))
            "style" (gobj/set o "style" (style-leaf v))
            "styles" (gobj/set o "styles" (selector-map v))
            "vars" (gobj/set o "vars" (selector-map v))
            "classNames" (gobj/set o "classNames" (class-names-map v))
            (gobj/set o ck (convert-value v))))))
    (when (some? hatch)
      (gobj/extend o (if (map? hatch) (clj->js hatch) hatch)))
    o))

(defn convert-children
  "Children path: prune nil, recursively flatten sequential collections (vectors,
  lazy seqs from `for`), everything else (strings, numbers, elements, render-prop
  fns) passes through. Returns a JS array. React reads :key from child props."
  [children]
  (let [out #js []]
    (letfn [(push! [c]
              (cond
                (nil? c) nil
                (sequential? c) (run! push! c)
                :else (.push out c)))]
      (run! push! children))
    out))
