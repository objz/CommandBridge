this update fixes a placeholder bug that prevented some scheduled tasks from
ever resuming, adds a configurable task-expiry timer, and redesigns the admin
CLI around resources with consistent rendering across console and in-game chat.

so whats new:

**scheduled tasks**

- fixed `${arg}` placeholders in `execute` target IDs not being resolved at
  queue time, so affected tasks could never match a connected client and
  never resumed
- new `tasks.expire-after` config option (seconds; default 86400, 0 disables);
  expired tasks are pruned on load and on the 5-minute save tick
- tasks left over with unresolved `${...}` placeholders from earlier versions
  are dropped on load with a warning

**admin commands**

- redesigned the `/cb` tree by resource:
  - `/cb script list | show <name> [group] | enable <name> | disable <name>`
  - `/cb task list | clear <id>`
  - `/cb client list | ping [id] | players <id>`
  - `/cb config show [section] | reload`
- `/cb script show <name>` opens a clickable section navigator (permissions,
  register, defaults, args, commands); pass a group to drill into just that
  group as a syntax-highlighted yaml card
- `/cb script enable|disable <name>` edits the `enabled:` line of the matching
  yaml file in place (comments preserved) and re-pushes registrations to
  connected clients
- `/cb config show` mirrors the same summary + sections layout; drill into a
  section via tab-completion or by clicking it in chat
- every argument-taking subcommand has live tab-completion now (script names,
  script groups, task ids, client ids, config keys)

**rendering**

- single `Report` builder drives every command's chat output, so kvs, section
  headers, bullets, list items, action buttons, and pagination look the same
  everywhere
- `DebugPrinter` exposes generic `printRecord`, `printRecordOverview`, and
  `printList` so any record renders as the same yaml card the old
  `/cb scripts` view used

**removed**

- `/cb scripts`, `/cb tasks`, `/cb list`, `/cb ping` — replaced by the
  resource-scoped forms above (no backwards-compat aliases on v3)
- `/cb task show <id>` and `/cb client show <id>` — their list views already
  show everything useful, no point duplicating

latest commit: 76edfda
