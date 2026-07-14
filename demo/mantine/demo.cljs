(ns mantine.demo
  "Dev-only harness proving the four wrapping patterns end-to-end:
  1. codegen'd core components (kebab props, sections, styles/classNames, polymorphic :component)
  2. controlled input (mantine.impl.factory/controlled shim on TextInput)
  3. hook (mantine.hooks/use-disclosure, raw tuple destructured positionally)
  4. imperative API (mantine.notifications/show + its provider)"
  (:require ["react" :as react]
            ["react-dom/client" :as rdom]
            ["@mantine/core" :refer [MantineProvider]]
            [mantine.core :as mc]
            [mantine.hooks :as mh]
            [mantine.notifications :as mn]))

(defn demo []
  (let [[opened handlers] (mh/use-disclosure false)
        [value set-value] (react/useState "hello")
        [fruit set-fruit] (react/useState "apple")]
    (mc/stack
     {:gap "md" :p "xl" :id "demo-root"}
     (mc/text {:size "lg" :fw 700} "mantine-ui-wrapper — four-pattern PoC")

     ;; codegen'd component: kebab props, section prop, styles/classNames, imperative call
     (mc/button
      {:id "btn-notify"
       :color "teal"
       :left-section "🔔"
       :styles {:root {:font-weight 900 :--poc-var "on"}}
       :class-names {:root ["poc-btn" "poc-btn-root"]}
       :on-click (fn [_]
                   (mn/show {:id "poc-note"
                             :title "It works"
                             :message "Sent from mantine.notifications/show"
                             :color "teal"
                             :auto-close false}))}
      "Show notification")

     ;; polymorphic component= -> renders an <a>
     (mc/button
      {:id "btn-anchor" :component "a" :href "https://mantine.dev" :variant "outline"}
      "Polymorphic anchor button")

     ;; controlled input through the controlled shim
     (mc/text-input
      {:id "input-name"
       :label "Name"
       :value value
       :on-change (fn [e] (set-value (.. ^js e -target -value)))})
     (mc/text {:id "input-echo"} (str "Echo: " value))

     ;; widened core surface: newly-generated components render as kebab factories
     (mc/alert {:id "alert" :title "Heads up" :color "blue"} "Widened core coverage")
     (mc/anchor {:id "anchor" :href "https://mantine.dev"} "Docs link")
     (mc/kbd {:id "kbd"} "Ctrl")

     ;; newly-curated controlled input: NativeSelect through the controlled shim
     ;; (real <select>, DOM-event onChange — same shim, shape-agnostic value read)
     (mc/native-select
      {:id "fruit-select"
       :label "Fruit"
       :data #js ["apple" "banana" "cherry"]
       :value fruit
       :on-change (fn [e] (set-fruit (.. ^js e -target -value)))})
     (mc/text {:id "fruit-echo"} (str "Fruit: " fruit))

     ;; hook-driven disclosure toggling a Collapse; children from a seq get flattened
     (mc/button
      {:id "btn-toggle" :variant "light" :on-click (fn [_] (.toggle ^js handlers))}
      (if opened "Hide details" "Show details"))
     (mc/collapse
      {:id "collapse" :expanded opened}
      (for [i (range 3)]
        (mc/badge {:key i :id (str "badge-" i) :component "span"} (str "Disclosed " i)))))))

(defn app []
  (react/createElement
   MantineProvider #js {}
   (mn/provider {:position "top-right"})
   (react/createElement demo)))

(defn init []
  (.render (rdom/createRoot (js/document.getElementById "app"))
           (react/createElement app)))
