<div align="center">

# CommandBridge

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![GPL-3.0 License][license-shield]][license-url]

**cross-server command execution for Minecraft networks**

define commands in YAML on Velocity, dispatch them across all connected backends over WebSocket or Redis.
no plugin messaging, no player-online requirements, no limitations.

[![Documentation](https://img.shields.io/badge/Documentation-cb.objz.dev-7c3aed?style=for-the-badge)](https://cb.objz.dev)
[![Discord](https://img.shields.io/badge/Discord-Join_Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/QPqBYb44ce)

[Report Bug](https://github.com/objz/CommandBridge/issues) · [Request Feature](https://github.com/objz/CommandBridge/issues)

</div>

---

## about

CommandBridge is a proxy-to-backend command bridge for Minecraft networks running on Velocity. you write commands as YAML scripts on the proxy, CB validates them, registers them on whichever servers you specified, and handles dispatch. a player runs `/lobby` on a backend, the proxy picks it up. an admin runs `/alert` on Velocity, every backend executes it. commands go through instantly.

the whole reason this exists: plugin messages need a connected player to work. if a server is empty, you can't send commands to it. CB uses persistent WebSocket connections (or Redis) to fix that. commands work regardless of player presence.

one jar works everywhere. install the same file on Velocity and all your backends. it auto-detects the platform.

for the full writeup with visuals check the [documentation](https://cb.objz.dev), the [Modrinth page](https://modrinth.com/plugin/commandbridge), [Hangar](https://hangar.papermc.io/objz/CommandBridge/), or [SpigotMC](https://www.spigotmc.org/resources/commandbridge.133217/).

## features

| | |
|---|---|
| **scripting** | YAML command definitions with aliases, permissions, cooldowns, 22 argument types |
| **transport** | WebSocket (default, TLS built in) or Redis |
| **execution** | `CONSOLE`, `PLAYER`, `OPERATOR` modes |
| **security** | HMAC-SHA256 mutual auth, TLS 1.3 (PLAIN / TOFU / STRICT) |
| **offline queue** | queues commands for offline players, survives restarts |
| **multi-proxy** | primary + client mode for additional proxies |
| **player tracking** | network-wide, real-time |
| **admin CLI** | `/cb help`, `info`, `scripts`, `reload`, `list`, `ping`, `debug`, `dump`, `migrate` |
| **developer API** | message channels, event subscriptions, player locator, broadcast |
| **optional integrations** | PlaceholderAPI, PacketEvents |

---

## how it's built

everything gets shaded into a single fat jar. there are a few modules but you only ever deal with one file.

`core` is the shared library that both sides depend on. networking (WebSocket via Undertow, Redis via Jedis), the YAML scripting engine with validation, security (TLS, auth), config management, and logging. anything that isn't platform-specific lives here.

`velocity` is the proxy-side plugin. command registration, the dispatch pipeline, the admin CLI, and the UI output all live here. this is the "server" side of the bridge.

`backends` is the shared client library for all backend platforms. it handles connecting to the proxy, routing incoming messages, and platform abstraction. underneath it there are platform-specific adapters for Bukkit, Paper, Folia, and Velocity-as-client. the adapters are kept thin on purpose, they only deal with thread dispatch and platform-specific API calls. shared logic stays in the parent module.

`api` is the public developer API for other plugins. channels, events, platform info. more on that below.

`dist` handles shadow JAR packaging and publishing to Modrinth and Hangar.

---

## building from source

requires JDK 21 (Temurin recommended).

```sh
git clone https://github.com/objz/CommandBridge.git
cd CommandBridge
git checkout v3
./gradlew shadowJar
# output: dist/build/libs/CommandBridge-<version>-all.jar
```

other useful commands:

```sh
# full build (compile + shadow JAR)
./gradlew build

# run checkstyle (this is what CI runs)
./gradlew check

# checkstyle for a specific module
./gradlew :core:checkstyleMain
./gradlew :velocity:checkstyleMain

# clean
./gradlew clean
```

---

## developer API

CB has a public API module for other plugins to interact with the bridge network. source is at [`api/`](https://github.com/objz/CommandBridge/tree/v3/api/src/main/java/dev/objz/commandbridge/api), full documentation will be on [cb.objz.dev](https://cb.objz.dev).

```java
CommandBridgeAPI api = CommandBridgeProvider.get();

// send a command as console to a backend
CommandChannel commands = api.channel(Channels.COMMAND);
commands.console(Platform.BACKEND.target("survival-1"), "say hello");

// listen for incoming messages
Subscription sub = commands.listen((ctx, payload) -> { /* ... */ });

// server events
api.onServerConnected(server -> { /* ... */ });
api.onServerDisconnected(server -> { /* ... */ });

// find a player across the network
api.playerLocator().ifPresent(locator -> locator.locate(playerUuid));

// broadcast to all servers
api.broadcast(commands, CommandPayload.console("say maintenance in 5 minutes"));
```

---

## dependencies

### shaded (bundled in the jar)

| Library | What it does |
|---|---|
| [Undertow](https://undertow.io/) | WebSocket server |
| [Jackson](https://github.com/FasterXML/jackson) | JSON serialization |
| [Jedis](https://github.com/redis/jedis) | Redis client |
| [OkHttp](https://github.com/square/okhttp) | HTTP client on backends |
| [Configurate](https://github.com/SpongePowered/Configurate) | YAML config loading |
| [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) | YAML parsing |
| [Adventure MiniMessage](https://docs.advntr.dev/minimessage/) | chat formatting |
| [BouncyCastle](https://www.bouncycastle.org/) | TLS and crypto |
| [bStats](https://bstats.org/) | anonymous usage metrics |

### runtime (you install these)

| Library | Required | What it does |
|---|---|---|
| [CommandAPI](https://modrinth.com/plugin/commandapi) | yes | argument parsing and tab completion on all servers |
| [PacketEvents](https://modrinth.com/plugin/packetevents) | no | enables extra argument types (like `PLAYERS` and `TIME`) on Velocity |
| [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) | no | external placeholder data (stats, economy, etc.) |
| [PapiProxyBridge](https://modrinth.com/plugin/papiproxybridge) | no | bridges PlaceholderAPI to the proxy side |

---

## roadmap

### current: v3.3.0

- [x] **backend platform support**
  - [x] Bukkit support
  - [x] Paper support
  - [x] Folia support
  - [x] Velocity-as-client support (for multi-proxy)
  - [x] automatic platform detection

- [x] **WebSocket transport**
  - [x] WebSocket server on Velocity
  - [x] WebSocket client on backends
  - [x] session and client management
  - [x] rate limiting
  - [x] automatic reconnection on disconnect (configurable)

- [x] **Redis transport**
  - [x] Redis config and connection management
  - [x] Redis implementation in core (SendOperation)
  - [x] channel mapping on Velocity
  - [x] sending/receiving on all platforms

- [x] **security**
  - [x] mutual authentication (HMAC-SHA256)
  - [x] TLS SPKI key pinning
  - [x] TLS 1.3 encryption
  - [x] multiple TLS modes (PLAIN, TOFU, STRICT)

- [ ] **YAML scripting**
  - [x] YAML command definitions with strict validation
  - [x] aliases, permissions, and cooldowns
  - [x] 22 argument types (strings, numbers, players, locations, items, entities, etc.)
  - [x] custom argument types via PacketEvents
  - [x] script migration tooling (`/cb migrate`)
  - [x] schema versioning (currently `version: 4`)
  - [ ] duplicate script name detection
  - [ ] more custom argument types

- [x] **cross-server command execution**
  - [x] remote and local command dispatch
  - [x] automatic command registration sync to backends
  - [x] placeholder and PlaceholderAPI support
  - [x] console, player, and operator execution modes
  - [x] offline command queue (persists across restarts)
  - [x] network-wide player tracking and UUID resolution

- [x] **admin CLI (`/cb`)**
  - [x] `/cb help`, `/cb info`, `/cb scripts`
  - [x] `/cb reload`, `/cb list`, `/cb ping`
  - [x] `/cb debug`, `/cb dump`, `/cb migrate`
  - [x] dual-mode output (chat + console)
  - [x] dump export to file or paste service
  - [x] automatic update checker
  - [x] bStats integration

- [ ] **developer API**
  - [x] public API module (`api/`)
  - [x] typed message channels with send, request, and listen
  - [x] command channel with console/player/operator dispatch
  - [x] server connect/disconnect event subscriptions
  - [x] connection state tracking
  - [x] player locator service
  - [x] broadcast to all connected servers
  - [ ] more channel types and lifecycle hooks

- [ ] **web interface**
  - [ ] ...

- [ ] **admin GUI**
  - [ ] ...

see the [open issues](https://github.com/objz/CommandBridge/issues) for bugs and feature requests.

---

## requirements

| | Version |
|---|---|
| Java | 21+ |
| Velocity | 3.4 - 3.5 |
| Minecraft | 1.20 - 1.21.x |

---

## contributing

contributions are welcome. fork it, branch it, PR it. make sure `./gradlew check` and `./gradlew build` both pass before opening a PR.

---

## license

GPL-3.0. see [LICENSE](LICENSE).

---

[contributors-shield]: https://img.shields.io/github/contributors/objz/CommandBridge.svg?style=for-the-badge
[contributors-url]: https://github.com/objz/CommandBridge/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/objz/CommandBridge.svg?style=for-the-badge
[forks-url]: https://github.com/objz/CommandBridge/network/members
[stars-shield]: https://img.shields.io/github/stars/objz/CommandBridge.svg?style=for-the-badge
[stars-url]: https://github.com/objz/CommandBridge/stargazers
[issues-shield]: https://img.shields.io/github/issues/objz/CommandBridge.svg?style=for-the-badge
[issues-url]: https://github.com/objz/CommandBridge/issues
[license-shield]: https://img.shields.io/github/license/objz/CommandBridge.svg?style=for-the-badge
[license-url]: https://github.com/objz/CommandBridge/blob/v3/LICENSE

