;; The Mantine anchor — the one canonical wrapped Mantine version (ADR 0005).
;;
;; The exact @mantine/* pins in package.json are the anchor. Every other appearance
;; of the version (generator constant, banners, deps.cljs ranges, build.clj prefix)
;; is a rendering that derives from or validates against it.
;;
;; PURE core + thin I/O: the arity-1 fns are unit-testable over a passed pins map.
(ns anchor
  (:require [cheshire.core :as json]
            [clojure.string :as str]))

(defn pins
  "Map of @mantine/* package -> exact pinned version string, from package.json
  devDependencies. Arity-1 is the pure parse over the file text."
  ([] (pins (slurp "package.json")))
  ([pkg-json-text]
   (->> (get (json/parse-string pkg-json-text) "devDependencies")
        (filter (fn [[k _]] (str/starts-with? k "@mantine/")))
        (into {}))))

(defn anchor-version
  "The single Mantine anchor version. Asserts the pins map is non-empty and every
  pin agrees; throws ex-info listing the disagreeing pins otherwise. Arity-1 is
  pure over the passed map."
  ([] (anchor-version (pins)))
  ([pins-map]
   (when (empty? pins-map)
     (throw (ex-info "no @mantine/* pins found in package.json devDependencies" {})))
   (let [versions (set (vals pins-map))]
     (when (> (count versions) 1)
       (throw (ex-info (str "@mantine/* pins disagree — expected one anchor version, got: "
                            (str/join ", " (map (fn [[k v]] (str k " " v))
                                                (sort pins-map))))
                       {:pins pins-map :versions versions})))
     (first versions))))
