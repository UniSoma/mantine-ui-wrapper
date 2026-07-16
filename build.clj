;; Source-only jar build + Clojars deploy for io.github.unisoma/mantine-ui-wrapper.
;; Driven by `bb jar` / `bb install` / `bb deploy` (see bb.edn, docs/release.md).
;; Version scheme 9.4.1.N — first three segments ARE the wrapped Mantine version,
;; N is the wrapper revision against it (see docs/adr/0001-clojars-release-process.md).
(ns build
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'io.github.unisoma/mantine-ui-wrapper)
(def version "9.4.1.0-SNAPSHOT")
(def class-dir "target/classes")
(def basis (delay (b/create-basis {:project "deps.edn"})))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar
  "Build the source-only jar: src/main (generated .cljc + impl + deps.cljs) + pom."
  [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis @basis
                :src-dirs ["src/main"]
                :scm {:url "https://github.com/unisoma/mantine-ui-wrapper"
                      :connection "scm:git:git://github.com/unisoma/mantine-ui-wrapper.git"
                      :developerConnection "scm:git:ssh://git@github.com/unisoma/mantine-ui-wrapper.git"
                      :tag (str "v" version)}
                :pom-data [[:licenses
                            [:license
                             [:name "MIT License"]
                             [:url "https://opensource.org/license/mit"]
                             [:distribution "repo"]]]]})
  (b/copy-dir {:src-dirs ["src/main"] :target-dir class-dir})
  (b/jar {:class-dir class-dir :jar-file jar-file})
  (println "JAR" jar-file))

(defn install
  "Install the jar into the local ~/.m2 repository (for local consumer verification)."
  [_]
  (jar nil)
  (b/install {:basis @basis :lib lib :version version
              :jar-file jar-file :class-dir class-dir})
  (println "INSTALLED" lib version "-> ~/.m2"))

(defn deploy
  "Deploy the jar to Clojars. Needs CLOJARS_USERNAME / CLOJARS_PASSWORD (a deploy token)."
  [_]
  (jar nil)
  (dd/deploy {:installer :remote
              :artifact jar-file
              :pom-file (b/pom-path {:lib lib :class-dir class-dir})})
  (println "DEPLOYED" lib version "-> Clojars"))
