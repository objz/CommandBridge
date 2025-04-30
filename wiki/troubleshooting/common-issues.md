---
description: Solutions to common problems when setting up or using CommandBridge.
---

# ❌ Common issues

Setting up a proxy and multiple servers can be tricky. Here are some common issues you might encounter with CommandBridge and how to resolve them.

### 1. Paper server cannot connect to Velocity (No connection / authentication fails)

**Symptoms:** In Velocity’s console, you **do not** see the “Client authenticated successfully” message for one or more Paper servers when they start. Paper server logs might show errors or timeout trying to connect.

**Possible Causes & Solutions:**

* **Secret key mismatch:** Ensure you correctly copied the secret key from Velocity’s `secret.key` into each Paper server’s `config.yml` (`secret` field). A wrong or missing key will prevent the Paper client from authenticating. _(If you regenerate the Velocity secret.key, you must update all clients again.)_
* **Host/Remote configuration:** Double-check the `host` in Velocity’s config and the `remote` in Paper’s config. For example, if Velocity’s host is set to `127.0.0.1` (localhost), then Paper’s `remote` must also point to `127.0.0.1` – and they _must be running on the same machine_. If your Paper server is on a different machine or container, Velocity’s `host` should be `0.0.0.0` (and Paper’s `remote` should use Velocity’s external IP or domain).
* **Port not open or mismatched:** Verify that the `port` number is the same in Velocity and all Paper configs. Also ensure that port is not blocked by a firewall:
  * If self-hosting, check your OS firewall or router port forwarding for the specified port.
  * If using a server host, use their panel to open the port for the Velocity proxy.
  * If the port is in use by another service, choose a different port in the configs and restart.
* **SAN not configured or wrong:** The **SAN (Subject Alternative Name)** in Velocity’s config must include the IP or domain that Paper clients use to connect. If this is incorrect, the SSL certificate will not match and the connection will fail. For example, if Paper’s `remote` is set to `"example.com"`, then Velocity’s `san` should be `"example.com"` as well. Do not include ports in the SAN or remote fields.

After checking these settings, restart Velocity first, then Paper servers. Watch the Velocity console for connection messages. If it still fails, turn on **debug mode** (set `debug: true` in the `config.yml` and restart) to get detailed logs, and look for any specific error messages.

### 2. “Address already in use” error on Velocity startup

**Symptom:** Velocity’s console shows an error indicating it couldn’t bind to the port (e.g., `java.net.BindException: Address already in use`).

**Cause:** The port configured for CommandBridge is already being used by another service (or another instance of Velocity).

**Solution:** Choose a different unused port in Velocity’s `config.yml` for CommandBridge and update all Paper servers’ `config.yml` to match the new port. Then restart Velocity (and Paper servers). Ensure no other application is using that port. Commonly, ports 3000–4000 are free, but it depends on your environment.

### 3. “Client authenticated successfully” appears, but commands aren’t executing

**Symptom:** Velocity’s log shows that clients connected, but when you run a CommandBridge command, nothing happens on the other side.

**Possible Causes & Solutions:**

* **Scripts not defined on that side:** Make sure you created the script configuration on the correct side. For example, if a player runs a command on a Paper server, that command needs to be defined either on the Paper side (to forward to Velocity) or on Velocity (to forward to Paper).
* **Permission issues:** By default, CommandBridge requires players to have permission to use custom commands. If `ignore-permission-check` is `false` in your script config, you need to grant the appropriate permission node via your permissions plugin:
  * The permission node is `commandbridge.command.<name>` (where `<name>` is the script name). For example, for a script named “hub”, give players `commandbridge.command.hub`.
  * Alternatively, you can set `ignore-permission-check: true` in the script config to temporarily bypass permission checks (not recommended for public commands).
* **Missing dependency:** Ensure you have a permission plugin installed on both Velocity and Paper. Without one, permission checks might automatically fail. (LuckPerms on both is recommended.)
* **Target server issues:** If a script is supposed to run a command on a target Paper server and nothing happens, check that the target server’s `client-id` exactly matches what the script’s `target-client-ids` list uses (case-sensitive). If the names don’t match, Velocity will send the command to a non-existent target.
* **Plugin not loaded or script disabled:** Confirm that the CommandBridge plugin is actually enabled on all servers (`/plugins` on Paper, or `/cb version` on Velocity’s console). If the plugin failed to load on a server (check console for errors on startup), fix that first. Also verify the script file has `enabled: true`.

### 4. “You do not have permission to use this command” message when using a script command

**Cause:** This indicates the player (or console) triggering the CommandBridge command doesn’t have permission, and the script has not ignored permission checks.

**Solution:** As mentioned above, grant the appropriate permission via your permissions plugin, or set the script’s `ignore-permission-check: true` if you want to bypass. If you prefer to hide this message without giving permission, you can set `hide-permission-warning: true` (though the command still won’t run). Generally, for player-facing commands, it’s best to give them permission in a controlled way.

### 5. Commands run, but placeholders aren’t working

**Symptom:** You used placeholders (like `%cb_player%` or a PlaceholderAPI code) in your script, but in the executed command it shows the literal placeholder text or nothing happens.

**Solution:**

* Make sure you have PlaceholderAPI installed on Paper if you are using placeholders other than the built-in `%cb_...%` ones. Without PAPI, only the built-in placeholders will be replaced.
* If using placeholders on Velocity side, consider installing PapiProxyBridge on Velocity and PAPI on Paper so that PAPI placeholders can be recognized on the proxy.
* Check logs in debug mode – CommandBridge will log whether it hooked into PlaceholderAPI. If it says PAPI not found, you know why placeholders weren’t parsed.
* Also ensure your placeholder syntax is correct and supported. For example, built-in placeholders use `%cb_player%` exactly (all lowercase).

### 6. Still having issues?

If you’ve checked the above and are still experiencing problems, see the [Support resources](support-resources.md) page for where to get additional help (Discord or GitHub). Enabling `debug: true` in the config and then replicating the issue can provide more detailed logs to share when asking for support.
