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

- [x] **Universal Backend Support:** Works on Bukkit, Paper, and Folia
- [x] **Secure Communication:** TLS 1.3, mutual auth, and auto secrets
- [x] **Declarative Scripting:** V2 YAML schema with strict validation
- [x] **Cross-Server Pipeline:** Local and remote command dispatching
- [x] **Resilient Connectivity:** Auto-reconnection and failsafe WsClient
- [x] **Persistent Queues:** Database storage for offline commands
- [x] **Multi-Proxy Support:** Proxy chaining support
- [x] **PlaceholderAPI Support:** Resolve PAPI on backends and velocity using papiproxybridge
- [ ] **Custom Command Types:** String with infinite args, Time Argument
- [ ] **Diagnostics Dump:** Debug reports via `/cb dump`
- [ ] **Web Interface:** Dashboard for monitoring clients and logs
- [ ] **Admin GUI:** In-game inventory menu
- [ ] **Developer API:** API for custom hooks and packets

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
