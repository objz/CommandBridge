# 💡 Example: /hub

**Goal:** Teleport the player to hub server and play music there.

```yaml
name: hub
enabled: true
ignore-permission-check: false
hide-permission-warning: false
commands:
  - command: "server hub"
    delay: 0
    target-executor: "player"
    check-if-executor-is-player: true
    check-if-executor-is-on-server: true

  - command: "music"
    delay: 2
    target-client-ids:
      - "hub"
    target-executor: "player"
    wait-until-player-is-online: true
    check-if-executor-is-player: true
    check-if-executor-is-on-server: true
```

#### Step-by-step:

1. Player runs `/hub` on any server.
2. First action teleports them to hub.
3. Second action waits until player is on hub server, then after two seconds plays music.

{% hint style="info" %}
No need to define `/hub` on Paper servers separately if using Velocity.
{% endhint %}
