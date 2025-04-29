---
description: Let's allow plugins to send players to the lobby server.
---

# 💡 Example: /lobby

**Goal:** Forward a proxy `/server lobby` command from a Paper server.

```yaml
name: lobby
enabled: true
ignore-permission-check: false
hide-permission-warning: false
commands:
  - command: "server lobby"
    delay: 0
    target-executor: "player"
    check-if-executor-is-player: true
    check-if-executor-is-on-server: true
```

#### What happens?

* Registers `/lobby` command.
* Runs `server lobby` on the player.
* Player is moved to the `lobby` server via proxy.

{% hint style="success" %}
This command is triggered from the **Paper server** and automatically forwarded to **Velocity**.
{% endhint %}

{% hint style="warning" %}
The permission `commandbridge.command.lobby` **must be set on the Paper server**, not on Velocity!
{% endhint %}
