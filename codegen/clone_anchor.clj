;; One-shot input refresh for a version bump: clone the Mantine tag that matches the
;; package.json anchor, run its docgen with the yarn its package.json pins under
;; "packageManager" (yarn is not on PATH; corepack, fetched via npx, resolves the pin),
;; then hand the clone to extract/write-inputs!. Bump the package.json pins FIRST —
;; extract asserts clone == anchor.
;;
;;   bb clone-anchor            ; clones into target/mantine-<anchor>, reuses it if present
(ns clone-anchor
  (:require [anchor]
            [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [extract]))

(defn- yarn
  "Run the clone's pinned yarn in clone-dir. Mantine stopped vendoring yarn under
  .yarn/releases in 9.7.0; corepack reads the packageManager pin instead."
  [clone-dir & args]
  (apply shell {:dir clone-dir :extra-env {"COREPACK_ENABLE_DOWNLOAD_PROMPT" "0"}}
         "npx" "--yes" "corepack" "yarn" args))

(defn -main [& _]
  (let [version (anchor/anchor-version)
        clone-dir (str (fs/path "target" (str "mantine-" version)))]
    (if (fs/exists? (fs/path clone-dir ".git"))
      (println "Reusing clone at" clone-dir)
      (do (fs/create-dirs "target")
          (shell "git" "clone" "--depth" "1" "--branch" version
                 "https://github.com/mantinedev/mantine" clone-dir)))
    (yarn clone-dir "install")
    (yarn clone-dir "tsx" "scripts/docgen")
    (extract/write-inputs! clone-dir)))
