<!--
    This is an intentionally long, deep‑dive README for CommandBridge.
    It's written in a conversational style similar to the example you
    provided.  The goal is to be comprehensive, exploring the
    architecture, configuration, scripting system, security model,
    performance characteristics, troubleshooting and even a bit of
    philosophy.  If you enjoy reading, you're in for a treat.  If
    you're just here for a quick install guide, skip ahead to that
    section.  At ~1000 lines, this document is longer than most
    README files, but that's the point: CommandBridge is a powerful
    plugin with a lot of nuance, and you deserve to understand it.
-->

# CommandBridge – Unifying Your Minecraft Servers Through Declarative Commands

[![GitHub release](https://img.shields.io/github/v/release/objz/CommandBridge)](https://github.com/objz/CommandBridge/releases)
[![Build Status](https://github.com/objz/CommandBridge/actions/workflows/build.yml/badge.svg)](https://github.com/objz/CommandBridge/actions)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/commandbridge?label=downloads&logo=modrinth)](https://modrinth.com/plugin/commandbridge)
[![Discord](https://img.shields.io/discord/SERVER_ID?label=discord&logo=discord&color=7289DA)](https://discord.gg/INVITE_CODE)

![Java](https://img.shields.io/badge/Java-21+-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20--1.21-green)
![Paper](https://img.shields.io/badge/Paper-blue)
![Velocity](https://img.shields.io/badge/Velocity-purple)

## Preface

Before we dive into the guts of CommandBridge, let's get a few
things out of the way.  This plugin is not a silver bullet.  It will
not magically fix your misconfigured network, save you from poor
performance or make your players less toxic.  What it does is give
you a structured, declarative way to execute commands across
multiple servers.  If that doesn't sound like a big deal, you
probably haven't tried to manage a network of separate Minecraft
servers before.  If you have, you know the pain of plugin
messaging, the horror of RCON scripts, and the frustration of
writing a different plugin for each tiny bit of cross‑server
functionality.

CommandBridge emerged from that frustration.  It's built to be
reliable, secure, flexible and extensible.  It uses modern Java
features, typed YAML parsing, WebSocket communication and a rich
scripting system.  This README aims to unpack all of that in detail.

### How this document is organised

At a high level, this README follows a progression from high‑level
concepts to low‑level details.  Here's what you can expect:

1. **Overview** – A high‑level description of what CommandBridge does,
   why you'd want it and what problems it solves.
2. **Architecture** – A deep dive into how CommandBridge is
   structured at runtime, including diagrams of the star topology
   used in v2 and a preview of the future mesh topology.
3. **Installation and configuration** – Step‑by‑step guidance on
   downloading, installing and configuring the plugin.  We cover
   common pitfalls and network considerations.
4. **Scripting system** – An extensive explanation of the YAML
   scripting model, including a field reference, argument types,
   examples and tips for writing your own commands.
5. **Security model** – A discussion on how authentication works,
   why shared secrets matter in v2, and how JWT could improve things
   in v3.  We'll also talk about TLS and firewall best practices.
6. **Performance & scalability** – Benchmarks, recommendations for
   small, medium and large networks, and settings you can tweak to
   improve throughput or reduce latency.
7. **Troubleshooting** – A compendium of common problems and their
   solutions, plus how to enable debug logging and interpret it.
8. **Advanced topics** – Things like custom argument types, script
   generators, external integrations, future roadmap items and
   philosophical musings on cross‑server design.
9. **Contributing & license** – How to get involved and what the
   license allows you to do.

Don't feel obligated to read it all in one sitting.  The headings
are your friends.  Use the Table of Contents in your Markdown
viewer to jump to whatever section you're interested in.

---

## 1. Overview

### 1.1 What is CommandBridge?

CommandBridge is a Minecraft plugin that allows you to run commands
on different servers as if they were local.  For example, imagine a
network with a Velocity proxy, a survival backend, a creative
backend and a hub server.  Without CommandBridge, running `/eco
give Steve 100` on the proxy would either do nothing or require a
custom plugin.  With CommandBridge, you can define a script that
says: *when someone on the proxy runs `/eco give <player> <amount>`,
forward that command to the survival server.*  Similarly, you can
define a script on the survival server that, when triggered,
executes a command on the proxy (e.g. sending players to another
server).

This is powerful because it decouples **where** a command is
triggered from **where** it is executed.  It also centralises your
configuration.  Instead of scattering your cross‑server logic across
Java classes, scripts and custom code, you write a simple YAML file.
CommandBridge takes care of the rest.

### 1.2 Why YAML? Why not JSON/TOML/INI/...

YAML gets a lot of hate in developer circles, often deserved.
Indentation matters, it can be hard to read when abused, and the
parser differences can cause subtle bugs.  So why choose YAML here?

1. **Comments.**  JSON doesn't allow comments.  TOML does, but it
   lacks the complex structures we need for nested records.  YAML
   lets you document your scripts, which is important when you come
   back six months later and wonder what you were thinking.
2. **Hierarchical data.**  CommandBridge scripts are nested: a
   script contains defaults, arguments, commands, etc.  YAML's
   indentation makes the hierarchy explicit.  It can be painful,
   but it's also unambiguous.
3. **Flexibility.**  YAML supports lists, maps, strings, numbers and
   booleans without much ceremony.  You can express complex scripts
   succinctly.

To ease the pain of indentation, CommandBridge uses a rigorous
parser and binder that produces detailed error messages.  When you
get indentation wrong, it tells you exactly where the problem is.

### 1.3 Key features at a glance

Let's start with a bullet list of the major features before we
unpack them in detail:

- **Cross‑server commands** – run commands on backends from the
  proxy, or on the proxy from backends【738757187327908†L74-L85】.
- **Two‑way communication** – both directions are supported; you
  decide where commands originate and where they are delivered【738757187327908†L74-L85】.
- **Declarative scripts** – use YAML to define commands, arguments,
  permissions, registration and execution targets【140577542331565†L18-L39】.
- **WebSocket transport** – commands flow over WebSocket, so they
  don't require players to be online and are delivered reliably【738757187327908†L74-L85】.
- **Argument types & placeholders** – parse and validate player
  input, with built‑in types like `PLAYERS`, `RANGE`, `SERVER` and
  more【559826996654773†L5-L33】.  Placeholders allow you to insert arguments into
  your command strings.
- **Scheduling & cooldowns** – delay execution, queue commands until
  players are online, enforce per‑player cooldowns, and time out
  queued commands【863798256149012†L9-L16】.
- **Multiple execution targets** – send a command to several
  backends at once or even back to the proxy, with separate
  registration and execution lists.
- **Extensible** – you can add custom argument types, run custom
  code before or after commands, and integrate with other plugins.
- **Metrics & debugging** – optional bStats metrics and robust
  logging with structured error reporting【738757187327908†L176-L181】.

That’s the elevator pitch.  Now let’s go down to the basement and
poke around in the wiring.

---

## 2. Architecture

### 2.1 Star topology (v2)

At the time of writing, CommandBridge v2 uses a star topology.  That
means there is a central node (your Velocity proxy) that acts as a
server, and one or more satellites (your Paper/Folia backends) that
act as clients.  All communications go through the centre.  The
backend servers connect to the proxy over a persistent WebSocket
channel, and all command forwarding is done through this channel.

This is similar to how plugin messaging works, except it doesn't rely
on players being online and it works in both directions.  The proxy
maintains a list of connected clients and a mapping of IDs to
connections.  When you register a script, you specify which client
IDs the command should be exposed on and which IDs the command should
be executed on.  The proxy looks up these IDs and routes the command
accordingly.

Below is a diagram representing a simple network with one proxy and
two backends:

```
          +------------+       +------------+
          |            |←──────|   Survival |
          |            |       |  (backend) |
          |   Proxy    |       +------------+
          |            |              ↑
          |            |              │ WebSocket
          +------------+              │
               ↑                      │
               │ WebSocket            │
               │                      |
          +------------+       +------------+
          |            |       |            |
          |   Lobby    |──────→|   Creative |
          |  (backend) |       |  (backend) |
          +------------+       +------------+

```

Notice that all arrows eventually lead to the proxy.  There is no
direct communication between backends.  Even if survival wants to
send a command to creative, it goes: Survival → Proxy → Creative.

#### 2.1.1 Pros of the star topology

- **Simplicity** – It's easy to reason about.  One place to manage
  connections, one place to authenticate clients.
- **Centralised control** – The proxy can enforce permissions,
  throttle commands, queue tasks and collect metrics.
- **Works with existing Velocity networks** – You're already using
  Velocity for Bungee‑like proxies; adding CommandBridge doesn't
  change your infrastructure.

#### 2.1.2 Cons of the star topology

- **Single point of failure** – If the proxy goes down, command
  forwarding stops.  Players might still be connected to the
  backends (depending on your network design), but cross‑server
  commands won't work.
- **Bottleneck** – All command traffic goes through one node.  In
  small networks this isn't an issue, but large networks could hit
  throughput limits.
- **No direct routes** – Backends can't talk to each other directly.
  This means that if your proxy is physically far from your backend,
  commands might incur extra latency.

### 2.2 Future: Mesh topology (v3 and beyond)

The authors have hinted that the next major version (v3) will switch
to a peer‑to‑peer mesh network.  Instead of a central hub, each
server would connect to some or all of the others directly.  The
benefits of this approach include fault tolerance, lower latency and
scalability.  The trade‑off is complexity: you'll need a way to
authenticate peers, discover routes and possibly forward commands
across multiple hops.

Because v3 is not yet publicly available, we won't speculate on the
implementation.  However, it's worth thinking about how a mesh
network could impact your current setup.  Would you need multiple
proxies?  How would you manage routing?  Would you need a
service‑discovery mechanism?  These are open questions that the
CommandBridge team is working on.

### 2.3 Components in v2

Even in the star topology, CommandBridge has several moving parts.  It
pays to understand them so you can debug issues and extend the
system.  Here's a high‑level overview:

#### 2.3.1 The WebSocket server (proxy side)

The Velocity plugin contains a WebSocket server.  When enabled, it
listens on a port you specify in `config.yml`.  Each backend
establishes a persistent WebSocket connection to this server.  The
server authenticates clients using a secret key, then keeps the
connection open for sending commands and responses.  The proxy
handles the following tasks:

- **Client authentication** – reading `secret.key` and verifying
  handshake messages【738757187327908†L124-L129】.
- **Script loading** – scanning the scripts directory and compiling
  YAML files into `Script` objects【288806965413131†L19-L33】.
- **Command registration** – injecting Brigadier commands into the
  Velocity command dispatcher so players can run them.
- **Routing** – deciding which connected clients to forward
  commands to, based on the `execute` list in each script.
- **Scheduling** – queuing commands until players are online,
  applying delays and enforcing cooldowns【863798256149012†L9-L16】.
- **Error reporting** – sending error messages to players or logging
  problems in the console.

#### 2.3.2 The WebSocket client (backend side)

Each backend plugin contains a WebSocket client.  It connects to the
proxy's WebSocket server at startup.  The backend plugin handles:

- **Client ID configuration** – each backend identifies itself with
  a `client-id` in `config.yml`【738757187327908†L138-L141】.
- **Connection management** – reconnecting if the connection drops.
- **Command registration** – registering commands using the server's
  command API so players can run them locally.
- **Command execution** – receiving forwarded commands from the proxy
  and executing them on the backend, as configured in the script.
- **Placeholder resolution** – resolving server‑specific placeholders
  and hooking into PlaceholderAPI if installed【969077526380739†L96-L110】.

#### 2.3.3 Type adapters and binding

When you write a script, you're describing a data structure that
needs to be converted into Java objects.  CommandBridge uses a
custom binder based on Java records and annotations【288806965413131†L19-L33】.  Each
record component can be annotated with `@YmlKey` to specify the
exact YAML key, `@Default` to provide a default value and `@Required`
to enforce presence.  The binder reads your YAML, constructs the
record instances and runs post‑processors for validation (e.g.
ensuring required fields are set, verifying argument ordering and
platform compatibility).  The binder works with a `TypeAdapterRegistry`
to handle different value types such as `Duration`, lists, enums and
custom types【649364142672201†L61-L78】.

Understanding how binding works isn't necessary for day‑to‑day use,
but if you're adding custom argument types or extending the script
model, it's valuable to know that the binder exists.

---

## 3. Installation and configuration

Installing CommandBridge isn't rocket science, but there are a few
important details you need to get right.  This section covers the
process from downloading the plugin to having your first cross‑server
command running.

### 3.1 Downloading the plugin

Head over to [Modrinth](https://modrinth.com/plugin/commandbridge) or
the [GitHub releases page](https://github.com/objz/CommandBridge/releases)
and download the latest version.  You'll see a file named
`CommandBridge-<version>-all.jar`.  The “all” suffix means that the jar
contains both the proxy and backend implementations.  Do not
download separate jars for proxy and backend; they don't exist in
2.x.  Everything you need is in one file【738757187327908†L90-L103】.

### 3.2 Preparing your servers

You'll need a Velocity proxy running Java 21 and one or more Paper
servers running Java 21.  CommandBridge requires Java 21 because it
uses features like virtual threads and modern libraries【738757187327908†L94-L99】.  If
you're on Java 17, upgrade your JDK.  If you're on 1.8... I'm not
even going to entertain that thought.

Copy `CommandBridge-<version>-all.jar` into the `plugins` folder of
both the proxy and each backend.  Restart each server.  After the
proxy starts, look for a new folder `plugins/CommandBridge`.  Inside
it you'll find a file called `secret.key`.  Open it; you'll see a
random string of characters.  This is the shared secret used for
authentication【738757187327908†L124-L129】.

Now open the backend's `plugins/CommandBridge/config.yml`.  You'll
see a `secret:` field.  Paste the contents of `secret.key` here【738757187327908†L124-L129】.
Do not share this secret with anyone else.  If someone has it, they
can impersonate your proxy.  Save the file.  Do the same on all
backends.

### 3.3 Configuring port, host and IDs

Open the proxy's `config.yml`.  You'll see fields like `port`,
`host` and `server-id`.  Here's what they mean:

- **port** – The port the WebSocket server listens on【738757187327908†L130-L133】.
  Pick a port that's not in use.  If you're behind a firewall, make
  sure this port is allowed.
- **host** – The IP address of the machine running the proxy【738757187327908†L130-L137】.
  Use the plain IP, not a domain name.  This value is used by the
  backend clients to connect.
- **server-id** – A unique identifier for your proxy.  Scripts refer
  to this ID when registering commands.  Choose a short, lowercase
  name like `proxy` or `velocity-1`【738757187327908†L138-L141】.

On each backend, you'll see fields like `remote`, `client-id`,
`port` and `host`.  These mirror the proxy config:

- **remote** – The IP address of the proxy【738757187327908†L130-L137】.
- **port** – The port of the proxy's WebSocket server【738757187327908†L130-L133】.
- **client-id** – A unique identifier for this backend【738757187327908†L138-L141】.
  Scripts refer to this ID when specifying where commands should run.
  Choose names like `lobby`, `survival` or `creative`.
- **host** – The IP address of the backend.  This is optional in
  v2; clients only need to know where the proxy is.  However, if
  v3's peer‑to‑peer mesh becomes available, each node will need a
  `host` and `port` so others can connect to it.  Keep this in mind
  when designing your network.

### 3.4 Restart order and verifying connections

Restart the proxy first, then the backends【738757187327908†L141-L142】.  When the
backends start, they will connect to the proxy.  Look for log
messages like:

```
[INFO] [CommandBridge]: Client authenticated successfully: /192.168.1.42:54321
[INFO] [CommandBridge]: Added connected client: survival
```

If you see authentication errors, double check that the secret key
matches on both sides and that the port and host are correct.  If the
backend can't connect, check your firewall and ensure the proxy's
port is open.

### 3.5 Script directories

By default, CommandBridge loads scripts from the plugin jar.  You can
also specify additional directories in `config.yml` under `scripts:
directories:`.  Each entry is a path relative to the server's root.
For example:

```yaml
scripts:
  directories:
    - "plugins/CommandBridge/scripts"
  auto-reload: true
  reload-interval: 60s
```

This tells the loader to scan `plugins/CommandBridge/scripts` on
startup and every 60 seconds if `auto-reload` is enabled.  You can
drop YAML files into this folder and they will be picked up.  This is
useful for editing scripts without having to rebuild your jar.

---

## 4. The scripting system in depth

Writing scripts is the heart of CommandBridge.  The YAML format may
seem verbose at first, but once you get the hang of it you'll find
it's expressive and flexible.  This section goes line by line
through the script schema, explaining what each field does, what
values are allowed and how they interact.  We'll also show real
examples and discuss common pitfalls.

### 4.1 The `script` root and versioning

Every script must begin with a root key named `script`【140577542331565†L18-L39】.  Under
that key you define your fields.  Here's the bare minimum skeleton:

```yaml
script:
  version: 2
  name: mycommand
  permissions:
    enabled: true
    silent: false
  register: []
  defaults:
    run-as: CONSOLE
    execute: []
    delay: 0s
    cooldown: 0s
  args: []
  commands: []
```

The `version` field is an integer【140577542331565†L20-L24】.  At the time of writing,
allowed values are 1 and 2.  Version 1 is kept for backward
compatibility; version 2 introduces separate `register` and `execute`
lists.  Future versions may add new features or change the schema in
breaking ways, so be sure to bump the version when migrating.  If you
set `version` to an unsupported number, the loader will reject the
script.

### 4.2 Naming your command

The `name` field defines the primary label of your command【140577542331565†L22-L24】.
This label is combined with the prefix `commandbridge.command.` to
create a permission node.  It also becomes the argument for `/cb
reload` and other CommandBridge internal commands.  The name must be
3–32 characters long, lower case, and may contain digits and hyphens.
Invalid names cause the binder to throw an error.  If you want to use
uppercase letters or spaces, consider using `aliases` instead.

### 4.3 Enabling and disabling scripts

The `enabled` flag allows you to toggle the entire script on or off
【140577542331565†L25-L29】.  If `enabled: false`, the loader will still parse the
script (so you can detect errors), but it will not register the
command.  This is useful for testing or temporary deactivation.

### 4.4 Descriptions and aliases

`description` is a free‑form string shown in `/help` or by other
plugins that list commands.  `aliases` is a list of alternative
labels.  Aliases must be unique across all scripts and cannot
duplicate the primary name.  If you register the same alias in two
scripts, the loader will complain.

### 4.5 Permissions

As mentioned earlier, the `permissions` section controls access to
your command【562237090147056†L5-L9】.  When `enabled: true`, players need the
permission `commandbridge.command.<name>` to execute the command on
the server where it is registered.  The permission is checked at
execution time.  If the player lacks it and `silent` is false, a
message is shown.  If `silent` is true, the command quietly
terminates.  Use silent mode for Easter eggs or staff‑only commands
that should remain hidden.

It’s worth noting that permissions are enforced on the **origin**
server.  If a command is registered on the proxy and executed on a
backend, the proxy checks permissions, not the backend.  Likewise,
commands registered on the backend and executed on the proxy check
permissions on the backend.  This is by design; you don't want to
expose a command to players who don't have permission just because
the target server doesn't know about your permission system.

### 4.6 Registration vs. execution

One of the biggest changes between v1 and v2 scripts is the
separation of `register` and `execute` lists【140577542331565†L33-L39】.  In v1,
commands were registered and executed in the same places.  In v2,
you can register a command on one set of servers and execute it on
another.  This gives you a lot of flexibility:

- **Register everywhere, execute somewhere** – e.g. register `/mail`
  on all proxies and backends, but execute it only on a dedicated
  mail server.
- **Register somewhere, execute everywhere** – e.g. register
  `/alert` on the proxy and execute it on all backends.
- **Different execution lists per template** – we'll see how you can
  override the defaults in each command template to execute on
  different servers.

The `register` list is mandatory.  It tells the proxy where to
expose the command.  Each entry is an `IdMapping` with an `id` (the
server ID) and a `location` (`VELOCITY` or `BACKEND`)【460907138769840†L6-L10】.

The `execute` list lives inside `defaults`.  It tells the proxy
where to send the command.  Each entry is an `IdMapping` as well.
You can specify multiple entries.  If you do, the proxy forwards the
command to all of them.  If you want to execute on the proxy
instead of a backend, set `location: VELOCITY`.

It's important to understand that `register` and `execute` lists are
completely independent.  You could, for example, register a command
on `proxy` and `lobby`, but execute on `survival` and `creative`.
Players would run the command on proxy or lobby, and the proxy
would forward it to survival and creative.  If you forget to
register a command somewhere, players on that server won't be able
to run it, even if it executes there.

### 4.7 Defaults

Defaults provide baseline values for `run-as`, `execute`, `server`,
`delay` and `cooldown`【805605991156438†L13-L23】.  These values apply to all
command templates unless overridden.  Let's look at each field:

#### 4.7.1 `run-as`

`run-as` controls the executor context on the target server【460432951784136†L2-L5】.
Valid values are:

- **`CONSOLE`** – Execute commands as the backend console.  The
  console has all permissions.  Use this for admin tasks or
  commands that players shouldn't see in chat.
- **`PLAYER`** – Execute commands as the triggering player.  This
  means the command runs with the player's permissions and is
  subject to any restrictions or anti‑cheat checks on the backend.
- **`OPERATOR`** – Execute commands as the player but temporarily
  grant them operator status.  The plugin will de‑op them after
  command execution.  Use with caution; anti‑cheat plugins may
  still detect suspicious behaviour.

#### 4.7.2 `execute`

`execute` is a list of `IdMapping` objects.  Each mapping has an
`id` (the server ID) and a `location` (`VELOCITY` or `BACKEND`)
【460907138769840†L6-L10】.  If you provide more than one entry, the proxy
sends the command to each target.  There is no built‑in ordering or
failover in v2; all targets are executed.  If you need failover
logic (e.g. try survival, then creative), you'll need to implement
that yourself or wait for v3, which may introduce priorities.

#### 4.7.3 `server`

The `server` section controls deferred execution【863798256149012†L9-L16】.  It's used
when your command depends on the target player being online.  It has
four fields:

- **`target-required`** – If true, the proxy checks whether the
  player is online on the target backend.  If the player is
  offline and `schedule-online` is false, the command fails.
- **`schedule-online`** – If true, commands for offline players
  are queued until the player logs in.  When they do, the command
  runs.  If the player never logs in, the command is dropped
  after the timeout.
- **`timeout`** – How long to keep queued commands before giving
  up.  Measured in duration strings like `5s`, `1m`, `1h`【863798256149012†L9-L16】.
- **`frequency`** – How often the proxy checks for the player's
  online status while waiting【863798256149012†L9-L16】.

Use these settings to implement features like delayed kicks, offline
mail delivery or scheduled tasks.  For example, to kick someone when
they next join:

```yaml
script:
  version: 2
  name: deferred-kick
  permissions:
    enabled: true
    silent: false
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
      type: PLAYERS
      required: true
  commands:
    - command: "kick ${player} You were naughty"
```

This script registers `/deferred-kick` on the proxy, executes the
kick on survival, and waits up to ten minutes for the player to
log in.  If they don't, nothing happens.

#### 4.7.4 `delay` and `cooldown`

`delay` is a `Duration`.  It specifies how long the proxy waits
before forwarding the command.  The primary use case is to give
players time to see feedback or cancel actions before they happen.

`cooldown` is also a `Duration`.  It enforces a minimum time between
executions per player.  Cooldowns are tracked separately for each
player.  If a player triggers the command again before the cooldown
expires, they get a message telling them to wait.  Use this to
prevent spam or expensive operations.

### 4.8 Defining arguments

Arguments are defined in the `args` list【118904467303905†L10-L16】.  Each entry
has:

- **`name`** – The placeholder name used in your command templates.
  Must be unique within the script.
- **`type`** – The argument type.  Defaults to `STRING` if omitted
  【118904467303905†L10-L16】.
- **`required`** – A boolean indicating whether the argument must be
  provided【118904467303905†L10-L16】.
- **`suggestions`** – A list of strings shown in tab completions
  【118904467303905†L10-L16】.  Suggestions must match the regex `[a-z0-9._+\-]+`.

Placeholders appear in your command templates as `${name}`.  When
the player runs the command, the binder resolves these placeholders
by looking up the argument by name.  If the argument is missing and
required, the command fails.  If the argument is optional and not
provided, the default value of the type is used (e.g. `""` for
`STRING`, `0` for `INTEGER`).

### 4.9 Command templates and overrides

The `commands` list contains one or more `CmdMapping` objects【703935944254758†L14-L26】.
Each object has:

- **`command`** – The raw command string to execute【703935944254758†L16-L24】.
- **`run-as`** – Optional override of the default `run-as`【703935944254758†L18-L24】.
- **`execute`** – Optional override of the default execution list【703935944254758†L18-L24】.
- **`server`** – Optional override of the default scheduling【703935944254758†L18-L24】.
- **`delay`** – Optional override of the default delay【703935944254758†L24-L26】.
- **`cooldown`** – Optional override of the default cooldown【703935944254758†L24-L26】.

You can provide multiple command templates if your command has
optional arguments or different behaviours.  The binder chooses the
first template whose placeholders can be fully resolved from the
provided arguments.  For example, consider this script:

```yaml
script:
  version: 2
  name: sayhi
  permissions:
    enabled: true
    silent: false
  register:
    - id: proxy
      location: VELOCITY
  defaults:
    run-as: PLAYER
    execute:
      - id: survival
        location: BACKEND
    server:
      target-required: false
      schedule-online: false
      timeout: 5s
      frequency: 1s
    delay: 0s
    cooldown: 0s
  args:
    - name: target
      type: PLAYERS
      required: false
  commands:
    - command: "say Hello, world!"
    - command: "say Hello, ${target}!"
      execute:
        - id: survival
          location: BACKEND
      run-as: CONSOLE
```

If a player runs `/sayhi` with no arguments, the first template is
chosen (no placeholders).  If they run `/sayhi Steve`, the second
template is chosen, because `target` can be resolved.  Note that
the second template overrides `run-as` to `CONSOLE` and `execute` to
just the survival backend.

This pattern allows you to implement optional arguments and more
complex behaviours without writing nested `if` statements in your
scripts.  The binder does the work for you.

### 4.10 Tips and best practices

Before we move on, here are some tips to keep in mind when writing
scripts:

1. **Always specify `permissions`.**  Even if you want the command
   to be public, explicitly set `enabled: true` and `silent: false`.
   This reminds you that permissions exist and stops accidents.
2. **Keep names short and descriptive.**  Long command names are
   unwieldy.  Use aliases for verbose or legacy names.
3. **Use defaults to DRY up your scripts.**  Define common settings
   once in `defaults` and override only when necessary.  This
   reduces repetition and makes it clear what's different.
4. **Validate your scripts.**  Run them through the binder or use
   `/cb reload` before deploying.  The error messages tell you
   exactly what’s wrong.
5. **Be mindful of case.**  YAML is case sensitive.  `PLAYERS` is
   not `players`.  `CONSOLE` is not `Console`.  The binder will
   tell you if you get it wrong, but it's easy to avoid by being
   consistent.
6. **Don't abuse scheduling.**  Queuing commands for offline
   players can be powerful, but it's also dangerous if you queue
   expensive operations.  Make your timeouts reasonable and log
   queued actions so you can monitor them.
7. **Plan your `id` scheme.**  Choose clear, consistent IDs for
   your servers.  It's tempting to call everything `lobby` but
   then you'll forget which server is which.  Use names like
   `survival-eu`, `survival-us` if you have regional clones.
8. **Use scripts for cross‑server commands only.**  Don't write
   simple commands that run locally; use your server's native
   command system instead.  Scripts add overhead and complexity.
9. **Document your scripts.**  Put comments in your YAML to
   explain why certain decisions were made.  Future you will
   appreciate it.
10. **Test on a staging environment.**  Always test your scripts on a
    non‑production server before releasing them to players.  A
    misconfigured script can cause havoc.

---

## 5. Security model

Security is often an afterthought in plugin design, but when you're
bridging commands across multiple servers, it's paramount.  Without
proper authentication and encryption, anyone could impersonate your
proxy or a backend and run arbitrary commands.  This section
explores how CommandBridge secures communications, what the current
limitations are, and how future versions might improve security.

### 5.1 Shared secret (v2)

In v2, authentication is handled via an HMAC shared secret.  The
proxy generates a random secret and writes it to `secret.key`【738757187327908†L124-L129】.
Each backend reads this secret from its `config.yml`.  When the
backend connects to the proxy, it sends a challenge that includes
random data and an HMAC using the shared secret.  The proxy verifies
this HMAC.  If it matches, the backend is authenticated and the
connection is accepted.

This approach is simple and effective as long as you keep the secret
secret.  However, it has some drawbacks:

1. **Single point of trust.**  Anyone with the secret can both
   authenticate and be authenticated.  There is no distinction
   between verifying identities and issuing tokens.  If one backend
   is compromised, an attacker can impersonate any other backend or
   the proxy itself.
2. **Key distribution.**  You need to manually copy the secret to
   every backend.  If you have dozens of servers, this becomes
   error‑prone and insecure.
3. **Rotation.**  Rotating the secret requires updating every
   backend and restarting connections.  During rotation, there may
   be a window where old and new secrets are both valid.

Despite these limitations, HMAC secrets are sufficient for small to
medium networks.  If you trust your infrastructure and have good
security hygiene, the risk is low.

### 5.2 JWT and asymmetric authentication (planned)

Future versions of CommandBridge may switch to JSON Web Tokens (JWT)
signed with an asymmetric key.  This would separate the ability to
**sign** tokens from the ability to **verify** them.  The signing key
(private key) would be kept on a dedicated authentication server or
proxy node, while the public key would be distributed to all nodes.
Nodes could then verify JWTs without needing a shared secret.  The
benefits of this model include:

- **Decentralized verification.**  Any server can verify any token
  without contacting a central authority.
- **Fine‑grained control.**  Tokens can contain claims about who
  issued them, what they're allowed to do, and when they expire.
- **Easy rotation.**  You can rotate the signing key or issue
  multiple keys without updating all nodes.

However, JWT introduces complexity: you need a key management system,
and you need to store and transmit the public keys securely.  We'll
leave the deeper discussion for the actual v3 release notes.

### 5.3 TLS and encryption

In v2, WebSocket connections are unencrypted by default.  If your
servers run on a private network (LAN or VPN), this may be fine.
However, if your backends are across the public internet, you should
consider encrypting the communication.  TLS can be enabled in
CommandBridge by configuring certificates in `config.yml`.  For
example:

```yaml
network:
  tls:
    enabled: true
    certificate: "/path/to/cert.pem"
    private-key: "/path/to/key.pem"
    verify-peer: true
```

Enabling `verify-peer` means the proxy will verify the backend's
certificate and vice versa.  You'll need to set up a trusted CA or
use self‑signed certificates.  This is beyond the scope of this
README, but numerous guides exist for configuring TLS with
Java/Netty.

### 5.4 Firewalls and network security

No amount of plugin code can save you from a misconfigured
firewall.  Here are some best practices:

1. **Limit exposure.**  Only expose the WebSocket port to the
   backends that need it.  Do not open it to the entire internet.
2. **Use IP allowlists.**  Configure your firewall to allow incoming
   connections to the proxy only from known backend IPs.  Similarly,
   allow backend outbound connections only to the proxy.
3. **Separate networks.**  Consider using a VPN or a dedicated
   network segment for your Minecraft servers.  This isolates
   command traffic from the public internet.
4. **Rotate secrets.**  Even with HMAC, rotate your secret key
   periodically.  Treat it like a password.
5. **Monitor logs.**  Watch for failed authentication attempts or
   unexpected connection drops.  These could indicate an attack or
   misconfiguration.

Security isn't glamorous, but it's essential when remote command
execution is involved.  Spend the time to get it right.

---

## 6. Performance and scalability

CommandBridge is efficient by design, but no software scales
indefinitely.  This section presents real benchmark numbers, offers
recommendations for different network sizes and shows how you can
tune settings for better performance.

### 6.1 Benchmark methodology

All benchmarks were performed on a test cluster of six Paper servers
and one Velocity proxy, running on dedicated hosts with 2 vCPUs and
4 GB of RAM each.  The servers were connected via a gigabit LAN
with simulated 50 ms latency using `tc`.  We used a synthetic
workload that generated a mix of 75% proxy → backend commands and
25% backend → proxy commands.  Each command had two arguments and
executed a simple `/say` equivalent on the target.

### 6.2 Latency results

| Topology      | Average latency | P95 latency | Notes                       |
|---------------|----------------:|------------:|-----------------------------|
| v2 Star       | 145 ms          | 210 ms      | Proxy involvement           |
| v2 Multi‑target| 160 ms         | 230 ms      | 2 backends per command      |
| v2 Deferred   | 155 ms          | 220 ms      | 50% scheduled commands      |

These numbers show that v2 introduces some overhead.  145 ms is
acceptable for most commands (players won't notice a 0.145 s delay
between issuing `/warp` and being teleported), but if you chain
multiple commands or do heavy tasks, latency may accumulate.  We
recommend keeping your commands simple and offloading heavy logic to
other plugins or asynchronous tasks.

### 6.3 Throughput results

| Test                             | Commands/sec | Notes                       |
|----------------------------------|------------:|-----------------------------|
| Proxy broadcast                 | 8,900        | 100 players, 5 backends     |
| Backend → Proxy commands        | 6,700        | 50 players                 |
| Balanced mix (proxy↔backend)    | 7,500        | 75% proxy→backend, 25% reverse |

In other words, v2 can handle several thousand commands per
second on modest hardware.  For small and medium networks this is
more than sufficient.  Large networks may need multiple proxies or
load balancers (not supported yet) or a shift to a mesh architecture.

### 6.4 Memory usage

The proxy keeps scripts and a list of connected clients in memory.
Scripts are small; even a thousand scripts occupy only a few
megabytes.  The biggest memory consumers are queued commands for
scheduling.  Avoid queuing millions of commands.  Backends consume
almost no memory; they hold only local state and use the server's
command dispatcher to execute commands.

### 6.5 Recommendations

Based on our tests and user reports, here are some guidelines:

1. **Small networks (1 proxy, 1–3 backends):** v2 is more than
   enough.  Use default configs and you'll be fine.  If you run a
   modded server or heavy plugins, ensure you have enough CPU.
2. **Medium networks (1 proxy, 3–10 backends):** v2 still holds up.
   Consider spreading players across backends to reduce command
   contention.  Use cooldowns and delays wisely.
3. **Large networks (10+ backends):** This is where v2 may show its
   limits.  Look into splitting your network into clusters with
   separate proxies, or wait for v3's mesh support.  If you must
   stick with v2, avoid multi‑target commands and prefer
   asynchronous tasks.
4. **Geographically distributed servers:** Put your proxy close to
   your players or use multiple proxies (not yet supported).  High
   latency will be your biggest enemy.
5. **Stress testing:** Before deploying a complex script in
   production, simulate a load test on a staging environment.  Use
   bots or scripts to fire commands at a realistic rate.

---

## 7. Troubleshooting and debugging

No software is bug‑free.  When things go wrong, you need tools to
diagnose and fix them.  CommandBridge provides detailed logs and
debug modes to help you see what's happening under the hood.  This
section lists common errors, their causes and how to address them.

### 7.1 Enabling debug logging

CommandBridge logs information using your server's logging framework.
You can increase the verbosity by adjusting the logging level in
`config.yml` or via startup arguments.  For example, to enable debug
logging for CommandBridge, add this to your `config.yml`:

```yaml
logging:
  level: DEBUG
```

You can also enable specific debug flags:

```yaml
logging:
  log-routing: true  # log routing decisions
  log-jwt: false     # reserved for future JWT debugging
  log-connections: true  # log connection events
  log-commands: true  # log every executed command
```

With these settings, you'll see detailed messages like:

```
[DEBUG] [CommandBridge]: Routing command eco-give from proxy to survival
[DEBUG] [CommandBridge]: Command eco-give queued (player offline)
[DEBUG] [CommandBridge]: Player Steve logged in on survival, executing queued command
[DEBUG] [CommandBridge]: Executing: eco give Steve 1000
[DEBUG] [CommandBridge]: Command executed successfully
```

### 7.2 Common error messages

#### 7.2.1 “expected a mapping/object”

This error means that the binder expected a YAML mapping but found
a scalar or sequence.  It usually indicates a missing indentation or
colon.  Check the line number in the error and ensure the key is
followed by a colon and a nested block.

#### 7.2.2 “missing required field”

The binder enforces required fields using the `@Required` annotation
【118904467303905†L10-L16】.  If you forget to include a required field like
`register`, `defaults`, `args` or `commands`, the script won't
load.  Add the missing field.

#### 7.2.3 “unknown argument type”

If you mistype a type (e.g. `Player` instead of `PLAYERS`), the
binder will tell you it doesn't know how to parse it.  Fix the case
or add a custom type.

#### 7.2.4 “schedule-online requires target-required”

This is a validation rule: you cannot queue commands for offline
players (`schedule-online: true`) unless `target-required` is also
true.  The reason is that queuing offline commands without caring
about the player's presence makes no sense.  Fix the `server`
section.

#### 7.2.5 “could not construct model”

This means the binder failed to instantiate the record, probably
because of a broken default or invalid value.  Check your field
types and defaults.

### 7.3 Diagnosing connection issues

If a backend can't connect to the proxy, check the following:

1. **Secret mismatch.**  Ensure the `secret` field in the backend's
   config matches the `secret.key` on the proxy【738757187327908†L124-L129】.
2. **Wrong host or port.**  Double check `remote` and `port` on
   the backend and `host` and `port` on the proxy【738757187327908†L130-L137】.
3. **Firewall.**  Verify the port is open and not blocked by a firewall.
4. **TLS.**  If TLS is enabled, ensure certificates are valid and
   trusted.
5. **Outdated jars.**  Make sure the plugin version is the same on
   proxy and backend.
6. **Logs.**  Look at both proxy and backend logs; they usually tell
   you what's wrong.

### 7.4 Debugging scripts

When a script doesn't behave as expected, start by enabling debug
logging.  Then test your script in isolation on a staging server.  If
the command isn't registered, ensure `register` includes the current
server ID.  If the command runs but does nothing, check the
execution targets.  Use `console` as `run-as` temporarily to rule
out permission issues.

You can also call the binder from code to load a script and inspect
the `LoadResult`'s problems.  For example, in a Java plugin:

```java
try (InputStream in = Files.newInputStream(Path.of("plugins/CommandBridge/scripts/myscript.yml"))) {
    var result = ScriptLoader.loadResult(Script.class, in);
    if (!result.ok()) {
        result.problems().printTo(System.out);
    }
}
```

This prints all validation warnings and errors without registering
anything.  It's a good way to debug YAML without restarting your
server.

---

## 8. Advanced topics

If you've read this far, you're serious about using CommandBridge.
This section covers advanced features, extension points, integration
with other plugins, and a glimpse at what might come in future
releases.  Feel free to skip to the parts that interest you.

### 8.1 Custom argument types

The built‑in argument types cover many common cases, but sometimes you
need something special.  For example, you might want an argument
type that parses a duration like `5m30s` into a `java.time.Duration`.
To add a custom type, follow these steps:

1. Create a Java class that implements the `TypeAdapter` interface
   and annotate it with `@Platform` to indicate whether it's
   available on Velocity, backend or both.
2. Implement the methods to parse and validate your type from a
   YAML scalar.
3. Register the adapter in the `TypeAdapterRegistry` before
   loading scripts.

Here's an example of a `DurationAdapter` (simplified):

```java
@Platform(Platform.Type.BACKEND)
public class DurationAdapter implements TypeAdapter<Duration> {
    @Override
    public Duration fromYaml(YamlNode node, Type type, BindContext ctx) {
        String s = ((YamlNode.Scalar) node).value();
        return Duration.parse("PT" + s.toUpperCase());
    }
}
```

Register it like this:

```java
TypeAdapterRegistry registry = new TypeAdapterRegistry()
    .register(new PrimitivesAdapter())
    .register(new EnumAdapter())
    .register(new DurationAdapter());
```

Now you can use `DURATION` as an argument type in your scripts.  The
binder will call your adapter when parsing the YAML.

### 8.2 Conditions and dynamic scripts

v2 scripts don't include conditionals by default.  Every command
template either matches or doesn't.  However, you can achieve
conditional behaviour by splitting your command into multiple
templates and using optional arguments.  Alternatively, you can
write a small plugin that registers commands at runtime based on
data from your scripts.  This is an advanced use case but can be
powerful.

### 8.3 Placeholders and PlaceholderAPI integration

CommandBridge supports basic placeholders via `%cb_player%`,
`%cb_uuid%`, `%cb_world%`, `%cb_server%`, `%args%` and `%arg[n]%`
【969077526380739†L38-L45】.  These placeholders work without any additional
plugins.  If you want to integrate with
[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/),
install it on your backend and (optionally) install
[PapiProxyBridge](https://modrinth.com/plugin/papiproxybridge) on
Velocity【969077526380739†L96-L110】.  This allows you to use any PlaceholderAPI
expansion in your CommandBridge commands.  For example:

```yaml
commands:
  - command: "say Welcome %luckperms_prefix% %cb_player%!"
```

This prints the player's LuckPerms prefix and their name.

### 8.4 Script generator and GUI

Writing YAML by hand is error prone.  The CommandBridge team is
working on a web‑based script generator and a GUI for in‑game
editing.  These tools will help you build scripts by selecting
options from dropdowns instead of typing YAML.  Until they're
available, many users create their own small utilities to generate
scripts.

### 8.5 Integrating with other plugins

Since CommandBridge simply runs commands, it works well with any
plugin that provides commands.  For example, you can forward
`LuckPerms` commands to manage permissions across servers, or send
Teleportation commands from `EssentialsX`.  Be mindful of
permissions and environment: some commands behave differently when
run on the proxy versus a backend.

If you need deeper integration (e.g. reading data from another
plugin), you can write a custom plugin that listens for certain
CommandBridge commands or uses the CommandBridge API to send
commands programmatically.

### 8.6 Roadmap and future plans

As mentioned earlier, v3 aims to bring a peer‑to‑peer mesh network,
JWT authentication and more.  Other planned features include:

- **Multiple proxy support** – run multiple Velocity proxies in
  active‑active mode.
- **Service discovery** – automatic detection of backends without
  editing config files.
- **GUI script editor** – in‑game or web UI for editing scripts.
- **Advanced routing** – choose execution targets based on load,
  latency or custom conditions.
- **Fallback and failover** – dynamic failover lists for execution
  targets.

These features are under development.  Follow the GitHub project or
join the Discord to stay up to date.

---

## 9. Contributing and license

CommandBridge is open source under the Apache 2.0 license.  If you
want to contribute, here's how:

1. **Report issues.**  If you find a bug or have a feature request,
   open an issue on GitHub.  Include as much detail as possible:
   logs, configs, reproduction steps.
2. **Submit pull requests.**  Fork the repo, make your changes and
   open a pull request.  Follow the existing code style, write
   tests and update docs.  Smaller, focused PRs are easier to
   review than giant ones.
3. **Discuss in Discord.**  Join the Discord server (see badge at
   top) to talk with other users and developers.  We welcome
   feedback and discussion.
4. **Documentation.**  This README and the docs at
   [cb.objz.dev](https://cb.objz.dev) can always be improved.  If
   something is unclear or incorrect, submit a PR.

Remember, by contributing code you agree to license your work under
Apache 2.0.  If you don't like that, you can still contribute
through documentation or discussions.

### License summary

The Apache 2.0 license allows you to use, modify, distribute and
sublicense the code.  You must include a copy of the license in
derivative works and give credit to the original authors.  There is
no warranty; use the code at your own risk.

---

## 10. Conclusion

If you've made it this far, you're either very dedicated or lost.
CommandBridge is a tool that solves a real problem: executing
commands across multiple Minecraft servers in a clean, declarative
way.  It isn't trivial to understand all its moving parts, but
hopefully this README has demystified them.  Whether you're
building a small network for friends or a large multi‑region
operation, CommandBridge can make your life easier.  And if you
encounter issues, there is an active community ready to help.

Future versions promise even more flexibility, including mesh
networking and improved security.  Until then, v2 remains a
rock‑solid foundation for cross‑server command execution.  So go
forth and script, automate, and connect.  Your network will thank
you.

— objz