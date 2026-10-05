#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns mantine.supplements.notifications
  "Hand-written supplement the generator HOISTS into the generated
  mantine.notifications ns. A committed generator INPUT: it compiles for editor and
  clj-kondo support but never ships as-is. The generator merges its :require
  entries into the generated ns and appends its top-level forms after the
  generated defs."
  (:refer-clojure :exclude [update promise])
  (:require
   ;; f is :clj-branch-only HERE, but the generated ns also uses it on :cljs
   ;; (f/factory for its component defs), so the require stays unconditional.
   #_{:clj-kondo/ignore [:unused-namespace]}
   [mantine.impl.factory :as f]
   #?@(:cljs [["@mantine/notifications" :refer [showNotification hideNotification
                                                updateNotification promiseNotification
                                                cleanNotifications cleanNotificationsQueue
                                                useNotifications]]
              [mantine.impl.props :as p]])))

(declare notifications)

(def provider
  "Alias for `notifications`, the renderer component that must be mounted once
  (inside MantineProvider) for the imperative notification fns to display anything."
  notifications)

(defn show
  "Show a notification. The options map goes through the standard props converter:
  :message (required), :title, :color, :icon, :loading, :radius, :auto-close,
  :position, :priority, :id, :on-close, :on-open, ... Returns the notification id."
  [data]
  #?(:cljs (showNotification (p/convert data))
     :clj ((f/not-implemented "mantine.notifications/show") data)))

(defn hide
  "Hide the notification with the given id (raw string in and out)."
  [id]
  #?(:cljs (hideNotification id)
     :clj ((f/not-implemented "mantine.notifications/hide") id)))

(defn update
  "Update a shown notification; matched by :id in the options map (converted like
  `show`). Returns the id."
  [data]
  #?(:cljs (updateNotification (p/convert data))
     :clj ((f/not-implemented "mantine.notifications/update") data)))

(defn promise
  "Show a loading notification while the promise `prom` is pending, then update it to
  a success or error state. Returns `prom`.

  `opts` takes :id (shared by all three states), :loading, :success and :error.
  Each state is a notification options map, converted like `show`'s. :success and
  :error can instead be a fn: it receives the resolved value or the rejection reason
  raw, and the map it returns is converted. Mantine applies `loading: true` and
  `autoClose: false` to :loading, and teal / red colors to :success / :error. On
  settling it resets auto-close, so the final notification uses the provider's
  default unless its map sets :auto-close."
  [prom opts]
  #?(:cljs (promiseNotification
            prom
            (p/convert (cond-> opts
                         (fn? (:success opts)) (assoc :success (comp p/convert (:success opts)))
                         (fn? (:error opts)) (assoc :error (comp p/convert (:error opts))))))
     :clj ((f/not-implemented "mantine.notifications/promise") prom opts)))

(defn clean
  "Remove all notifications, active and queued."
  []
  #?(:cljs (cleanNotifications)
     :clj ((f/not-implemented "mantine.notifications/clean"))))

(defn clean-queue
  "Remove only queued notifications (not yet shown)."
  []
  #?(:cljs (cleanNotificationsQueue)
     :clj ((f/not-implemented "mantine.notifications/clean-queue"))))

(def use-notifications
  "Reactive hook over the default notifications store. Raw passthrough: returns the
  raw JS NotificationsState (read via interop: .-notifications, .-queue, ...)."
  #?(:cljs useNotifications
     :clj (f/not-implemented "mantine.notifications/use-notifications")))
