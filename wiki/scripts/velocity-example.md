---
description: Let's create a simple network-wide alert.
---

# 💡 Example: /alert

**Goal:** Broadcast a message on multiple servers with `/alert`.

```yaml
name: alert
enabled: true
ignore-permission-check: false
hide-permission-warning: false
commands:
  - command: "say Attention: Maintenance in 5 minutes!"
    delay: 0
    target-client-ids:
      - "lobby"
      - "survival"
      - "minigame"
    target-executor: "console"
    wait-until-player-is-online: false
    check-if-executor-is-player: false
    check-if-executor-is-on-server: false
```

#### What happens?

* Registers `/alert` command.
* Runs `say ...` on 3 Paper servers.
* Executed immediately by each server console.

{% hint style="success" %}
This command is triggered from the **Velocity proxy** and forwarded to the **Paper servers**.
{% endhint %}

{% hint style="warning" %}
The permission `commandbridge.command.alert` **must be set on the Velocity proxy**, not on Paper!
{% endhint %}
