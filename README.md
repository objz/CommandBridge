## Commandbridge
**Commandbridge** is a versatile plugin that enables remote execution of server commands across the Velocity network. It uses a predefined set of commands specified in a configuration file, enhancing server management and interaction.

### Features

- **Placeholders in Commands**: Utilize placeholders such as `%player%` for player username, `%uuid%` for player UUID, and `%server%` for the server name where the player is located during command execution.
- **Execution Mode**: Choose whether the command is executed as the player or the console.
- **Execution Delay**: Set a delay in seconds for when the command is executed after being sent by the player.
- **Server Targeting**: Change which server the commands are executed on within the Velocity network.
- **Execution upon Player Join**: Wait for the player to join the target server before executing the command, with a timeout limit of 20 seconds.
- **Multiple Command Execution**: Execute as many commands as desired.
- **Verbose Output**: Provides detailed output for monitoring and debugging purposes.

### Compatibility

- **Compatible Versions**: 1.20x
- **Software Support**:
  - Bukkit-based servers (Spigot, Paper, Pufferfish, Purpur, etc.)
  - Velocity

### Setup

1. Download the latest releases of Commandbridge.
2. Install `commandbridge-bukkit` on your Bukkit server.
3. Install `commandbridge-velocity` on your Velocity server.
4. Restart the servers to apply changes.

### Permissions

> `commandbridge.admin` 
> 
> `commandbridge.command.<specific-command>`

### Documentation

[Access detailed documentation and configuration guides here.](LINK)

### Issues or Feature requests

[Open a Issue to report bugs or request a feature](https://github.com/72-S/CommandBridge/issues)
