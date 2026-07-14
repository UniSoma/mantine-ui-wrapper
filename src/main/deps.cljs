;; Consumer-facing npm-dep declaration shipped in the jar: shadow-cljs auto-installs
;; these (npm install --save --save-exact) when absent. react/react-dom deliberately
;; NOT shipped — the consuming app owns React (avoids split-context/duplicate React).
{:npm-deps {"@mantine/core" "^9.4.1"
            "@mantine/hooks" "^9.4.1"
            "@mantine/notifications" "^9.4.1"}}
