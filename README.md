### **CommandBridge**

![overview](https://cdn.modrinth.com/data/cached_images/ddfd1c06df1ab99ee271859ee78e5e4d6d34ac49_0.webp)

This plugin connects **Velocity** and **Paper** servers seamlessly, enabling **cross-server command execution**. Here's what it does exactly:

- **Custom Scripts**: Define commands in scripts for both server types.  
- **Two-Way Communication**:  
   - Run a command on Velocity → Matching commands execute on Paper.  
   - Run a command on Paper → Matching commands execute on Velocity.  
- **WebSocket-Powered**: Ensures fast, reliable and **realtime** communication between servers.  
- **Placeholders Supported**: Use placeholders for dynamic command execution.  

With this plugin, you can create **global gameplay experiences** that feel smooth and connected!

---

### Requirements

**CommandBridge** v2.0.0 introduces new requirements and updates:  

- **Java 21 Required**: The server must run on Java 21.  
- **Minecraft Compatibility**: Fully compatible with Minecraft 1.21.x and 1.20.x.
- **Single JAR File**: Only download `CommandBridge-XXX-all.jar`.  
- **Dual Placement**: The JAR must be placed in both the target Paper server and the Velocity server to function.  
- **Plugin Compatibility**: Built for Paper servers, though it may work on other software (not tested).  

> **Important**: These requirements apply only to versions **2.0.0** and above.

---

### News

The **2.0.0 release** brings major updates and improvements:  

- **Rewritten Code**: Cleaner, faster, and more reliable.  
- **WebSocket Technology**: Replaces plugin messaging to fix issues where messages could not be sent if no players were online.  
- **Alpha Release**: Still in early stages, so expect some rough edges.  

---

### Installation (Short Version)

For detailed instructions, visit the [website](https://cb.objz.dev).

1. **Add the JAR**: Place the plugin JAR in the `plugins` folder of both the Paper and Velocity servers.  
2. **Restart Servers**: Restart both servers. After the restart, a `secret.key` file will be generated in the Velocity server's `plugins/CommandBridge` folder.  
3. **Secure the Key**:  
   - Open `secret.key` on the Velocity server.  
   - Copy the key and paste it into the `secret` field in the `config.yml` of all Paper servers.  
   - **Do not share this key.**  
4. **Open a Port**:  
   - Choose an unused port for the Velocity server.  
   - Configure this port in the `config.yml` of both Velocity and Paper servers.  
5. **Set Server IP**:  
   - Find your Velocity server's IP address (plain IP, no domain).  
   - Update the `host` field in Velocity's `config.yml` and the `remote` field in all Paper servers with this IP.  
6. **Set Identifiers**:  
   - In Velocity: Set a `server-id` (any name you prefer).  
   - In Paper: Set a matching `client-id`.  
7. **Restart Order**: Restart the Velocity server **before** the Paper servers.  

After setup, you should see logs like this in the Velocity console:  
```plaintext
[21:10:10 INFO] [CommandBridge]: New connection attempt from /127.0.0.1:42918
[21:10:10 INFO] [CommandBridge]: Client authenticated successfully: /127.0.0.1:42918
[21:10:10 INFO] [CommandBridge]: Added connected client: lobby
```

Now you're ready to create scripts!  

---

### What Comes in the Future

Exciting features and improvements are planned for **CommandBridge**:

- **Multiple Velocity Server Support**: Connect additional Velocity servers to act as clients.  
- **GUI Menu for Scripts**: Create and manage scripts with an easy-to-use graphical interface.  
- **Bug Fixes**: Continuous improvements to ensure stability and performance.   
- **Dump Command & Tools**: Integration with a website and Discord bot for troubleshooting and support.  
- **And More!**  

Stay tuned for updates and new features! 🎉  

---

### Help or Issues

- Report issues on [GitHub](https://github.com/objz/CommandBridge/issues).  
- Join the [Discord server](https://discord.gg/QPqBYb44ce) for support.  

---

### Metrics Collection

This plugin collects anonymous server statistics via [bStats](https://bstats.org/), an open-source statistics service for Minecraft plugins. You can disable this in `plugins/bStats/config.yml`.  

![bStats](https://bstats.org/signatures/velocity/CommandBridge.svg)

---

### Contributing

Join the [Discord server](https://discord.gg/QPqBYb44ce) or visit the [GitHub](https://github.com/objz/CommandBridge) for more information on contributing to the project.
