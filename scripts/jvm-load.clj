;; JVM-loadability guard: requires every generated mantine.* namespace on the JVM
;; (the :clj reader-conditional branch — no @mantine JS, only the not-implemented
;; stubs) and samples, per namespace, that calling a wrapper throws the named
;; ClojureScript-only error. Generated code that fails to load, or a stub that no
;; longer names itself, fails the build.
;;
;; Run with: clojure -M -e '(load-file "scripts/jvm-load.clj")'
(require '[clojure.java.io :as io]
         '[clojure.string :as str])
(import '[clojure.lang ExceptionInfo ArityException])

(def generated-nses
  (->> (.listFiles (io/file "src/main/mantine"))
       (filter #(.isFile %))
       (map #(.getName %))
       (filter #(str/ends-with? % ".cljc"))
       (map #(symbol (str "mantine." (subs % 0 (- (count %) (count ".cljc"))))))
       sort))

(defn sample-named-throw
  "Call public vars of ns-sym until one throws an ExceptionInfo naming itself as a
  ClojureScript-only wrapper. Arity-N supplement fns that reject a 0-arg call are
  skipped. Returns [var-sym message] or nil."
  [ns-sym]
  (some (fn [[sym v]]
          (when (fn? @v)
            (try (@v) nil
                 (catch ExceptionInfo e
                   (when (str/includes? (.getMessage e) "ClojureScript-only")
                     [sym (.getMessage e)]))
                 (catch ArityException _ nil))))
        (sort-by key (ns-publics ns-sym))))

(println "JVM-loading" (count generated-nses) "generated namespaces:")
(doseq [ns-sym generated-nses] (require ns-sym))
(println "  loaded:" (str/join " " (map str generated-nses)))

(let [results (for [ns-sym generated-nses] [ns-sym (sample-named-throw ns-sym)])
      unnamed (remove (comp second) results)]
  (doseq [[ns-sym [sym _]] results
          :when sym]
    (println (format "  %-24s call-time named throw OK (sampled %s)" ns-sym sym)))
  (if (seq unnamed)
    (do (println "JVM-LOAD FAILED — no named call-time throw sampled in:"
                 (str/join " " (map first unnamed)))
        (System/exit 1))
    (do (println (format "JVM-LOAD OK — %d namespaces load; each samples a named throw."
                         (count generated-nses)))
        (System/exit 0))))
