# CommandBridge

![Version](https://img.shields.io/badge/version-2.2.9-blue.svg)
![Minecraft](https://img.shields.io/badge/minecraft-1.20.x--1.21.x-brightgreen.svg)
![Java](https://img.shields.io/badge/java-21-orange.svg)
![License](https://img.shields.io/badge/license-GPLv3%20%2F%20Apache--2.0-red.svg)
![Platform](https://img.shields.io/badge/platform-Velocity%20%7C%20Paper-blueviolet.svg)
![Build](https://img.shields.io/badge/build-gradle-02303a.svg)

-----

## What is this thing?

Alright, so you run a Minecraft network. Maybe you’ve got a Velocity proxy in front of a bunch of Paper servers - lobby, survival, creative, skyblock, whatever. And you want to run commands that work **everywhere at once**, right? Like giving a player money across all your economy-synced servers, or broadcasting a message, or kicking someone from the entire network.

Normally you’d have to write a bunch of Java code, compile plugins, deal with plugin messaging channels (which only work when players are online, by the way), and basically make your life more complicated than it needs to be.

**CommandBridge** fixes that. It’s a single JAR that you drop on both your Velocity proxy and your Paper servers, and it creates a bridge between them using WebSockets.   Then you define your commands in simple YAML files instead of writing Java, and boom - you’ve got network-wide commands that execute in real-time across your entire server infrastructure. 

No coding required. Well, unless you want to extend it, then go wild.

## Why does this exist?

Plugin messaging is kinda garbage because it only works when at least one player is online. With WebSockets, your servers can talk to each other 24/7 regardless of player count.  Just makes sense.

## Features that might interest you

- **YAML-based scripting** - Define commands without touching Java code
- **Real-time WebSocket communication** - Fast, reliable, works even with zero players online 
- **Cross-server command execution** - Run commands on multiple backend servers simultaneously 
- **20+ argument types** - Players, entities, ranges, worlds, locations, and more
- **Smart permissions** - Auto-generated permission nodes with optional silent mode
- **Deferred execution** - Schedule commands to run when a player logs in (even if they’re offline now)
- **Flexible execution contexts** - Run commands as CONSOLE, PLAYER, or OPERATOR
- **Built-in cooldowns and delays** - Rate limiting and timing without extra plugins
- **Tab completion** - Full argument suggestions support 
- **Multiple proxy support** - Connect multiple Velocity instances if your network is that big
- **Auto-detection** - Same JAR file works on both Velocity and Paper (figures it out automatically)
- **Secure authentication** - HMAC-based auth with a shared secret key 

Oh, and it’s got a `/cb dump` command that uploads your config to a website for troubleshooting.  Because sometimes you just need to show someone what’s going on without copy-pasting 500 lines into Discord.

## Not for 1.8. Not sorry.

This plugin supports **Minecraft 1.20.x through 1.21.x**.  If you’re still running 1.8 in 2025… I don’t know what to tell you. Move on. The ecosystem has moved on. Java 21 is required. 

Supported platforms:

- **Proxy**: Velocity, Waterfall
- **Backend**: Paper, Folia, Purpur, Bukkit, Spigot 

## Installation

### Step 1: Download the JAR

Grab the latest `CommandBridge-xxx-all.jar` from:

- [Modrinth](https://modrinth.com/plugin/commandbridge) (recommended)
- [GitHub Releases](https://github.com/objz/CommandBridge/releases)

### Step 2: Install everywhere

Take that **same JAR file** and put it in the `plugins` folder on:

1. Your Velocity proxy server
1. All your Paper/Spigot backend servers

The plugin automatically detects what type of server it’s running on and configures itself accordingly. No separate downloads, no confusion.

### Step 3: First startup

Restart all servers. CommandBridge will generate its configuration files: 

**On Velocity:**

```
plugins/CommandBridge/
├── config.yml
├── clients.yml
├── secret.key       ← Important!
└── scripts/         ← Put your YAML scripts here
```

**On Paper:**

```
plugins/CommandBridge/
├── config.yml
└── scripts/         ← Usually empty on backends
```

### Step 4: Configure authentication

This is the part where you actually have to read. I know, I’m asking a lot.

**On your Velocity server**, open `plugins/CommandBridge/config.yml`:

```yaml
host: 0.0.0.0              # Bind address (0.0.0.0 = all interfaces)
port: 8080                 # WebSocket port
server-id: "velocity-main" # Unique identifier for this proxy
```

Now open `plugins/CommandBridge/secret.key` and **copy the entire key**. 

**On each Paper server**, open `plugins/CommandBridge/config.yml`:

```yaml
remote: "127.0.0.1"        # Your Velocity server's IP (plain IP, no domain names)
port: 8080                 # Must match the Velocity port
secret: ""                 # ← PASTE THE KEY HERE
client-id: "lobby"         # Unique identifier for this server (e.g., "survival", "creative")
```

Replace `"lobby"` with something that identifies this specific server. Each backend needs a unique `client-id`.

**Important:** Use the actual IP address in the `remote` field, not a domain name.  Yes, I know domains are nicer. Do it anyway.

### Step 5: Restart in the right order

1. Restart your **Velocity** server first (it needs to start the WebSocket server)
1. Then restart your **Paper** servers (they’ll connect as clients) 

If everything worked, you should see logs like:

```
[INFO] [CommandBridge]: Client authenticated successfully: /127.0.0.1:42918
[INFO] [CommandBridge]: Added connected client: lobby
```

If you don’t see that, something’s wrong. Check that:

- The port matches between Velocity and Paper configs
- The secret key was copied correctly (no extra spaces or line breaks)
- Your firewall isn’t blocking the port
- You actually restarted Velocity before the Paper servers

## Commands

CommandBridge adds a couple admin commands for you:

### `/cb dump`

Dumps your current configuration and loaded scripts to a website so you can share it for troubleshooting.  Super useful when something isn’t working and you need help. Added in v2.2.9 because I got tired of people pasting 100 lines of YAML in Discord.

### `/cbc reconnect`

Allows Paper servers to reconnect to the Velocity server if the connection drops. Requires `commandbridge.admin` permission. 

### `/cbc help`

Shows help. You probably could’ve guessed that.

### Your custom commands

Every command you define in a script file gets automatically registered. The permission for each command is `commandbridge.command.<name>` where `<name>` is the script’s name field. Grant these permissions in your permission plugin (LuckPerms, etc.) to control who can use what.

-----

## The Scripting System (aka the entire point)

Alright, buckle up. This is the part where I explain how the scripting system actually works, because it’s honestly the coolest part of CommandBridge and why I built it in the first place.

### The problem I was trying to solve

Before this system existed, every time I wanted a proxy command that did something across servers, I had to:

1. Open neovim
1. Create a new Java class
1. Implement the command interface
1. Write argument parsing logic
1. Write permission checking logic
1. Write the forwarding logic to send commands to backend servers
1. Compile the entire plugin
1. Upload the JAR
1. Restart the server
1. Test it
1. Find a bug
1. Go back to step 1

This sucked. It really sucked. And it meant that non-developers couldn’t add commands to their networks without learning Java, setting up a development environment, understanding the codebase, etc.

So I thought: what if commands were just **data** instead of **code**? What if you could describe what a command does in a configuration file, and the plugin handles all the boring stuff automatically?

That’s what the YAML scripting system does. You define commands as structured data, and CommandBridge validates, registers, and executes them for you.

### How it works under the hood

When CommandBridge starts up, here’s what happens:

1. **ScriptLoader** scans the `scripts/` directory for `.yml` files
1. Each file is parsed as YAML and bound to Java record classes using reflection
1. A series of **validation processors** walk through the data structure:
- **DefaultProcessor** applies default values and checks for redundant overrides
- **PlatformProcessor** ensures argument types match platform capabilities (e.g., backend-only types aren’t used in proxy commands)
- **ResolvableProcessor** verifies that placeholder references in command templates actually exist
- Field validators check things like minimum values, required fields, and regex patterns
1. If validation passes, the commands are registered on the appropriate platforms (Velocity, Paper, or both)
1. If validation fails, errors are collected in a **ProblemSink** and logged so you can fix them

At runtime, when someone executes one of your scripted commands:

1. Arguments are parsed and validated based on the argument types you defined
1. Placeholders in the command template are replaced with actual values
1. The command is forwarded via WebSocket to the target server(s)
1. The target server(s) execute the command in the specified context (console, player, or operator)
1. If there’s a delay or cooldown, those are enforced
1. If the target player needs to be online and they’re not, the command can be queued until they log in

All of this happens automatically. You just write YAML.

### Script file structure

Scripts are YAML files placed in `plugins/CommandBridge/scripts/`. Name them whatever you want - `economy.yml`, `broadcast.yml`, `staff-tools.yml`, etc. The filename doesn’t matter; what’s inside does.

Every script **must** include these top-level keys:

#### 1. `version`

```yaml
version: 2
```

This is the schema version. Right now it’s `2`. If I ever make breaking changes to the format, I’ll bump this so old scripts don’t accidentally load with the wrong parser. Scripts without a version or with an unknown version are rejected.

#### 2. `name`

```yaml
name: economy
```

This is the canonical name of your script. It becomes:

- The primary command label on the proxy (so `/economy` in this example)
- Part of the permission node: `commandbridge.command.economy`
- The identifier used in logs and error messages

**Rules:**

- Must be 3-32 characters
- Lowercase letters, digits, and hyphens only
- No spaces, no underscores, no special characters

The validator enforces this with a regex pattern, so if you try something like `my_command!` it’ll yell at you.

#### 3. `description`

```yaml
description: "Economy management commands"
```

A short description of what this command does. Shows up in `/help` or plugin management GUIs. Keep it concise.

#### 4. `enabled`

```yaml
enabled: true
```

If you set this to `false`, the script is completely ignored. Nothing gets registered, nothing gets loaded. Useful for temporarily disabling commands without deleting the files.

#### 5. `aliases` (optional)

```yaml
aliases:
  - eco
  - money
```

Alternative names for the command. This must be a YAML list (the `- ` format) even if you only have one alias. These are registered on both Velocity and backend servers.

So with the above, players could type `/economy`, `/eco`, or `/money` and they’d all work.

#### 6. `permissions` (optional)

```yaml
permissions:
  enabled: true
  silent: false
```

Controls permission checking:

- **enabled**: If `true`, players need `commandbridge.command.<name>` to use the command
- **silent**: If `true` and the player lacks permission, nothing happens (no error message). Useful for hiding commands from players who can’t use them.

If you omit this section entirely, it defaults to `enabled: true` and `silent: false`.

#### 7. `register`

```yaml
register:
  - id: proxy-1
    location: VELOCITY
```

This is where you specify **where the command should be registered**. This is super important and people get confused about it, so pay attention.

`register` determines where the command appears - i.e., which server’s command list includes this command.

- **id**: The server identifier (either from your `clients.yml` or the `client-id`/`server-id` in configs)
- **location**: Either `VELOCITY` or `BACKEND`

**Most of the time**, you want `location: VELOCITY` because you want the command available on your proxy. But you can also register commands on specific backend servers if you want.

You can register the same command on multiple proxies:

```yaml
register:
  - id: proxy-1
    location: VELOCITY
  - id: proxy-2
    location: VELOCITY
```

#### 8. `defaults`

This is where things get interesting. The `defaults` section defines the default behavior for all commands in this script. Individual commands can override these settings if needed.

```yaml
defaults:
  run-as: CONSOLE
  execute:
    - id: lobby
      location: BACKEND
    - id: survival
      location: BACKEND
  server:
    target-required: true
    schedule-online: false
    timeout: 10s
    frequency: 2s
  delay: 0s
  cooldown: 0s
```

Let’s break down each part:

##### `run-as`

Determines **who executes the command** on the backend server:

- **CONSOLE** - Runs as the backend server’s console (full permissions, no restrictions)
- **PLAYER** - Runs as the player who triggered the command (uses their permissions)
- **OPERATOR** - Temporarily gives the player OP status just for this command, then removes it

Most of the time you want `CONSOLE` because your scripted commands are doing administrative things that players shouldn’t normally be able to do directly.

##### `execute`

Specifies **where to send the command for execution**:

```yaml
execute:
  - id: lobby
    location: BACKEND
  - id: survival
    location: BACKEND
  - id: creative
    location: BACKEND
```

This is a list of targets. When the command is triggered, it’s sent to **all** of these servers simultaneously. So if you have three servers listed, the command runs on all three.

- **id**: The client ID of the target server
- **location**: Usually `BACKEND`, but technically `VELOCITY` is possible if you want proxy-side execution

**Important distinction:**

- `register` = where the command appears (usually Velocity)
- `execute` = where the command actually runs (usually your Paper servers)

##### `server`

This section controls what happens when the target player is offline:

```yaml
server:
  target-required: true
  schedule-online: false
  timeout: 10s
  frequency: 2s
```

- **target-required** - If `true`, the backend server must see the player as online or the command fails
- **schedule-online** - If `true` and the player is offline, queue the command until they log in
- **timeout** - How long to wait for the player to log in before giving up (e.g., `10m`, `1h`, `30s`)
- **frequency** - How often to check if the player came online

Durations support `s` (seconds), `m` (minutes), `h` (hours), `d` (days), or just a number (interpreted as seconds).

**Example use case:** You want to kick a player from the network, but they’re currently offline. Set `target-required: true` and `schedule-online: true`, and the kick command will automatically execute as soon as they log in (within the timeout period).

##### `delay`

```yaml
delay: 2s
```

Wait this long before forwarding the command to the backend servers. Useful if you need to ensure something happens before the command runs (like waiting for a player to fully load into a server).

##### `cooldown`

```yaml
cooldown: 5s
```

Minimum time between repeated uses of this command by the same player. Set to `0s` to disable. Prevents spam and protects expensive commands.

#### 9. `args`

This is where you define the arguments your command accepts:

```yaml
args:
  - name: player
    type: PLAYERS
    required: true
    suggestions:
      - "@a"
      - "@p"
      - "Steve"
  - name: amount
    type: RANGE
    required: true
```

Each argument has:

- **name** - The identifier used in command templates (without the `${}`)
- **type** - The argument type (see the full list below)
- **required** - Whether the user must provide this argument
- **suggestions** - Optional list of strings for tab completion

**Argument names must be unique.** The validator will reject scripts with duplicate names.

#### 10. `commands`

Finally, the actual command definitions:

```yaml
commands:
  - command: "eco give ${player} ${amount}"
    run-as: CONSOLE
    execute:
      - id: survival
        location: BACKEND
    server:
      target-required: false
    delay: 0s
  - command: "say ${player} received ${amount} coins"
    execute:
      - id: lobby
        location: BACKEND
```

This is a list of command templates. Each one can override the defaults.

- **command** - The template string with `${placeholder}` syntax referencing your arguments
- Everything else - Optional overrides for `run-as`, `execute`, `server`, `delay`, `cooldown`

If you don’t specify a field, it inherits from `defaults`. If you do specify it, it overrides the default just for that command.

**Note:** The validator will warn you if you override a default with the exact same value (because that’s redundant and probably a mistake).

### Argument types

CommandBridge supports over 20 argument types thanks to CommandAPI integration. Each type is annotated with which platforms support it.

Here’s the full list:

|Type           |Description                              |Platforms        |Notes                                                         |
|---------------|-----------------------------------------|-----------------|--------------------------------------------------------------|
|`STRING`       |Single word/token                        |Velocity, Backend|                                                              |
|`TEXT`         |Remainder of input (can include spaces)  |Velocity, Backend|Captures everything after this argument                       |
|`INTEGER`      |Whole numbers                            |Velocity, Backend|                                                              |
|`DOUBLE`       |Floating-point numbers                   |Velocity, Backend|                                                              |
|`BOOLEAN`      |true or false                            |Velocity, Backend|                                                              |
|`RANGE`        |Numeric range like `1..10` or `5.5..20.3`|Velocity, Backend|No min/max validation - do that in your backend command       |
|`PLAYERS`      |Player selector or name                  |Velocity, Backend|Supports `@a`, `@p`, `@r`, or explicit names. Yes it’s plural.|
|`ENTITIES`     |Entity selector                          |Backend only     |                                                              |
|`ENTITY_TYPE`  |Entity type (zombie, creeper, etc.)      |Backend only     |                                                              |
|`WORLD`        |World name                               |Backend only     |                                                              |
|`SERVER`       |Velocity server name                     |Velocity only    |                                                              |
|`LOCATION`     |3D coordinates (x, y, z)                 |Backend only     |                                                              |
|`LOCATION_2D`  |2D coordinates (x, z)                    |Backend only     |                                                              |
|`ANGLE`        |Rotation angle                           |Backend only     |                                                              |
|`ROTATION`     |Full rotation (pitch and yaw)            |Backend only     |                                                              |
|`ITEM_STACK`   |Minecraft item                           |Backend only     |                                                              |
|`ENCHANTMENT`  |Enchantment type                         |Backend only     |                                                              |
|`POTION_EFFECT`|Potion effect type                       |Backend only     |                                                              |
|`SOUND`        |Sound effect                             |Backend only     |                                                              |
|`BIOME`        |Biome identifier                         |Backend only     |                                                              |
|`TIME`         |Time value (ticks or duration)           |Backend only     |                                                              |

**Platform validation:** The validator checks that you don’t use backend-only types in commands registered on Velocity. If you try to use `ENTITY_TYPE` in a proxy command, it’ll fail validation with a clear error message.

**Why is it `PLAYERS` and not `PLAYER`?** Because selectors like `@a` can match multiple players. The type returns a collection. If you want just one player, use `@p` or require a specific username.

**What happened to RANGE min/max?** They were removed in v2.x. If you need bounds validation, do it in your backend command logic. The `RANGE` type just parses the syntax and passes it through.

### Placeholders and resolution

Command templates use `${name}` syntax for placeholders:

```yaml
command: "eco give ${player} ${amount}"
```

When a player runs the command, placeholders are replaced with actual values:

1. `/economy Steve 100`
1. `${player}` → `"Steve"`
1. `${amount}` → `"100"`
1. Final command sent to backend: `eco give Steve 100`

Placeholder names must match your argument names. If you reference `${player}` but don’t have a `player` argument, validation fails.

You can also use special syntax for type defaults:

```yaml
command: "tp ${player} ${args.LOCATION}"
```

This references the default value handling for the `LOCATION` type. Mostly useful for complex types, but honestly just define your arguments explicitly instead.

### Inheritance and the @Merge annotation

This is a technical detail but kinda cool. The `@Merge` annotation in the code tells the system which fields can be overridden from defaults.

When you define a command without specifying `run-as`, it **inherits** from `defaults.run-as`. When you do specify it, it **overrides** for that command only:

```yaml
defaults:
  run-as: CONSOLE
  delay: 0s

commands:
  - command: "say hello"
    delay: 2s              # Overrides default
    # run-as inherited as CONSOLE
  
  - command: "say goodbye"
    # Both inherited
```

The DefaultProcessor (one of the validation processors) handles this inheritance and warns you if you override with the same value:

```yaml
defaults:
  run-as: CONSOLE

commands:
  - command: "example"
    run-as: CONSOLE       # ← Redundant! Validator warns about this
```

### Deferred execution (the cool part)

Let’s say you want to run a command on a player who’s currently offline. Normally, that doesn’t work - they’re not online, command fails, done.

But with CommandBridge’s deferred execution, you can **queue** the command until they log in:

```yaml
defaults:
  server:
    target-required: true      # Command needs player online
    schedule-online: true      # If offline, queue it
    timeout: 1h                # Wait up to 1 hour
    frequency: 30s             # Check every 30 seconds

commands:
  - command: "kick ${player} You have been banned"
```

**What happens:**

1. Admin runs `/ban OfflinePlayer`
1. Player isn’t online, so command can’t run immediately
1. CommandBridge queues the command
1. Every 30 seconds, it checks if the player came online
1. Player logs in 20 minutes later
1. Command executes immediately
1. Player gets kicked with the message

If the player doesn’t log in within 1 hour (`timeout`), the command is discarded.

This is super powerful for moderation commands, scheduled actions, or any situation where you need eventual execution.

### Complete example script

Here’s a full, realistic script that demonstrates most features:

```yaml
version: 2
name: economy
description: "Network-wide economy commands"
enabled: true

aliases:
  - eco
  - money

permissions:
  enabled: true
  silent: false

register:
  - id: proxy-1
    location: VELOCITY

defaults:
  run-as: CONSOLE
  execute:
    - id: lobby
      location: BACKEND
    - id: survival
      location: BACKEND
    - id: creative
      location: BACKEND
  server:
    target-required: true
    schedule-online: false
    timeout: 5m
    frequency: 10s
  delay: 0s
  cooldown: 3s

args:
  - name: player
    type: PLAYERS
    required: true
    suggestions:
      - "@a"
      - "@p"
      - "@r"
  
  - name: amount
    type: RANGE
    required: true

commands:
  # Give money to a player on all economy servers
  - command: "eco give ${player} ${amount}"
    run-as: CONSOLE
    # Uses default execute (all three servers)
    # Uses default server settings
  
  # Broadcast who received money (only on lobby)
  - command: "say ${player} received ${amount} coins"
    execute:
      - id: lobby
        location: BACKEND
    delay: 1s              # Wait 1 second so it shows after the first command
    cooldown: 0s           # No cooldown on broadcasts
```

**What this does:**

1. Registers `/economy`, `/eco`, and `/money` commands on the proxy
1. Requires `commandbridge.command.economy` permission
1. Takes two arguments: a player selector and a numeric range
1. When executed, sends two commands:
- First: `eco give <player> <amount>` to lobby, survival, and creative servers as console
- Second: `say <player> received <amount> coins` only to lobby server, after a 1-second delay
1. Enforces a 3-second cooldown on the main command (but not the broadcast)

### Multiple Velocity support

If you’re running a large network with multiple Velocity proxies, you can connect them all:

```yaml
register:
  - id: proxy-na
    location: VELOCITY
  - id: proxy-eu
    location: VELOCITY

defaults:
  execute:
    - id: survival-na-1
      location: BACKEND
    - id: survival-eu-1
      location: BACKEND
```

The command gets registered on both proxies, and when executed, forwards to both backend servers regardless of which proxy it came from.

### Extending the system

Want to add your own custom argument types? It’s actually pretty straightforward:

1. Implement your argument class using CommandAPI (e.g., `DurationArgument`)
1. Add a constant to the `ArgType` enum with the `@Platform` annotation
1. Add the mapping in `ArgumentMapper`
1. Done - ScriptLoader picks it up automatically

Example:

```java
// In ArgType.java
@Platform({ BACKEND })
DURATION,

// In ArgumentMapper.java
case DURATION -> new DurationArgument(...)
```

Now you can use `type: DURATION` in your scripts.

### Common mistakes and gotchas

I’ve seen people mess these things up, so here’s a list:

❌ **DON’T** use duplicate argument names:

```yaml
args:
  - name: player
    type: PLAYERS
  - name: player        # ← ERROR: duplicate name
    type: TEXT
```

❌ **DON’T** try to use `PLAYER` (singular):

```yaml
args:
  - name: target
    type: PLAYER        # ← ERROR: doesn't exist, use PLAYERS
```

❌ **DON’T** use backend-only types in Velocity commands:

```yaml
register:
  - id: proxy-1
    location: VELOCITY

args:
  - name: target
    type: ENTITY_TYPE   # ← ERROR: ENTITY_TYPE only works on backends
```

❌ **DON’T** override defaults with the same value:

```yaml
defaults:
  run-as: CONSOLE

commands:
  - command: "example"
    run-as: CONSOLE     # ← WARNING: redundant override
```

❌ **DON’T** confuse `register` and `execute`:

```yaml
register:             # Where command appears (usually proxy)
  - id: proxy-1
    location: VELOCITY

defaults:
  execute:            # Where command runs (usually backends)
    - id: survival
      location: BACKEND
```

✅ **DO** use unique argument names  
✅ **DO** use `PLAYERS` (plural) for player selectors  
✅ **DO** match platforms between registration and argument types  
✅ **DO** use cooldowns to protect expensive commands from spam  
✅ **DO** test deferred execution thoroughly  
✅ **DO** check logs for validation errors

### Validation and error handling

When ScriptLoader processes your YAML files, it runs multiple validation passes. If anything’s wrong, it collects all the problems and logs them.

You’ll see messages like:

```
[ERROR] Script 'economy' failed validation:
  - Argument 'player' is referenced but not defined
  - Field 'run-as' has invalid value: 'ADMIN' (must be CONSOLE, PLAYER, or OPERATOR)
  - Type 'ENTITY_TYPE' cannot be used in VELOCITY location
```

Fix the errors, reload the script (or restart), and try again. The validator is pretty good about telling you exactly what’s wrong and where.

**Pro tip:** Use the `/cb dump` command to upload your config and scripts to a website. Makes debugging way easier when you can share a link instead of pasting 200 lines of YAML.

-----

## Complete usage example

Let’s walk through creating a real command from scratch.

**Goal:** Create a `/broadcast` command that sends a message to all players on all servers.

**Step 1:** Create `scripts/broadcast.yml`

```yaml
version: 2
name: broadcast
description: "Send a message to all players network-wide"
enabled: true

aliases:
  - bc
  - announce

permissions:
  enabled: true
  silent: false

register:
  - id: proxy-1
    location: VELOCITY

defaults:
  run-as: CONSOLE
  execute:
    - id: lobby
      location: BACKEND
    - id: survival
      location: BACKEND
    - id: creative
      location: BACKEND
    - id: skyblock
      location: BACKEND
  server:
    target-required: false
    schedule-online: false
  delay: 0s
  cooldown: 10s

args:
  - name: message
    type: TEXT
    required: true

commands:
  - command: "say [Network] ${message}"
```

**Step 2:** Grant permission

In LuckPerms (or whatever you use):

```
/lp group admin permission set commandbridge.command.broadcast true
```

**Step 3:** Use it

```
/broadcast The server will restart in 5 minutes!
```

**What happens:**

1. You run the command on the Velocity proxy
1. Permission check: Do you have `commandbridge.command.broadcast`? Yes, continue
1. Argument parsing: `${message}` = “The server will restart in 5 minutes!”
1. Cooldown check: Have you used this in the last 10 seconds? No, continue
1. Command forwarding: Send `say [Network] The server will restart in 5 minutes!` to:
- lobby server (as console)
- survival server (as console)
- creative server (as console)
- skyblock server (as console)
1. All servers execute the command simultaneously
1. All players see: `[Network] The server will restart in 5 minutes!`
1. Cooldown activated for you for 10 seconds

Done. Network-wide broadcast with a single command, zero Java code written.

-----

## Architecture

CommandBridge is split into three Gradle modules:

### core

Contains all the shared code:

- WebSocket implementations (client and server)
- Scripting engine (ScriptLoader, validators, argument mapper)
- Data models (Script, Defaults, ArgMapping, etc.)
- Utility functions

### velocity

The Velocity plugin:

- WebSocket **server** (listens for connections)
- Command registration for proxy-side commands
- Authentication handling (HMAC with shared secret)
- Generates `secret.key` file

### paper

The Paper plugin:

- WebSocket **client** (connects to Velocity)
- Command registration for backend-side commands
- Command execution (receives forwarded commands from proxy)
- Player online checking for deferred execution

**Communication flow:**

```
Player on Velocity
    ↓
/economy Steve 100
    ↓
[Velocity] Parse command, validate args, resolve placeholders
    ↓
[WebSocket] Send: { command: "eco give Steve 100", server: "survival", runAs: "CONSOLE" }
    ↓
[Paper Backend] Receive message, authenticate, execute command as console
    ↓
Backend plugin runs: eco give Steve 100
```

All communication is authenticated using HMAC. When a Paper server connects, it proves it knows the secret key without sending the key itself. Prevents unauthorized servers from joining your network.

### Why WebSockets instead of plugin messaging?

Plugin messaging (BungeeCord/Velocity’s built-in system) only works when **at least one player is online**. If your network is empty, messages don’t get delivered.

WebSockets keep a persistent connection between servers regardless of player count. This means:

- Commands can run 24/7
- Automated systems can communicate even at 3 AM when nobody’s online
- More reliable, lower latency
- Better error handling

The tradeoff is that you have to configure port and authentication, but honestly that’s not a big deal and you only do it once.

-----

## Building from source

If you want to modify the plugin or build it yourself:

### Requirements

- **Java 21** JDK
- **Git**

### Clone and build

```bash
git clone https://github.com/objz/CommandBridge.git
cd CommandBridge
./gradlew shadowJar
```

On Windows use `gradlew.bat` instead.

The compiled JAR will be in:

```
<module>/build/libs/CommandBridge-<version>-all.jar
```

You want the `-all.jar` variant (it includes dependencies).

### Development setup

I recommend IntelliJ IDEA. Just open the project (it’ll detect Gradle automatically) and you’re good to go.

The codebase is pretty well organized:

- `/core/src/main/java/` - Shared code
- `/velocity/src/main/java/` - Velocity-specific
- `/paper/src/main/java/` - Paper-specific

Most of the interesting stuff is in the `scripting` package under `core`.

-----

## Troubleshooting

### “Client failed to authenticate”

Your secret key doesn’t match. Double-check:

- You copied the entire key from Velocity’s `secret.key` file
- No extra spaces or line breaks in Paper’s config
- You restarted both servers after changing the config

### “Connection refused”

Velocity’s WebSocket server isn’t running or the port is blocked:

- Make sure Velocity started successfully
- Check that the port is the same in both configs
- Verify your firewall isn’t blocking the port
- Try `127.0.0.1` instead of `localhost` (or vice versa)

### “Command not registered”

Your script has validation errors:

- Check the server logs for error messages
- Use `/cb dump` to see exactly what the loader sees
- Common issues: duplicate argument names, wrong enum values, invalid placeholders

### “Player is not online”

The backend server doesn’t see the player as online:

- They might be on a different server than you think
- The server ID in your `execute` list might be wrong
- Try setting `target-required: false` if the command doesn’t actually need the player online

### “Permission denied”

Grant the permission:

```
commandbridge.command.<scriptname>
```

For admin commands use:

```
commandbridge.admin
```

### Something else is broken

Use `/cb dump` and share the link. Makes debugging way easier.

-----

## Migration from v1.x

If you’re upgrading from CommandBridge 1.x, **this is a breaking change**. The entire plugin was rewritten for v2.

### What changed

- Communication switched from plugin messaging to WebSockets (you’ll need to configure ports and authentication)
- Script format completely changed (you’ll need to manually convert all scripts to version 2)
- Java 21 is now required
- Only supports 1.20.x-1.21.x (dropped legacy version support)
- Single JAR for both platforms instead of separate downloads
- Better validation and error reporting
- More argument types supported
- New permission system

### Migration steps

1. **Backup everything** - Your old configs and scripts
1. **Delete your old CommandBridge JAR** - Don’t try to run both versions
1. **Install the new JAR** on both Velocity and Paper
1. **Delete your old `config.yml`** - Let it generate fresh
1. **Configure authentication** following the installation guide above
1. **Manually convert your scripts** to the new format (reference the scripting section)
1. **Test thoroughly** before going live

There’s no automatic migration. Sorry. The changes were too fundamental. But the new system is much better, I promise.

Legacy documentation for v1.x is still available at [objz.github.io/CommandBridge](https://objz.github.io/CommandBridge) if you need it.

-----

## Performance and scaling

CommandBridge is pretty lightweight. The WebSocket connections are persistent but idle most of the time. Commands are forwarded instantly with minimal overhead.

**Tested with:**

- 5 backend servers
- 100+ players online
- Dozens of scripted commands
- No noticeable performance impact

If you’re running a massive network with hundreds of servers… honestly I haven’t tested that. It’ll probably work fine, but let me know how it goes.

The scripting system uses Java records and immutable data structures, so there’s no weird state bugs or memory leaks. Validation happens once at load time, not every time a command runs.

-----

## Contributing

This is open source (GPLv3 / Apache 2.0, depending on where you look - I should probably sort that out).

If you want to contribute:

1. Fork the repo
1. Create a branch for your changes
1. Make your changes (please follow the existing code style)
1. Test thoroughly
1. Submit a pull request

**Areas where contributions are welcome:**

- More argument types
- Better validation error messages
- Performance improvements
- Documentation improvements (especially examples)
- Bug fixes

**Please don’t submit PRs for:**

- Adding 1.8 support (not happening)
- Changing the entire architecture (open an issue first to discuss)
- Reformatting the entire codebase (I will close it)

-----

## Future plans

Some things I’m considering for future versions:

- **GUI for script editing** - YAML is great, but a web UI would be nice for non-technical server owners
- **More validation tools** - Better script testing and debugging
- **Conditional execution** - Run commands only if certain conditions are met
- **Response handling** - Get feedback from backend commands back to the proxy
- **Public API** - Let other plugins integrate with CommandBridge
- **Discord bot integration** - Manage scripts and see logs from Discord
- **More platforms** - BungeeCord support (maybe, if people actually want it)

No promises on timeline. This is a side project and I work on it when I have time.

If you have feature requests, open an issue on GitHub. No guarantees I’ll implement it, but I do read them.

-----

## Support

**Documentation:** [cb.objz.dev/docs](https://cb.objz.dev/docs/)  
**Issues:** [github.com/objz/CommandBridge/issues](https://github.com/objz/CommandBridge/issues)  
**Downloads:** [modrinth.com/plugin/commandbridge](https://modrinth.com/plugin/commandbridge)

If something’s not working, please:

1. Check this README first
1. Check the documentation
1. Use `/cb dump` and look at the output
1. Search existing GitHub issues
1. If still stuck, open a new issue with all the details (versions, configs, logs, etc.)

I’m usually pretty responsive, but I have a job and a life, so please be patient.

-----

## Statistics

CommandBridge uses **bStats** to collect anonymous usage statistics. This helps me understand how the plugin is used and prioritize development.

Data collected:

- Server software and version
- Player count
- Number of scripted commands
- Plugin version

No personal information, server IPs, or command content is collected.

To disable: Edit `plugins/bStats/config.yml` and set `enabled: false`

-----

## License

This project is licensed under **GPLv3** (according to the GitHub repo) or **Apache 2.0** (according to Modrinth).

Honestly I’m not sure which one it actually is at this point. Both are open source. Just don’t sell it or claim you wrote it.

-----

## Final thoughts

I built CommandBridge because I was tired of the old way of doing things. Writing Java code for every single proxy command was tedious and error-prone, and it meant non-developers couldn’t extend their servers without hiring someone.

The YAML scripting system isn’t perfect - YAML has its quirks and limitations - but it’s way better than the alternative. You can add new network commands in minutes instead of hours, and if something breaks, you just edit the file and reload.

The v2 rewrite was a massive undertaking. I basically threw away the entire codebase and started over with a better architecture, better validation, better everything. It took months. Was it worth it? Yeah, I think so.

If you find bugs, report them. If you have feature ideas, suggest them. If you just want to say thanks, that’s cool too.

Happy scripting.

— objz