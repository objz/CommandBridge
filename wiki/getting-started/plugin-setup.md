
---
description: >-
  After installing the CommandBridge plugin and restarting your servers, follow this guide to configure it properly. This guide assumes you have restarted all servers with the plugin installed.
---

# ⚙️ Plugin setup

### **Important Terminology**

<table>
  <thead>
    <tr><th width="200">Clients</th><th>Server</th></tr>
  </thead>
  <tbody>
    <tr><td>All Paper servers</td><td>The Velocity proxy</td></tr>
  </tbody>
</table>

In this guide, when referring to "clients," it means all the Paper servers connected to your Velocity server (the "server").

***

### **Verify File Generation**

After restarting your servers, the following files and folders should be generated:

**On Each Paper Server:**

```markdown
paper/
└── plugins/
    └── CommandBridge/
        ├── config.yml             # Main configuration file for the Paper server
        └── scripts/               # Folder for script configurations
            └── example.yml        # Example script configuration
``` 

**On the Velocity Server:**

```markdown
velocity/
└── plugins/
    └── CommandBridge/
        ├── config.yml             # Main configuration file for the Velocity proxy
        ├── secret.key             # Secret key for authentication (only on Velocity)
        └── scripts/               # Folder for script configurations
            └── example.yml        # Example script configuration
``` 

#### Key Notes:

* The **`secret.key`** file is **only generated on the Velocity server**.  
* Both Velocity and Paper servers have:  
  * **`config.yml`** – Configuration file specific to each platform (proxy or server).  
  * **`scripts/`** – A folder containing script configuration files (with a default `example.yml` provided).

If these files and folders match your setup, proceed to the next steps. Otherwise, ensure the plugin is installed correctly on all servers.

***

### **Step 1: Set the Secret Key**

1. **Open the `secret.key` file** in the Velocity `plugins/CommandBridge/` folder.  
2. Copy the key (a long string) and paste it into the `secret` field in the `config.yml` file of **each Paper server**:  

   ```yaml
   secret: "CHANGE_ME"
   ```  

   Replace `"CHANGE_ME"` with the value from the Velocity `secret.key` file.

{% hint style="danger" %}  
Do not share this key. **Anyone with this key can connect to your CommandBridge and execute commands on your network.**  
{% endhint %}

***

### **Step 2: Set Unique IDs**

Each Paper server is a client and needs a unique identifier:

- **On Paper (Client)**: In each Paper server’s `config.yml`, set the **`client-id`** to a unique name for that server. For example, if the server is called "lobby", use:  

  ```yaml
  client-id: "lobby"
  ```  

  Ensure no two Paper servers share the same `client-id`.

- **On Velocity (Server)**: In the Velocity `config.yml`, set the **`server-id`**. By default, this is `"main"`. You can typically leave this as is (only change if you intend to have multiple CommandBridge servers).

***

### **Step 3: Configure Velocity Network Settings**

1. **Host** (`host` in Velocity `config.yml`): By default this is `0.0.0.0` (all network interfaces).  
   - If your Velocity proxy and Paper servers run on the **same machine** (self-hosted setup), you can set this to `127.0.0.1` to bind locally.  
   - If you are using an external hosting provider or different machines, leave it as `0.0.0.0` so it listens on all interfaces.

2. **Port** (`port` in Velocity `config.yml`): Choose an unused TCP port for CommandBridge to communicate.  
   - Update the `port` field with that number. For example:  

     ```yaml
     port: 3000
     ```  

   - If self-hosting, ensure this port is open on your machine’s firewall.  
   - If using a hosting provider, use their panel/tools to open this port for your Velocity proxy.

3. **SAN (Subject Alternative Name)** (`san` in Velocity `config.yml`):  
   - Set this to the **IP address or domain name of your Velocity server** (**do not include the port**). For example:  

     ```yaml
     san: "152.248.198.124"
     ```  

   - This is used for SSL certificate generation. It must match the address that clients (Paper servers) use to connect.

{% hint style="warning" %}  
Do not include the port in the SAN value. For example, use `"152.248.198.124"`, **not** `"152.248.198.124:3000"`.  
{% endhint %}

***

### **Step 4: Configure Paper Clients**

For each Paper server (client):

1. **Remote Host** (`remote` in Paper `config.yml`): Set this to the IP address or domain of the Velocity server (the same value you used for Velocity’s SAN, without port). For example:  

   ```yaml
   remote: "152.248.198.124"
   ```  

2. **Port** (`port` in Paper `config.yml`): Set this to the same port number you configured in the Velocity config (e.g., `3000` in our example).

These settings tell each Paper server how to reach the Velocity server’s CommandBridge socket.

***

### **Step 5: Restart Servers**

Now restart your servers in the following order:

1. **Restart Velocity first** (the proxy server).  
2. Once Velocity is up, restart all the connected Paper servers.

Starting Velocity first ensures it’s ready to accept connections from the Paper clients.

***

### **Validation**

While the servers are starting up, check the **Velocity console** for log messages like these:

```
[21:10:10 INFO] [CommandBridge]: New connection attempt from /127.0.0.1:42918  
[21:10:10 INFO] [CommandBridge]: Client authenticated successfully: /127.0.0.1:42918  
[21:10:10 INFO] [CommandBridge]: Added connected client: lobby
```

These logs indicate a Paper client (`lobby` in this case) connected and authenticated. If you see messages like that for each Paper server, your configuration is successful!

If these logs do **not** appear (or you see errors), double-check the configuration files for typos or mismatched values and ensure any required ports are open.

