;; Anchor-bump tasks over a clone of the Mantine tag that matches the package.json
;; anchor. upstream-diff prints what changed in the upstream contract since HEAD's
;; anchor. -main is the one-shot input refresh: clone the tag, run its docgen with the
;; yarn its package.json pins under "packageManager" (yarn is not on PATH; corepack,
;; fetched via npx, resolves the pin), then hand the clone to extract/write-inputs!.
;; Bump the package.json pins first: extract asserts clone == anchor.
;;
;;   bb upstream-diff           ; diff stat of the upstream contract, HEAD's anchor -> the new one
;;   bb clone-anchor            ; clones into target/mantine-<anchor>, reuses it if present
(ns clone-anchor
  (:require [anchor]
            [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [extract]))

(def ^:private upstream-contract
  "The paths of a Mantine checkout whose change can break clone-anchor's yarn install,
  docgen or extract steps. Not every path those steps read: the docgen output under
  apps/mantine.dev/src/.docgen is gitignored upstream, so it has no diff between tags, and
  packages/@mantine/core/package.json changes on every release for its version alone."
  [;; vendored yarn releases, read by `yarn install` through .yarnrc.yml yarnPath
   ;; until 9.7.0
   ".yarn"
   ;; yarn config read by `yarn install`
   ".yarnrc.yml"
   ;; its packageManager field pins the yarn version corepack runs
   "package.json"
   ;; the docgen run before extract
   "scripts/docgen"
   ;; the mdx-*-data.ts docs-data files extract reads
   "apps/mantine.dev/src/mdx/data"
   ;; not read; a new docs-data file shows up here as a new import
   "apps/mantine.dev/src/mdx/mdx-data.ts"])

(defn- clone!
  "Clone the Mantine tag `version` into target/mantine-<version>, or reuse the clone
  already there. Returns the clone dir."
  [version]
  (let [clone-dir (str (fs/path "target" (str "mantine-" version)))]
    (if (fs/exists? (fs/path clone-dir ".git"))
      (println "Reusing clone at" clone-dir)
      (do (fs/create-dirs "target")
          (shell "git" "clone" "--depth" "1" "--branch" version
                 "https://github.com/mantinedev/mantine" clone-dir)))
    clone-dir))

(defn- yarn
  "Run the clone's pinned yarn in clone-dir. Mantine stopped vendoring yarn under
  .yarn/releases in 9.7.0; corepack reads the packageManager pin instead."
  [clone-dir & args]
  (apply shell {:dir clone-dir :extra-env {"COREPACK_ENABLE_DOWNLOAD_PROMPT" "0"}}
         "npx" "--yes" "corepack@0.36.0" "yarn" args))

(defn upstream-diff
  "Print the diff stat of the upstream contract between HEAD's anchor and package.json's.
  Clones the new anchor tag into target/mantine-<anchor>, or reuses that clone, and
  fetches the old tag into it. Prints a notice and clones nothing when the two anchors
  match. Meant to run before clone-anchor, whose yarn install a toolchain change can break."
  []
  (let [new-v (anchor/anchor-version)
        old-v (anchor/anchor-version
               (anchor/pins (:out (shell {:out :string} "git" "show" "HEAD:package.json"))))]
    (if (= old-v new-v)
      (println "HEAD and package.json both anchor at" new-v "- nothing to diff.")
      (let [clone-dir (clone! new-v)]
        (shell {:dir clone-dir} "git" "fetch" "--depth" "1" "origin" "tag" old-v)
        (println (str "Upstream contract, " old-v " -> " new-v ":"))
        (apply shell {:dir clone-dir} "git" "diff" "--stat" old-v new-v "--" upstream-contract)))))

(defn -main [& _]
  (let [clone-dir (clone! (anchor/anchor-version))]
    (yarn clone-dir "install")
    (yarn clone-dir "tsx" "scripts/docgen")
    (extract/write-inputs! clone-dir)))
