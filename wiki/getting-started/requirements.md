---
description: >-
  Before installing CommandBridge, ensure that your server setup meets the following requirements. The plugin has specific dependencies and version requirements to function correctly.
---

# 📘 Requirements

[#summary-for-lazy-people](requirements.md#summary-for-lazy-people "mention")

{% hint style="warning" %}  
For older plugin versions, please refer to the [legacy wiki](https://docs.old.huraxdax.club).  
{% endhint %}

### **Java Requirements**

| Plugin version      | Java version |
| ------------------- | ------------ |
| 2.0.0 and above     | 21           |
| below 2.0.0 (1.8.4) | 17           |

### **Minecraft Compatibility**

| Plugin version      | Minecraft version |
| ------------------- | ----------------- |
| 2.0.0 and above     | 1.20.x – 1.21.x   |
| below 2.0.0 (1.8.4) | 1.20.x – 1.21.1   |

***

### **Permission Management**

CommandBridge requires a **permission plugin** to manage command execution effectively.

* **Recommended**: [LuckPerms](https://luckperms.net/) (highly compatible and actively maintained).  
* **Required Setup**:  
  * Install a permission plugin **on both the Velocity and Paper servers**.  
  * Any permission plugin will work, but ensure it’s installed on both platforms.

***

### **Plugin Compatibility**

CommandBridge is a **cross-compatible plugin** designed to run on both **Velocity** and **Paper** servers using the same JAR file. Below are the specific compatibility details:

| Server Type                            | Compatibility       | Notes                                                         |
| -------------------------------------- | ------------------- | ------------------------------------------------------------- |
| Velocity                               | ✅ Supported         | Fully supported. Designed for the Velocity proxy.             |
| Paper                                  | ✅ Supported         | Fully supported. Built on the Paper API.                      |
| Spigot                                 | ✅ Supported         | Supported via Paper compatibility layer.                      |
| Bukkit                                 | ✅ Supported         | Supported via Paper compatibility layer.                      |
| Folia (Paper fork)                     | ✅ Supported         | Uses regionized multithreading. Should work but may contain bugs. |
| Purpur (Paper fork)                    | ⚠️ Not Fully Tested | Should work (based on Paper), but not officially tested.      |
| Tuinity (Paper fork)                   | ⚠️ Not Fully Tested | May work, but not officially tested or guaranteed.            |
| Cross-platform proxies (e.g., Waterfall) | ⚠️ Not Tested       | Should work with Velocity on backend, but not explicitly tested. |
| Modloaders (Forge/Fabric etc.)         | ❌ Not Supported     | Only plugin platforms are supported (no modloader compatibility). |

### **Summary (for lazy people)**

| Requirement           | Details                                                      |
| --------------------- | ------------------------------------------------------------ |
| **Java Version**      | Java 21 (for v2.0.0+). For older plugin versions (v1.8.4 and below), use Java 17. |
| **Minecraft Version** | Supports Minecraft 1.20.x – 1.21.x for v2.0.0+. Older Minecraft versions require using the legacy plugin (latest legacy version: 1.8.4). |
| **Permission Plugin** | Required on both Velocity and Paper servers. Recommended: [LuckPerms](https://luckperms.net/). |
| **Server Software**   | Use Velocity (proxy) and Paper (server). Bukkit/Spigot work via Paper compatibility but are not separately tested. Modloaders are **not** supported. |
