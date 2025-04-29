# 📜 Script Structure

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

#### Fields explained

| Field                     | Type    | Description                                                                                                              |
| ------------------------- | ------- | ------------------------------------------------------------------------------------------------------------------------ |
| `name`                    | String  | <p>Command <code>test</code>. </p><p>Becomes <code>/test</code> ingame.<br>(don't need <code>/</code> in the script)</p> |
| `enabled`                 | Boolean | Whether the command is active.                                                                                           |
| `ignore-permission-check` | Boolean | Skip internal permission checking.                                                                                       |
| `hide-permission-warning` | Boolean | Hide the denied permission messages.                                                                                     |
| `commands`                | List    | List of command actions to perform.                                                                                      |

#### Commands fields

| Field                            | Type              | Purpose                                                                                       |
| -------------------------------- | ----------------- | --------------------------------------------------------------------------------------------- |
| `command`                        | String            | <p>Actual command to run </p><p>on the client.</p><p>(no leading <code>/</code>)</p>          |
| `delay`                          | Integer (seconds) | Wait time before running this command.                                                        |
| `target-executor`                | String            | Who runs the command (`player` or `console`).                                                 |
| `target-client-ids`              | List              | <p>Target Paper servers.</p><p><strong>(Velocity only)</strong> </p>                          |
| `wait-until-player-is-online`    | Boolean           | <p>Wait for player presence on the clients server.</p><p><strong>(Velocity only)</strong></p> |
| `check-if-executor-is-player`    | Boolean           | Check if a player triggered it.                                                               |
| `check-if-executor-is-on-server` | Boolean           | Only run if player is online.                                                                 |

{% hint style="info" %}
Velocity-specific fields like `target-client-ids` and `wait-until-player-is-online` are ignored by Paper.
{% endhint %}
