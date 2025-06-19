[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/commandbridge?logo=modrinth&label=downloads)](https://modrinth.com/plugin/commandbridge)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/projects/jdk/21/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.x--1.21.x-green.svg)](https://minecraft.net)
[![Paper](https://img.shields.io/badge/Server-Paper-blue.svg)](https://papermc.io/)
[![Velocity](https://img.shields.io/badge/Proxy-Velocity-purple.svg)](https://velocitypowered.com/)

---

CommandBridge connects Velocity and Paper servers using WebSockets. 

- Supports Minecraft 1.20.x to 1.21.x (Not for 1.8, not sorry).

- Java 21 only.

- Paper, Folia, Purpur, Bukkit, Spigot, Velocity, Waterfall.

Build with:

```bash
git clone https://github.com/objz/CommandBridge.git
cd CommandBridge
./gradlew shadowJar
````

Installation and configuration guide is [here](https://cb.objz.dev/docs/installation/).

---

Velocity runs the WebSocket server. Paper runs the client. The connection is authenticated using a shared secret (HMAC). Configs are generated on first run. Port and address settings must match. You copy the key manually, yes, on purpose.

Once connected, you’ll see something like:

```
[INFO] [CommandBridge]: Client authenticated successfully
[INFO] [CommandBridge]: Added connected client: lobby
```

---

Architecture is three modules:

* `core` – WebSocket implementations, util stuff
* `velocity` – plugin impl + server
* `paper` – plugin impl + client


Still to come:

* GUI for managing scripts (cause YAML is great, but not that great)
* Multi-Velocity support 
* Public API for extensions, message types, etc

[Website](https://cb.objz.dev)
[Discord](https://discord.gg/QPqBYb44ce)

Licensed under GPLv3. Don’t sell it, don’t strip the license, thanks!

