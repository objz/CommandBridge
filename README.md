<div align="center">

# CommandBridge

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![GPL-3.0 License][license-shield]][license-url]

**Cross-server command execution for Minecraft networks**

A WebSocket-based command bridge enabling reliable cross-server communication and declarative command scripting for Velocity and Paper servers.

[Report Bug](https://github.com/objz/CommandBridge/issues) · [Request Feature](https://github.com/objz/CommandBridge/issues)

</div>

---

## About The Project

CommandBridge is a high-performance command bridging system designed for large-scale Minecraft networks running Velocity proxies and Paper backend servers. It solves the fundamental problem of cross-server command execution by establishing persistent WebSocket connections that operate independently of player presence.

### The Problem

Traditional Minecraft networks face several critical limitations:

- **Plugin messaging requires online players** - commands cannot execute when servers are empty
- **No cross-proxy coordination** - distributed proxy setups cannot share state or execute commands across boundaries
- **Manual command registration** - every command requires Java compilation and plugin deployment
- **Unreliable delivery** - plugin channels drop messages under load

### The Solution

CommandBridge provides:

- **Persistent WebSocket connections** using Undertow - operates 24/7 regardless of player presence
- **Declarative YAML scripting** - define commands in configuration files without writing code
- **Mutual TLS authentication** - certificate-based security with automatic key generation
- **Record-based message protocol** - type-safe communication using Java records and Jackson serialization
- **Platform abstraction layer** - single JAR deployment that auto-detects Velocity or Paper runtime

<p align="right">(<a href="#top">back to top</a>)</p>

## Built With

* [![Java][Java]][Java-url] - Java 21 with Records, Pattern Matching, Sealed Types
* [![Undertow][Undertow]][Undertow-url] - WebSocket server/client implementation
* [![Jackson][Jackson]][Jackson-url] - JSON serialization with datatype modules
* [![Velocity][Velocity]][Velocity-url] - Proxy server API
* [![Paper][Paper]][Paper-url] - Backend server API
* [![Gradle][Gradle]][Gradle-url] - Multi-project build system

<p align="right">(<a href="#top">back to top</a>)</p>

## Architecture Overview

### Module Structure

CommandBridge uses a multi-project Gradle build with strict module boundaries:

```
CommandBridge/
├── core/              [java-library] - Platform-agnostic shared code
│   ├── net/          - WebSocket communication layer (Undertow)
│   ├── scripting/    - YAML script engine (49 classes)
│   ├── security/     - TLS/authentication system
│   ├── config/       - Configuration management
│   └── logging/      - Centralized logging
│
├── velocity/          [java-library] - Proxy implementation
│   ├── net/          - WebSocket server (port 8080)
│   ├── cmd/          - Command registry and argument mapping
│   └── cli/          - Admin commands (/cb)
│
├── backends/          [java-library] - Backend platform abstraction
│   ├── net/          - WebSocket client
│   ├── platform/     - Platform detection and adapters
│   ├── bukkit/       - Bukkit implementation
│   ├── paper/        - Paper-specific features
│   └── folia/        - Folia scheduler support
│
└── dist/              [shadow-plugin] - Fat JAR assembly
    └── shadowJar     - Relocates dependencies, merges resources
```

### Communication Architecture

#### WebSocket Protocol

**Transport Layer:**
- Velocity runs Undertow WebSocket server on configurable port (default 8080)
- Paper servers establish client connections on startup
- Connections persist independently of player presence
- TLS 1.3 with mutual certificate verification

**Message Protocol:**

All messages use the `Envelope` record for type-safe serialization:

```java
public record Envelope(
    int v,              // Protocol version (currently 1)
    UUID id,            // Unique message ID for request/response correlation
    MessageType type,   // Enum: AUTH_REQUEST, INVOKED_COMMAND, etc.
    String from,        // Sender client-id
    String to,          // Target client-id  
    long ts,            // Unix timestamp (milliseconds)
    JsonNode payload    // Jackson JsonNode containing type-specific data
)
```

**Message Types:**

```java
public enum MessageType {
    AUTH_REQUEST,              // Initial authentication handshake
    AUTH_OK,                   // Authentication successful
    AUTH_FAIL,                 // Authentication failed
    REGISTER_COMMANDS,         // Server → Client: register these commands
    REGISTER_COMMANDS_RESULT,  // Client → Server: registration result
    INVOKED_COMMAND,          // Command execution request
    PING,                     // Keepalive ping
    PONG                      // Keepalive pong
}
```

#### Request/Response Pattern

CommandBridge implements asynchronous request/response using `ResponseAwaiter`:

```java
// Velocity side - sending a command
CompletableFuture<Envelope> future = awaiter.expect(envelope.id());
outNode.send(targetClientId, envelope);
Envelope response = future.get(5, TimeUnit.SECONDS);
```

#### Authentication Flow

1. **Startup (Velocity)**:
   ```java
   TlsResolver.resolveServer(dataDir, config.security())
   // Generates CA cert, server cert, private keys
   // Stores in: dataDir/tls/ca.crt, server.p12
   ```

2. **Connection (Paper)**:
   ```java
   TlsResolver.resolveClient(dataDir, config.security())
   // Loads CA cert for server verification
   // Generates client cert signed by CA
   ```

3. **Handshake**:
   ```
   Paper → Velocity: AUTH_REQUEST {clientId, nonce, signature}
   Velocity validates signature using HMAC-SHA256
   Velocity → Paper: AUTH_OK {assigned serverId}
   ```

<p align="right">(<a href="#top">back to top</a>)</p>

## Scripting System

### Overview

The scripting system transforms YAML declarations into registered commands through a multi-stage pipeline:

```
YAML File → Parser → Binder → Validators → Script Record → Command Registration
```

### Script Model

Scripts are represented as immutable Java records with annotation-driven validation:

```java
@ModelRoot("script")
public record Script(
    @Min(1) @Max(2) @Required int version,
    @Required @Pattern(regex = "^[a-z][a-z0-9-]{2,32}$") String name,
    @Default("true") boolean enabled,
    String description,
    List<String> aliases,
    @Required Permissions permissions,
    @Required List<IdMapping> register,
    @Required Defaults defaults,
    @Required List<ArgMapping> args,
    @Required List<CmdMapping> commands
)
```

### YAML Structure

```yaml
version: 2

# Command metadata
name: "economy"
description: "Cross-server economy management"
enabled: true
aliases: ["eco", "money"]

# Permission configuration
permissions:
  enabled: true
  silent: false

# Registration targets
register:
  - id: "proxy-main"
    location: VELOCITY

# Default execution parameters
defaults:
  run-as: CONSOLE
  execute:
    - id: "survival"
      location: BACKEND
    - id: "creative"
      location: BACKEND
  server:
    target-required: true
    schedule-online: false
    timeout: 10m
    frequency: 30s
  delay: 0s
  cooldown: 0s

# Argument definitions
args:
  - name: player
    type: PLAYERS
    required: true
    suggestions: ["@a", "@p", "@r"]
  
  - name: amount
    type: INTEGER
    required: true

# Command templates
commands:
  - command: "eco give ${player} ${amount}"
  - command: "eco set ${player} ${amount}"
    run-as: OPERATOR
    delay: 2s
```

### Argument Types

Arguments are strongly typed with platform validation enforced at load time:

```java
public enum ArgType {
    // Universal types (Velocity + Backend)
    @Platform({ VELOCITY, BACKEND }) STRING,
    @Platform({ VELOCITY, BACKEND }) INTEGER,
    @Platform({ VELOCITY, BACKEND }) BOOLEAN,
    @Platform({ VELOCITY, BACKEND }) DOUBLE,
    @Platform({ VELOCITY, BACKEND }) TEXT,
    
    // Backend-only types
    @Platform({ BACKEND }) RANGE,
    @Platform({ BACKEND }) PLAYERS,
    @Platform({ BACKEND }) ENTITIES,
    @Platform({ BACKEND }) ENTITY_TYPE,
    @Platform({ BACKEND }) WORLD,
    @Platform({ BACKEND }) LOCATION,
    @Platform({ BACKEND }) LOCATION_2D,
    @Platform({ BACKEND }) ANGLE,
    @Platform({ BACKEND }) ROTATION,
    @Platform({ BACKEND }) ITEM_STACK,
    @Platform({ BACKEND }) ENCHANTMENT,
    @Platform({ BACKEND }) POTION_EFFECT,
    @Platform({ BACKEND }) SOUND,
    @Platform({ BACKEND }) BIOME,
    @Platform({ BACKEND }) TIME,
    
    // Proxy-only types
    @Platform({ VELOCITY }) SERVER
}
```

**Type Validation Example:**

```java
// This will FAIL validation (WORLD is backend-only)
register:
  - id: "proxy-main"
    location: VELOCITY
args:
  - name: worldName
    type: WORLD  // ❌ PlatformProcessor rejects this

// This is valid
register:
  - id: "survival"
    location: BACKEND
args:
  - name: worldName
    type: WORLD  // ✅ Valid on backend
```

### Placeholder System

Templates use `${argumentName}` syntax with compile-time validation:

```java
public class PlaceholderExtractor {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]*)}");
    
    public static List<String> extract(String command) {
        List<String> result = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(command);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (name != null && !name.isBlank()) {
                result.add(name.trim());
            }
        }
        return List.copyOf(result);
    }
}
```

**Validation:**

```java
// Script loading - ResolvableProcessor validates all placeholders
List<String> usedNames = PlaceholderExtractor.extractAll(commandStrings);
Map<String, ArgMapping> argsByName = args.stream()
    .collect(Collectors.toMap(ArgMapping::name, arg -> arg));

for (String name : usedNames) {
    if (!argsByName.containsKey(name)) {
        problems.add("Unresolved placeholder: ${" + name + "}");
    }
}
```

### Validation Pipeline

Scripts pass through multiple validation processors:

1. **DefaultProcessor** - Applies `@Default` annotations, warns on redundant overrides
2. **PlatformProcessor** - Validates argument types match registration location
3. **ResolvableProcessor** - Ensures all `${placeholders}` resolve to defined arguments
4. **MinProcessor** - Validates `@Min` constraints (delays, cooldowns, etc.)
5. **MaxProcessor** - Validates `@Max` constraints
6. **RequiredProcessor** - Ensures `@Required` fields are present
7. **PatternProcessor** - Validates string patterns (e.g., command names)

### Execution Contexts

```java
public enum RunAs {
    CONSOLE,   // Execute as backend console (elevated privileges)
    PLAYER,    // Execute as the player who triggered the command
    OPERATOR   // Temporarily grant OP status for command duration
}
```

**Implementation on Paper:**

```java
switch (runAs) {
    case CONSOLE -> server.dispatchCommand(server.getConsoleSender(), command);
    case PLAYER -> {
        Player p = server.getPlayer(executorUUID);
        if (p != null) server.dispatchCommand(p, command);
    }
    case OPERATOR -> {
        Player p = server.getPlayer(executorUUID);
        if (p != null) {
            boolean wasOp = p.isOp();
            try {
                p.setOp(true);
                server.dispatchCommand(p, command);
            } finally {
                p.setOp(wasOp);
            }
        }
    }
}
```

### Deferred Execution

Commands can be queued for offline players:

```yaml
server:
  target-required: true
  schedule-online: true
  timeout: 24h
  frequency: 1m
```

**Implementation:**

```java
// When player is offline
if (targetRequired && !isPlayerOnline(targetUUID)) {
    if (scheduleOnline) {
        queue.add(new DeferredCommand(
            command, targetUUID, System.currentTimeMillis(), timeout
        ));
    }
}

// Periodic check (every frequency interval)
scheduler.scheduleAtFixedRate(() -> {
    queue.removeIf(deferred -> {
        if (System.currentTimeMillis() - deferred.queuedAt > deferred.timeout) {
            return true; // Expired
        }
        if (isPlayerOnline(deferred.targetUUID)) {
            execute(deferred.command);
            return true; // Executed
        }
        return false; // Keep in queue
    });
}, frequency, frequency, TimeUnit.SECONDS);
```

<p align="right">(<a href="#top">back to top</a>)</p>

## Getting Started

### Prerequisites

- **Java 21** - Requires JDK 21 or higher
- **Velocity** - Velocity proxy server (or Waterfall)
- **Paper** - Paper 1.20.x - 1.21.x (Folia, Purpur, Spigot, Bukkit also supported)

### Installation

1. **Download the JAR**
   ```sh
   wget https://github.com/objz/CommandBridge/releases/latest/download/CommandBridge-all.jar
   ```

2. **Deploy to servers**
   ```sh
   # Same JAR works on both platforms (auto-detection)
   cp CommandBridge-all.jar velocity/plugins/
   cp CommandBridge-all.jar paper/plugins/
   ```

3. **Start Velocity first**
   ```sh
   # Generates configs and TLS certificates
   cd velocity && ./start.sh
   ```

4. **Configure authentication**
   
   Velocity generates TLS certificates in `plugins/CommandBridge/tls/`:
   ```
   tls/
   ├── ca.crt      # Certificate Authority
   ├── ca.key      # CA private key
   ├── server.p12  # Server certificate (PKCS12)
   └── server.crt  # Server certificate (PEM)
   ```

5. **Copy CA certificate to Paper**
   ```sh
   cp velocity/plugins/CommandBridge/tls/ca.crt \
      paper/plugins/CommandBridge/tls/
   ```

6. **Configure network settings**
   
   **Velocity** (`plugins/CommandBridge/config.yml`):
   ```yaml
   config-version: 3
   server-id: "proxy-main"
   bind-host: "0.0.0.0"
   bind-port: 8080
   
   security:
     tls:
       enabled: true
       mode: MUTUAL
   ```
   
   **Paper** (`plugins/CommandBridge/config.yml`):
   ```yaml
   config-version: 3
   client-id: "survival"
   remote-host: "proxy.example.com"
   remote-port: 8080
   
   security:
     tls:
       enabled: true
       mode: MUTUAL
   ```

7. **Start Paper servers**
   ```sh
   cd paper && ./start.sh
   ```

8. **Verify connection**
   ```
   [Velocity] [CommandBridge] Client authenticated: survival (TLS)
   [Paper] [CommandBridge] Connected to proxy at proxy.example.com:8080
   ```

<p align="right">(<a href="#top">back to top</a>)</p>

## Usage

### Creating Scripts

Scripts are placed in `plugins/CommandBridge/scripts/` as `.yml` files.

#### Example 1: Economy Command

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
  - id: "proxy-main"
    location: VELOCITY

defaults:
  run-as: PLAYER
  execute:
    - id: "survival"
      location: BACKEND
  cooldown: 3s

args:
  - name: target
    type: PLAYERS
    required: false
    suggestions: ["@p"]

commands:
  - command: "balance ${target}"
```

#### Example 2: Cross-Server Teleport

```yaml
version: 2
name: "tpserver"
description: "Teleport to another server"
enabled: true

register:
  - id: "proxy-main"
    location: VELOCITY

defaults:
  run-as: CONSOLE
  execute:
    - id: "proxy-main"
      location: VELOCITY
  delay: 1s
  cooldown: 5s

args:
  - name: player
    type: PLAYERS
    required: true
  
  - name: targetServer
    type: SERVER
    required: true
    suggestions: ["survival", "creative", "lobby"]

commands:
  - command: "send ${player} ${targetServer}"
```

#### Example 3: Deferred Punishment

```yaml
version: 2
name: "kicklater"
description: "Kick player on next login"
enabled: true

register:
  - id: "proxy-main"
    location: VELOCITY

defaults:
  run-as: CONSOLE
  execute:
    - id: "survival"
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

### Admin Commands

**Velocity:**
```
/cb reload              - Reload configs and scripts
/cb list                - List connected clients
/cb dump                - Generate debug dump
/cb tls info            - Show TLS certificate info
/cb tls regenerate      - Regenerate certificates
/cb help                - Show help
```

**Paper:**
```
/cbc reconnect          - Reconnect to proxy
```

<p align="right">(<a href="#top">back to top</a>)</p>

## Building from Source

### Build Steps

```sh
# Clone repository
git clone https://github.com/objz/CommandBridge.git
cd CommandBridge

# Checkout v3 branch
git checkout v3

# Build with Gradle
./gradlew shadowJar

# Output: dist/build/libs/CommandBridge-all.jar
```

### Module Dependencies

```kotlin
// settings.gradle.kts
include("core")
include("velocity")
include("backends")
include("backends:bukkit")
include("backends:paper")
include("backends:folia")
include("dist")
```

### Dependency Relocation

The shadow plugin relocates dependencies to avoid conflicts:

```kotlin
relocate("com.fasterxml.jackson", "dev.objz.libs.jackson")
relocate("io.undertow", "dev.objz.libs.undertow")
relocate("org.xnio", "dev.objz.libs.xnio")
relocate("org.spongepowered.configurate", "dev.objz.libs.configurate")
```

<p align="right">(<a href="#top">back to top</a>)</p>

## Roadmap

- [x] Core WebSocket communication layer
- [x] TLS mutual authentication
- [x] YAML scripting engine with 25+ argument types
- [x] Placeholder validation and resolution
- [x] Deferred execution for offline players
- [x] Command cooldowns and delays
- [ ] Advanced command parsing on Velocity
    - [ ] Full argument validation on proxy
    - [ ] Tab completion forwarding
    - [ ] Syntax error reporting
- [ ] Developer API
    - [ ] Public API for third-party plugins
    - [ ] Programmatic command execution
    - [ ] Custom message type registration
    - [ ] Execution pipeline hooks
- [ ] Web-based configuration panel
    - [ ] Script editor with syntax highlighting
    - [ ] Real-time connection monitoring
    - [ ] Certificate management UI
    - [ ] Command execution history
- [ ] Advanced features
    - [ ] Command macros and chaining
    - [ ] Conditional execution predicates
    - [ ] Database integration
    - [ ] Metrics and analytics

See the [open issues](https://github.com/objz/CommandBridge/issues) for a full list of proposed features.

<p align="right">(<a href="#top">back to top</a>)</p>

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Guidelines

- Follow existing code style (Java 21 idioms, immutable records)
- Add tests for new features
- Update documentation
- Ensure builds pass: `./gradlew build`

<p align="right">(<a href="#top">back to top</a>)</p>

## License

Distributed under the GPL-3.0 License. See `LICENSE` for more information.

<p align="right">(<a href="#top">back to top</a>)</p>

## Contact

Project Link: [https://github.com/objz/CommandBridge](https://github.com/objz/CommandBridge)

<p align="right">(<a href="#top">back to top</a>)</p>

## Acknowledgments

* [Undertow](https://undertow.io/) - High-performance WebSocket implementation
* [Jackson](https://github.com/FasterXML/jackson) - JSON serialization
* [CommandAPI](https://github.com/JorelAli/CommandAPI) - Advanced argument types
* [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) - YAML parsing
* [Velocity](https://papermc.io/software/velocity) - Modern Minecraft proxy
* [Paper](https://papermc.io/software/paper) - High-performance Minecraft server

<p align="right">(<a href="#top">back to top</a>)</p>

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

[Java]: https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[Java-url]: https://openjdk.org/projects/jdk/21/
[Undertow]: https://img.shields.io/badge/Undertow-2.3-red?style=for-the-badge
[Undertow-url]: https://undertow.io/
[Jackson]: https://img.shields.io/badge/Jackson-2.18-blue?style=for-the-badge
[Jackson-url]: https://github.com/FasterXML/jackson
[Velocity]: https://img.shields.io/badge/Velocity-3.x-00ADD8?style=for-the-badge
[Velocity-url]: https://papermc.io/software/velocity
[Paper]: https://img.shields.io/badge/Paper-1.20--1.21-00ADD8?style=for-the-badge
[Paper-url]: https://papermc.io/software/paper
[Gradle]: https://img.shields.io/badge/Gradle-8.x-02303A?style=for-the-badge&logo=gradle
[Gradle-url]: https://gradle.org/
