---
description: >-
  After installing the CommandBridge plugin and restarting your servers, follow
  this guide to configure it properly. This guide assumes you have restarted all
  servers with the plugin installed.
---

# ⚙️ Plugin setup

### **Important Information**

<table><thead><tr><th width="165">Clients</th><th width="161">Server</th></tr></thead><tbody><tr><td>Paper Servers</td><td>Velocity Server</td></tr></tbody></table>

In this guide, when referring to "clients," it means all the Paper servers connected to your Velocity server.

***

### **Verify File Generation**

After restarting your servers, the following files and folders should be generated:

**Paper Servers**

```markdown
paper/
└── plugins/
    └── CommandBridge/
        ├── config.yml             # Main configuration file for the Paper server
        └── scripts/               # Folder for script configurations
            └── example.yml        # Example script configuration
```

**Velocity Server**

<pre class="language-markdown"><code class="lang-markdown"><strong>velocity/
</strong>└── plugins/
    └── CommandBridge/
        ├── config.yml             # Main configuration file for the Velocity server
        ├── secret.key             # Secret key for authentication (only on Velocity)
        └── scripts/               # Folder for script configurations
            └── example.yml        # Example script configuration
</code></pre>

#### Key Notes:

* The `secret.key` file is **only generated on the Velocity server**
* Both Velocity and Paper servers have:
  * `config.yml`: Configuration file specific to each server
  * `scripts/`: A folder containing the `example.yml` script template

If these files and folders match your setup, proceed to the next steps. Otherwise, ensure the plugin is installed correctly.

***

### **Step 1: Setup the Secret Key**

1. **Open the `secret.key` file** in the Velocity `plugins/CommandBridge` folder.
2.  Copy the key and paste it into the `secret` field in the `config.yml` file of **all Paper** servers:

    ```yaml
    secret: "CHANGE_ME"
    ```

    Replace `"CHANGE_ME"` with the value from `secret.key`.

{% hint style="danger" %}
Do not share this key. Anyone with access to the key can connect to your CommandBridge server and execute commands.
{% endhint %}

***

### **Step 2: Set Up Client and Server IDs**

Each Paper server is a client and needs a unique identifier.

**On Paper**

*   In `config.yml`, set the `client-id` for each Paper server.\
    Ensure each client has a unique identifier. For example, if the server is called "lobby," set the `client-id` to `lobby`:

    ```yaml
    client-id: "lobby"
    ```

**On Velocity**

* In `config.yml`, set the `server-id`.\
  By default, this is set to `main`. You can leave this as is unless you’re connecting additional servers.

***

### **Step 3: Configure Velocity**

1. **Set the Host**
   * Open on Velocity `config.yml`.
   * Locate the `host` field. By default, this is set to `0.0.0.0`.
   * If you’re using a hosting provider, leave it as is.
   * If self-hosting, you can set it to `127.0.0.1` (localhost) if the server and clients run on the same machine.
2. **Open a Port**
   * Choose an unused **TCP port** for CommandBridge.
   *   Update the `port` field in `config.yml` with the open port.\
       Example:

       ```yaml
       port: 3000
       ```
   * **Self-Hosting:** Open the port on your machine. This depends on your OS and network configuration.
   * **Hosting Providers:** Use their control panel to open an additional port.
3. **Set the SAN (Subject Alternative Name)**
   * Locate the `san` field in `config.yml`.
   *   Add the **IP address** of your Velocity server or the domain name (without the port).\
       Example:

       ```yaml
       san: "152.248.198.124"
       ```

{% hint style="warning" %}
Do not include the **port** in the SAN value. For example, `152.248.198.124:3000` is incorrect.
{% endhint %}

***

### **Step 4: Configure Paper Clients**

1. **Set the Remote and Port**
   *   Set the `remote` field to the **IP address of the Velocity server** (without the port).\
       Example:

       ```yaml
       remote: "152.248.198.124"
       ```
   * Set the `port` field to match the port configured in the Velocity `config.yml`.

***

### **Step 5: Restart Servers**

Restart your servers in the following order:

1. **Restart Velocity first**.
2. Then, restart all connected Paper servers.

***

### **Validation**

While the servers are starting, check the Velocity console for logs like these:

```
[21:10:10 INFO] [CommandBridge]: New connection attempt from /127.0.0.1:42918
[21:10:10 INFO] [CommandBridge]: Client authenticated successfully: /127.0.0.1:42918
[21:10:10 INFO] [CommandBridge]: Added connected client: lobby
```

If these logs appear, your configuration is successful. If not, review the configuration files for errors.
