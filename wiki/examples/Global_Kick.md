# ❌ Example: Global Kick

Kick a player from every server.

### 📋 Script: `gkick.yml`

```yaml
name: gkick
enabled: true
commands:
  - command: "kick {player} {args}"
    delay: 0
    target-client-ids: ["lobby", "survival"]
    target-executor: "console"
```

### 🧪 Example Use

```plaintext
/gkick Notch Being rude
```

> Kicks the player across all listed servers.

---
