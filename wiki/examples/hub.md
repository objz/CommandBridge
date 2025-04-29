# 🏠 Example: /hub with Follow-up

Send player to hub and play music.

### 📋 Script: `hub.yml`

```yaml
name: hub
enabled: true
commands:
  - command: "send {player} hub"
    target-client-ids: ["velocity"]
    target-executor: "console"
  - command: "music play"
    target-client-ids: ["lobby"]
    wait-until-player-is-online: true
    target-executor: "player"
```

### 🧪 Example Use

```plaintext
/hub
```

> Player is sent to hub and music plays after.

---
