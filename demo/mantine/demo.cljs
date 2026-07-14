(ns mantine.demo
  "Dev-only harness proving the four wrapping patterns end-to-end:
  1. codegen'd core components (kebab props, sections, styles/classNames, polymorphic :component)
  2. controlled input (mantine.impl.factory/controlled shim on TextInput)
  3. hooks (mantine.hooks) sampled across the return-shape split: tuple
     (use-disclosure/use-counter), object via ^js interop (use-viewport-size), scalar (use-id)
  4. imperative API (mantine.notifications/show + its provider)"
  (:require ["react" :as react]
            ["react-dom/client" :as rdom]
            ["@mantine/core" :refer [MantineProvider]]
            [mantine.core :as mc]
            [mantine.dates :as md]
            [mantine.charts :as mch]
            [mantine.hooks :as mh]
            [mantine.notifications :as mn]
            [mantine.modals :as mm]
            [mantine.spotlight :as ms]))

(defn demo []
  (let [[opened handlers] (mh/use-disclosure false)
        [value set-value] (react/useState "hello")
        [fruit set-fruit] (react/useState "apple")
        [date set-date] (react/useState "2026-07-14")
        ;; hooks return-shape split, one sample each (raw JS returns, zero conversion):
        [count counter-handlers] (mh/use-counter 5) ; tuple, destructured positionally
        viewport (mh/use-viewport-size)             ; object, read via interop (^js)
        generated-id (mh/use-id)]                   ; scalar (string)
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

     ;; @mantine/dates: controlled DatePicker (inline calendar, bare-value onChange
     ;; through the same shim) verified end-to-end
     (md/date-picker
      {:id "date-picker"
       :value date
       :on-change (fn [v] (set-date v))})
     (mc/text {:id "date-echo"} (str "Date: " date))

     ;; @mantine/charts: representative rendered chart
     (mch/line-chart
      {:id "line-chart"
       :h 200
       :data #js [#js {:month "Jan" :sales 100}
                  #js {:month "Feb" :sales 140}
                  #js {:month "Mar" :sales 120}]
       :data-key "month"
       :series #js [#js {:name "sales" :color "blue.6"}]})

     ;; hook-driven disclosure toggling a Collapse; children from a seq get flattened
     (mc/button
      {:id "btn-toggle" :variant "light" :on-click (fn [_] (.toggle ^js handlers))}
      (if opened "Hide details" "Show details"))
     (mc/collapse
      {:id "collapse" :expanded opened}
      (for [i (range 3)]
        (mc/badge {:key i :id (str "badge-" i) :component "span"} (str "Disclosed " i))))

     ;; hooks return-shape split: tuple (use-counter), object via ^js interop
     ;; (use-viewport-size), scalar (use-id). Proves the raw-JS interop path, not
     ;; just that the aliases compile.
     (mc/button
      {:id "btn-count" :variant "default" :on-click (fn [_] (.increment ^js counter-handlers))}
      (str "Count: " count))
     (mc/text {:id "viewport-size"}
              (str "Viewport: " (.-width ^js viewport) "x" (.-height ^js viewport)))
     (mc/text {:id "generated-id"} (str "ID: " generated-id))

     ;; imperative modals API: open drives the ModalsProvider, close by modal id
     (mc/button
      {:id "btn-open-modal"
       :on-click (fn [_]
                   (mm/open {:modal-id "demo-modal"
                             :title "Demo modal"
                             :children (mc/text {:id "modal-body"} "Imperative modal body")}))}
      "Open modal")
     (mc/button
      {:id "btn-close-modal" :on-click (fn [_] (mm/close "demo-modal"))}
      "Close modal")

     ;; @mantine/spotlight: the Spotlight component is the UI; toggle drives the store
     (ms/spotlight
      {:actions #js [#js {:id "act-1"
                          :label "First action"
                          :description "Spotlight action rendered from actions prop"}]})
     (mc/button
      {:id "btn-toggle-spotlight" :on-click (fn [_] (ms/toggle))}
      "Toggle spotlight"))))

(defn app []
  (react/createElement
   MantineProvider #js {}
   (mn/provider {:position "top-right"})
   (mm/provider {} (react/createElement demo))))

(defn init []
  (.render (rdom/createRoot (js/document.getElementById "app"))
           (react/createElement app)))
