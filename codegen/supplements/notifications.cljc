(ns mantine.supplements.notifications
  "Hand-written supplement HOISTED by the generator into the generated
  mantine.notifications ns. Committed generator INPUT — compilable for editor/
  clj-kondo support, but never shipped as-is: its :require entries are merged into
  the generated ns and its top-level forms are appended after the codegen'd defs."
  (:refer-clojure :exclude [update])
  (:require
   [mantine.impl.factory :as f]
   #?@(:cljs [["@mantine/notifications" :refer [showNotification hideNotification
                                                updateNotification cleanNotifications
                                                cleanNotificationsQueue useNotifications]]
              [mantine.impl.props :as p]])))

(declare notifications)

(def provider
  "Alias for `notifications` — the renderer component that must be mounted once
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

(defn clean
  "Remove all notifications — active and queued."
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
