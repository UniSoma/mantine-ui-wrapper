#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns mantine.supplements.core
  "Hand-written supplement HOISTED by the generator into the generated
  mantine.core ns. Committed generator INPUT — compilable for editor/clj-kondo
  support, but never shipped as-is: its :require entries are merged into the
  generated ns and its top-level forms are appended after the codegen'd defs.

  Backfills the @mantine/core surface docgen omits: provider/primitive
  components (MantineProvider, Box, DirectionProvider), the create-theme / rem
  functions, the package-local color-scheme hooks, and the dot-notation compound
  parts docgen does not list (Menu.Dropdown, AppShell.Main, Modal.Body, ...).
  The compound-part coverage check in scripts/coverage-check.clj is the source of
  truth for this list — it fails if any wrapped component grows a static part that
  is neither generated from docgen nor defined here."
  (:refer-clojure :exclude [rem])
  (:require
   [mantine.impl.factory :as f]
   #?@(:cljs [["@mantine/core" :as mantine-core
               :refer [AccordionChevron AccordionPanel ActionIconGroupSection AppShellMain
                       Box ComboboxChevron ComboboxClearButton ComboboxEmpty
                       ComboboxFooter ComboboxHeader ComboboxHiddenInput ComboboxOptions
                       ComboboxPopoverTarget ComboboxSearch DataListItem DataListItemLabel
                       DataListItemValue DirectionProvider DrawerBody DrawerCloseButton
                       DrawerContent DrawerHeader DrawerOverlay DrawerRoot
                       DrawerStack DrawerTitle EmptyStateActions EmptyStateDescription
                       EmptyStateIndicator EmptyStateTitle HoverCardDropdown InputClearButton
                       InputPlaceholder InputSuccess MantineProvider MenuDivider
                       MenuDropdown MenuLabel MenuSubDropdown MenubarDropdown
                       MenubarMenu MenubarTarget ModalBody ModalCloseButton
                       ModalContent ModalHeader ModalOverlay ModalRoot
                       ModalStack ModalTitle PaginationLabel ProgressLabel
                       SplitterPane StepperCompleted TableCaption TableScrollContainer
                       TableTbody TableTd TableTfoot TableTh
                       TableThead TableTr TooltipFloating TooltipGroup
                       createTheme useComputedColorScheme useMantineColorScheme useMantineTheme]]])))

;; ---- providers / primitives ----

(def mantine-provider
  "MantineProvider — application root; supplies theme, color scheme and CSS
  variables. Nothing Mantine renders without it. Optional leading props map;
  remaining args are children."
  #?(:cljs (f/factory MantineProvider)
     :clj (f/not-implemented "mantine.core/mantine-provider")))

(def box
  "Box — polymorphic layout primitive (accepts :component / :render-root).
  Optional leading props map; remaining args are children."
  #?(:cljs (f/factory Box)
     :clj (f/not-implemented "mantine.core/box")))

(def direction-provider
  "DirectionProvider — sets LTR/RTL text direction for descendants.
  Optional leading props map; remaining args are children."
  #?(:cljs (f/factory DirectionProvider)
     :clj (f/not-implemented "mantine.core/direction-provider")))

;; ---- functions ----

(defn create-theme
  "Build a Mantine theme object from a Clojure map.

  Wraps clj->js, so theme keys are CAMELCASE-ONLY: clj->js does NOT camelize
  kebab-case, so write :fontFamily / :primaryColor / :defaultRadius (not
  :font-family). Freeform nested maps (:colors, :other) pass straight through —
  their keys are user data, not Mantine prop names."
  [theme]
  #?(:cljs (createTheme (clj->js theme))
     :clj ((f/not-implemented "mantine.core/create-theme") theme)))

(def rem
  "rem — Mantine px->rem helper. Raw passthrough of the JS fn (pass a number
  or string; returns a rem string)."
  #?(:cljs mantine-core/rem
     :clj (f/not-implemented "mantine.core/rem")))

;; ---- hooks (raw passthrough) ----

(def use-mantine-theme
  "useMantineTheme — read the resolved Mantine theme. Raw passthrough: returns
  the raw JS theme object (read via interop)."
  #?(:cljs useMantineTheme
     :clj (f/not-implemented "mantine.core/use-mantine-theme")))

(def use-mantine-color-scheme
  "useMantineColorScheme — read/set the color scheme. Raw passthrough: returns
  the raw JS value (.-colorScheme, .-setColorScheme, .-toggleColorScheme, ...)."
  #?(:cljs useMantineColorScheme
     :clj (f/not-implemented "mantine.core/use-mantine-color-scheme")))

(def use-computed-color-scheme
  "useComputedColorScheme — the resolved light/dark scheme (never 'auto').
  Raw passthrough: returns the raw JS string."
  #?(:cljs useComputedColorScheme
     :clj (f/not-implemented "mantine.core/use-computed-color-scheme")))

;; ---- compound parts (dot-notation subcomponents docgen omits) ----

(def accordion-chevron
  "Accordion.Chevron — compound part of Accordion (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory AccordionChevron)
     :clj (f/not-implemented "mantine.core/accordion-chevron")))

(def accordion-panel
  "Accordion.Panel — compound part of Accordion (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory AccordionPanel)
     :clj (f/not-implemented "mantine.core/accordion-panel")))

(def action-icon-group-section
  "ActionIcon.GroupSection — compound part of ActionIcon (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ActionIconGroupSection)
     :clj (f/not-implemented "mantine.core/action-icon-group-section")))

(def app-shell-main
  "AppShell.Main — compound part of AppShell (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory AppShellMain)
     :clj (f/not-implemented "mantine.core/app-shell-main")))

(def combobox-chevron
  "Combobox.Chevron — compound part of Combobox (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ComboboxChevron)
     :clj (f/not-implemented "mantine.core/combobox-chevron")))

(def combobox-clear-button
  "Combobox.ClearButton — compound part of Combobox (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ComboboxClearButton)
     :clj (f/not-implemented "mantine.core/combobox-clear-button")))

(def combobox-empty
  "Combobox.Empty — compound part of Combobox (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ComboboxEmpty)
     :clj (f/not-implemented "mantine.core/combobox-empty")))

(def combobox-footer
  "Combobox.Footer — compound part of Combobox (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ComboboxFooter)
     :clj (f/not-implemented "mantine.core/combobox-footer")))

(def combobox-header
  "Combobox.Header — compound part of Combobox (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ComboboxHeader)
     :clj (f/not-implemented "mantine.core/combobox-header")))

(def combobox-hidden-input
  "Combobox.HiddenInput — compound part of Combobox (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ComboboxHiddenInput)
     :clj (f/not-implemented "mantine.core/combobox-hidden-input")))

(def combobox-options
  "Combobox.Options — compound part of Combobox (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ComboboxOptions)
     :clj (f/not-implemented "mantine.core/combobox-options")))

(def combobox-popover-target
  "ComboboxPopover.Target — compound part of ComboboxPopover (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ComboboxPopoverTarget)
     :clj (f/not-implemented "mantine.core/combobox-popover-target")))

(def combobox-search
  "Combobox.Search — compound part of Combobox (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ComboboxSearch)
     :clj (f/not-implemented "mantine.core/combobox-search")))

(def data-list-item
  "DataList.Item — compound part of DataList (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory DataListItem)
     :clj (f/not-implemented "mantine.core/data-list-item")))

(def data-list-item-label
  "DataList.ItemLabel — compound part of DataList (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory DataListItemLabel)
     :clj (f/not-implemented "mantine.core/data-list-item-label")))

(def data-list-item-value
  "DataList.ItemValue — compound part of DataList (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory DataListItemValue)
     :clj (f/not-implemented "mantine.core/data-list-item-value")))

(def drawer-body
  "Drawer.Body — compound part of Drawer (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory DrawerBody)
     :clj (f/not-implemented "mantine.core/drawer-body")))

(def drawer-close-button
  "Drawer.CloseButton — compound part of Drawer (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory DrawerCloseButton)
     :clj (f/not-implemented "mantine.core/drawer-close-button")))

(def drawer-content
  "Drawer.Content — compound part of Drawer (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory DrawerContent)
     :clj (f/not-implemented "mantine.core/drawer-content")))

(def drawer-header
  "Drawer.Header — compound part of Drawer (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory DrawerHeader)
     :clj (f/not-implemented "mantine.core/drawer-header")))

(def drawer-overlay
  "Drawer.Overlay — compound part of Drawer (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory DrawerOverlay)
     :clj (f/not-implemented "mantine.core/drawer-overlay")))

(def drawer-root
  "Drawer.Root — compound part of Drawer (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory DrawerRoot)
     :clj (f/not-implemented "mantine.core/drawer-root")))

(def drawer-stack
  "Drawer.Stack — compound part of Drawer (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory DrawerStack)
     :clj (f/not-implemented "mantine.core/drawer-stack")))

(def drawer-title
  "Drawer.Title — compound part of Drawer (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory DrawerTitle)
     :clj (f/not-implemented "mantine.core/drawer-title")))

(def empty-state-actions
  "EmptyState.Actions — compound part of EmptyState (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory EmptyStateActions)
     :clj (f/not-implemented "mantine.core/empty-state-actions")))

(def empty-state-description
  "EmptyState.Description — compound part of EmptyState (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory EmptyStateDescription)
     :clj (f/not-implemented "mantine.core/empty-state-description")))

(def empty-state-indicator
  "EmptyState.Indicator — compound part of EmptyState (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory EmptyStateIndicator)
     :clj (f/not-implemented "mantine.core/empty-state-indicator")))

(def empty-state-title
  "EmptyState.Title — compound part of EmptyState (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory EmptyStateTitle)
     :clj (f/not-implemented "mantine.core/empty-state-title")))

(def hover-card-dropdown
  "HoverCard.Dropdown — compound part of HoverCard (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory HoverCardDropdown)
     :clj (f/not-implemented "mantine.core/hover-card-dropdown")))

(def input-clear-button
  "Input.ClearButton — compound part of Input (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory InputClearButton)
     :clj (f/not-implemented "mantine.core/input-clear-button")))

(def input-placeholder
  "Input.Placeholder — compound part of Input (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory InputPlaceholder)
     :clj (f/not-implemented "mantine.core/input-placeholder")))

(def input-success
  "Input.Success — compound part of Input (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory InputSuccess)
     :clj (f/not-implemented "mantine.core/input-success")))

(def menu-divider
  "Menu.Divider — compound part of Menu (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory MenuDivider)
     :clj (f/not-implemented "mantine.core/menu-divider")))

(def menu-dropdown
  "Menu.Dropdown — compound part of Menu (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory MenuDropdown)
     :clj (f/not-implemented "mantine.core/menu-dropdown")))

(def menu-label
  "Menu.Label — compound part of Menu (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory MenuLabel)
     :clj (f/not-implemented "mantine.core/menu-label")))

(def menu-sub-dropdown
  "MenuSub.Dropdown — compound part of MenuSub (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory MenuSubDropdown)
     :clj (f/not-implemented "mantine.core/menu-sub-dropdown")))

(def menubar-dropdown
  "Menubar.Dropdown — compound part of Menubar (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory MenubarDropdown)
     :clj (f/not-implemented "mantine.core/menubar-dropdown")))

(def menubar-menu
  "Menubar.Menu — compound part of Menubar (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory MenubarMenu)
     :clj (f/not-implemented "mantine.core/menubar-menu")))

(def menubar-target
  "Menubar.Target — compound part of Menubar (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory MenubarTarget)
     :clj (f/not-implemented "mantine.core/menubar-target")))

(def modal-body
  "Modal.Body — compound part of Modal (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ModalBody)
     :clj (f/not-implemented "mantine.core/modal-body")))

(def modal-close-button
  "Modal.CloseButton — compound part of Modal (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ModalCloseButton)
     :clj (f/not-implemented "mantine.core/modal-close-button")))

(def modal-content
  "Modal.Content — compound part of Modal (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ModalContent)
     :clj (f/not-implemented "mantine.core/modal-content")))

(def modal-header
  "Modal.Header — compound part of Modal (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ModalHeader)
     :clj (f/not-implemented "mantine.core/modal-header")))

(def modal-overlay
  "Modal.Overlay — compound part of Modal (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ModalOverlay)
     :clj (f/not-implemented "mantine.core/modal-overlay")))

(def modal-root
  "Modal.Root — compound part of Modal (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ModalRoot)
     :clj (f/not-implemented "mantine.core/modal-root")))

(def modal-stack
  "Modal.Stack — compound part of Modal (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ModalStack)
     :clj (f/not-implemented "mantine.core/modal-stack")))

(def modal-title
  "Modal.Title — compound part of Modal (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ModalTitle)
     :clj (f/not-implemented "mantine.core/modal-title")))

(def pagination-label
  "Pagination.Label — compound part of Pagination (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory PaginationLabel)
     :clj (f/not-implemented "mantine.core/pagination-label")))

(def progress-label
  "Progress.Label — compound part of Progress (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory ProgressLabel)
     :clj (f/not-implemented "mantine.core/progress-label")))

(def splitter-pane
  "Splitter.Pane — compound part of Splitter (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory SplitterPane)
     :clj (f/not-implemented "mantine.core/splitter-pane")))

(def stepper-completed
  "Stepper.Completed — compound part of Stepper (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory StepperCompleted)
     :clj (f/not-implemented "mantine.core/stepper-completed")))

(def table-caption
  "Table.Caption — compound part of Table (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory TableCaption)
     :clj (f/not-implemented "mantine.core/table-caption")))

(def table-scroll-container
  "Table.ScrollContainer — compound part of Table (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory TableScrollContainer)
     :clj (f/not-implemented "mantine.core/table-scroll-container")))

(def table-tbody
  "Table.Tbody — compound part of Table (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory TableTbody)
     :clj (f/not-implemented "mantine.core/table-tbody")))

(def table-td
  "Table.Td — compound part of Table (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory TableTd)
     :clj (f/not-implemented "mantine.core/table-td")))

(def table-tfoot
  "Table.Tfoot — compound part of Table (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory TableTfoot)
     :clj (f/not-implemented "mantine.core/table-tfoot")))

(def table-th
  "Table.Th — compound part of Table (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory TableTh)
     :clj (f/not-implemented "mantine.core/table-th")))

(def table-thead
  "Table.Thead — compound part of Table (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory TableThead)
     :clj (f/not-implemented "mantine.core/table-thead")))

(def table-tr
  "Table.Tr — compound part of Table (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory TableTr)
     :clj (f/not-implemented "mantine.core/table-tr")))

(def tooltip-floating
  "Tooltip.Floating — compound part of Tooltip (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory TooltipFloating)
     :clj (f/not-implemented "mantine.core/tooltip-floating")))

(def tooltip-group
  "Tooltip.Group — compound part of Tooltip (docgen omits it). Optional
  leading props map; remaining args are children."
  #?(:cljs (f/factory TooltipGroup)
     :clj (f/not-implemented "mantine.core/tooltip-group")))
