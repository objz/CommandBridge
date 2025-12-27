# 🌉 CommandBridge v3

<div align="center">

![License](https://img.shields.io/badge/license-GPLv3-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.x--1.21.x-green.svg)
![Version](https://img.shields.io/badge/version-3.0.0-brightgreen.svg)
![Status](https://img.shields.io/badge/status-active%20development-yellow.svg)

**The smart way to bridge commands across your Minecraft network**

[Features](#-features) • [Installation](#-installation) • [Scripting](#-the-scripting-system) • [Architecture](#-architecture) • [Examples](#-examples)

</div>

---

## 🤔 what even is this?

alright, let's cut to the chase. you run a Minecraft network with Velocity proxies and Paper servers. normally, making them talk to each other is a nightmare. plugin messaging is unreliable, especially when nobody's online. writing Java command classes for every little proxy command gets old fast. 

**CommandBridge solves this.**

it's a WebSocket-based communication system that lets your proxies and servers actually talk to each other reliably. no players required online. no sketchy plugin channels. just solid TCP connections that work.

but here's where it gets good: v3 introduces a **declarative scripting system** where you define commands in YAML files. want to add `/eco give` to your proxy? drop a 20-line YAML file in a folder. done. no Java compilation, no plugin reloads, no sanity loss.

## 🎯 what problem does this solve?

**the problem**: you want to run commands on your backend servers from your proxy, but:
- plugin messaging doesn't work without players online
- writing Java command classes is tedious and requires compilation
- coordinating between multiple proxies is basically impossible
- syncing configs across a distributed network is manual hell

**the solution**: CommandBridge gives you:
- ✅ **persistent WebSocket connections** between proxies and servers (works 24/7, no players needed)
- ✅ **YAML-based command scripting** (define commands in config files, not code)
- ✅ **multi-proxy mesh networking** (proxies can talk to each other and share state)
- ✅ **powerful placeholder system** with 25+ argument types
- ✅ **cross-server command execution** (run commands anywhere in your network from anywhere)
- ✅ **deferred execution** (queue commands for offline players, execute when they log in)

## 🆕 why v3? (and why it's a complete rewrite)

v2 was already a complete rewrite from v1. it switched from plugin messaging to WebSockets, added the scripting system, and actually worked. but v2 has one big limitation:

**v2 only supports ONE Velocity proxy with multiple backends.**

that's fine for small networks. but if you're running:
- multiple geographically distributed proxies (US, EU, Asia)
- redundant proxies for failover
- enterprise-scale networks

...then v2 doesn't cut it. you can't have proxies coordinate commands or share state.

### what's new in v3?

| Feature | v2 | v3 |
|---------|----|----|
| **Architecture** | Single proxy → multiple backends | Multi-proxy mesh topology |
| **Authentication** | HMAC shared secrets | Modern auth with mutual TLS |
| **Topology** | Star (hub-and-spoke) | Distributed mesh |
| **Cross-proxy commands** | ❌ No | ✅ Yes |
| **Scalability** | Limited to one proxy | Horizontal scaling |
| **State sync** | Single proxy only | Client registry synced across mesh |
| **Security** | Static secrets | TLS encryption with certificate validation |

**the big changes:**
- 🔄 **Multiple Velocity servers can act as both servers AND clients** in the network
- 🔐 **Mutual TLS authentication** replaces shared secret system (way more secure)
- 🌐 **Distributed topology** where proxies discover and talk to each other
- ⚡ **Cross-proxy command execution** – run a command on proxy-us, have it execute on proxy-eu's backend
- 📈 **Better scalability** for enterprise networks
- 🏗️ **Currently implementing advanced command parsing on Velocity** with full argument validation
- 🎛️ **Planned online configuration panel** to manage everything through a web UI
- 🔌 **Developer API in development** for third-party plugins to hook into the system

v3 also uses modern dependencies and is built for Java 21. no legacy baggage.

**script compatibility**: v2 scripts work in v3 without modification (still schema version 2).

## ✨ features

### networking & communication

- 🔌 **WebSocket-based** – persistent TCP connections using Undertow, not that plugin messaging garbage
- 🌐 **Multi-proxy mesh** – connect multiple Velocity servers to each other
- 🔒 **Mutual TLS authentication** – industry-standard encryption with certificate validation
- 🔄 **Service discovery** – proxies find and connect to each other automatically
- 💾 **Distributed state** – all proxies know about all connected clients
- 📡 **Works without players online** – biggest win over v1, still crucial

### scripting system (the magic) ✨

- 📝 **YAML-based command definitions** – no Java code required
- 🎯 **25+ argument types** – STRING, INTEGER, PLAYERS, ENTITIES, WORLD, SERVER, LOCATION, ITEM_STACK, ENCHANTMENT, POTION_EFFECT, SOUND, BIOME, TIME, and more
- 🔧 **Placeholder system** – `${player}`, `${amount}`, dynamic argument substitution
- 👤 **Run-as modes** – execute as CONSOLE, PLAYER, or OPERATOR (temp op)
- ⏰ **Deferred execution** – queue commands for offline players, run when they log in
- ⏱️ **Cooldowns and delays** – per-player rate limiting and scheduled execution
- 🔐 **Permission integration** – automatic `commandbridge.command.<name>` permissions
- 🎯 **Multi-target execution** – send one command to multiple backends simultaneously
- 🖥️ **Platform-specific arguments** – validation ensures backend-only types (WORLD) don't get used on proxy commands
- ✅ **Annotation-driven validation** – scripts validated at load time, not runtime
- 🔄 **Hot reload** – change scripts without restarting servers

### platform support

- ☕ **Java 21 only** – we're not supporting Java 8, get with the times
- 🎮 **Minecraft 1.20.x to 1.21.x**
- 🚀 **Velocity** (primary), Waterfall (supported)
- 📄 **Paper** (primary), Folia, Purpur, Spigot, Bukkit (probably works)
- 📦 **Single JAR deployment** – one file, auto-detects platform

### architecture

- 🏗️ **Modular structure** – core (shared), velocity (proxy), backends (Paper/Bukkit)
- 📦 **Fat JAR with shadowJar** – all dependencies bundled, no conflicts
- 🔧 **Gradle build system** – modern toolchain with Java 21 support
- 🏷️ **Record-based models** – immutable data structures for scripts
- 🎨 **Annotation processors** – `@Default`, `@Merge`, `@Platform`, `@Required`, `@Min`, `@Max`

## 🚀 installation

### requirements

| Component | Version | Notes |
|-----------|---------|-------|
| **Java** | 21+ | Not 17, not 11, definitely not 8 |
| **Proxy** | Velocity or Waterfall | Velocity preferred |
| **Backend** | Paper, Folia, Purpur, Spigot, Bukkit | Paper recommended |
| **Minecraft** | 1.20.x - 1.21.x | No 1.8 support, not sorry |

### basic setup

1. **Download the JAR**
   ```bash
   # Get CommandBridge-3.0.0-all.jar from releases
   wget https://github.com/objz/CommandBridge/releases/download/v3.0.0/CommandBridge-3.0.0-all.jar
   ```

2. **Install on ALL servers**
   ```bash
   # Same JAR goes in both Velocity AND Paper plugins folders
   # Yeah, one JAR for everything. It auto-detects the platform.
   cp CommandBridge-3.0.0-all.jar velocity/plugins/
   cp CommandBridge-3.0.0-all.jar paper/plugins/
   ```

3. **Start Velocity first**
   ```bash
   # Start Velocity to generate configs
   # It will create:
   # - plugins/CommandBridge/config.yml
   # - plugins/CommandBridge/tls/server.p12 (TLS certificate)
   # - plugins/CommandBridge/scripts/ (script directory)
   ```

4. **Configure TLS authentication** (new in v3)
   
   v3 uses mutual TLS instead of shared secrets. here's how it works:
   
   **Option A: Auto-enrollment** (easiest for testing)
   ```yaml
   # velocity/plugins/CommandBridge/config.yml
   authentication:
     auto-enroll: true  # Accepts any client on first connect
   ```
   
   **Option B: Manual certificates** (recommended for production)
   ```yaml
   # velocity/plugins/CommandBridge/config.yml
   authentication:
     auto-enroll: false  # Require pre-configured certificates
   ```
   
   Then copy the CA certificate to your Paper servers:
   ```bash
   # Copy Velocity's CA cert to Paper server
   cp velocity/plugins/CommandBridge/tls/ca.crt paper/plugins/CommandBridge/tls/
   ```

5. **Configure connection settings**
   
   **Velocity config** (`velocity/plugins/CommandBridge/config.yml`):
   ```yaml
   config-version: 3
   server-id: "proxy-main"  # Unique ID for this proxy
   
   network:
     host: "0.0.0.0"  # Listen on all interfaces
     port: 8080       # WebSocket port
     tls:
       enabled: true  # Enable TLS (required for v3)
       mode: MUTUAL   # Require client certificates
   ```
   
   **Paper config** (`paper/plugins/CommandBridge/config.yml`):
   ```yaml
   config-version: 3
   client-id: "survival"  # Unique ID for this backend
   
   network:
     remote: "your-velocity-ip"  # Velocity server IP
     port: 8080                   # WebSocket port
     tls:
       enabled: true  # Enable TLS
       verify: true   # Verify server certificate
   ```

6. **Start everything in order**
   ```bash
   # 1. Start Velocity servers (they're the WebSocket servers)
   # 2. Wait for them to be ready
   # 3. Start Paper servers (they connect as clients)
   ```

7. **Verify connections**
   ```
   # Velocity logs should show:
   [INFO] [CommandBridge]: Client authenticated successfully: survival (TLS)
   [INFO] [CommandBridge]: Added connected client: survival
   
   # Paper logs should show:
   [INFO] [CommandBridge]: Connected to proxy at your-velocity-ip:8080
   [INFO] [CommandBridge]: Authentication successful
   ```

### multi-proxy setup (v3 exclusive) 🌐

want multiple Velocity proxies coordinating? here's how:

1. **Configure proxy mesh in primary Velocity**:
   ```yaml
   # velocity-1/plugins/CommandBridge/config.yml
   server-id: "proxy-us"
   
   proxy-mesh:
     enabled: true
     discovery-interval: 30s
     peers:
       - id: proxy-eu
         host: "proxy-eu.example.com"
         port: 8080
       - id: proxy-asia
         host: "proxy-asia.example.com"
         port: 8080
   ```

2. **Configure other proxies similarly**:
   ```yaml
   # velocity-2/plugins/CommandBridge/config.yml
   server-id: "proxy-eu"
   
   proxy-mesh:
     enabled: true
     discovery-interval: 30s
     peers:
       - id: proxy-us
         host: "proxy-us.example.com"
         port: 8080
       - id: proxy-asia
         host: "proxy-asia.example.com"
         port: 8080
   ```

3. **Sync TLS certificates**:
   ```bash
   # All proxies need the same CA certificate
   # Copy the CA cert from your primary proxy to all others
   scp proxy-us:/velocity/plugins/CommandBridge/tls/ca.crt \
       proxy-eu:/velocity/plugins/CommandBridge/tls/
   ```

4. **Scripts can now target any client**:
   ```yaml
   # This script registered on proxy-us can execute on
   # backends connected to proxy-eu or proxy-asia!
   execute:
     - id: survival-eu
       location: BACKEND
     - id: creative-asia
       location: BACKEND
   ```

## 🎮 commands

### `/cb` (or `/commandbridge`)

Main admin command. Requires `commandbridge.admin` permission.

| Command | Description | Platform |
|---------|-------------|----------|
| `/cb reload` | Reload configs and scripts | Both |
| `/cb list` | List all connected clients (v3 shows which proxy) | Velocity |
| `/cb dump` | Generate debug dump URL for troubleshooting | Both |
| `/cb mesh status` | Show proxy mesh topology and peer status | Velocity |
| `/cb tls info` | Show TLS certificate information | Both |
| `/cb tls regenerate` | Regenerate TLS certificates | Velocity |
| `/cb help` | Show command help | Both |

### `/cbc reconnect` (Paper only)

Reconnect Paper server to Velocity. Useful if connection drops.

Requires `commandbridge.admin` permission.

### script-defined commands

Any command you define in scripts gets registered automatically with permission `commandbridge.command.<script-name>` (unless you override it).

## 📜 the scripting system

okay, this is the real magic of CommandBridge. forget writing Java command classes. forget recompiling every time you want to change a command. v3's scripting system is **declarative, validated, and actually works**.

### how it works

Scripts live in `plugins/CommandBridge/scripts/` (configurable). Drop a `.yml` file in there, and CommandBridge:

1. ✅ Parses the YAML into Java records
2. ✅ Validates everything (required fields, platform compatibility, placeholder resolution)
3. ✅ Registers commands on the right platforms
4. ✅ Sets up argument types and suggestions
5. ✅ Handles execution, placeholder substitution, and cooldowns

if your script is broken, you get detailed errors at load time. no runtime surprises.

### what you define in a script

- 🏷️ **Command name and aliases**
- 📍 **Where to register** (which proxies, which backends)
- 📥 **Arguments** (type, required/optional, suggestions)
- ⚙️ **Execution settings** (run as console/player/operator, delays, cooldowns)
- 🎯 **Target backends** (where to execute, can be multiple)
- 📋 **Command templates** (with placeholder substitution)
- 🔐 **Permissions** (auto-generated or custom)

### script structure

here's a fully annotated example:

```yaml
version: 2  # Schema version (v3 still uses schema 2)

# Basic metadata
name: "eco"
description: "Proxy economy commands to backends"
enabled: true
aliases: ["economy", "money"]

# Permission system
permissions:
  enabled: true   # Requires commandbridge.command.eco
  silent: false   # Show error message if permission missing

# Where to register the command (v3 multi-proxy support!)
register:
  - id: proxy-main
    location: VELOCITY  # Register on this proxy
  - id: proxy-eu
    location: VELOCITY  # Also register on EU proxy
  # Command is now on BOTH proxies!

# Default execution behavior (can be overridden per command)
defaults:
  run-as: CONSOLE  # Execute as backend console
  
  # Where to execute (v3 targets any client in the mesh)
  execute:
    - id: survival
      location: BACKEND
    - id: creative
      location: BACKEND
  # Command executes on BOTH backends
  
  # Server behavior for player-targeted commands
  server:
    target-required: true   # Abort if player not online
    schedule-online: false  # Don't queue for later
    timeout: 10m            # Max wait time for scheduled commands
    frequency: 30s          # Check frequency for scheduled commands
  
  delay: 0s      # Wait before executing
  cooldown: 0s   # Per-player cooldown

# Argument definitions
args:
  - name: player
    type: PLAYERS  # Minecraft selector syntax (@a, @p, names)
    required: true
    suggestions:
      - "@a"  # All players
      - "@p"  # Nearest player
      - "@r"  # Random player
  
  - name: amount
    type: RANGE  # Numeric range (1..100)
    required: true

# Actual commands to execute
commands:
  - command: "eco give ${player} ${amount}"
    # Inherits all defaults
    
  - command: "eco set ${player} ${amount}"
    run-as: OPERATOR  # Override: temp op for this command
    delay: 2s         # Override: wait 2 seconds
    
  - command: "eco take ${player} ${amount}"
    server:
      target-required: false  # Override: don't require player online
```

### argument types (all 25+)

the `type` field determines what kind of value is accepted and how it's parsed. some types are platform-specific.

#### 🌍 universal types (work anywhere)

| Type | Description | Example |
|------|-------------|---------|
| `STRING` | Single word | `"hello"` |
| `INTEGER` | Whole numbers | `42` |
| `BOOLEAN` | True/false | `true` |
| `DOUBLE` | Decimals | `3.14` |
| `TEXT` | Rest of line (with spaces) | `"hello world"` |
| `RANGE` | Numeric ranges | `1..100` |

#### 👥 selector types

| Type | Description | Platform |
|------|-------------|----------|
| `PLAYERS` | Player selector (@a, @p, names) | Backend |
| `ENTITIES` | Entity selector (@e[type=zombie]) | Backend |
| `ENTITY_TYPE` | Entity type (zombie, creeper) | Backend |

#### 🏗️ backend-only types

| Type | Description | Use Case |
|------|-------------|----------|
| `WORLD` | World names | Teleportation, world management |
| `LOCATION` | 3D coordinates (x y z) | Teleports, spawning |
| `LOCATION_2D` | 2D coordinates (x z) | Map markers |
| `ANGLE` | Rotation angle | Entity facing |
| `ROTATION` | Full rotation (yaw/pitch) | Player orientation |
| `ITEM_STACK` | Items with NBT | Item commands |
| `ENCHANTMENT` | Enchantment types | Enchant commands |
| `POTION_EFFECT` | Potion effects | Effect commands |
| `SOUND` | Sound identifiers | Sound commands |
| `BIOME` | Biome types | World generation |
| `TIME` | Time values (ticks/duration) | Time commands |

#### 🚀 proxy-only types

| Type | Description | Use Case |
|------|-------------|----------|
| `SERVER` | Velocity server names | Server switching, targeting |

**validation**: if you try to use a backend-only type (like `WORLD`) in a command registered on Velocity, the `PlatformProcessor` will reject it at load time. same for proxy-only types on backends.

### placeholder resolution

templates use `${argumentName}` syntax. when a command runs:

1. **Arguments are substituted** – `${player}` becomes `Steve`, `${amount}` becomes `100`
2. **Validation ensures all placeholders resolve** – if you reference `${nonexistent}`, the script fails to load
3. **No runtime errors** – everything checked at startup

the `ResolvableProcessor` walks through all command templates and verifies every placeholder matches a defined argument name.

**example**:
```yaml
args:
  - name: target
    type: PLAYERS
  - name: amount
    type: INTEGER

commands:
  - command: "give ${target} diamond ${amount}"
    # ✅ Valid: both ${target} and ${amount} are defined
  
  - command: "give ${player} diamond ${amount}"
    # ❌ Invalid: ${player} not defined (should be ${target})
    # Script fails to load with detailed error
```

### execution contexts (run-as)

the `run-as` field controls who executes the command on the backend:

| Mode | Description | Use Case | Risk |
|------|-------------|----------|------|
| **CONSOLE** | Backend console (elevated privileges) | Admin commands | Low |
| **PLAYER** | Player who triggered command | Player-permission commands | Low |
| **OPERATOR** | **Temporary op** for command duration | Powerful commands | **High** ⚠️ |

**OPERATOR mode warning**: player gets opped, command executes, then deopped. some anti-cheat plugins freak out. test thoroughly before using in production.

### deferred execution (offline player commands)

one of the coolest features: queue commands for offline players.

```yaml
server:
  target-required: true   # Command needs player online
  schedule-online: true   # Queue if offline
  timeout: 1h             # Give up after 1 hour
  frequency: 30s          # Check every 30 seconds
```

**use case**: kick someone who's offline. they get kicked the moment they log in.

**how it works**:
1. Command executed while player offline
2. `target-required: true` checks for player, doesn't find them
3. `schedule-online: true` adds command to queue
4. Every 30 seconds, proxy checks if player online
5. When they log in, command executes
6. If 1 hour passes, command dropped

### cooldowns and delays

**delays** wait before executing:
```yaml
delay: 5s  # Wait 5 seconds, then execute
```

**cooldowns** prevent spam (per-player):
```yaml
cooldown: 30s  # Player can only use command once per 30 seconds
```

**duration format**: `5s`, `10m`, `2h`, `1d`, or bare numbers (seconds)

### multi-target execution (v3 highlight) 🌐

execute one command on multiple backends:

```yaml
execute:
  - id: survival
    location: BACKEND
  - id: creative
    location: BACKEND
  - id: skyblock
    location: BACKEND
```

run `/broadcast hello` on proxy → executes on all three backends.

**v3 extends this across the proxy mesh**: target clients connected to ANY proxy in your network, not just the one the player is on.

### inheritance and overrides

commands inherit from `defaults` unless explicitly overridden:

```yaml
defaults:
  run-as: CONSOLE
  delay: 0s
  cooldown: 10s

commands:
  - command: "foo"
    # ✅ Inherits all defaults
    
  - command: "bar"
    run-as: PLAYER
    # ✅ Overrides run-as, inherits delay and cooldown
    
  - command: "baz"
    delay: 5s
    cooldown: 10s
    # ⚠️ Redundant cooldown override (same as default)
    # DefaultProcessor will warn you
```

### validation pipeline

when scripts load, they go through:

1. **YAML parsing** – SnakeYAML binds to Java records
2. **DefaultProcessor** – applies `@Default` annotations, warns about redundant overrides
3. **PlatformProcessor** – validates argument types match platform (no WORLD on proxy commands)
4. **ResolvableProcessor** – ensures all placeholders resolve, checks duplicate argument names
5. **RangeValidator** – validates `@Min` and `@Max` annotations
6. **RequiredValidator** – ensures `@Required` fields present

if any processor finds problems → collected in `ProblemSink` → logged → script not registered.

### best practices

✅ **DO**:
- Use unique argument names (duplicates rejected)
- Use case-sensitive enums (`PLAYERS` not `players`)
- Use `PLAYERS` with selectors like `@p` for single player (no singular PLAYER type)
- Test OPERATOR mode carefully (anti-cheat implications)
- Use cooldowns to protect expensive commands
- Check `ProblemSink` logs for validation errors

❌ **DON'T**:
- Use backend-only types (WORLD, LOCATION) on proxy-registered commands
- Use proxy-only types (SERVER) on backend-registered commands
- Add redundant overrides (same value as default)
- Rely on RANGE min/max validation (removed from model, check in backend)
- Forget to validate script loading output

## 📚 examples

### example 1: simple economy command

```yaml
version: 2
name: "balance"
description: "Check player balance"
enabled: true
aliases: ["bal", "money"]

permissions:
  enabled: true
  silent: false

register:
  - id: proxy-main
    location: VELOCITY

defaults:
  run-as: PLAYER
  execute:
    - id: survival
      location: BACKEND
  server:
    target-required: true
    schedule-online: false
  delay: 0s
  cooldown: 3s

args:
  - name: player
    type: PLAYERS
    required: false
    suggestions: ["@p"]

commands:
  - command: "balance ${player}"
  - command: "balance"  # No argument = executor's balance
```

### example 2: cross-server teleport

```yaml
version: 2
name: "tpserver"
description: "Teleport player to different server"
enabled: true

permissions:
  enabled: true

register:
  - id: proxy-main
    location: VELOCITY

defaults:
  run-as: CONSOLE
  execute:
    - id: proxy-main
      location: VELOCITY
  delay: 1s
  cooldown: 5s

args:
  - name: player
    type: PLAYERS
    required: true
  
  - name: server
    type: SERVER  # Proxy-only type!
    required: true
    suggestions: ["survival", "creative", "lobby"]

commands:
  - command: "send ${player} ${server}"
```

### example 3: deferred kick (offline players)

```yaml
version: 2
name: "kicklater"
description: "Kick player when they next log in"
enabled: true

permissions:
  enabled: true

register:
  - id: proxy-main
    location: VELOCITY

defaults:
  run-as: CONSOLE
  execute:
    - id: survival
      location: BACKEND
  server:
    target-required: true
    schedule-online: true  # Queue if offline!
    timeout: 24h           # Wait up to 24 hours
    frequency: 1m          # Check every minute
  delay: 0s
  cooldown: 0s

args:
  - name: player
    type: PLAYERS
    required: true
  
  - name: reason
    type: TEXT
    required: true

commands:
  - command: "kick ${player} ${reason}"
```

### example 4: global announcement (v3 multi-target)

```yaml
version: 2
name: "globalannounce"
description: "Announce to all servers in network"
enabled: true
aliases: ["ga", "broadcast"]

permissions:
  enabled: true

register:
  - id: proxy-main
    location: VELOCITY
  - id: proxy-eu
    location: VELOCITY

defaults:
  run-as: CONSOLE
  execute:
    - id: survival
      location: BACKEND
    - id: creative
      location: BACKEND
    - id: lobby
      location: BACKEND
    - id: skyblock
      location: BACKEND
    - id: minigames
      location: BACKEND
  delay: 0s
  cooldown: 30s

args:
  - name: message
    type: TEXT
    required: true

commands:
  - command: "say [Network] ${message}"
```

## 🏗️ architecture

### module structure

```
CommandBridge/
├── core/              # Shared code for all platforms
│   ├── WebSocket client/server (Undertow)
│   ├── TLS authentication
│   ├── Scripting engine
│   ├── Record models (Script, ArgMapping, etc.)
│   ├── Validation processors
│   └── Type adapters
│
├── velocity/          # Velocity proxy plugin
│   ├── WebSocket server
│   ├── TLS server setup
│   ├── Proxy mesh management
│   ├── Command registration
│   ├── Client connection handling
│   └── Script loading
│
├── backends/          # Backend platform code
│   ├── bukkit/       # Bukkit implementation
│   ├── paper/        # Paper-specific features
│   ├── folia/        # Folia support
│   └── Common backend logic
│
└── dist/             # Fat JAR assembly
    └── Single unified JAR for all platforms
```

### networking architecture

**WebSocket protocol**:
- Velocity runs Undertow-based WebSocket server
- Paper servers connect as WebSocket clients
- Persistent bidirectional TCP connections
- No dependency on plugin messaging or player presence

**message protocol**:

```java
// Envelope structure (JSON-based)
record Envelope(
    int v,              // Protocol version
    UUID id,            // Message ID
    MessageType type,   // AUTH_REQUEST, INVOKED_COMMAND, etc.
    String from,        // Sender client-id
    String to,          // Target client-id
    long ts,            // Timestamp
    JsonNode payload    // Message payload
)
```

**message types**:
- `AUTH_REQUEST` / `AUTH_OK` / `AUTH_FAIL` – TLS authentication handshake
- `REGISTER_COMMANDS` / `REGISTER_COMMANDS_RESULT` – Command registration
- `INVOKED_COMMAND` – Command execution
- `PING` / `PONG` – Keepalive

**v3 mesh topology**:
- Proxies connect to each other as peers
- Client registry synchronized across mesh
- Commands routed to any client regardless of proxy
- Service discovery via configured peer list

### TLS authentication (v3)

**why TLS over shared secrets?**
- ✅ Industry-standard encryption (TLS 1.3)
- ✅ Mutual authentication (both sides verify)
- ✅ Certificate-based trust (no shared secrets to leak)
- ✅ Perfect forward secrecy
- ✅ Built into Java (javax.net.ssl)

**TLS flow**:
1. Velocity generates CA certificate and server certificate on first start
2. Paper connects with TLS handshake
3. Mutual verification: server verifies client cert, client verifies server cert
4. Encrypted channel established
5. All messages encrypted in transit

**certificate structure**:
```
velocity/plugins/CommandBridge/tls/
├── ca.crt          # CA certificate (trust anchor)
├── ca.key          # CA private key
├── server.p12      # Server certificate (PKCS#12)
└── server.crt      # Server certificate (PEM)

paper/plugins/CommandBridge/tls/
├── ca.crt          # Copy of Velocity's CA cert
├── client.p12      # Client certificate (PKCS#12)
└── client.crt      # Client certificate (PEM)
```

### state management

**client registry**:
- Velocity maintains map of connected clients
- v3: registry synced across proxy mesh
- includes: client-id, connection timestamp, last heartbeat, connected proxy

**deferred command queue**:
- stored per client
- checked at configured frequency
- items expire based on timeout
- commands execute when player detected online

**cooldown tracking**:
- per-player cooldown map
- resets on server restart (not persisted)
- stored in memory on proxy side

## 🛠️ building from source

### requirements

- Java Development Kit 21
- Git
- Gradle (wrapper included)

### build steps

```bash
# Clone repo
git clone https://github.com/objz/CommandBridge.git
cd CommandBridge

# Checkout v3 branch
git checkout v3

# Build with Gradle
./gradlew shadowJar

# Output: dist/build/libs/CommandBridge-3.0.0-all.jar
```

the `shadowJar` task creates a fat JAR with all dependencies bundled and relocated to avoid conflicts.

### key dependencies

| Dependency | Purpose |
|------------|---------|
| `io.undertow:undertow-websockets-jsr` | WebSocket server/client |
| `org.bouncycastle:bcprov-jdk18on` | TLS/SSL crypto |
| `com.fasterxml.jackson` | JSON serialization |
| `org.spongepowered:configurate-yaml` | Config parsing |
| `dev.jorel:commandapi-spigot-core` | Advanced argument types |
| Velocity API | Proxy integration |
| Paper/Bukkit API | Backend integration |

## 🚧 current development status

v3 is **actively in development**. here's what's working and what's coming:

### ✅ implemented

- [x] Core WebSocket communication
- [x] Mutual TLS authentication
- [x] Multi-proxy mesh topology
- [x] YAML scripting system with 25+ argument types
- [x] Placeholder resolution and validation
- [x] Command registration on Velocity and backends
- [x] Deferred execution for offline players
- [x] Cooldowns and delays
- [x] Cross-proxy command execution
- [x] Client registry synchronization
- [x] Admin commands (`/cb`, `/cbc`)
- [x] Script hot reload

### 🚧 in progress

- [ ] **Advanced command parsing on Velocity** – implementing full argument validation and suggestions on the proxy side (currently basic)
- [ ] **Developer API** – public API for third-party plugins to:
  - Send commands programmatically
  - Register custom message types
  - Hook into command execution pipeline
  - Create custom argument types
- [ ] **Online configuration panel** – web-based UI to:
  - Manage scripts without editing YAML
  - View connected clients and topology
  - Monitor command execution and logs
  - Manage TLS certificates
  - Real-time network status

### 📋 planned features

- [ ] Command execution history and analytics
- [ ] Advanced rate limiting (global, per-server)
- [ ] Command macros (chain multiple scripts)
- [ ] Conditional execution based on player state
- [ ] Integration with external databases
- [ ] Plugin marketplace for community scripts
- [ ] Metrics and monitoring dashboard
- [ ] Webhook support for command events

## 🔌 developer API (coming soon)

the developer API will allow third-party plugins to integrate with CommandBridge:

**planned API features**:

```java
// Send commands programmatically
CommandBridge.execute(
    CommandRequest.builder()
        .command("eco give Steve 1000")
        .target("survival")
        .runAs(RunAs.CONSOLE)
        .build()
);

// Register custom message handlers
CommandBridge.registerMessageHandler(MyMessageType.class, handler);

// Hook into command execution
CommandBridge.addCommandInterceptor((cmd, ctx) -> {
    // Modify, log, or cancel commands
    return InterceptResult.CONTINUE;
});

// Create custom argument types
CommandBridge.registerArgumentType(
    "CUSTOM_TYPE",
    new MyCustomArgumentType(),
    Platform.BACKEND
);
```

**use cases**:
- economy plugins that need cross-server transactions
- punishment plugins that work across the network
- custom admin tools
- monitoring and logging systems
- integration with external services

## 🐛 troubleshooting

### clients won't connect

**symptoms**: Paper can't connect to Velocity, authentication failures

**fixes**:
- ✅ Check TLS certificates are properly configured
- ✅ Verify Velocity port is open (firewall/security groups)
- ✅ Ensure Velocity IP in Paper config is correct
- ✅ Check Velocity logs for TLS errors
- ✅ Try regenerating certificates with `/cb tls regenerate`
- ✅ Verify `tls.enabled: true` on both sides

### commands not executing

**symptoms**: command runs on proxy but nothing happens on backend

**fixes**:
- ✅ Verify script is loaded (check `/cb list` or logs)
- ✅ Ensure `execute` list includes correct client-id
- ✅ Check backend is connected (logs: "Client authenticated successfully")
- ✅ Review script validation errors in console
- ✅ Try `/cb reload` to reload scripts

### "placeholder could not be resolved" errors

**symptoms**: script fails to load with placeholder errors

**fixes**:
- ✅ Check all `${placeholders}` match argument names (case-sensitive!)
- ✅ Ensure argument names are unique
- ✅ Verify no typos in placeholder names
- ✅ Check for missing argument definitions

### "argument type not supported on platform" errors

**symptoms**: script fails validation with platform compatibility errors

**fixes**:
- ✅ Don't use backend-only types (WORLD, LOCATION) in Velocity commands
- ✅ Don't use proxy-only types (SERVER) in backend commands
- ✅ Review `@Platform` annotations on `ArgType` enum
- ✅ Separate commands by platform if needed

### proxy mesh not working

**symptoms**: commands don't execute on clients connected to other proxies

**fixes**:
- ✅ Verify `proxy-mesh.enabled: true` on all proxies
- ✅ Check peer configuration (correct host/port)
- ✅ Ensure TLS certificates are synced across proxies
- ✅ Review proxy logs for peer connection status
- ✅ Try `/cb mesh status` to see topology
- ✅ Confirm firewalls allow proxy-to-proxy communication

### TLS handshake failures

**symptoms**: TLS errors in logs, connection refused

**fixes**:
- ✅ Regenerate certificates: `/cb tls regenerate`
- ✅ Copy CA certificate to all clients
- ✅ Check certificate expiration
- ✅ Verify TLS mode matches (`MUTUAL` on both sides)
- ✅ Check system time is synchronized

## 📊 performance

v3 is faster than v2 for command execution (better Undertow handling), but mesh topology adds overhead for cross-proxy communication.

**benchmarks** (single proxy):
- Command execution latency: ~5-15ms
- WebSocket message roundtrip: ~2-5ms
- Script validation: ~50-100ms per script
- TLS handshake: ~10-20ms (one-time)

**mesh overhead**:
- Cross-proxy routing: +5-10ms
- Client registry sync: ~100ms (periodic)
- Mesh discovery: ~500ms (periodic)

TLS encryption is computationally cheap (hardware-accelerated). message serialization is JSON-based (fast enough for commands).

## 🤝 contributing

PRs welcome! please:
- Follow existing code style
- Add tests for new features
- Update documentation
- Test thoroughly before submitting

## 📄 license

GPLv3 – see [LICENSE](LICENSE) file for full terms

## 🔗 links

- **Repository**: https://github.com/objz/CommandBridge
- **Issues**: https://github.com/objz/CommandBridge/issues
- **Discussions**: https://github.com/objz/CommandBridge/discussions
- **Website**: https://cb.objz.dev (coming soon)

## 💬 support

- GitHub Issues for bug reports
- GitHub Discussions for questions
- Discord server (check repo for invite)

---

<div align="center">

**made with ☕ and frustration at plugin messaging**

*yeah that's CommandBridge v3. it bridges commands. across multiple proxies. with TLS. revolutionary stuff.*

</div>
