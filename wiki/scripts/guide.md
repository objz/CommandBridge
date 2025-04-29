---
description: >-
  Comprehensive guide to CommandBridge's script system, including configuration format, options, and examples.
---

# 📜 Scripts Guide

After installation and setup, you are ready to create scripts! CommandBridge’s **script system** allows you to define custom commands that bridge your Velocity proxy and Paper servers.

Each script is defined in a YAML file located in the `CommandBridge/scripts/` folder of your server. Velocity and Paper each maintain their own set of script files:
- **Velocity scripts** (in the Velocity plugin folder) handle commands executed on the proxy that affect one or more Paper servers.
- **Paper scripts** (in each Paper server’s plugin folder) handle commands executed on that Paper server that may trigger actions on the Velocity proxy (or possibly other servers via the proxy).

## Script File Structure

A script configuration file consists of a top-level structure with these main keys:

```yaml
name: "<command_name>"
enabled: <true|false>
ignore-permission-check: <true|false>
hide-permission-warning: <true|false>
commands:
  - command: "<command_to_execute_1>"
    delay: <seconds>
    target-executor: "<player|console>"
    [target-client-ids: 
      - "<server_id1>"
      - "<server_id2>"]
    [wait-until-player-is-online: <true|false>]
    check-if-executor-is-player: <true|false>
    check-if-executor-is-on-server: <true|false>
  - command: "<command_to_execute_2>"
    ... (same structure as above)
```

**Field explanations:**

- **`name`**: The name of the command players will use (without the slash). This will register as `/name`. For example, `name: hub` defines a `/hub` command.  
- **`enabled`**: Whether this script is active. If `false`, the command will not be registered or usable.  
- **`ignore-permission-check`**: If `false`, CommandBridge will require the executor to have permission `commandbridge.command.<name>` to use this command. If `true`, anyone can use the command regardless of permissions. *(Even if ignored here, you can still use a permissions plugin to restrict the actual in-game command as needed.)*  
- **`hide-permission-warning`**: If `false`, a player without permission will receive a "no permission" message when trying the command. If `true`, no warning is shown (the command will appear non-functional if they lack permission). Typically used in combination with `ignore-permission-check`.  
- **`commands`**: A list of one or more actions to execute when the custom command is run. Each action has several sub-fields:
  - **`command`**: The exact command string to run (do **not** include the leading `/`). For example, `say Hello` or `server hub`. You can include placeholders here (see **Placeholders** below).  
  - **`delay`**: Delay in seconds before executing this command after the previous action. Use `0` for no delay (all commands run in immediate succession). This can be used to space out sequential actions.  
  - **`target-executor`**: Who will execute the command on the target side. Options are:
    - `"player"`: execute as the player who triggered the script.  
    - `"console"`: execute as the server console.
  - **`target-client-ids`** (**Velocity scripts only**): A list of target Paper server IDs on which to execute this command. This field **only applies in Velocity’s script files**, since Velocity can dispatch commands to any connected Paper servers. For example, specifying two IDs will run the command on both servers. If this field is present, the command will be sent to those remote servers. If this field is omitted (or in a Paper script), the command will be executed on the same platform where it’s triggered.
  - **`wait-until-player-is-online`** (**Velocity scripts only**): If `true`, for each target server listed, the plugin will wait until the player is actually online on that target server before executing the command there. This is useful if the previous action moves the player to another server (e.g., using a Velocity `/server` command to teleport). Setting this ensures the next command (perhaps on the destination server) runs only after the player arrives.  
  - **`check-if-executor-is-player`**: If `true`, the script will only run if the command executor is a player (not console). If `false`, it doesn’t check – meaning console or command blocks could trigger it.  
  - **`check-if-executor-is-on-server`**: If `true`, ensures that the command executor is currently online (connected) when triggering the script. Usually this should be `true` for player-triggered commands to avoid issues if a player disconnects. Set `false` if you want the command to run even if the triggering entity is not a player or not online.

**Note:** *Velocity vs Paper scripts:* The YAML structure is the same on both, but **Velocity-specific fields** (`target-client-ids` and `wait-until-player-is-online`) are only used in Velocity’s context. Paper scripts do not need those because a Paper server only forwards commands to the Velocity proxy (single target). In a Paper script, any command listed will be executed on the Velocity side by default (since Velocity is the only connection target). Conversely, in a Velocity script, commands can be executed on one or many connected Paper servers via `target-client-ids`. 

## Creating a Simple Script (Basic Example)

Let's create a simple script on the Velocity side that broadcasts a message to all servers. Suppose we want a command `/alert` that makes all servers announce a warning.

**On Velocity (proxy):** Create a file `plugins/CommandBridge/scripts/alert.yml` with the following content:

```yaml
name: alert
enabled: true
ignore-permission-check: false
hide-permission-warning: false
commands:
  - command: 'broadcast Attention: Maintenance in 5 minutes!'
    delay: 0
    target-client-ids:
      - 'lobby'
      - 'survival'
      - 'minigame'
    target-executor: 'console'
    wait-until-player-is-online: false
    check-if-executor-is-player: false
    check-if-executor-is-on-server: false
```

In this example:
- We define a new command `/alert` (since `name: alert`).  
- It's enabled and will require the executor to have permission `commandbridge.command.alert` (because we left `ignore-permission-check: false`).  
- When someone with permission runs `/alert` on Velocity, the plugin will execute one action: run the `broadcast ...` command on the console of three target servers (`lobby`, `survival`, `minigame`). Each of those Paper servers will broadcast the maintenance message to their players.  
- We didn’t need any delay or waiting, since it’s a direct broadcast. We also allowed console execution (so even the Velocity console could run `/alert`).

Because this script is on Velocity, players on Paper servers **cannot** directly use `/alert` unless you also create a corresponding script on each Paper server (or you run it from the Velocity console or as an admin on Velocity). For a command that should be usable from anywhere, see the next example.

## Advanced Script Example

For a more complex scenario, let’s implement the **global** `/hub` command described in the use-cases (Example 1), which involves multiple sequential actions across servers:

**Goal:** `/hub` will teleport the player to the hub server (via Velocity) and then play music on that hub server (via a Paper plugin command).

We will create a **Velocity script** for `/hub` because Velocity can coordinate both the teleport and issuing the next command on the hub server:

```yaml
name: hub
enabled: true
ignore-permission-check: false
hide-permission-warning: false
commands:
  - command: 'server hub'
    delay: 0
    target-executor: 'player'
    # (No target-client-ids here, because 'server hub' is a Velocity command executed by the player on Velocity)
    check-if-executor-is-player: true
    check-if-executor-is-on-server: true

  - command: 'music'
    delay: 0
    target-client-ids:
      - 'hub'
    target-executor: 'player'
    wait-until-player-is-online: true
    check-if-executor-is-player: true
    check-if-executor-is-on-server: true
```

Breaking down what happens when a player uses `/hub` on **any server**:
1. If run on a Paper server, the Paper plugin will forward the request to Velocity (because `/hub` is not a known Paper command but is known to the proxy). If run on Velocity directly, it just triggers there. Either way, Velocity receives the command trigger from the player.  
2. **First action:** Velocity runs its built-in `/server hub` command **as the player**, teleporting that player to the `hub` server. The plugin immediately moves to the next action.  
3. **Second action:** The plugin sends the `music` command to the `hub` server’s CommandBridge client. Because `wait-until-player-is-online: true`, CommandBridge will **wait** until the player is actually connected to the `hub` server before executing `music` as that player on the hub server. This ensures the `/music` command runs at the right time (after teleport).  
4. The player hears the music on the hub server. The sequence is complete.

With this one script on Velocity, players can use `/hub` from any server. The Paper servers don’t even need their own `/hub` script file in this case—any unknown command on Paper is passed to Velocity, and if Velocity recognizes it (which it will, since we made a script for it), it handles the rest. However, if you prefer, you could also create a matching script on each Paper server for `/hub` that simply forwards to Velocity (e.g., using a `velocity ...` command), but it's not necessary due to how Velocity proxies player commands.

{% hint style="info" %}  
**Tip:** To make a command truly global (usable on both Velocity and Paper), define it on Velocity as shown. Velocity will catch the command from players on any connected server. Ensure that the players have the necessary permission if required (e.g., `commandbridge.command.hub` in our example) on whichever platform they might execute it.  
{% endhint %}

## Placeholders in Scripts

CommandBridge supports placeholders to make your scripts dynamic. By default, the plugin provides a few **built-in placeholders** that you can use in your command strings:

- **%cb_player%** – The name of the player who triggered the script.  
- **%cb_uuid%** – That player’s UUID.  
- **%cb_world%** – (*Paper only*) The name of the world the player is currently in (if applicable).

These placeholders will be replaced with the actual values at runtime. For example, a command entry:
```yaml
- command: "say %cb_player% has initiated the event!"
  target-executor: "console"
  ...
``` 
would broadcast “<playername> has initiated the event!” on the target server.

If you have **PlaceholderAPI** installed on your Paper servers, CommandBridge will automatically hook into it on the Paper side. This means you can use any PAPI placeholders in your script commands that are executed on Paper servers. Similarly, if you have **PapiProxyBridge** on Velocity, PlaceholderAPI placeholders can work in commands executed on Velocity or passed through Velocity.

{% hint style="info" %}  
**Note on PlaceholderAPI:** When PlaceholderAPI is present, CommandBridge will detect it and log “Hooked into PlaceholderAPI” (or PapiProxyBridge on Velocity) on startup. You can then include placeholders like `%server_time%` or any custom placeholders from your plugins in the `command` strings, and they will be parsed. If PAPI is not installed, only the built-in `%cb_...%` placeholders will work.  
{% endhint %}

With the script system configured, you can design a wide variety of cross-server commands to suit your network’s needs – from simple one-step forwards to complex multi-step orchestrations. Be sure to test each script thoroughly and use the `debug` mode in configs (set `debug: true`) if you need detailed logs for troubleshooting script behavior.
