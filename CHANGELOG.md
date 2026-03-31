this update adds a public developer API for third-party plugins, hardens auth security,
and fixes a bunch of stability issues across the board.

so whats new:

**developer API**

- new `api` module for third-party plugin integration, available on maven central
- typed message channels via `CommandBridgeAPI.channel(Class)` with send, request, broadcast, and listen
- delivery conditions on senders: `requirePlayer(UUID)` to gate on player presence, `whenOnline(UUID)` to queue until the player connects
- server lifecycle events via `onServerConnected` and `onServerDisconnected` (proxy only)
- connection state tracking via `onConnectionStateChanged` (all platforms)
- player locator service for resolving which server a player is on (proxy only)
- `CommandBridgeProvider.get()` and `CommandBridgeProvider.get(Class)` to obtain the API instance
- proxy-only methods return `Optional<Subscription>` instead of raw `Subscription`, returning `Optional.empty()` on backends
- full JDK-style JavaDocs across all API types

**security**

- hardened auth flow with constant-time HMAC comparison to prevent timing attacks
- fixed `AUTH_OK` race condition where messages could be sent before the client processed auth success
- added reconnect on failed server proof verification
- operator execution no longer grants wildcard permissions, only explicit ones

**fixes**

- fixed cooldown being applied before dispatch instead of after
- fixed MiniMessage tags in error messages not being escaped
- fixed script reload not being thread-safe
- fixed player join events firing for already-tracked players
- fixed polling not being reset on shutdown
- fixed auth check missing on config reload
- fixed `Log` varargs handling for single-argument messages
- fixed config name not being passed to `ConfigManager` in the velocity backend adapter
- fixed operator permission resolution being inconsistent across platform executors
- left-aligned all chat UI output and removed pixel-width centering
- various null guard and stability improvements across dispatch, registration, and player tracking

**internals**

- modernized codebase to java 21 idioms
- rebuilt test suite from scratch with 136 tests across core, velocity, and backends
- added CI test workflow and testing documentation

breaking changes:
- `RunAs` and `ConnectionState` moved to the `api` package, internal duplicates removed

latest commit: d7f43a1
