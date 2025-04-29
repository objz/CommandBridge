# ⚙️ Script Configuration

Each script is a `.yml` file stored in:

```plaintext
plugins/CommandBridge/scripts/
```

### 🔑 Required Fields

| Field                        | Description                                 |
|-----------------------------|---------------------------------------------|
| `name`                      | Command trigger name (e.g. `/alertall`)     |
| `enabled`                   | Enables/disables the script                 |
| `commands:`                 | List of commands to execute                 |
| `target-client-ids:`        | Paper server IDs to send command to         |
| `target-executor:`          | `player` or `console`                       |

> 💡 Define one script per `.yml` file.

---
