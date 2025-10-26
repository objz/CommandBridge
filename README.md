# CommandBridge scripting concept

These notes explain how the YAML scripting system for **CommandBridge** works and what I was thinking when I wrote the example in `concept.yml`.  They are written from my perspective as the author of the plugin (objz).  This is not meant to be polished end‑user documentation.  It jumps between high‑level ideas and concrete details, references the code with `@link` tags, and generally serves as a design doc for developers hacking on the bridge.

## Why scripts?

For a long time I hard‑coded commands in Java.  Every time I wanted to proxy a backend economy command or warp command I had to write and compile a new class.  That got old fast.  I wanted a way to declare commands in configuration instead of code.  YAML was the obvious choice: drop a file into `velocity/src/main/resources/scripts` (or whatever you configure) and the proxy will pick it up, validate it and register the commands.  The same script also instructs the backend what to execute when somebody runs the proxy command.  Essentially this is a thin configuration layer between Velocity and your Paper/Spigot backends.

Under the hood there is a `ScriptLoader` that binds the YAML into a `Script` record.  That record references other records like `Defaults`, `ArgMapping` and `CmdMapping`.  Each field has annotations such as `@Default`, `@Min`, `@Required`, `@Merge` and so on.  The processors in `dev.objz.commandbridge.scripting.validation.processor` walk through the bound record, apply defaults, validate ranges, ensure platform compatibility and collect problems.  If you want to understand what you can and cannot put in the YAML, read the code in `core/src/main/java/dev/objz/commandbridge/scripting/model` and the processors next to it.

## Top‑level keys

Every script starts with a few required top‑level keys.  These keys map directly to fields on the `Script` record:

* **version**: the schema version.  At the time of writing the current version is `2`.  The loader will reject scripts with unknown versions.  If you change the format in a breaking way, bump this number.

* **name**: the canonical name of your script and command.  This becomes the primary label on the proxy.  The `@Pattern` annotation on the `name` field enforces lower‑case letters, digits and hyphens (between three and thirty‑two characters).  When the command is registered on Velocity the permission node will be `commandbridge.command.<name>`.

* **description**: a short description shown in `/help` or plugin GUIs.  Keep it concise.

* **enabled**: if `false`, the script is ignored entirely.  Nothing is registered.  This makes it easy to disable a command without deleting the file.

* **aliases**: a list of alternative names.  Velocity (and the backend) both support alias registration.  This must be a YAML list even if you have only one alias.

* **permissions**: a nested record controlling whether the command requires a permission check.  When `enabled` is `true` the proxy looks for the permission `commandbridge.command.<name>`.  If the player lacks the permission and `silent` is `false` they will see an error message.  If `silent` is `true` the command simply does nothing on the proxy side.  See `@link dev.objz.commandbridge.scripting.model.Permissions`.

* **register**: this list explicitly tells CommandBridge where to register the proxy command.  Each entry is an `IdMapping` with two fields: `id` (the client id from your `clients.yml` or equivalent config) and `location` (either `VELOCITY` or `BACKEND` from the `Location` enum).  You can register the same command on multiple proxies if you run a network.  See `@link dev.objz.commandbridge.scripting.model.records.mapping.IdMapping`.

* **defaults**: a nested object that defines how commands behave by default.  The `Defaults` record has five top‑level fields:

  * `run-as`: one of `CONSOLE`, `PLAYER` or `OPERATOR` (see `@link dev.objz.commandbridge.scripting.model.enums.RunAs`).  This determines the executor context on the backend.  `CONSOLE` runs as the backend console (useful for commands requiring elevated privileges).  `PLAYER` runs as the player who triggered the proxy command.  `OPERATOR` temporarily grants op status to the player when executing the command.

  * `execute`: a list of `IdMapping` entries.  Each entry specifies an `id` and a `location`.  This list tells the bridge where to send the command for execution by default.  You can forward the same command to multiple backends by listing multiple entries.  The `Location` enum has two values: `VELOCITY` and `BACKEND`.  Typically you execute commands on a backend, not on the proxy.

  * `server`: a nested `Server` record with four fields: `target-required`, `schedule-online`, `timeout` and `frequency` (see `@link dev.objz.commandbridge.scripting.model.records.Server`).  These control how the proxy interacts with the backend when a command involves a player.  `target-required` (boolean) says whether the backend must see the player online or else the command fails.  `schedule-online` (boolean) enables deferred execution: if the player is offline the proxy will queue the command until they join.  `timeout` (duration) is how long to wait before aborting a scheduled command, and `frequency` (duration) determines how often to poll the backend for the player's online status.  Durations can be specified as `1s`, `5m`, `2h`, `1d` or bare numbers (interpreted as seconds).

  * `delay` and `cooldown`: durations applied to every command unless overridden.  `delay` waits before forwarding a command.  `cooldown` enforces a minimum interval between repeated uses of the same command by the same player.  A value of zero disables them.  See the `@Min` annotation on these fields for restrictions.

* **args**: a list of argument definitions.  Each entry binds to an `ArgMapping`.  An argument must have at least a `name` (the placeholder without `${}`), a `type` (from `ArgType`) and optionally a `required` flag and `suggestions`.  See the next section for details.

* **commands**: a list of command templates and overrides.  Each command in the list binds to a `CmdMapping`.  The `command` field is a template string that can contain `${placeholders}` referencing your arguments.  Each command may override any field in `Defaults` via the `@Merge` annotation on `CmdMapping`: `run-as`, `execute`, `server`, `delay` and `cooldown` can all be overridden locally.

## Arguments and types

Arguments are the inputs to your backend command.  They live under the top‑level `args` key.  The binder collects them into a list of `ArgMapping` records.  An argument has the following fields:

* **name**: the placeholder name used in your command template.  For example in `eco give ${player} ${amount}` the arguments are named `player` and `amount`.  Names must be unique.  The `ResolvableProcessor` rejects duplicates.

* **type**: one of the values from the `ArgType` enum.  The enum is annotated with `@Platform({ VELOCITY, BACKEND })` on each constant to indicate where it is supported.  If you use a backend‑only type in a command that is registered on the proxy, the `PlatformProcessor` will flag an error.  Supported types are:

  * `STRING` – a basic single token.
  * `INTEGER` – whole numbers.
  * `BOOLEAN` – true or false.
  * `DOUBLE` – floating point numbers.
  * `TEXT` – the remainder of the line, including spaces and quoted strings.
  * `RANGE` – a numeric range like `1..10`.  The current implementation uses CommandAPI’s `DoubleRangeArgument`, which accepts ranges but does not enforce a minimum or maximum.  There are no `min`/`max` fields in the model any more; if you want to validate bounds you will have to do it in your backend command.
  * `PLAYERS` – a player selector (returns one or more players).  There is no singular `PLAYER` type; selectors always return collections.
  * `ENTITIES` – an entity selector.
  * `ENTITY_TYPE` – the type of an entity (e.g. `zombie`).
  * `WORLD` – a world on the backend (backend only).
  * `SERVER` – a Velocity server (proxy only).
  * `LOCATION` – a 3D location (backend only).
  * `LOCATION_2D` – a 2D location (backend only).
  * `ANGLE` – a rotation angle (backend only).
  * `ROTATION` – a full rotation (backend only).
  * `ITEM_STACK`, `ENCHANTMENT`, `POTION_EFFECT`, `SOUND`, `BIOME` – various Minecraft types available only on the backend.
  * `TIME` – a time value (ticks or duration) on the backend.

* **required**: a boolean.  If `true`, the user must supply this argument when invoking the command.  It defaults to `false`.  Optional arguments can be omitted.  Required arguments that are missing cause the proxy to reject the command before contacting the backend.

* **suggestions**: a list of strings.  These are presented to the player as tab completions.  Suggestions are optional and completely free‑form.  You can leave this field out if you do not need suggestions.

There used to be fields like `min` and `max` on range arguments.  They were removed from the `ArgMapping` model.  The `DoubleRangeArgument` does not enforce numeric bounds.  If you want to limit a numeric argument you could use `INTEGER` or `DOUBLE` and check the bounds on the backend, or implement your own type and binder.

## Command templates and overrides

Each entry in the `commands` list is a `CmdMapping`.  The `command` field is a template string that can include placeholders like `${player}` or `${amount}`.  Placeholders are resolved at runtime in two passes: first by looking up defined arguments, and then by looking up default values for argument types via the `${args.TYPE}` syntax.  If a placeholder does not match any argument or type, the loader will report an error.

Command mappings inherit the values from `defaults` unless you override them.  The `@Merge` annotation on each field in `CmdMapping` means “if the field is not provided in the YAML, use the value from the defaults provider.”  For example:

```yaml
defaults:
  run-as: CONSOLE
  execute:
    - id: client-1
      location: BACKEND
  server:
    target-required: true
    schedule-online: true
    timeout: 1s
    frequency: 2s
  delay: 0s
  cooldown: 0s

commands:
  - command: "eco give ${player} ${amount}"
    run-as: CONSOLE        # overrides the default (still CONSOLE here)
    execute:
      - id: client-1
        location: BACKEND  # overrides the default target list (still client-1 here)
    server:
      target-required: false   # do not abort if the target is offline
      schedule-online: false   # do not schedule; run immediately
    delay: 2s                  # wait two seconds before executing
```

If you omit a field in a command, it inherits the corresponding value from `defaults`.  If you specify a field with the same value as the default, it is redundant.  The `DefaultProcessor` warns you about redundant overrides.

### Example: eco give (updated)

The `concept.yml` file in this repository defines a simple script that proxies the `/eco` command.  Here is a summary of how it works:

* The top‑level `register` section registers the command on the Velocity proxy (`client-1`).  You could register it on multiple proxies by adding more entries.

* The `defaults` section specifies that commands run as `CONSOLE` by default, forward to `client-1` on the backend, and require the target to be online or else schedule the command.  It also sets no delay and no cooldown.

* Under `args` two arguments are defined: `player` (type `PLAYERS`, required) and `amount` (type `RANGE`, required).  There are no `min`/`max` fields any more.  The `PLAYERS` type uses the entity selector syntax so the executor can type a player name or `@a` to select multiple players.  The `RANGE` type expects a number, a range like `5..10`, or a list of numbers according to how CommandAPI parses ranges.

* The single command template is `eco give ${player} ${amount}`.  At runtime the proxy substitutes `player` and `amount`, waits two seconds (because of `delay: 2s`) and forwards the final string to the backend console.  Because the command overrides `server.target-required` and `server.schedule-online` to `false`, it does not check whether the target player is online.  This is useful for commands like giving money; the backend economy plugin usually stores balances offline anyway.

You can add more command mappings to the list.  They are processed in order.  The first command whose placeholders can all be resolved will be executed.  This allows you to implement optional arguments by providing multiple templates with different placeholder sets.

## Handling multiple clients

In earlier versions of this system there was a `target.id` and a `kind` object specifying where to register and execute the command.  This has been replaced with two separate concepts:

* `register`: a top‑level list that tells the proxy where to expose the command.  Each entry has an `id` and a `location` (VELOCITY or BACKEND).  Commands can be registered on multiple proxies.  Registration is entirely separate from execution.

* `execute`: a list under `defaults` (and overridable per command) that tells the bridge where to forward the command for execution.  Each entry again has an `id` and a `location`.  You can forward the same command to multiple backends simultaneously by listing multiple entries here.  Execution entries with `location: VELOCITY` are rare but possible if you have a plugin on the proxy that understands your command.

Suppose you run a network with two proxies (`proxy-1` and `proxy-2`) and three backend servers (`survival`, `creative`, `lobby`).  You could configure a script like this:

```yaml
register:
  - id: proxy-1
    location: VELOCITY
  - id: proxy-2
    location: VELOCITY

defaults:
  run-as: PLAYER
  execute:
    - id: survival
      location: BACKEND
    - id: creative
      location: BACKEND
  server:
    target-required: true
    schedule-online: false
    timeout: 10s
    frequency: 2s
  delay: 0s
  cooldown: 5s

commands:
  - command: "homes ${player}"

```

This registers `/homes` on both proxies.  When a player runs the command, it is forwarded to both the survival and creative backends, running as the player.  It will abort if the player is offline (`target-required: true`) and uses a small cooldown to prevent spam.

## Server scheduling

The `server` section under `defaults` (and overridable per command) gives you control over deferred execution.  This is handy when your backend commands require the target player to be online.  The fields are:

* **target-required**: if `true` the bridge checks that the target player is online on the backend.  If they are not, and `schedule-online` is `false`, the command aborts immediately.  If `schedule-online` is `true`, the command is queued.

* **schedule-online**: if `true` and `target-required` is also `true`, the command is added to a queue.  The proxy periodically checks the backend to see if the player has logged in.  When they do, the command runs.

* **timeout**: how long to wait before giving up on a scheduled command.  Use suffixes (s, m, h, d) or bare numbers for seconds.

* **frequency**: how often to poll the backend while waiting.  Shorter frequencies react faster but do more work.

A typical use case is kicking a player when they next log in.  Here is an example using the new structure:

```yaml
version: 2
name: deferred-kick
register:
  - id: proxy
    location: VELOCITY
defaults:
  run-as: CONSOLE
  execute:
    - id: survival
      location: BACKEND
  server:
    target-required: true
    schedule-online: true
    timeout: 10m
    frequency: 30s
  delay: 0s
  cooldown: 0s
args:
  - name: player
    required: true
    type: PLAYERS
commands:
  - command: "kick ${player} You were naughty"

```

When the proxy receives `/deferred-kick Steve`, it checks whether Steve is online on the `survival` backend.  If not, it schedules the kick.  Every thirty seconds it asks the backend if Steve has joined.  If he doesn’t log in within ten minutes, the job is dropped.  When he finally joins, the backend runs `kick Steve You were naughty` as console.

## Extending argument types

The `ArgType` enum covers basic types like strings, numbers, booleans, selectors and various Minecraft objects.  If you want to add your own type you can extend the core.  Implement a new argument class (for example a duration parser) in the backend, add a corresponding constant to `ArgType` with the appropriate `@Platform` annotation, and provide a mapping in `ArgumentMapper`.  The binder uses reflection to map YAML fields into Java records.  If your new type requires additional fields, add them as parameters to a record implementing `ArgMapping` and write a processor to validate them.

For example, to add a **duration** argument that parses strings like `5m` or `30s`, you could implement a `DurationArgument` class on the backend and register it in `ArgumentMapper`.  Then add `DURATION` to `ArgType` and annotate it with `@Platform({ BACKEND })` if it only works on the backend.  The script loader and processors will pick it up automatically.  See `@link dev.objz.commandbridge.scripting.bind.TypeAdapterRegistry` for examples of how to adapt complex types.

## Tips and gotchas

* Always name your arguments uniquely.  Duplicate names are rejected.
* Use the correct case for enum values.  The YAML is case sensitive.  `PLAYERS` is different from `players`.
* There is no singular `PLAYER` type any more.  Use `PLAYERS` and rely on the selector syntax to pick one player.  CommandAPI’s selectors handle single and multiple targets gracefully.
* `RANGE` no longer has `min` and `max` fields.  If you need bounds, validate in your backend command or use `INTEGER`/`DOUBLE` and check the value yourself.
* `OPERATOR` replaces the old `OP-PLAYER`.  When you run a command as `OPERATOR` the player is temporarily given operator status on the backend during execution.  Be careful with anti‑cheat and other plugins; test thoroughly.
* Use `cooldown` to protect expensive commands from spam.  The cooldown is per player.  Multiple players can run the command concurrently.
* If you override a field with the same value as the default it has no effect.  The validator warns about redundant overrides.
* When in doubt, inspect the result of `ScriptLoader.load(...)`.  The loader returns a `LoadResult` containing a `ProblemSink`.  Log or print the problems to see what you might have missed.

## Conclusion

This scripting system is designed to make proxy ↔ backend command wiring declarative rather than imperative.  Instead of writing a Java command class for every command, you drop a YAML file with a few lines.  The rest is handled by the code in `dev.objz.commandbridge.scripting`.  If something doesn’t work, read the processors and the enum definitions — they are the source of truth.  I hope these notes help you understand the concept and adapt it to your own plugins.
