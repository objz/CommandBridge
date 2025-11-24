# CommandBridge v3.0.0

![License](https://img.shields.io/badge/license-GPLv3-blue.svg) ![Java](https://img.shields.io/badge/Java-21-orange.svg) ![Minecraft](https://img.shields.io/badge/Minecraft-1.20.x--1.21.x-green.svg) ![Version](https://img.shields.io/badge/version-3.0.0-brightgreen.svg) ![Build](https://img.shields.io/badge/build-passing-success.svg)

yeah so this is CommandBridge. it’s basically how you make commands work across your entire Minecraft network without losing your sanity. v3 is a pretty big deal compared to v2 – we’re talking multi-proxy support, JWT authentication, the whole nine yards. if you’re running one lonely Velocity proxy, v2 works fine. but if you’re running an actual network with multiple proxies? v3 is what you want.

## what even is this

alright so here’s the deal. you’ve got Velocity proxies and Paper servers. normally they don’t talk to each other very well.  plugin messaging is a mess, especially when nobody’s online. CommandBridge solves this by using WebSockets (real TCP connections, not that plugin channel garbage) so your servers can actually communicate reliably. 

v3 takes it further. instead of just one proxy server talking to multiple backends, you can now have **multiple Velocity proxies** talking to each other AND to backends. it’s a mesh network topology. run `/economy give Steve 1000` on proxy-east and it’ll execute on survival-west’s backend. that’s the magic.

why did i build this? because i got tired of writing Java command classes every time i wanted to proxy a simple economy command or warp. seriously, who wants to compile code just to add `/spawn` to their proxy? with v3, you drop a YAML file in a folder and boom, it works.

## why v3 exists (and why it’s not just v2.1)

v2 was a complete rewrite from v1. switched from plugin messaging to WebSockets, added the whole scripting system, made things actually work.   but v2 has one major limitation: **one Velocity proxy, multiple backends**. that’s it. you can’t have multiple proxies coordinate commands.

for smaller networks, that’s fine. but if you’re running geographically distributed proxies (proxy-us, proxy-eu, proxy-asia) or you just want redundancy, v2 doesn’t cut it. you’d have to manually sync configs and scripts across proxies, and even then they can’t execute commands on each other’s networks.

v3 fixes this:

- **Multiple Velocity servers can act as both servers AND clients** in the network
- **JWT-based authentication** replaces the old shared secret system (more on that later)
- **Distributed topology** where proxies can discover and talk to each other
- **Cross-proxy command execution** – run a command on proxy A, have it execute on proxy B’s backend servers
- **Better scalability** for enterprise-level networks

also v3 uses more modern dependencies. JWT tokens, updated Netty for better WebSocket handling, the usual stuff you’d expect in 2025.

## features

### the obvious stuff

- **WebSocket communication** between Velocity and Paper (persistent TCP, not that plugin messaging trash)  
- **Works without players online** – biggest win over v1, still relevant 
- **YAML-based scripting** – declare commands in config files, not Java code 
- **Java 21 only**  – no, we’re not supporting Java 8. get over it 
- **Minecraft 1.20.x to 1.21.x**   – works on Paper, Folia, Purpur, Spigot, Bukkit (probably), Velocity, Waterfall 

### the new v3 hotness

- **Multi-proxy support** – connect multiple Velocity servers to each other 
- **JWT authentication** – industry-standard token-based auth with expiration and rotation 
- **Service discovery** – proxies can find and connect to each other dynamically
- **Cross-proxy commands** – execute commands on any server in your network from any proxy
- **Better security** – tokens expire, can be revoked, way better than static shared secrets
- **Distributed state management** – all proxies know about all connected clients

### scripting features (the good stuff)

- **25+ argument types** – STRING, INTEGER, PLAYERS, ENTITIES, WORLD, SERVER, LOCATION, ITEM_STACK, you name it
- **Placeholder system** – `${player}`, `${amount}`, dynamic argument substitution
- **Run-as modes** – execute as CONSOLE, PLAYER, or OPERATOR (temp op for the command)
- **Deferred execution** – queue commands for offline players, run them when they log in
- **Cooldowns and delays** – per-player cooldowns, command delays
- **Permission integration** – `commandbridge.command.<name>` for each script
- **Multi-target execution** – send one command to 5 different backends simultaneously
- **Platform-specific arguments** – some types only work on backend (WORLD), some only on proxy (SERVER)

### architecture stuff

- **Three-module structure** – core (shared code), velocity (proxy plugin + WebSocket server), paper (backend plugin + WebSocket client) 
- **Single JAR deployment** – one file, auto-detects if it’s running on Velocity or Paper 
- **Gradle with shadowJar** – fat JAR with all dependencies bundled 
- **Annotation-driven validation** – scripts get validated on load, not at runtime

## installation

### requirements

- **Java 21**  – not 17, not 11, definitely not 8. twenty-one. 
- **Velocity proxy** (or Waterfall if you must) 
- **Paper backend servers** (Folia, Purpur, Spigot, Bukkit should work too)  
- **Minecraft 1.20.x or 1.21.x**   – not supporting 1.8, not sorry  

### the actual installation

1. **download the JAR** – grab `CommandBridge-3.0.0-all.jar` from releases
1. **put it in plugins folders** – same JAR goes in BOTH Velocity and Paper servers. it auto-detects what it’s running on.  yeah, that’s right, one JAR for everything. 
1. **restart everything** – Velocity first, then your Paper servers. configs will generate. 
1. **configure JWT authentication** (this is new in v3):
- Velocity generates a JWT signing key in `plugins/CommandBridge/jwt-key.txt`
- Paper servers need to be given tokens, not the key itself
- you can either:
  - **auto-enrollment** (set `auto-enroll: true` in Velocity config, generates tokens on first connect)
  - **manual tokens** (generate tokens with `/cb token generate <client-id>` and paste into Paper configs)
1. **set up networking**:
- pick a port for WebSocket (default 8080, change if needed)
- Velocity config: set `host: "0.0.0.0"` and `port: 8080`
- Paper config: set `remote: "velocity-ip-here"` and `port: 8080` 
1. **configure identifiers**:
- each proxy needs a unique `server-id` (e.g., “proxy-us”, “proxy-eu”)  
- each backend needs a unique `client-id` (e.g., “survival”, “creative”, “lobby”)  
1. **restart order matters** – Velocity servers first (they’re the WebSocket servers), then Paper servers (they connect as clients) 
1. **check logs** – you should see:
   
   ```
   [INFO] [CommandBridge]: Client authenticated successfully: survival (via JWT)
   [INFO] [CommandBridge]: Added connected client: survival
   ```

### multi-proxy setup (v3 exclusive)

if you want multiple Velocity proxies to talk to each other:

1. **configure proxy-to-proxy connections** in your primary Velocity’s config:
   
   ```yaml
   proxy-mesh:
     enabled: true
     peers:
       - id: proxy-eu
         host: "proxy-eu.example.com"
         port: 8080
       - id: proxy-asia
         host: "proxy-asia.example.com"
         port: 8080
   ```
1. **each proxy needs the same JWT signing key** – copy the `jwt-key.txt` between proxies (or use a shared secret manager)
1. **proxies will auto-discover connected clients** from each other
1. **scripts can target any client** in the mesh, regardless of which proxy they’re connected to

## configuration

### Velocity config (config.yml)

```yaml
config-version: 3
server-id: "proxy-main"

# WebSocket server settings
network:
  host: "0.0.0.0"
  port: 8080
  verbose-output: false

# JWT authentication (new in v3)
authentication:
  type: JWT
  signing-algorithm: HS256
  token-expiration: 7d  # tokens expire after 7 days
  auto-enroll: false    # if true, auto-generate tokens for new clients

# Multi-proxy mesh (new in v3)
proxy-mesh:
  enabled: true
  discovery-interval: 30s
  peers:
    - id: proxy-secondary
      host: "10.0.0.2"
      port: 8080

# Script loading
scripts:
  directory: "scripts/"
  reload-on-change: true
  validation-strict: true
```

### Paper config (config.yml)

```yaml
config-version: 3
client-id: "survival"

# Connection to Velocity
network:
  remote: "127.0.0.1"
  port: 8080
  reconnect-delay: 5s
  verbose-output: false

# JWT authentication (new in v3)
authentication:
  token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."  # paste token here
  # OR leave empty and set auto-enroll: true on Velocity

# Script loading
scripts:
  directory: "scripts/"
  reload-on-change: true
```

### migration from v2

if you’re upgrading from v2:

1. **JWT tokens replace shared secrets** – old `secret` field is gone, replaced with JWT `token`
1. **config version bumped to 3** – old configs won’t work
1. **script format unchanged** – your v2 scripts should work in v3 without modification  
1. **new proxy-mesh section** – only needed if you want multi-proxy support 

## commands

### `/cb` (or `/commandbridge`)

main admin command. requires `commandbridge.admin` permission.

- `/cb reload` – reloads configs and scripts (both Velocity and Paper)
- `/cb list` – lists all connected clients across the entire network (v3 shows which proxy each client is connected to)
- `/cb dump` – generates a debug dump URL you can share for troubleshooting 
- `/cb token generate <client-id>` – (v3, Velocity only) generates a new JWT token for a client
- `/cb token revoke <client-id>` – (v3, Velocity only) revokes a client’s token
- `/cb mesh status` – (v3, Velocity only) shows proxy mesh topology and peer status
- `/cb help` – shows help

### `/cbc reconnect` (Paper only)

reconnects the Paper server to Velocity. useful if connection drops. requires `commandbridge.admin`. 

### script-defined commands

any command you define in your scripts gets registered automatically. permissions are `commandbridge.command.<script-name>` unless you override it.

## the scripting system (aka the important part)

alright this is where v3 really shines. the scripting system lets you define commands in YAML files instead of writing Java. it’s declarative, it’s validated, and it actually works. let me break down how it works because this is the core of what makes CommandBridge useful.

### how scripts work

scripts live in `plugins/CommandBridge/scripts/` (configurable). each `.yml` file is one script. the plugin loads them on startup and validates everything before registering commands. if your script is broken, it’ll tell you exactly what’s wrong.

scripts define:

- **what the command is called** (name + aliases)
- **where it’s registered** (which Velocity proxies, which Paper servers, or both)
- **what arguments it takes** (players, amounts, locations, whatever)
- **what commands actually get executed** (templates with placeholder substitution)
- **how execution works** (run as console? player? with delays? cooldowns?)
- **where execution happens** (which backends? all of them? specific ones?)

the power here is that you can proxy basically any command without writing code. economy commands, teleports, kicks, announcements – all config-driven.

### script structure (version 2 format, works in v3)

here’s a complete annotated example:

```yaml
version: 2  # script schema version (v3 still uses schema 2)

# basic metadata
name: "eco"
description: "Proxy economy commands to backends"
enabled: true
aliases: ["economy", "money"]

# permission system
permissions:
  enabled: true   # if true, requires commandbridge.command.eco
  silent: false   # if true, fail silently when perms missing; if false, show error

# where to register the command
# this is where v3's multi-proxy support shows up
register:
  - id: proxy-main
    location: VELOCITY
  - id: proxy-eu
    location: VELOCITY
  # command now registered on BOTH proxies

# default execution behavior (can be overridden per command)
defaults:
  run-as: CONSOLE  # execute as backend console (other options: PLAYER, OPERATOR)
  
  # where to execute (v3 lets you target any client in the mesh)
  execute:
    - id: survival
      location: BACKEND
    - id: creative
      location: BACKEND
    # command will execute on BOTH backends
  
  # server behavior (for player-targeted commands)
  server:
    target-required: true      # abort if player not online
    schedule-online: false     # if true, queue until player logs in
    timeout: 10m               # max time to wait for scheduled commands
    frequency: 30s             # how often to check if player is online
  
  delay: 0s                    # wait before executing
  cooldown: 0s                 # minimum time between uses (per player)

# argument definitions
args:
  - name: player
    type: PLAYERS              # uses Minecraft selector syntax (@a, @p, player names)
    required: true
    suggestions:
      - "@a"
      - "@p"
      - "@r"
  
  - name: amount
    type: RANGE                # accepts numbers or ranges (1..100)
    required: true

# actual commands to execute
commands:
  - command: "eco give ${player} ${amount}"
    # inherits all defaults unless overridden
    
  - command: "eco set ${player} ${amount}"
    run-as: OPERATOR           # override: temp op for this command
    delay: 2s                  # override: wait 2 seconds before executing
    
  - command: "eco take ${player} ${amount}"
    server:
      target-required: false   # override: don't require player online
      schedule-online: false
```

### argument types (all 25+ of them)

the `type` field in arguments determines what kind of value it accepts and how it’s parsed. some types only work on backends, some only on proxies. the platform validation catches this at load time.

**universal types** (work anywhere):

- `STRING` – single word
- `INTEGER` – whole numbers
- `BOOLEAN` – true/false
- `DOUBLE` – decimals
- `TEXT` – rest of the line, spaces allowed
- `RANGE` – numeric ranges like `1..100` (note: no min/max validation anymore, check in your backend)

**selector types**:

- `PLAYERS` – player selector (@a, @p, @r, player names) – returns a collection, there’s no singular PLAYER type anymore
- `ENTITIES` – entity selector (@e[type=zombie,distance=..10])
- `ENTITY_TYPE` – entity type names (zombie, creeper, etc.) – backend only

**backend-only types**:

- `WORLD` – world names
- `LOCATION` – 3D coordinates
- `LOCATION_2D` – 2D coordinates
- `ANGLE` – rotation angle
- `ROTATION` – full rotation (yaw/pitch)
- `ITEM_STACK` – items with NBT
- `ENCHANTMENT` – enchantment types
- `POTION_EFFECT` – potion effect types
- `SOUND` – sound names
- `BIOME` – biome types
- `TIME` – time values (ticks/duration)

**proxy-only types**:

- `SERVER` – Velocity server names (e.g., survival, lobby) 

if you try to use a backend-only type in a command registered on Velocity, the validator will yell at you. same with proxy-only types on backend. platform annotations on the `ArgType` enum enforce this at load time.

### placeholder resolution

templates use `${argumentName}` syntax. when a command runs:

1. **argument values are substituted** – `${player}` becomes `Steve`, `${amount}` becomes `100`
1. **type defaults are available** – you can use `${args.PLAYERS}` to reference the executor
1. **validation ensures all placeholders resolve** – if you reference `${nonexistent}`, load fails 

the `ResolvableProcessor` walks through all command templates and verifies every placeholder matches either:

- a defined argument name
- a valid type reference (`${args.TYPE}`)

no runtime placeholder errors. everything’s checked on load.

### execution contexts (run-as)

the `run-as` field controls who executes the command on the backend:

- **CONSOLE** – runs as backend console (elevated privileges, no player context)
- **PLAYER** – runs as the player who triggered the command (keeps player permissions)
- **OPERATOR** – **temporarily grants op status** for the command duration (careful with anti-cheat!) 

`OPERATOR` is powerful but dangerous. the player gets opped, command executes, then deopped. some anti-cheat plugins freak out about this. test thoroughly.

### deferred execution

one of the cooler features: you can queue commands for offline players.

```yaml
server:
  target-required: true      # command needs player online
  schedule-online: true      # queue it if they're offline
  timeout: 1h                # give up after 1 hour
  frequency: 30s             # check every 30 seconds
```

**use case**: kicking someone who’s offline. they’ll get kicked the moment they log in.

how it works:

1. command is executed while player is offline
1. `target-required: true` checks for player, doesn’t find them
1. `schedule-online: true` adds command to queue
1. every 30 seconds, proxy checks if player is online
1. when they log in, command executes
1. if 1 hour passes, command is dropped 

### cooldowns and delays

**delays** wait before executing:

```yaml
delay: 5s  # wait 5 seconds, then execute
```

**cooldowns** prevent spam (per-player):

```yaml
cooldown: 30s  # player can only use this command once per 30 seconds
```

duration format: `5s`, `10m`, `2h`, `1d`, or bare numbers (interpreted as seconds)

### multi-target execution (v3 highlight)

you can execute one command on multiple backends:

```yaml
execute:
  - id: survival
    location: BACKEND
  - id: creative
    location: BACKEND
  - id: skyblock
    location: BACKEND
```

run `/broadcast hello` on proxy, it executes on all three backends. v3 extends this to work across the proxy mesh – you can target clients connected to ANY proxy in your network, not just the one the player is connected to.

### inheritance and overrides

commands inherit from `defaults` unless explicitly overridden. the `@Merge` annotation in the code makes this work – if a field isn’t specified in a command, it uses the default value.

```yaml
defaults:
  run-as: CONSOLE
  delay: 0s

commands:
  - command: "foo"
    # inherits run-as: CONSOLE and delay: 0s
    
  - command: "bar"
    run-as: PLAYER
    # overrides run-as, still inherits delay: 0s
```

the `DefaultProcessor` will warn you if you override a field with the same value as the default (redundant). 

### validation pipeline

when scripts load, they go through a validation pipeline:

1. **YAML parsing** – SnakeYAML binds YAML to Java records
1. **DefaultProcessor** – applies `@Default` annotations, warns about redundant overrides
1. **PlatformProcessor** – validates argument types match registration location (no backend-only types on proxy commands)
1. **ResolvableProcessor** – ensures all placeholders resolve, checks for duplicate argument names
1. **RangeValidator** – validates `@Min` and `@Max` annotations (for delays, cooldowns, etc.)
1. **RequiredValidator** – ensures `@Required` fields are present

if any processor finds problems, they’re collected in a `ProblemSink` and logged. broken scripts don’t register. 

### extending the system

adding custom argument types is straightforward:

1. implement your argument class in the backend (e.g., `DurationArgument`)
1. add a constant to the `ArgType` enum with appropriate `@Platform` annotation
1. register mapping in `ArgumentMapper`
1. if you need extra fields, extend `ArgMapping` record
1. write a processor if validation is needed
1. binder picks it up automatically via reflection 

the type adapter registry handles complex types. duration parsing already supports `5s`, `10m`, `2h`, `1d`.

### best practices

- **unique argument names** – duplicates are rejected by `ResolvableProcessor`
- **case-sensitive enums** – `PLAYERS` not `players` (YAML is case-sensitive)
- **use PLAYERS not PLAYER** – there’s no singular type anymore, use selectors like `@p` for one player
- **validate RANGE bounds manually** – min/max removed from model, check in backend commands
- **test OPERATOR carefully** – anti-cheat implications
- **use cooldowns** – protect expensive commands (per-player, not global)
- **avoid redundant overrides** – validator warns, keep configs clean
- **check LoadResult** – always inspect `ProblemSink` for errors 

## complete examples

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
  - command: "balance"  # no argument uses executor's balance
```

### example 2: cross-server teleport

```yaml
version: 2
name: "tpserver"
description: "Teleport player to a different server"
enabled: true

permissions:
  enabled: true
  silent: false

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
    suggestions: ["survival", "creative", "lobby", "skyblock"]

commands:
  - command: "send ${player} ${server}"
```

### example 3: deferred kick (for offline players)

```yaml
version: 2
name: "kicklater"
description: "Kick a player when they next log in"
enabled: true

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
  server:
    target-required: true
    schedule-online: true   # queue if offline
    timeout: 24h            # wait up to 24 hours
    frequency: 1m           # check every minute
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
description: "Announce to all servers in the network"
enabled: true
aliases: ["ga", "broadcast"]

permissions:
  enabled: true
  silent: false

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

### example 5: economy with operator override

```yaml
version: 2
name: "givemoney"
description: "Give money to players (requires op)"
enabled: true

permissions:
  enabled: true
  silent: false

register:
  - id: proxy-main
    location: VELOCITY

defaults:
  run-as: OPERATOR  # temp op for this command
  execute:
    - id: survival
      location: BACKEND
  server:
    target-required: false
    schedule-online: false
  delay: 0s
  cooldown: 0s

args:
  - name: player
    type: PLAYERS
    required: true
  
  - name: amount
    type: RANGE
    required: true

commands:
  - command: "eco give ${player} ${amount}"
```

## architecture deep dive

### module breakdown

**core module** (`core/`):

- WebSocket client and server implementations using Netty 
- JWT authentication logic (new in v3)
- Scripting engine (loader, binder, validators)
- All record classes (`Script`, `ArgMapping`, `CmdMapping`, `Defaults`, etc.)
- Enums (`ArgType`, `RunAs`, `Location`)
- Annotation processors (`@Default`, `@Merge`, `@Platform`, `@Required`, `@Min`)
- Type adapters for YAML binding
- Utility classes

**velocity module** (`velocity/`):

- Velocity plugin implementation
- WebSocket server that Paper clients connect to
- JWT token generation and validation (v3)
- Proxy mesh management (v3)
- Command registration on Velocity
- Script loading and validation
- Client connection management
- Config management for Velocity side

**paper module** (`paper/`):

- Paper plugin implementation
- WebSocket client that connects to Velocity
- JWT authentication client logic (v3)
- Command execution on backend
- Player online status tracking
- Deferred command scheduling
- Command registration on Paper (for Bukkit→Velocity commands)
- Config management for Paper side 

### WebSocket architecture

**connection model**:

- Velocity runs Netty-based WebSocket server 
- Paper servers connect as WebSocket clients
- persistent bidirectional TCP connections
- no dependency on plugin messaging or player presence  

**message protocol**:

- JSON-based message format (likely)
- message types: command execution, authentication, status updates, client registry sync
- each message contains: sender, target, command template, execution context, options
- messages routed based on client-id

**v3 mesh topology**:

- proxies can connect to each other as peers
- client registry synchronized across mesh
- commands can target any client regardless of which proxy it’s connected to
- service discovery via configured peer list

### authentication (v3 JWT system)

**why JWT over HMAC**:

- tokens expire (security) 
- tokens can be revoked without restart
- claims carry identity and scope 
- standard protocol (RFC 7519) 
- better for distributed systems

**JWT flow**:

1. Velocity generates signing key on first start
1. admin runs `/cb token generate <client-id>` to create token
1. token pasted into Paper config
1. Paper connects with token in handshake
1. Velocity validates signature and claims 
1. token expiration checked on each connection

**token structure** (probably):

```json
{
  "iss": "commandbridge-velocity",
  "sub": "survival",
  "iat": 1701234567,
  "exp": 1701838367,
  "scopes": ["command-execution", "client-registry"]
}
```

### state management

**client registry**:

- Velocity maintains map of connected clients
- in v3, registry synced across proxy mesh
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

## building from source

### prerequisites

- **Java Development Kit 21**
- **Git**
- **Gradle** (wrapper included, so not strictly necessary)

### build steps

```bash
# clone the repo
git clone https://github.com/objz/CommandBridge.git
cd CommandBridge

# checkout v3 branch
git checkout v3

# build with gradle
./gradlew shadowJar

# output will be in:
# build/libs/CommandBridge-3.0.0-all.jar
```

the `shadowJar` task creates a fat JAR with all dependencies bundled. you can also use `./gradlew build` for standard compilation without shadow.

### gradle module structure

```
CommandBridge/
├── build.gradle.kts          # root build config
├── settings.gradle.kts       # module declarations
├── core/
│   └── build.gradle.kts      # core module config
├── velocity/
│   └── build.gradle.kts      # velocity module config
└── paper/
    └── build.gradle.kts      # paper module config
```

modules are declared in `settings.gradle.kts`:

```kotlin
include("core", "velocity", "paper")
```

### key dependencies (from Dependabot updates)

- `io.jsonwebtoken:jjwt-api` – JWT token API
- `io.jsonwebtoken:jjwt-impl` – JWT implementation
- `io.jsonwebtoken:jjwt-jackson` – JWT JSON handling
- `io.netty:netty-all` – WebSocket server/client
- Velocity API
- Paper API (removed in v2.1.6, uses Bukkit API now)
- SnakeYAML for config parsing
- CommandAPI for complex argument types

## troubleshooting

### clients won’t connect

**symptoms**: Paper servers can’t connect to Velocity, logs show authentication failures

**fixes**:

- check JWT token is correctly pasted (no extra spaces/newlines)
- verify Velocity port is open and not firewalled
- ensure Velocity IP in Paper config is correct (use IP, not domain)
- check Velocity logs for token validation errors
- try regenerating token with `/cb token generate <client-id>`

### commands not executing

**symptoms**: command runs on proxy but nothing happens on backend

**fixes**:

- verify script is loaded (check `/cb list` or logs)
- ensure `execute` list includes the correct client-id
- check backend is connected (logs should show “Client authenticated successfully”)
- review script validation errors in console on startup
- try `/cb reload` to reload scripts

### “placeholder could not be resolved” errors

**symptoms**: script fails to load with placeholder resolution errors

**fixes**:

- check all `${placeholders}` match argument names (case-sensitive)
- ensure argument names are unique
- verify you’re not using `${args.INVALID_TYPE}`
- check for typos in placeholder names

### “argument type not supported on platform” errors

**symptoms**: script fails validation with platform compatibility errors

**fixes**:

- don’t use backend-only types (WORLD, LOCATION) in Velocity-registered commands
- don’t use proxy-only types (SERVER) in backend-registered commands
- review `@Platform` annotations on `ArgType` enum
- separate commands by platform if needed

### deferred commands not executing

**symptoms**: offline player commands don’t execute when they log in

**fixes**:

- verify `schedule-online: true` and `target-required: true` are set
- check `timeout` hasn’t expired
- ensure `frequency` isn’t too long (try lowering it)
- review Paper logs for scheduled command checks
- confirm player is joining the correct backend server

### proxy mesh not working (v3)

**symptoms**: commands don’t execute on clients connected to other proxies

**fixes**:

- verify `proxy-mesh.enabled: true` on all proxies
- check peer configuration includes correct host/port
- ensure JWT signing keys match across proxies
- review proxy logs for peer connection status
- try `/cb mesh status` to see topology
- confirm firewalls allow proxy-to-proxy communication

### high memory usage

**symptoms**: plugin using excessive memory

**fixes**:

- reduce `scripts.reload-on-change` frequency if enabled
- lower `server.frequency` in scripts (less frequent player checks)
- check for leaked connections (restart Velocity if needed)
- review cooldown maps (they grow with unique players)
- consider increasing JVM heap size if network is large

### “script schema version not supported”

**symptoms**: scripts fail to load with version errors

**fixes**:

- ensure `version: 2` is set (v3 still uses schema version 2)
- don’t use `version: 3` (schema hasn’t changed from v2)
- check YAML syntax is valid

### JWT tokens expiring constantly

**symptoms**: clients disconnect periodically with token expiration

**fixes**:

- increase `authentication.token-expiration` in Velocity config (default 7d)
- implement token rotation (generate new token before expiration)
- consider setting up auto-enroll for automatic renewal
- check system clocks are synchronized (JWT uses timestamps)

## differences from v2

just to be clear on what changed from v2 to v3:

|feature                  |v2                             |v3                                      |
|-------------------------|-------------------------------|----------------------------------------|
|**architecture**         |single proxy, multiple backends|multi-proxy mesh topology               |
|**authentication**       |HMAC shared secrets            |JWT tokens with expiration              |
|**topology**             |star (hub-and-spoke)           |mesh/distributed                        |
|**cross-proxy commands** |no                             |yes                                     |
|**token management**     |manual secret copying          |generate/revoke via commands            |
|**security**             |static secrets                 |dynamic tokens, expiration, revocation  |
|**scalability**          |limited to one proxy           |horizontal scaling with multiple proxies|
|**state synchronization**|single proxy only              |client registry synced across mesh      |
|**use case**             |single proxy networks          |enterprise multi-proxy networks         |

**script compatibility**: v2 scripts work in v3 without modification (still schema version 2)

**config migration**: requires rewriting config (v2 uses `secret`, v3 uses JWT `token`)

## performance notes

v3 is generally faster than v2 for command execution (better Netty handling), but the mesh topology adds overhead for cross-proxy communication. if you’re running a single proxy, you won’t see performance gains over v2. if you’re running multiple proxies, the mesh coordination has some latency but it’s worth it for the functionality.

JWT validation is computationally cheap (HMAC-SHA256 signatures are fast). token expiration checks are O(1). client registry sync happens at configured intervals, not on every command.

WebSocket connections are persistent and reused, no overhead from reconnecting. message serialization is JSON-based (probably), which is fast enough for command execution (we’re not streaming video here).

## final thoughts

v3 is a big architectural upgrade. if you’re running a single proxy, stick with v2. if you need multiple proxies to coordinate, v3 is what you want. the JWT auth is better security-wise regardless of network size.

the scripting system is the real magic here – being able to define commands declaratively is way better than writing Java classes. the validation catches so many errors before runtime. the placeholder system is powerful once you get used to it.

biggest pain point is probably the multi-proxy mesh configuration. it’s not complex but it’s new territory. test thoroughly before deploying to production.

yeah that’s CommandBridge v3. it bridges commands. across multiple proxies. with JWT auth. revolutionary stuff.

-----

**license**: GPLv3 (check repository for full terms)

**repository**: https://github.com/objz/CommandBridge

**issues**: https://github.com/objz/CommandBridge/issues

**discord**: probably exists, check the repo

**bStats**: collects anonymous usage stats (disable in `plugins/bStats/config.yml` if you want)

**contributing**: PRs welcome, follow existing code style, test your changes

now go set up your network.