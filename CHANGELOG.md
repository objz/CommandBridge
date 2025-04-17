**[v2.2.0] - 2025-04-17**

- Replaced entire backend with Netty, providing improved performance and enhanced security.
- Clients now automatically attempt to reconnect to the server after shutdown, with a configurable timeout. Manual reconnection is possible afterward.
- Fixed a bug where the WebSocket server would not shut down properly.  
  This resolves [#11](https://github.com/72-S/CommandBridge/issues/11).

> Major version bump due to complete backend reimplementation.  
> In the next release, a WebUI for managing commands and a `/dump` command will be introduced.
