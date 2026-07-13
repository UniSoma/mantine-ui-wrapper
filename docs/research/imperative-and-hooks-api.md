> Research for ticket mnt-01kxe8gzj38z (imperative + hooks API surface)

Scope: the non-component API surface of Mantine v9 NOT captured by component docgen — the imperative
singletons of `@mantine/notifications`, `@mantine/modals`, `@mantine/spotlight`, plus the `@mantine/hooks`
export list and return-shape conventions. Facts taken from mantinedev/mantine source (shallow clone).

---

## 0. Common shape across the 3 imperative packages

All three follow the same architecture that the CLJS wrapper must mirror:

1. A **singleton object** of functions is exported (`notifications`, `modals`, `spotlight`) — these are
   callable from anywhere, no React context needed at the call site.
2. State lives in a **store / event bus**, not React state:
   - notifications + spotlight use `@mantine/store` (`createStore` / `useStore`), exposed via a
     `useNotifications` / `useSpotlight` hook.
   - modals uses a DOM CustomEvent bus (`createUseExternalEvents('mantine-modals')`) — the imperative
     functions dispatch events; the provider subscribes.
3. A **provider/renderer component must be mounted once** for the imperative calls to render anything:
   `<Notifications />`, `<ModalsProvider>`, `<Spotlight />` (or `<Spotlight.Root>`). Calling the
   imperative API without the component mounted silently no-ops (notifications/spotlight) or throws for
   the `useModals` hook.
4. `open`-style functions **return a string id** (auto-generated via `randomId()` if not supplied) usable
   for later `update`/`close`.
5. Every store factory has a `create*` variant (`createNotificationsStore`, `createSpotlight`,
   store `store` prop) so multiple independent instances are possible — the default singleton is the
   common case.

---

## 1. @mantine/notifications

### Imperative API (`notifications` singleton, from `notifications.store.ts`)
Also exported as standalone functions and as static methods on the `Notifications` component.

| method | signature | returns |
|---|---|---|
| `notifications.show(data, store?)`      | `showNotification`        | `id: string` |
| `notifications.hide(id, store?)`        | `hideNotification`        | `id: string` |
| `notifications.update(data, store?)`    | `updateNotification` (matches by `data.id`) | `id` |
| `notifications.clean(store?)`           | `cleanNotifications` — removes all (active + queue) | void |
| `notifications.cleanQueue(store?)`      | `cleanNotificationsQueue` — drops only queued | void |
| `notifications.updateState(store, fn)`  | low-level state updater | void |

Standalone exports: `showNotification`, `hideNotification`, `updateNotification`, `cleanNotifications`,
`cleanNotificationsQueue`, `updateNotificationsState`, `createNotificationsStore`, `notificationsStore`,
`useNotifications`.

Reactive hook: `useNotifications(store = notificationsStore)` → the whole `NotificationsState`:
`{ notifications: NotificationData[], queue: NotificationData[], defaultPosition, limit }`.

### Options object passed to `show`/`update` — `NotificationData`
Extends `NotificationProps` (the `Notification` component props: `title`, `color`, `icon`, `loading`,
`withBorder`, `withCloseButton`, `radius`, `className`, `style`, etc.), minus `onClose`, plus:
- `id?: string`
- `message: React.ReactNode` (**required**)
- `position?: NotificationPosition`
- `priority?: number` (default 0; higher shown first when over `limit`)
- `autoClose?: boolean | number` (ms; overrides provider default)
- `allowClose?: boolean` (default true)
- `onClose?: (props: NotificationData) => void`
- `onOpen?: (props: NotificationData) => void`
- arbitrary `data-*` attributes

`NotificationPosition = 'top-left' | 'top-right' | 'top-center' | 'bottom-left' | 'bottom-right' | 'bottom-center'`.

### Provider / mount requirement
Mount `<Notifications />` **once** near app root. Key props (`NotificationsProps`, all optional, defaults shown):
`position='bottom-right'`, `autoClose=4000` (`number | false`), `transitionDuration=250`,
`allowDragDismiss=true`, `allowScrollDismiss=true`, `containerWidth=440`, `notificationMaxHeight=200`,
`limit=5`, `zIndex=400`, `withinPortal=true`, `portalProps`, `store`, `pauseResetOnHover='all' | 'notification'`.
Renders six fixed-position containers (one per position) inside an `OptionalPortal`.

---

## 2. @mantine/modals

### Imperative API (`modals` singleton, from `events.ts`)
Dispatches CustomEvents on the `mantine-modals` bus. Open functions return a string id.

| method | signature | returns |
|---|---|---|
| `modals.open(props)`               | `openModal(ModalSettings)`            | `id` |
| `modals.openConfirmModal(props)`   | `openConfirmModal(OpenConfirmModal)`  | `id` |
| `modals.openContextModal(props)`   | `openContextModal({ modal, innerProps, ...ModalSettings })` | `id` |
| `modals.close(id)`                 | `closeModal`                          | void |
| `modals.closeAll()`                | `closeAllModals`                      | void |
| `modals.updateModal(payload)`      | `{ modalId, ...Partial<ModalSettings> }` | void |
| `modals.updateContextModal(payload)` | `{ modalId, ...Partial<OpenContextModal> }` | void |

Note: on the singleton `openContextModal` takes a **single object** with `modal` key
(`{ modal: 'myModal', innerProps: {...}, title, ... }`); the context-hook version takes `(modalKey, props)`
as two args. Also exported standalone: `openModal`, `closeModal`, `closeAllModals`, `openConfirmModal`,
`openContextModal`, `updateModal`, `updateContextModal`.

Reactive hook: `useModals()` (uses React context; **throws** if `ModalsProvider` is absent) →
`ModalsContextProps` = `{ modalProps, modals: ModalState[], openModal, openConfirmModal, openContextModal,
closeModal, closeContextModal, closeAll, updateModal, updateContextModal }`.

### Option shapes
- `ModalSettings = Partial<Omit<ModalProps,'opened'>> & { modalId?: string }` — i.e. any `Modal` prop
  (`title`, `children`, `size`, `centered`, `fullScreen`, `withCloseButton`, `onClose`, ...) plus `modalId`.
- `OpenConfirmModal = ModalSettings & ConfirmModalProps`, where `ConfirmModalProps` =
  `{ id?, children?, onCancel?(), onConfirm?(), closeOnConfirm?=true, closeOnCancel?=true,
  cancelProps?, confirmProps?, groupProps?, labels?: {confirm, cancel} }`.
- `OpenContextModal<CustomProps> = ModalSettings & { innerProps: CustomProps }`.

### Provider / context-modal registration
Mount `<ModalsProvider>` wrapping the app. Props (`ModalsProviderProps`):
- `children`
- `modals?: Record<string, React.FC<ContextModalProps<any>>>` — **this is how context modals are
  registered**: a name→component map. `openContextModal({ modal: 'deleteAccount', ... })` looks the
  component up by key.
- `modalProps?: ModalSettings` — shared props applied to every modal.
- `labels?: { confirm, cancel }` — default confirm/cancel button labels.

A context modal component receives `ContextModalProps<T> = { context: ModalsContextProps, innerProps: T, id: string }`.
Type augmentation of `MantineModalsOverride` gives type-safe modal keys. The provider renders a **single**
`<Modal>` and swaps its content based on the top of the modal stack (reducer state).

---

## 3. @mantine/spotlight

### Imperative API (`spotlight` singleton, from `spotlight.store.ts`)
`createSpotlight()` returns `[store, actions]`; the default `[spotlightStore, spotlight]` is exported.

| method | effect |
|---|---|
| `spotlight.open()`   | `openSpotlight`  — sets `opened:true, selected:-1` |
| `spotlight.close()`  | `closeSpotlight` — sets `opened:false` |
| `spotlight.toggle()` | `toggleSpotlight` |

Also exported standalone: `openSpotlight`, `closeSpotlight`, `toggleSpotlight`, `createSpotlight`,
`createSpotlightStore`, `useSpotlight`. (The `spotlightActions` object with the full low-level action set —
`setQuery`, `selectNextAction`, `triggerSelectedAction`, `registerAction`, etc. — is internal, not exported.)

Reactive hook: `useSpotlight(store)` → `SpotlightState`.

### Store data shape — `SpotlightState`
`{ opened: boolean, selected: number (-1 = none), listId: string, query: string, empty: boolean,
registeredActions: Set<string> }`.

### Component surface
`Spotlight` is a compound component AND carries static imperative methods:
- Static methods: `Spotlight.open`, `Spotlight.close`, `Spotlight.toggle` (= the singleton).
- Sub-components: `Spotlight.Root`, `Spotlight.Search`, `Spotlight.ActionsList`, `Spotlight.Action`,
  `Spotlight.ActionsGroup`, `Spotlight.Empty`, `Spotlight.Footer`.

Two usage modes:
1. **Declarative data** — `<Spotlight actions={...} />` (`SpotlightProps extends SpotlightRootProps`):
   `actions: SpotlightActions[]`, `filter?`, `nothingFound?`, `highlightQuery?=false`, `limit?=Infinity`,
   `searchProps?`, `scrollAreaProps?`.
2. **Composable** — build with `<Spotlight.Root>` + sub-components manually.

Action data shapes (for mode 1):
- `SpotlightActionData extends SpotlightActionProps { id: string; group?: string }` where
  `SpotlightActionProps` = `{ label?, description?, leftSection?, rightSection?, children?, keywords?,
  onClick?, highlightQuery?, closeSpotlightOnTrigger?, ... }`.
- `SpotlightActionGroupData { group: string; actions: SpotlightActionData[] }`.
- `SpotlightActions = SpotlightActionData | SpotlightActionGroupData`.

### Provider / mount requirement
`<Spotlight>` / `<Spotlight.Root>` must be mounted. `SpotlightRootProps` (extends most `ModalProps`) key props:
`store?`, `query?` / `onQueryChange?` (controlled search), `clearQueryOnClose?=true`,
`shortcut?='mod + K'` (`string | string[] | null`), `tagsToIgnore?=['input','textarea','select']`,
`triggerOnContentEditable?=false`, `disabled?`, `onSpotlightOpen?`, `onSpotlightClose?`, `forceOpened?`,
`closeOnActionTrigger?=true`, `maxHeight?=400`, `scrollable?=false`. The `shortcut` prop wires the global
hotkey that toggles the store — no external hotkey wiring needed.

---

## 4. @mantine/hooks

### Enumerating the full list programmatically
The canonical source is the package index `packages/@mantine/hooks/src/index.ts` (barrel of re-exports)
plus `src/utils/index.ts`. Programmatic enumeration options:
- Parse the `export { ... } from './...'` lines in `index.ts` (79 `use*` symbols + non-hook helpers).
- Or at runtime: `Object.keys(require('@mantine/hooks'))` and filter `^use`.
- Non-hook exports also live here: `randomId`, `clamp`, `range`, `upperFirst`, `lowerFirst`,
  `shallowEqual`, `useCallbackRef` (utils); plus `mergeRefs`, `assignRef`, `readLocalStorageValue`,
  `readSessionStorageValue`, `getHotkeyHandler`, `clampUseMovePosition`, `normalizeRadialValue`,
  `formatMask`/`unformatMask`/`isMaskComplete`/`generatePattern`, `useMutationObserverTarget`.

### Full hook list (79), categorized

**State management (tuple `[value, handlers|setter]`)**
useDisclosure, useToggle, useCounter, useListState, useSetState, useMap, useSet, useQueue,
useStateHistory, useValidatedState, useInputState, useUncontrolled

**Debounce / throttle / timing**
useDebouncedValue, useDebouncedState, useDebouncedCallback, useThrottledValue, useThrottledState,
useThrottledCallback, useInterval, useTimeout, useIdle

**Storage / persistence**
useLocalStorage, useSessionStorage (+ `readLocalStorageValue`, `readSessionStorageValue`)

**UI / DOM element state (object return)**
useHover, useFocusWithin, useMove, useDrag, useRadialMove, useResizeObserver, useElementSize,
useMouse, useMousePosition, useIntersection, useInViewport, useScrollIntoView, useScrollSpy,
useScroller, useTextSelection, useSelection, useRovingIndex, useSplitter, useCollapse,
useHorizontalCollapse, useLongPress

**Viewport / window / media**
useViewportSize, useWindowScroll, useMediaQuery, useReducedMotion, useOrientation, useHeadroom,
useScrollDirection, useColorScheme

**Lifecycle / effects**
useDidUpdate, useShallowEffect, useIsomorphicEffect, useIsFirstRender, useMounted, useForceUpdate,
usePrevious, useLogger, useId

**Browser / device APIs**
useClipboard, useFullscreenDocument, useFullscreenElement, useEyeDropper, useFileDialog, useNetwork,
useOs, useDocumentTitle, useDocumentVisibility, useFavicon, useHash, useHotkeys, useOnceEffect(n/a),
useWindowEvent, useEventListener, useClickOutside, usePageLeave, useMutationObserver, useFetch

**Focus / a11y**
useFocusTrap, useFocusReturn, useFocusWithin (also above)

**Refs / utilities**
useMergedRef, useCallbackRef, usePagination, useMask, useFloatingWindow

### Representative return shapes — TUPLE vs OBJECT (the CLJS-conversion split)

This is the critical distinction: a tuple must become a positional CLJS vector `[v handlers]` (destructure by
position), whereas an object return becomes a map with keyword keys.

| hook | signature | return | kind |
|---|---|---|---|
| `useDisclosure(initial=false, {onOpen,onClose}?)` | | `[opened: boolean, { open, close, toggle, set }]` | **TUPLE** |
| `useToggle(options=[false,true])` | | `[value, toggleFn]` (`toggleFn(next?)`) | **TUPLE** |
| `useCounter(initial=0, {min,max})` | | `[count, { increment, decrement, set, reset }]` | **TUPLE** |
| `useListState(initial)` | | `[list, { append, prepend, insert, remove, reorder, setItem, setState, ... }]` | **TUPLE** |
| `useLocalStorage({key, defaultValue, ...})` | | `[value, setValue, removeValue]` (3-tuple) | **TUPLE** |
| `useSessionStorage(...)` | | `[value, setValue, removeValue]` | **TUPLE** |
| `useResizeObserver()` | | `[refCallback, rect]` | **TUPLE** |
| `useMediaQuery(query, initial?, {getInitialValueInEffect}?)` | | `boolean` | **SCALAR** |
| `useHover<T>()` | | `{ ref, hovered }` | **OBJECT** |
| `useElementSize<T>()` | | `{ ref, width, height }` | **OBJECT** |
| `useClipboard({timeout=2000}?)` | | `{ copy, reset, error, copied }` | **OBJECT** |

General rule observed in v9: **state-mutation hooks return tuples** `[value, handlersObjOrSetter]`
(useDisclosure, useToggle, useCounter, useListState, useSetState, useSet, useMap, useQueue,
useStateHistory, useDebouncedState/Value, useThrottledState/Value, useInputState, useValidatedState,
useUncontrolled, useHash, useWindowScroll — and the storage hooks are the 3-element outlier
`[value, set, remove]`). **DOM/element and status hooks return objects** (useHover, useElementSize,
useClipboard, useMouse, useMove, useNetwork, useIntersection, useInViewport, useFocusWithin, useOs,
useScrollIntoView, ...). Some return a bare **scalar/ref** (useMediaQuery→boolean, usePrevious→value,
useFocusTrap→refCallback, useViewportSize→`{width,height}`, useDocumentVisibility→string).

The wrapper must special-case tuple hooks (positional) vs object hooks (map) — a blanket
JS-object→CLJS-map conversion would break the tuple hooks, which are the majority of the commonly-used ones.

### `useLocalStorage` options (`UseStorageOptions<T>`)
`{ key (required), defaultValue?, getInitialValueInEffect?=true, sync?=true (cross-tab),
serialize?, deserialize? }`. `readLocalStorageValue({key, defaultValue, deserialize})` reads without a hook.
