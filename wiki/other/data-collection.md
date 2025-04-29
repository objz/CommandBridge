---
description: >-
  Information on what data CommandBridge collects and how to opt out.
---

# 🔢 Data collection

CommandBridge uses an open-source metrics service called [bStats](https://bstats.org/) to collect **anonymous statistics** about plugin usage. This helps the developer understand how the plugin is used (like how many servers use it, what Minecraft version, etc.) and improve it over time.

**What is collected**: Basic server info such as plugin version, Minecraft version, player count, and other non-personal statistics. **No personal or sensitive data** (like IPs or chat) is collected.

**How to opt out**: You can disable metrics collection by editing the `plugins/bStats/config.yml` file on each server and setting `enabled: false`. After that, no data will be sent to bStats from that server.

![bStats](https://bstats.org/signatures/velocity/CommandBridge.svg)

*The chart above is an example of a bStats graph for CommandBridge (showing the number of servers using the plugin over time).*

This data collection follows the standard practice for many Minecraft plugins and is completely anonymous. It can be useful for the developer and does not impact performance. However, the choice is yours – you can easily opt out as described.

_For more details on bStats, you can visit their site [here](https://bstats.org/)._ 
