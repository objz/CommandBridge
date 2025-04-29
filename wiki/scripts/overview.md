
CommandBridge offers a powerful **script system** that bridges commands across your network.

| Context | Purpose |
|:---|:---|
| **Velocity scripts** | Run commands from the proxy onto Paper servers. |
| **Paper scripts** | Run commands from a Paper server back to the proxy. |

Scripts are simple YAML files stored under the `/scripts/` folder inside your plugin directory.

{% hint style="info" %}
Scripts define **custom commands** that your players or console can use!
{% endhint %}

Each script file can register a command like `/hub` or `/alert` with specific actions attached.
