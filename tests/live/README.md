# Isolated live validation

**Preparation does not start servers. Run live tests only after the parent has committed fix20.** This harness does not run Gradle, manage Redis, or modify production sources.

## Prepare and check without servers

From the repository root:

```sh
python3 tests/live/harness.py prepare
python3 tests/live/harness.py self-test
node --check tests/live/clients.mjs
```

Requirements: Python 3, Node >=22, JDK21 at `/usr/lib/jvm/java-21-openjdk`, cached Gradle dependencies, and the authorized server assets described below. The public API probe compiles against `dist/build/libs/CommandBridge-3.3.7-all.jar`; `prepare` needs that file to exist but never builds it. `run` recompiles the probe against the parent's latest JAR.

Everything generated goes under ignored `build/live-validation`, including npm dependencies/cache/lockfile, downloaded JARs, compiled probe classes, configurations, worlds, logs, and results. Mineflayer is pinned to 4.39.0. Preparation creates a build-local npm lockfile and subsequent preparation uses `npm ci`. Preserve that lockfile to reproduce the same transitive dependencies; a clean preparation resolves them again.

Assets copied, without copying existing configs, plugins directories, or worlds:

- Paper 1.21.8 and its `cache`, `libraries`, and `versions` from `/home/consti/servers/serverback`.
- Only `CommandAPI-11.1.0-Paper.jar` from that server's plugins directory.
- Velocity `velocity-3.5.0-SNAPSHOT-601.jar` from `/home/consti/servers/dev/velocity`.
- Existing `eula=true` acceptance checked at `/home/consti/servers/dev/paper/eula.txt`.
- CommandAPI 11.2.0 Velocity downloaded from the supplied Modrinth URL, verified against the supplied SHA-512 before use.

## Live rounds: parent authorization required

The parent must first finish fix20, commit it, and build the runtime JAR. Only then:

```sh
export CB_LIVE_AFTER_COMMIT="$(git rev-parse HEAD)"
python3 tests/live/harness.py run --transport tls
python3 tests/live/harness.py run --transport plain
```

The harness checks that the authorized SHA is still HEAD and the production modules have no tracked differences from HEAD. This guard cannot decide whether the commit contains fix20; the parent owns that decision.

The parent starts and stops a **dedicated** Redis container bound to `127.0.0.1`, then supplies its published host port:

```sh
export CB_LIVE_REDIS_PORT=16379  # replace with the parent's actual dedicated host port
export CB_LIVE_REDIS_ISOLATED=1
python3 tests/live/harness.py run --transport redis
```

Redis must allow unauthenticated local test connections. The harness checks `PING`, does not flush Redis, and never starts or stops containers. CommandBridge uses shared channel names, so do not point this at an existing Redis service.

Each round allocates unused loopback ports, creates a new world and server directories, and launches one primary Velocity, one Paper backend, and one Velocity in `act-as-client` mode. The test-only probe lives on the two proxies. Three offline clients join through the proxies. A procfs check verifies that owned Java listening sockets are loopback-only.

The harness prints case names and the result path. `result.json` records the tested commit, runtime JAR SHA-256, transport, completed assertions, and overall outcome. Failure aborts the round and cleans up owned processes. Ctrl-C and SIGTERM also trigger cleanup. Individual waits, startup, and graceful shutdown have deadlines; process-group termination is the fallback. The parent-owned Redis container, wiki8080, and Wrangler8787 are never stopped.

## Parent setup/pairing/editor smoke

To test a new, unpaired Paper instance through the running local relay and wiki:

```sh
python3 tests/live/harness.py run --transport tls --fresh-paper --relay-smoke --hold 900
```

This mode skips the automated suite. It prepares a new primary with explicit loopback configuration and no saved setup, an unpaired Paper with no supplied CommandBridge config, and a preconfigured secondary proxy. It opens setup/editor sessions through the **actual Java relay clients**, then holds for the parent. It does not test the automatic default-config first-start banner: the production default binds all interfaces, which this harness deliberately avoids.

Find the new run directory under `build/live-validation`. `run.json` contains safe port information. `session-links.private.json` contains the capability links needed by the parent browser. Read these privately; do not paste them into chat, screenshots, or published evidence. Console output masks URLs, shared secrets, and TLS pins. Server-created logs remain private while running and are redacted after shutdown. Review artifacts before sharing.

During the bounded hold, the parent can append JSON lines to `<run>/commands.jsonl`:

```json
{"target":"primary","command":"cb setup confirm <private-code-from-browser>"}
{"target":"paper","command":"cbc pair <private-pairing-code>"}
{"target":"paper","command":"cbc pair confirm"}
{"target":"primary","command":"cb editor confirm <private-code-from-browser>"}
```

Use only the isolated run's console targets: `primary`, `paper`, `secondary`. Each line needs a trailing newline. This input file can contain private codes, so do not publish it. Pairing the Paper backend and editor save/confirm behavior require parent browser assertions; the automated result does not count them as completed. Velocity client mode currently exposes reconnect but no pairing subcommand.

To open relay sessions after the automated checks instead:

```sh
python3 tests/live/harness.py run --transport tls --relay-smoke --hold 900
```

## Evidence matrix

These are intended assertions, not claims that live execution has passed. Only a completed run's `result.json` is execution evidence.

| Case | Automated assertion | Limit |
| --- | --- | --- |
| Isolation | New directories/world; assigned localhost ports; procfs checks owned listeners | Port reservation is released immediately before startup; another process could race to bind, causing failure |
| WS/TLS | Both actual Java clients authenticate; generated keystore exists; a real TLS handshake succeeds | Self-signed TOFU only; no live STRICT, certificate rotation, or hostile MITM test |
| Plain WS | Same command/network suite with PLAIN mode | Only loopback; not a production deployment recommendation |
| Redis | Dedicated broker answers PING; Java clients authenticate and execute the same suite | No broker restart, ACL/password, cross-tenant, or encrypted transport test |
| Real clients | Three offline 1.21.8 clients spawn through both proxies | No Mojang authentication, signed chat, or graphical client rendering |
| Local script | Primary player command produces one Paper `say` marker | One STRING argument, CONSOLE execution, no placeholders/plugins |
| Backend registration | Paper-registered player command produces one secondary Velocity probe marker | Test-only marker command substitutes for a proxy `say` command |
| Secondary registration | Secondary Velocity player command produces one Paper `say` marker | One client-mode proxy, one backend |
| Permissions | Allowed primary player executes; denied primary player receives denial and produces no Paper marker | Probe supplies only script permission decisions; no LuckPerms, backend permission allow case, OPERATOR or PLAYER run-as |
| Public API | Primary API inventory contains both clients; built-in CommandPayload reaches Paper and secondary exactly once | No custom payload/request-response, player conditions, subscription cancellation, or queued replay |
| Built-ins | Expected output for help, info, script list, empty task list, client list/players/ping, config reload | Output smoke checks, not all options/error paths; no dump/migrate/debug or destructive commands |
| Reload | Edited script's new `_RELOADED` suffix reaches Paper once | No invalid YAML recovery or full plugin restart |
| Reconnect | Paper `cbc reconnect` reauthenticates and subsequent script executes | No network partition, long outage, or retry-exhaustion test |
| Setup/editor Java relay | Actual Java code opens both sessions and returns local page links | Opening is not an encrypted browser round trip, confirm, save, or pair proof |
| Fresh Paper pairing | New unpaired backend and parent-controlled consoles are available | Parent must prove plan/apply, pair confirm, saved config, authentication, and browser behavior |
| `lastcommands` | Not automated | No matching command/symbol found in the inspected Java source; do not claim coverage |

“Exactly once” means one unique marker in the target process's captured console over the bounded observation window. It is not a durable-delivery or load-test guarantee. The harness uses unique `say` markers and a test-only console marker command; it does not issue grant, ban, or operator commands.
