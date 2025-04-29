---
# 🧲 Script File Structure

Scripts are written in **YAML format** and have a standard structure:

```yaml
name: "<command_name>"
enabled: true
ignore-permission-check: false
hide-permission-warning: false
commands:
  - command: "<command_to_execute>"
    delay: 0
    target-executor: "player|console"
    target-client-ids:
      - "server_id"
    wait-until-player-is-online: true
    check-if-executor-is-player: true
    check-if-executor-is-on-server: true
```

### Fields explained

| Field | Type | Description |
|:---|:---|:---|
| `name` | String | Command name (no `/`). Becomes `/name` ingame. |
| `enabled` | Boolean | Whether the script is active. |
| `ignore-permission-check` | Boolean | Skip internal permission checking. |
| `hide-permission-warning` | Boolean | Hide no-permission messages. |
| `commands` | List | List of command actions to perform. |

### Commands fields

| Field | Type | Purpose |
|:---|:---|:---|
| `command` | String | Actual command to run (no leading `/`). |
| `delay` | Integer (seconds) | Wait time before running this command. |
| `target-executor` | String | Who runs the command (`player` or `console`). |
| `target-client-ids` | List | (Velocity only) Target Paper servers. |
| `wait-until-player-is-online` | Boolean | Wait for player presence (Velocity only). |
| `check-if-executor-is-player` | Boolean | Only run if a player triggered it. |
| `check-if-executor-is-on-server` | Boolean | Only run if player is online. |

{% hint style="info" %}
Velocity-specific fields like `target-client-ids` and `wait-until-player-is-online` are ignored by Paper.
{% endhint %}
