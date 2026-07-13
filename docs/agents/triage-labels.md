# Triage Labels

The skills speak in terms of five canonical triage roles. This repo tracks
issues with `knot`, which has no free-form "labels" — instead these roles map
onto knot's native dimensions (`mode`, `status`) where one exists, and onto
tags otherwise.

| Canonical role    | Knot representation                       | Meaning                                  |
| ----------------- | ----------------------------------------- | ---------------------------------------- |
| `needs-triage`    | tag `needs-triage`                        | Maintainer needs to evaluate this issue  |
| `needs-info`      | tag `needs-info`                          | Waiting on reporter for more information |
| `ready-for-agent` | `--mode afk`                              | Fully specified, ready for an AFK agent  |
| `ready-for-human` | `--mode hitl`                             | Requires human implementation            |
| `wontfix`         | `knot close --summary …` + tag `wontfix`  | Will not be actioned                     |

## How skills apply each role

- **ready-for-agent / ready-for-human** — set the mode:
  `knot update <id> --mode afk` (or `--mode hitl`). Query agent-runnable work
  with `knot ready --mode afk`. Mode is the contract: don't autonomously pick up
  `hitl` tickets.
- **needs-triage** — `knot update <id> --add-tag needs-triage`. Remove it once
  the ticket has been assessed (mode set, priority set):
  `knot update <id> --remove-tag needs-triage`.
- **needs-info** — `knot update <id> --add-tag needs-info`. List with
  `knot list --tag needs-info`.
- **wontfix** — `knot update <id> --add-tag wontfix` then
  `knot close <id> --summary "wontfix: <reason>"`. The tag keeps it queryable in
  the archive via `knot list --tag wontfix`.
