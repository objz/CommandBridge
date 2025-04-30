---
description: >-
  CommandBridge offers a powerful script system that bridges commands across
  your network.
---

# 📎 Overview

<figure><img src="../.gitbook/assets/overview.png" alt=""><figcaption></figcaption></figure>

| Context              | Purpose                                             |
| -------------------- | --------------------------------------------------- |
| **Velocity scripts** | Run commands from the proxy onto Paper servers.     |
| **Paper scripts**    | Run commands from a Paper server back to the proxy. |

Scripts are simple YAML files stored under the `CommandBridge/scripts/` folder inside your plugin directory.

{% hint style="info" %}
Scripts define **custom commands** that your players or console can use!
{% endhint %}

Each script file can register a command like `/alert` or `/lobby` with specific actions attached.
