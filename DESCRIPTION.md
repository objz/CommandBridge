![CommandBridge](https://cb.objz.dev/media/bars/compact.svg)

CommandBridge is a cross-server command execution plugin for Minecraft networks running on Velocity. You define commands in YAML scripts on the proxy, and CB registers and dispatches them across all connected backend servers. No plugin messaging, no player-online requirements, no limitations.

A player runs `/lobby` on a backend, the proxy picks it up. An admin runs `/alert` on Velocity, every backend executes it. Commands go through instantly over WebSocket or Redis.

![separator](https://cb.objz.dev/media/bars/separator.svg)

<div align="center">
  <img src="https://cb.objz.dev/media/wordmark.svg" alt="CommandBridge" width="560">

</div>

<br>

<div align="center">

[![Documentation](https://img.shields.io/badge/Documentation-cb.objz.dev-7c3aed?style=for-the-badge)](https://cb.objz.dev)
[![GitHub](https://img.shields.io/badge/GitHub-Source_Code-181717?style=for-the-badge&logo=github)](https://github.com/objz/CommandBridge)
[![Discord](https://img.shields.io/badge/Discord-Join_Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/QPqBYb44ce)

</div>


![separator](https://cb.objz.dev/media/bars/separator.svg)



### How it works

Everything runs through scripts. You create a `.yml` file, define the command name, arguments, where it registers, and what happens when someone runs it. Drop it in the scripts folder on Velocity and you're done. CB reads, validates, and registers commands on whichever servers you specified. If something is wrong, it tells you exactly what and skips that script. The rest still loads fine.

Here is a quick example. This registers `/alert` on the proxy and broadcasts a message to two backends as console:

```yaml
version: 3
name: alert
description: Broadcast an alert to all servers

register:
  - id: "proxy-1"
    location: VELOCITY

defaults:
  run-as: CONSOLE
  execute:
    - id: "lobby"
      location: BACKEND
    - id: "survival"
      location: BACKEND

args:
  - name: message
    required: true
    type: GREEDY_STRING

commands:
  - command: "say [Alert]: ${message}"
```

That's a real script. You can do a lot more than that, but this shows how simple the basics are.

![separator](https://cb.objz.dev/media/bars/separator.svg)

### Platforms

One jar works everywhere. Install the same file on Velocity and all your backends.

![6 Platforms](https://cb.objz.dev/media/bars/platforms.svg)

### Transport

Pick one. WebSocket is the default and works out of the box. Redis is there if you need it.

![Transport](https://cb.objz.dev/media/bars/transport.svg)

WebSocket mode: Velocity hosts the server, backends connect to it. No external dependencies. TLS built in.

Redis mode: all instances connect to your existing Redis server. Useful if your servers are behind NAT or you already run Redis for other things.

Both modes support multi-proxy setups. One Velocity runs as the primary, any additional proxies connect in client mode.

### Security

Every connection is authenticated with HMAC-SHA256. Both sides prove they know the secret without ever sending it over the wire. On top of that you get TLS encryption with three modes to choose from.

![Security](https://cb.objz.dev/media/bars/security.svg)

TOFU is the default. Velocity generates a self-signed certificate on first startup, backends pin it automatically. Zero manual certificate management, encrypted from the start.

### Execution modes

Commands can run in three different contexts depending on what you need.

![Execution Modes](https://cb.objz.dev/media/bars/executors.svg)

`CONSOLE` runs with full permissions. `PLAYER` runs as the player who triggered it. `OPERATOR` gives temporary elevated permissions for that specific command and nothing else.

### Arguments

Full argument parsing with tab completion via CommandAPI. 22 types covering everything from basic strings to Minecraft-specific types like players, locations, items, and entities.

![22 Argument Types](https://cb.objz.dev/media/bars/args.svg)

Arguments become `${name}` placeholders in your command strings. PlaceholderAPI is supported too if you need external data like player stats or economy values.

![separator](https://cb.objz.dev/media/bars/separator.svg)

### Built for real networks

CommandBridge is designed for production. Commands can be rate limited per player with cooldowns from milliseconds to hours. If a player is offline when a command targets them, CB queues it and executes it when they come back, even across server restarts.

You can apply delays to individual commands, reload all scripts without restarting with `/cb reload`, and run multiple commands in a single script with different targets and settings each. Everything is managed from one place on the Velocity proxy.

Player presence is tracked across the entire network, including multi-proxy setups. CB knows where every player is at all times, so commands that depend on a player being on a specific server just work. No guessing, no race conditions.

### Requirements

![Requirements](https://cb.objz.dev/media/bars/requirements.svg)

[CommandAPI](https://modrinth.com/plugin/commandapi) is required on every server. [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) via [PapiProxyBridge](https://modrinth.com/plugin/papiproxybridge) and [PacketEvents](https://modrinth.com/plugin/packetevents) are optional.

![separator](https://cb.objz.dev/media/bars/separator.svg)

### Metrics

This plugin collects anonymous statistics via [bStats](https://bstats.org/). You can disable this in `plugins/bStats/config.yml`.

![bStats](https://bstats.org/signatures/velocity/CommandBridge.svg)
