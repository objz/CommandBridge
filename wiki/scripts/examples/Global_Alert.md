# 🔊 Example: Global Alert

Send a message to all servers.

### 📋 Script: `alertall.yml`

```yaml
name: alertall
enabled: true
commands:
  - command: "broadcast [Global] {message}"
    delay: 0
    target-client-ids: ["lobby", "survival"]
    target-executor: "console"
```

### 🧪 Example Use

```plaintext
/alertall Hello players!
```

> Broadcasts to all target servers via `/broadcast`.

---
