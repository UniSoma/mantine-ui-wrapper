;; Consumer-facing npm-dep declaration shipped in the jar: shadow-cljs auto-installs
;; these (npm install --save --save-exact) when absent. react/react-dom deliberately
;; NOT shipped — the consuming app owns React (avoids split-context/duplicate React).
;; Only PEER deps of the @mantine/* packages are declared here (dayjs, recharts);
;; regular deps ride transitively via npm (e.g. rrule via @mantine/schedule).
{:npm-deps {"@mantine/core" "^9.4.1"
            "@mantine/hooks" "^9.4.1"
            "@mantine/notifications" "^9.4.1"
            "@mantine/schedule" "^9.4.1"
            "@mantine/modals" "^9.4.1"
            "@mantine/form" "^9.4.1"
            "@mantine/spotlight" "^9.4.1"
            "@mantine/dates" "^9.4.1"
            "@mantine/charts" "^9.4.1"
            "@mantine/dropzone" "^9.4.1"
            "dayjs" "^1.11.21"
            "recharts" "^3.9.2"}}
