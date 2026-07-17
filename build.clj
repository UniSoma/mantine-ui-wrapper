;; Source-only jar build + Clojars deploy for io.github.unisoma/mantine-ui-wrapper.
;; Driven by `bb jar` / `bb install` / `bb deploy` (see bb.edn, docs/release.md).
;; Version scheme 9.4.1.N — first three segments ARE the wrapped Mantine version,
;; N is the wrapper revision against it (see docs/adr/0001-clojars-release-process.md).
(ns build
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'io.github.unisoma/mantine-ui-wrapper)
(def version "9.4.1.0-SNAPSHOT")
(def class-dir "target/classes")
(def basis (delay (b/create-basis {:project "deps.edn"})))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn- pom-basis
  "The deployed pom's dependency list. The published artifact is CLJS source only —
  consumers bring their own ClojureScript/Clojure — so it has NO runtime deps. The
  org.clojure/clojure in deps.edn is for local JVM tooling only (clj-kondo, cljdoc,
  jvm-load); keep it OUT of the pom so we don't pin consumers to a Clojure version."
  []
  (b/create-basis {:root nil :user nil :project {:deps {}}}))

(defn- scm-tag
  "The git revision cljdoc checks out to read sources + doc/cljdoc.edn. SNAPSHOTs have
  no `v<version>` tag, so use the built commit SHA (must be pushed to GitHub); releases
  use the immutable `v<version>` tag (cut + pushed per docs/release.md)."
  []
  (if (str/ends-with? version "-SNAPSHOT")
    (str/trim (:out (sh/sh "git" "rev-parse" "HEAD")))
    (str "v" version)))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar
  "Build the source-only jar: src/main (generated .cljc + impl + deps.cljs) + pom."
  [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis (pom-basis)
                :src-dirs ["src/main"]
                :scm {:url "https://github.com/unisoma/mantine-ui-wrapper"
                      :connection "scm:git:git://github.com/unisoma/mantine-ui-wrapper.git"
                      :developerConnection "scm:git:ssh://git@github.com/unisoma/mantine-ui-wrapper.git"
                      :tag (scm-tag)}
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
