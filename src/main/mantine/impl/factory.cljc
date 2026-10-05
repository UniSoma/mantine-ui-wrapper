(ns ^:no-doc mantine.impl.factory
  "Factory + JVM stubs the generated namespaces delegate to.

  :cljs exposes `factory` (variadic factory over react/createElement) and
  `controlled` (minimal controlled-input shim the generator applies to a curated
  component set). :clj exposes only `not-implemented`, so generated .cljc
  namespaces load on the JVM (clj-kondo, cljdoc, tests) and throw only when a
  factory is called."
  #?(:cljs (:require ["react" :as react]
                     [mantine.impl.props :as p])))

(defn not-implemented
  "Returns a fn that throws when called, naming `wrapper-name`. The :clj branch of
  every generated def."
  [wrapper-name]
  (fn [& _]
    (throw (ex-info (str wrapper-name " is ClojureScript-only; the Mantine wrapper cannot be invoked on the JVM.")
                    {:wrapper wrapper-name}))))

#?(:cljs
   (def ^:private raw-key
     "String JS property key under which a factory carries its underlying Mantine
     component, so mantine.interop/raw-component can reach it. :advanced-safe."
     "mantine$raw"))

#?(:cljs
   (defn component-of
     "Returns the raw Mantine component tagged on `wrapper` by `factory` or
     `controlled`, or nil when `wrapper` carries no tag. Backs
     mantine.interop/raw-component."
     [wrapper]
     (when (fn? wrapper) (unchecked-get wrapper raw-key))))

#?(:cljs
   (defn factory
     "Wrap a React component in a variadic CLJS factory. The optional leading props
     map is detected with map?, so (button \"Click\") and (button {:color \"teal\"} \"Click\")
     both work. Everything after the props is children.

     Tags the returned fn with its underlying Mantine component (JS prop `mantine$raw`)
     so raw-component can recover it. If `component` is itself a tagged shim (from
     `controlled`), `factory` reads the tag through and records the true component,
     not the shim."
     [component]
     (let [raw (or (component-of component) component)
           wrapper (fn [& args]
                     (let [[props children] (if (map? (first args))
                                              [(first args) (rest args)]
                                              [nil args])]
                       (.apply react/createElement nil
                               (.concat #js [component (p/convert props)]
                                        (p/convert-children children)))))]
       (unchecked-set wrapper raw-key raw)
       wrapper)))

#?(:cljs
   (defn- change-event-value
     "Mantine inputs are split between DOM-event onChange (TextInput, Textarea, ...)
     and bare-value onChange (Select, NumberInput, ...); read the new value either way."
     [x]
     (if (and (some? x) (some? (.-target ^js x)) (some? (.-currentTarget ^js x)))
       (.. ^js x -target -value)
       x)))

#?(:cljs
   (defn controlled
     "Minimal controlled/uncontrolled shim for value/onChange inputs: renders from local
     state that is updated synchronously on change (so the cursor doesn't jump when the
     owner re-renders asynchronously) and re-synced when the external :value prop
     changes. Uncontrolled usage (no :value) passes straight through. Covers value and
     onChange only; it is not a form abstraction.

     Tags the returned shim with the underlying `component` (JS prop `mantine$raw`) so
     `factory` records the true Mantine component, not this shim, for raw-component."
     [component]
     (let [shim
           (fn [^js js-props]
             (let [ext (.-value js-props)
                   controlled? (not (undefined? ext))
                   [local set-local] (react/useState ext)
                   last-ext (react/useRef ext)]
               ;; external value changed -> adopt it (React render-phase state adjustment)
               (when (and controlled? (not= ext (.-current last-ext)))
                 (set! (.-current last-ext) ext)
                 (set-local ext))
               (if-not controlled?
                 (react/createElement component js-props)
                 (let [user-on-change (.-onChange js-props)]
                   (react/createElement
                    component
                    (js/Object.assign
                     #js {} js-props
                     #js {:value (if (undefined? local) "" local)
                          :onChange (fn [e]
                                      (set-local (change-event-value e))
                                      (when user-on-change (user-on-change e)))}))))))]
       (unchecked-set shim raw-key component)
       shim)))
