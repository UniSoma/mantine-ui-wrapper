;; One-shot input refresh for a version bump: clone the Mantine tag that matches the
;; package.json anchor, run its docgen with the yarn the clone vendors (yarn is not on
;; PATH; the clone pins it under .yarn/releases via .yarnrc.yml), then hand the clone to
;; extract/write-inputs!. Bump the package.json pins FIRST — extract asserts clone == anchor.
;;
;;   bb clone-anchor            ; clones into target/mantine-<anchor>, reuses it if present
(ns clone-anchor
  (:require [anchor]
            [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [extract]))

(defn- vendored-yarn
  "Clone-relative path of the yarn release the clone vendors (run with :dir clone-dir)."
  [clone-dir]
  (if-let [yarn (first (fs/glob (fs/path clone-dir ".yarn/releases") "yarn-*.cjs"))]
    (str ".yarn/releases/" (fs/file-name yarn))
    (throw (ex-info (str "no vendored yarn under " clone-dir "/.yarn/releases") {}))))

(defn -main [& _]
  (let [version (anchor/anchor-version)
        clone-dir (str (fs/path "target" (str "mantine-" version)))]
    (if (fs/exists? (fs/path clone-dir ".git"))
      (println "Reusing clone at" clone-dir)
      (do (fs/create-dirs "target")
          (shell "git" "clone" "--depth" "1" "--branch" version
                 "https://github.com/mantinedev/mantine" clone-dir)))
    (let [yarn (vendored-yarn clone-dir)]
      (shell {:dir clone-dir} "node" yarn "install")
      (shell {:dir clone-dir} "node" yarn "tsx" "scripts/docgen"))
    (extract/write-inputs! clone-dir)))
