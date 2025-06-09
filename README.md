[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/commandbridge?logo=modrinth&label=downloads)](https://modrinth.com/plugin/commandbridge)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.x--1.21.x-green.svg)](https://minecraft.net)
[![Paper](https://img.shields.io/badge/Server-Paper-blue.svg)](https://papermc.io/)
[![Velocity](https://img.shields.io/badge/Proxy-Velocity-purple.svg)](https://velocitypowered.com/)

[Website](https://cb.objz.dev) / [Discord](https://discord.gg/QPqBYb44ce) / [Modrinth](https://modrinth.com/plugin/commandbridge) / [Issues](https://github.com/objz/CommandBridge/issues)

![CommandBridge](https://cdn.modrinth.com/data/cached_images/ddfd1c06df1ab99ee271859ee78e5e4d6d34ac49_0.webp)

CommandBridge seamlessly connects **Velocity** and **Paper** servers using WebSockets, enabling cross-server commands.

## Features

- Real-time WebSocket communication
- Bidirectional command execution
- Flexible scripting with placeholder support
- Secure, zero-player dependency
- High-performance, minimal latency

## Supported Platforms

- **Paper**: 1.20.x - 1.21.x (Primary)
- **Velocity**: Latest recommended
- **Java**: Version 21 (required)

## Installation

For detailed instructions, visit the [website](https://cb.objz.dev).

1. Place the JAR in the `plugins` folder of both Paper and Velocity.
2. Restart servers to generate configs.
3. Copy `secret.key` from Velocity to Paper's `config.yml`.
4. Choose and set an unused port in configs.
5. Set Velocity's IP in Paper's `remote` and Velocity's `host` fields.
6. Set matching `server-id` (Velocity) and `client-id` (Paper).
7. Restart Velocity, then Paper servers.

Check Velocity logs for successful connection:

```plaintext
[INFO] [CommandBridge]: Client authenticated successfully
[INFO] [CommandBridge]: Added connected client: lobby
```

## Building

- Requires **Java 21**, **Git**

```bash
git clone https://github.com/objz/CommandBridge.git
cd CommandBridge
./gradlew shadowJar
```

## Roadmap

- GUI Script Manager
- Multi-Velocity support
- Plugin API

## Help & Documentation

- [Issues](https://github.com/objz/CommandBridge/issues)
- [Discord](https://discord.gg/QPqBYb44ce)
- [Docs](https://cb.objz.dev)
