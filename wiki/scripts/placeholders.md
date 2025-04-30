---
description: CommandBridge supports dynamic placeholders in your command strings.
---

# 📃 Placeholders

They allow you to inject player-specific data like name, UUID, server, or even command arguments into scripted commands.

***

### 🔠 Internal Placeholders

These are built-in and always available without any external plugins:

| Placeholder                  | Paper | Velocity | Description                                    |
| ---------------------------- | :---: | :------: | ---------------------------------------------- |
| `%cb_player%`                |   ✅   |     ✅    | Player's name or username.                     |
| `%cb_uuid%`                  |   ✅   |     ✅    | Player's unique UUID.                          |
| `%cb_world%`                 |   ✅   |     ❌    | World name the player is in.                   |
| `%cb_server%`                |   ❌   |     ✅    | Server name the player is connected to.        |
| `%args%`                     |   ✅   |     ✅    | The full raw argument string from the command. |
| `%arg[0]%`, `%arg[1]%`, etc. |   ✅   |     ✅    | Specific argument by index (starting at 0).    |

{% hint style="info" %}
Placeholders like `%cb_world%` and `%cb_server%` are platform-specific and are ignored if unsupported.
{% endhint %}

***

### 💬 Argument Placeholders: `%args%` and `%arg[n]%`

These are especially useful when your script needs to **pass user input** to another command.

* `%args%` = all arguments passed after the command name
* `%arg[0]%` = first argument
* `%arg[1]%` = second argument
* ... and so on

***

### 🧪 Example: Custom message forwarder

Let’s say you want to allow players to broadcast a custom message via `/announce <message>`:

```yaml
name: announce
enabled: true
ignore-permission-check: false
hide-permission-warning: false
commands:
  - command: "broadcast [Notice] %args%"
    delay: 0
    target-client-ids:
      - "lobby"
      - "survival"
    target-executor: "console"
    wait-until-player-is-online: false
    check-if-executor-is-player: true
    check-if-executor-is-on-server: true
```

#### What happens?

* A player types `/announce Hello everyone!`
* `%args%` becomes `Hello everyone!`
* Velocity sends this message as:

> `[Notice] Hello everyone!` on the target servers

***

### 🔌 PlaceholderAPI Support

CommandBridge also supports full **PlaceholderAPI (PAPI)** integration:

* On **Paper**, just install **PlaceholderAPI**.
* On **Velocity**, install **PapiProxyBridge**.

{% hint style="danger" %}
PAPI placeholders only work when the command is run by a player — not from the console.
{% endhint %}

This enables **any PAPI placeholder** (like `%luckperms_prefix%`, `%vault_eco_balance%`, etc.).

```yaml
- command: "say Welcome %luckperms_prefix% %cb_player%!"
```

> Displays: `Welcome [Admin] Alex!`

{% hint style="info" %}
Using PAPI is **optional**, but highly recommended for dynamic placeholders beyond the built-ins.
{% endhint %}
