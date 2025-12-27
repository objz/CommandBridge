# CommandBridge v3.0.0

![License](https://img.shields.io/badge/license-GPLv3-blue.svg) ![Java](https://img.shields.io/badge/Java-21-orange.svg) ![Minecraft](https://img.shields.io/badge/Minecraft-1.20.x--1.21.x-green.svg) ![Version](https://img.shields.io/badge/version-3.0.0-brightgreen.svg) ![Build](https://img.shields.io/badge/build-passing-success.svg)

yeah so this is CommandBridge. it's basically how you make commands work across your entire Minecraft network without losing your sanity. v3 is a complete ground-up rewrite – we rewrote the entire codebase from scratch. new module architecture, new networking layer, new scripting engine, everything.

## what even is this

alright so here's the deal. you've got Velocity proxies and Paper servers. normally they don't talk to each other very well. plugin messaging is a mess, especially when nobody's online. CommandBridge solves this by using WebSockets (real TCP connections, not that plugin channel garbage) so your servers can actually communicate reliably.

why did i build this? because i got tired of writing Java command classes every time i wanted to proxy a simple economy command or warp. seriously, who wants to compile code just to add `/spawn` to your proxy? with v3, you drop a YAML file in a folder and boom, it works.

## why v3 is a complete rewrite

v2 worked. it switched from plugin messaging to WebSockets, added a scripting system, solved the core problem. but the codebase had grown organically and there were fundamental architectural limitations that couldn't be fixed without starting over.

here's what actually changed in the v3 rewrite:

### code architecture (242 files changed, 17,246 insertions, 11,988 deletions)

**build system restructure**:
- v2: monolithic `build.gradle.kts` at root with gradle.extra properties
- v3: proper multi-project Gradle build with separate module configs

**module reorganization**:
- v2: 3 modules (paper, velocity, core)
- v3: 6 modules (core, velocity, backends, backends:bukkit, backends:paper, backends:folia, dist)
  - backends module with platform-specific sub-modules
  - dedicated dist module for fat JAR assembly
  - proper module separation with clean dependencies

**core package complete rewrite**:

v2 had this structure:
```
core/src/main/java/dev/objz/commandbridge/core/
├── Logger.java
├── json/
│   ├── MessageBuilder.java
│   └── MessageParser.java
├── utils/
│   ├── ConfigManager.java
│   ├── ScriptManager.java
│   ├── StringParser.java
│   ├── TLSUtils.java
│   └── VersionChecker.java
└── websocket/
    ├── WebSocketClient.java
    └── WebSocketServer.java
```

v3 completely restructured into:
```
core/src/main/java/dev/objz/commandbridge/
├── cmd/                    # NEW: command abstraction layer
│   ├── ArgumentMapperInterface.java
│   ├── CommandRegistryInterface.java
│   └── ref/               # NEW: reference types for locations, entities
├── config/                # REWRITTEN: new config system
│   ├── ConfigManager.java (completely new implementation)
│   ├── ConfigKeys.java
│   ├── model/            # NEW: record-based config models
│   └── profile/          # NEW: config profiles with validation
├── logging/              # REWRITTEN: new logging system
│   ├── Log.java (replaced Logger.java with 568 lines)
│   └── Summary.java
├── net/                  # REWRITTEN: new networking layer
│   ├── InNode.java, OutNode.java
│   ├── InboundHandler.java, OutboundHandler.java
│   ├── ResponseAwaiter.java, SendOperation.java
│   ├── payloads/         # NEW: structured payload system
│   │   ├── cmd/
│   │   ├── feedback/
│   │   └── util/
│   └── proto/            # NEW: protocol definition
│       ├── Envelope.java (replaced MessageBuilder/Parser)
│       └── MessageType.java
├── scripting/            # COMPLETELY NEW: 65+ files
│   ├── ScriptLoader.java
│   ├── DebugPrinter.java (665 lines)
│   ├── bind/             # NEW: YAML binding system
│   ├── model/            # NEW: script models (records)
│   ├── validation/       # NEW: validation pipeline
│   └── yaml/             # NEW: custom YAML parser
└── security/             # REWRITTEN: TLS authentication
    ├── AuthService.java
    ├── TLS.java, TlsResolver.java
    ├── StrictKeystore.java
    ├── SecretLoader.java
    └── TrustManager.java
```

**what got deleted**:
- `WebSocketClient.java`, `WebSocketServer.java` - replaced with Undertow-based system
- `MessageBuilder.java`, `MessageParser.java` - replaced with Envelope protocol
- `ScriptManager.java`, `StringParser.java` - completely rewritten
- `TLSUtils.java`, `VersionChecker.java` - removed or replaced
- entire v2 json package - replaced with Jackson

**what's completely new**:
- scripting engine with 65+ files (YAML parser, binder, validators, processors)
- Envelope-based message protocol with proper typing
- record-based models throughout (immutable, type-safe)
- TLS authentication system
- platform abstraction layer with adapters
- proper annotation-driven validation

the rewrite wasn't about adding features. it was about building the right foundation. v2's code worked but was becoming unmaintainable. v3 starts clean with modern Java patterns, proper separation of concerns, and extensibility built in from the start.

## features

### networking & communication

- WebSocket-based persistent TCP connections using Undertow
- works without players online
- Envelope message protocol with structured payloads
- proper request/response handling with ResponseAwaiter
- TLS-based authentication (mutual verification)
- distributed state management

### scripting system

- YAML-based command definitions
- 25+ argument types (STRING, INTEGER, PLAYERS, ENTITIES, WORLD, SERVER, LOCATION, ITEM_STACK, ENCHANTMENT, POTION_EFFECT, SOUND, BIOME, TIME, and more)
- placeholder system with `${argumentName}` syntax
- run-as modes: CONSOLE, PLAYER, OPERATOR
- deferred execution for offline players
- cooldowns and delays (per-player)
- permission integration
- multi-target execution
- platform-specific argument validation (backend-only types, proxy-only types)

### platform support

- Java 21 only (uses records, pattern matching, modern APIs)
- Minecraft 1.20.x to 1.21.x
- Velocity (primary), Waterfall (supported)
- Paper (primary), Folia, Purpur, Spigot, Bukkit

### architecture

- modular Gradle build with clean dependencies
- single JAR deployment (auto-detects platform)
- shadowJar with dependency relocation
- annotation-driven validation (@Default, @Merge, @Platform, @Required, @Min, @Max)
- record-based immutable models
- platform abstraction with adapters

## installation

### requirements

- Java 21 (not 17, not 11, definitely not 8)
- Velocity proxy (or Waterfall)
- Paper backend servers (Folia, Purpur, Spigot, Bukkit should work)
- Minecraft 1.20.x or 1.21.x

### basic setup

1. download `CommandBridge-3.0.0-all.jar` from releases
2. put it in plugins folders on BOTH Velocity and Paper (same JAR, auto-detects platform)
3. restart Velocity first, then Paper servers (configs will generate)
4. configure TLS authentication:
   - Velocity generates certificates in `plugins/CommandBridge/tls/`
   - Paper servers need CA certificate copied from Velocity
   - or use auto-enroll mode for testing (not recommended for production)
5. set up networking:
   - Velocity: `host: "0.0.0.0"`, `port: 8080`
   - Paper: `remote: "velocity-ip"`, `port: 8080`
6. configure identifiers:
   - each proxy needs unique `server-id`
   - each backend needs unique `client-id`
7. restart in order (Velocity first, then Paper)
8. check logs for successful authentication

## commands

### `/cb` (or `/commandbridge`)

main admin command. requires `commandbridge.admin` permission.

- `/cb reload` - reload configs and scripts
- `/cb list` - list all connected clients
- `/cb dump` - generate debug dump URL
- `/cb tls info` - show TLS certificate information
- `/cb tls regenerate` - regenerate TLS certificates
- `/cb help` - show help

### `/cbc reconnect` (Paper only)

reconnect Paper server to Velocity. requires `commandbridge.admin`.

### script-defined commands

any command defined in scripts gets registered automatically with permission `commandbridge.command.<script-name>`.

## the scripting system

scripts live in `plugins/CommandBridge/scripts/`. each `.yml` file defines one command. the plugin loads them on startup and validates everything before registering.

### script structure

```yaml
version: 2

name: "eco"
description: "Proxy economy commands to backends"
enabled: true
aliases: ["economy", "money"]

permissions:
  enabled: true
  silent: false

register:
  - id: proxy-main
    location: VELOCITY

defaults:
  run-as: CONSOLE
  execute:
    - id: survival
      location: BACKEND
    - id: creative
      location: BACKEND
  server:
    target-required: true
    schedule-online: false
    timeout: 10m
    frequency: 30s
  delay: 0s
  cooldown: 0s

args:
  - name: player
    type: PLAYERS
    required: true
    suggestions: ["@a", "@p", "@r"]
  
  - name: amount
    type: RANGE
    required: true

commands:
  - command: "eco give ${player} ${amount}"
  - command: "eco set ${player} ${amount}"
    run-as: OPERATOR
    delay: 2s
  - command: "eco take ${player} ${amount}"
    server:
      target-required: false
```

### argument types

**universal types** (work anywhere):
- STRING, INTEGER, BOOLEAN, DOUBLE, TEXT, RANGE

**selector types**:
- PLAYERS (player selector: @a, @p, @r, names)
- ENTITIES (entity selector)
- ENTITY_TYPE (zombie, creeper, etc.)

**backend-only types**:
- WORLD, LOCATION, LOCATION_2D, ANGLE, ROTATION
- ITEM_STACK, ENCHANTMENT, POTION_EFFECT
- SOUND, BIOME, TIME

**proxy-only types**:
- SERVER (Velocity server names)

the `PlatformProcessor` validates argument types match registration location at load time. using a backend-only type (like WORLD) in a proxy-registered command fails validation.

### placeholder resolution

templates use `${argumentName}` syntax. when a command runs, arguments are substituted. the `ResolvableProcessor` validates all placeholders resolve to defined arguments at load time.

example:
```yaml
args:
  - name: target
    type: PLAYERS

commands:
  - command: "give ${target} diamond 64"  # valid
  - command: "give ${player} diamond 64"  # invalid - ${player} not defined
```

### execution contexts (run-as)

- **CONSOLE**: runs as backend console (elevated privileges)
- **PLAYER**: runs as player who triggered command (keeps player permissions)
- **OPERATOR**: temporarily grants op for command duration (careful with anti-cheat)

### deferred execution

queue commands for offline players:

```yaml
server:
  target-required: true
  schedule-online: true
  timeout: 1h
  frequency: 30s
```

command executes when player logs in. if timeout expires, command is dropped.

### cooldowns and delays

```yaml
delay: 5s      # wait before executing
cooldown: 30s  # per-player rate limit
```

duration format: `5s`, `10m`, `2h`, `1d`

### multi-target execution

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

### inheritance and overrides

commands inherit from `defaults` unless explicitly overridden:

```yaml
defaults:
  run-as: CONSOLE
  delay: 0s

commands:
  - command: "foo"
    # inherits all defaults
    
  - command: "bar"
    run-as: PLAYER
    # overrides run-as, inherits delay
```

### validation pipeline

when scripts load:

1. YAML parsing (SnakeYAML binds to Java records)
2. DefaultProcessor (applies @Default annotations)
3. PlatformProcessor (validates argument types match platform)
4. ResolvableProcessor (ensures placeholders resolve)
5. RangeValidator (validates @Min and @Max)
6. RequiredValidator (ensures @Required fields present)

broken scripts don't register. errors logged with details.

## examples

### simple economy command

```yaml
version: 2
name: "balance"
description: "Check player balance"
enabled: true
aliases: ["bal", "money"]

permissions:
  enabled: true

register:
  - id: proxy-main
    location: VELOCITY

defaults:
  run-as: PLAYER
  execute:
    - id: survival
      location: BACKEND
  cooldown: 3s

args:
  - name: player
    type: PLAYERS
    required: false
    suggestions: ["@p"]

commands:
  - command: "balance ${player}"
  - command: "balance"
```

### cross-server teleport

```yaml
version: 2
name: "tpserver"
description: "Teleport player to different server"
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
    type: SERVER
    required: true
    suggestions: ["survival", "creative", "lobby"]

commands:
  - command: "send ${player} ${server}"
```

### deferred kick (offline players)

```yaml
version: 2
name: "kicklater"
description: "Kick player when they next log in"
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
    schedule-online: true
    timeout: 24h
    frequency: 1m

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

### global announcement

```yaml
version: 2
name: "globalannounce"
description: "Announce to all servers in network"
enabled: true
aliases: ["ga", "broadcast"]

register:
  - id: proxy-main
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
  cooldown: 30s

args:
  - name: message
    type: TEXT
    required: true

commands:
  - command: "say [Network] ${message}"
```

## architecture

### module structure

```
CommandBridge/
├── core/              # shared code for all platforms
│   ├── WebSocket client/server (Undertow)
│   ├── TLS authentication
│   ├── scripting engine
│   ├── record models
│   ├── validation processors
│   └── type adapters
│
├── velocity/          # Velocity proxy plugin
│   ├── WebSocket server
│   ├── TLS server setup
│   ├── command registration
│   ├── client connection handling
│   └── script loading
│
├── backends/          # backend platform code
│   ├── bukkit/       # Bukkit implementation
│   ├── paper/        # Paper-specific features
│   ├── folia/        # Folia support
│   └── common backend logic
│
└── dist/             # fat JAR assembly
    └── single unified JAR for all platforms
```

### networking architecture

**WebSocket protocol**:
- Velocity runs Undertow-based WebSocket server
- Paper servers connect as WebSocket clients
- persistent bidirectional TCP connections
- no dependency on plugin messaging

**message protocol**:

```java
record Envelope(
    int v,              // protocol version
    UUID id,            // message ID
    MessageType type,   // AUTH_REQUEST, INVOKED_COMMAND, etc.
    String from,        // sender client-id
    String to,          // target client-id
    long ts,            // timestamp
    JsonNode payload    // message payload
)
```

message types:
- AUTH_REQUEST, AUTH_OK, AUTH_FAIL
- REGISTER_COMMANDS, REGISTER_COMMANDS_RESULT
- INVOKED_COMMAND
- PING, PONG

### TLS authentication

**why TLS**:
- industry-standard encryption (TLS 1.3)
- mutual authentication (both sides verify)
- certificate-based trust
- perfect forward secrecy

**TLS flow**:
1. Velocity generates CA certificate and server certificate on first start
2. Paper connects with TLS handshake
3. mutual verification: server verifies client cert, client verifies server cert
4. encrypted channel established
5. all messages encrypted in transit

**certificate structure**:
```
velocity/plugins/CommandBridge/tls/
├── ca.crt          # CA certificate
├── ca.key          # CA private key
├── server.p12      # server certificate
└── server.crt      # server certificate (PEM)

paper/plugins/CommandBridge/tls/
├── ca.crt          # copy of Velocity's CA cert
├── client.p12      # client certificate
└── client.crt      # client certificate (PEM)
```

### state management

**client registry**:
- Velocity maintains map of connected clients
- includes: client-id, connection timestamp, last heartbeat

**deferred command queue**:
- stored per client
- checked at configured frequency
- items expire based on timeout
- commands execute when player detected online

**cooldown tracking**:
- per-player cooldown map
- resets on server restart (not persisted)
- stored in memory on proxy side

## building from source

### requirements

- Java Development Kit 21
- Git
- Gradle (wrapper included)

### build steps

```bash
# clone repo
git clone https://github.com/objz/CommandBridge.git
cd CommandBridge

# checkout v3 branch
git checkout v3

# build with Gradle
./gradlew shadowJar

# output: dist/build/libs/CommandBridge-3.0.0-all.jar
```

the `shadowJar` task creates a fat JAR with all dependencies bundled and relocated.

### key dependencies

- io.undertow:undertow-websockets-jsr - WebSocket server/client
- org.bouncycastle:bcprov-jdk18on - TLS/SSL crypto
- com.fasterxml.jackson - JSON serialization
- org.spongepowered:configurate-yaml - config parsing
- dev.jorel:commandapi-spigot-core - advanced argument types
- Velocity API, Paper/Bukkit API

## current development status

v3 is in active development. here's what's implemented and what's coming:

### implemented

- core WebSocket communication
- TLS authentication
- YAML scripting system with 25+ argument types
- placeholder resolution and validation
- command registration on Velocity and backends
- deferred execution for offline players
- cooldowns and delays
- admin commands
- script hot reload

### in progress

- advanced command parsing on Velocity (implementing full argument validation on proxy side)
- developer API (public API for third-party plugins to send commands, register custom message types, hook into execution pipeline)
- online configuration panel (web-based UI to manage scripts, view clients, monitor execution, manage certificates)

### planned

- command execution history and analytics
- advanced rate limiting (global, per-server)
- command macros (chain multiple scripts)
- conditional execution based on player state
- integration with external databases
- metrics and monitoring dashboard

## developer API (coming soon)

planned API features:

```java
// send commands programmatically
CommandBridge.execute(
    CommandRequest.builder()
        .command("eco give Steve 1000")
        .target("survival")
        .runAs(RunAs.CONSOLE)
        .build()
);

// register custom message handlers
CommandBridge.registerMessageHandler(MyMessageType.class, handler);

// hook into command execution
CommandBridge.addCommandInterceptor((cmd, ctx) -> {
    // modify, log, or cancel commands
    return InterceptResult.CONTINUE;
});
```

use cases: economy plugins, punishment systems, admin tools, monitoring, external service integration.

## troubleshooting

### clients won't connect

- check TLS certificates are properly configured
- verify Velocity port is open (firewall/security groups)
- ensure Velocity IP in Paper config is correct
- check logs for TLS errors
- try regenerating certificates with `/cb tls regenerate`

### commands not executing

- verify script is loaded (check `/cb list` or logs)
- ensure `execute` list includes correct client-id
- check backend is connected (logs: "Client authenticated successfully")
- review script validation errors in console
- try `/cb reload`

### "placeholder could not be resolved" errors

- check all `${placeholders}` match argument names (case-sensitive)
- ensure argument names are unique
- verify no typos

### "argument type not supported on platform" errors

- don't use backend-only types (WORLD, LOCATION) in Velocity commands
- don't use proxy-only types (SERVER) in backend commands
- review @Platform annotations on ArgType enum

### TLS handshake failures

- regenerate certificates: `/cb tls regenerate`
- copy CA certificate to all clients
- check certificate expiration
- verify TLS mode matches (MUTUAL on both sides)
- check system time is synchronized

## performance

command execution latency: ~5-15ms
WebSocket message roundtrip: ~2-5ms
script validation: ~50-100ms per script
TLS handshake: ~10-20ms (one-time)

TLS encryption is computationally cheap (hardware-accelerated). message serialization is JSON-based (fast enough for commands).

## contributing

PRs welcome. please:
- follow existing code style
- add tests for new features
- update documentation
- test thoroughly

## license

GPLv3 - see LICENSE file for full terms

## links

- repository: https://github.com/objz/CommandBridge
- issues: https://github.com/objz/CommandBridge/issues
- discussions: https://github.com/objz/CommandBridge/discussions

---

yeah that's CommandBridge v3. completely rewritten from the ground up. better architecture, better code, same idea.
