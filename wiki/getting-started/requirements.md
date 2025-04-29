---
description: >-
  Before installing CommandBridge, ensure that your server setup meets the
  following requirements. The plugin has specific dependencies and version
  requirements to function correctly.
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

### Minecraft Compatibility

| Plugin version      | Minecraft version |
| ------------------- | ----------------- |
| 2.0.0 and above     | 1.20.x - 1.21.x   |
| below 2.0.0 (1.8.4) | 1.20.x - 1.21.1   |

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

| Server Type                            | Compatibility       | Notes                                                                  |
| -------------------------------------- | ------------------- | ---------------------------------------------------------------------- |
| Velocity                               | ✅ Supported         | Fully supported. Designed for Velocity plugin loader.                  |
| Paper                                  | ✅ Supported         | Fully supported. Built on the Paper API.                               |
| Spigot                                 | ✅ Supported         | Supported via Paper compatibility.                                     |
| Bukkit                                 | ✅ Supported         | Supported via Paper compatibility.                                     |
| Folia (Paper Fork)                     | ✅ Supported         | Folia adds regionized multithreading. Supported, but may contain bugs. |
| Purpur (Paper Fork)                    | ⚠️ Not Fully Tested | Should work based on Paper, but not officially tested.                 |
| Tuinity (Paper Fork)                   | ⚠️ Not Fully Tested | May work, but not officially tested or guaranteed.                     |
| Cross-loaders (e.g., Velocity + Paper) | ⚠️ Not Tested       | May work, but cross-loader support is not officially tested.           |
| Modloaders (Forge/Fabric etc.)         | ❌ Not Supported     | Only plugin loaders are supported. No modloader compatibility.         |

### **Summary (for lazy people)**

| Requirement                 | Details                                                                                        |
| --------------------------- | ---------------------------------------------------------------------------------------------- |
| **Java Version**            | Java 21 (for 2.0.0+). For older plugin versions, use Java 17.                                  |
| **Minecraft Version**       | Supports Minecraft 1.20.x - 1.21.x. Older versions require the legacy plugin (latest: 1.8.4).  |
| **Permission Plugin**       | Required on both Velocity and Paper servers. Recommended: [LuckPerms](https://luckperms.net/). |
| **Server Type**             | Paper and Velocity servers are fully supported. Bukkit and Spigot are **not supported**.       |
| **Paper API Compatibility** | Ensure the server runs a compatible API version (1.21.x for 2.0.0+).                           |
| **Velocity and Forks**      | Fully supported without requiring a separate JAR file.                                         |
