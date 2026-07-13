# Issue tracker: knot

Issues and specs for this repo are tracked with the `knot` CLI — a file-based
tracker that stores each ticket as a markdown file under `.tickets/` (closed
tickets auto-move to `.tickets/archive/`). Config is in `.knot.edn` at the repo
root (prefix `mnt`).

## The one rule: use the CLI

Read tickets only via `knot show` / `knot list` / `knot ready` / `knot blocked`
/ `knot closed` / `knot prime`. Write only via `knot create` / `knot start` /
`knot status` / `knot close` / `knot add-note` / `knot update` / `knot dep` /
`knot link`. Never `cat`, `grep`, `ls`, or hand-edit files under `.tickets/` —
`knot show <id>` resolves partial ids across live + archive and keeps
frontmatter consistent.

## When a skill says "publish to the issue tracker"

Create a ticket: `knot create "<title>" -t <type> -d "<description>"`. Set
`--mode afk` when the work is fully specified and an agent can run it end-to-end;
otherwise leave the `hitl` default. Add `--acceptance "<criterion>"` (repeatable)
for acceptance criteria, and `--parent <id>` to nest under an epic.

## When a skill says "fetch the relevant ticket"

`knot show <id>` (accepts partial ids). For a subset, filter rather than eyeball:
`knot list --type bug`, `knot ready --mode afk`, `knot list --tag <tag>`.

## When a skill says "publish a spec / PRD"

Specs live as knot tickets too — use type `epic` or `feature` and record the
spec body via `-d`/`--design`, with implementation tickets created as children
(`--parent <epic-id>`).

## For decision logic, use --json

Every read and write command accepts `--json` and emits a stable envelope
(`{schema_version, ok, data}`). Prefer `knot list --json | jq …` over parsing
tables. Mutating commands return the touched ticket under `.data` — no
read-after-write round-trip needed.

## Valid values (from `.knot.edn`)

- Statuses: `open` → `in_progress` → `closed` (terminal)
- Types: `bug`, `feature`, `task`, `epic`, `chore`
- Modes: `afk` (agent-runnable), `hitl` (needs a human)
- Priority: 0 (highest) … 4; default 2
