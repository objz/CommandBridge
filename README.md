<div align="center">

# CommandBridge

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![GPL-3.0 License][license-shield]][license-url]

**Cross-server command execution for Minecraft networks**

A WebSocket bridge for Velocity and Paper servers to run commands anywhere, anytime.

[Report Bug](https://github.com/objz/CommandBridge/issues) · [Request Feature](https://github.com/objz/CommandBridge/issues)

</div>

---

## About The Project

CommandBridge connects your Velocity proxy to your backend servers using WebSockets. It lets you run commands across your network even if no players are online.

### The Problem

Plugin messages need a player to work. If a server is empty, you can't send commands to it. CommandBridge uses persistent connections to fix this.

<p align="right">(<a href="#top">back to top</a>)</p>

## Built With

[![Java][Java]][Java-url]
[![Undertow][Undertow]][Undertow-url]
[![Jackson][Jackson]][Jackson-url]
[![Velocity][Velocity]][Velocity-url]
[![Paper][Paper]][Paper-url]
[![Gradle][Gradle]][Gradle-url]

<p align="right">(<a href="#top">back to top</a>)</p>

## Building from Source

```sh
git clone https://github.com/objz/CommandBridge.git
cd CommandBridge

git checkout v3

./gradlew shadowJar

# Output: dist/build/libs/CommandBridge-all.jar
```
<p align="right">(<a href="#top">back to top</a>)</p>

## Roadmap

### Current Status: Beta (v3.0)

- [x] **Backend Platform Support**
  - [x] Bukkit support
  - [x] Paper support
  - [x] Folia support
  - [x] Velocity(as client) support
  - [x] Automatic platform detection

- [x] **WebSocket Communication**
  - [x] WebSocket server on Velocity
  - [x] WebSocket client on backends
  - [x] Session and client management
  - [x] Rate limiting
  - [x] Automatic reconnection on disconnect(configurable)

- [x] **Security**
  - [x] Mutual authentication (HMAC-SHA256)
  - [x] TLS SPKI key pinning
  - [x] TLS 1.3 encryption
  - [x] Multiple TLS modes (PLAIN, TOFU, STRICT)

- [x] **YAML Scripting**
  - [x] YAML command definitions
  - [x] Strict validation
  - [x] Aliases, permissions, and cooldowns
  - [x] Argument types (strings, numbers, players, locations, items, etc.)
  - [x] Custom argument types via PacketEvents
  - [ ] Duplicate script name detection
  - [ ] More custom argument types

- [x] **Cross-Server Command Execution**
  - [x] Remote and local command execution
  - [x] Automatic command registration sync to backends
  - [x] Placeholder and PlaceholderAPI support
  - [x] Console, player, and operator execution modes
  - [x] Offline command queue
  - [ ] Database backend for task queue (currently JSON file) | maybe?

- [x] **Admin Commands (`/cb`)**
  - [x] `/cb help`
  - [x] `/cb info`
  - [x] `/cb scripts`
  - [x] `/cb reload`
  - [x] `/cb list`
  - [x] `/cb ping`
  - [x] `/cb debug`
    - [ ] `/cb dump`
  - [x] Dual-mode output (chat and console)
  - [ ] Automatic update checker
  - [ ] Dump export to file or paste service
  - [ ] bstats

- [ ] **Web Interface**
  - [ ] ...

- [ ] **Admin GUI**
  - [ ] ...

- [ ] **Developer API**
  - [ ] Public API module
  - [ ] Custom pipeline stages
  - [ ] Command lifecycle event hooks
  - [ ] Custom argument type registration
  - [ ] Custom message type registration
  - [ ] Documentation and examples

See the [open issues](https://github.com/objz/CommandBridge/issues) for features and bugs.

<p align="right">(<a href="#top">back to top</a>)</p>

## Contributing

Contributions are welcome.

- Update documentation
- Ensure builds pass: `./gradlew shadowJar`

<p align="right">(<a href="#top">back to top</a>)</p>

## License

Distributed under the GPL-3.0 License. See `LICENSE`.

<p align="right">(<a href="#top">back to top</a>)</p>

## Acknowledgments

* [Undertow](https://undertow.io/) 
* [Jackson](https://github.com/FasterXML/jackson) 
* [CommandAPI](https://github.com/JorelAli/CommandAPI) 
* [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml) 
* [Velocity](https://papermc.io/software/velocity) 
* [Paper](https://papermc.io/software/paper) 

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
