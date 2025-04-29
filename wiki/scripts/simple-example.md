# 💡 Example: /alert

Let's create a simple network-wide alert.

**Goal:** Broadcast a message on multiple servers with `/alert`.

```yaml
name: alert
enabled: true
ignore-permission-check: false
hide-permission-warning: false
commands:
  - command: "broadcast Attention: Maintenance in 5 minutes!"
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
* Runs `broadcast` on 3 Paper servers.
* Executed immediately by each server console.

{% hint style="info" %}
Anyone with `commandbridge.command.alert` permission can trigger it.
{% endhint %}
