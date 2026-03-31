this update is the big architecture and API release. the entire codebase was modernized to java 21,
a public developer API module was added, security was hardened, and the test suite was rebuilt from scratch.

so whats new:

**developer API**

- added `api` module with a full public developer API for third-party plugins
- typed message channels with target builder pattern, send, request, broadcast, and listen
- delivery conditions on senders: `requirePlayer(UUID)` and `whenOnline(UUID)` for conditional and queued delivery
- `whenOnline` queue with player join and disconnect lifecycle wiring on the proxy
- server connect/disconnect event subscriptions via `onServerConnected` and `onServerDisconnected`
- connection state tracking via `onConnectionStateChanged`, available on all platforms
- player locator service for resolving which server a player is on (proxy only)
- `CommandBridgeProvider.get()` and `CommandBridgeProvider.get(Class)` for obtaining the API instance
- proxy-only methods return `Optional<Subscription>` instead of raw `Subscription`, returning `Optional.empty()` on backends
- comprehensive JDK-style JavaDocs across all 13 API source files with `@param`, `@return`, `@throws`, and cross-references
- each type's JavaDoc examples are scoped to its own responsibility, no tutorial-style chains leaking into other types
- maven central publishing configuration

**security**

- hardened auth flow with constant-time HMAC comparison to prevent timing attacks
- fixed `AUTH_OK` race condition where the server could send messages before the client processed auth success
- added reconnect on failed server proof verification
- removed wildcard permission grant in operator execution, only grants explicit permissions now
- fixed concurrency issues in `CommandBridgeProvider`, `RateLimiter`, `InNode`, and `OutNode`

**fixes**

- fixed thread-safe script reload and session replacement notifications
- fixed player join events firing for already-tracked players
- fixed `WsEndpoint` send completion callback not propagating properly
- fixed `ResponseAwaiter` to use UUID directly instead of string conversion
- fixed cooldown being applied before dispatch instead of after
- fixed MiniMessage tags in error messages not being escaped
- fixed `PlaceholderStage` being recreated on every dispatch instead of reused
- fixed polling not being reset on shutdown
- fixed `serverId` overwrite guard missing in registration handler
- fixed auth check missing on reload
- fixed `Log` varargs handling for single-argument messages
- fixed `UserCache` creating its own `ObjectMapper` instead of using `Envelope.MAPPER`
- fixed `FoliaExecutor` duplicating logic from `PlatformExecutor`
- fixed `RunAs.OPERATOR` resolution inconsistency across platform executors
- fixed null guards missing in merge processor, command dispatcher, registration request, and player tracker
- fixed config name not being passed to `ConfigManager` in the velocity backend adapter
- left-aligned all Minecraft chat UI output and removed pixel-width centering

**refactoring**

- modernized entire codebase to java 21 idioms: records, sealed interfaces, pattern matching, switch expressions, `var`, `List.of()`, `Map.of()`
- improved type safety across `OutNode`, `InNode`, and handler registration
- redesigned public API from wrapper types to target builder pattern on `MessageChannel`
- removed internal `RunAs` and `ConnectionState` duplicates in favor of the API types
- deduplicated `FoliaExecutor` to inherit from `PlatformExecutor`
- renamed `VelocityExecutor` to `LocalDispatcher` for clarity
- extracted `ScheduleHandle` abstraction for platform adapters
- added debug observability improvements throughout dispatch pipeline

**testing**

- deleted all existing AI-generated test files (22 files, ~233 methods)
- rebuilt test suite from scratch with 136 tests across core (84), velocity (40), and backends (12)
- added shared test fixtures: `TestEndpoint` and `ScriptFixtures`
- covers: `AuthService`, `RateLimiter`, `CooldownManager`, `PlayerTracker`, `SessionHub`, `ConfigManager`, `SecretLoader`, `ResponseAwaiter`, `ProblemSink`, `PlaceholderExtractor`, `EnumAdapter`, `DurationAdapter`, validation processors, pipeline stages, command dispatcher, platform detection, and more
- added `test.yml` CI workflow
- added `TESTING.md` documentation

breaking changes:
- the public API module is new and the channel/event interfaces may still evolve
- internal `RunAs` and `ConnectionState` enums were removed, use the API types instead
- `VelocityExecutor` was renamed to `LocalDispatcher`

latest commit: ad03680
