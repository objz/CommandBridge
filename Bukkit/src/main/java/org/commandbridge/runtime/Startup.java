package org.commandbridge.runtime;

import org.bukkit.plugin.java.JavaPlugin;
import org.commandbridge.CommandBridge;
import org.commandbridge.message.channel.MessageListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Startup {
    private final CommandBridge plugin;


    public Startup(CommandBridge plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        copyExampleScripts();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            plugin.getConfig().set("server-id", "REPLACE THIS WITH YOUR SERVER NAME");
            plugin.getConfig().set("verbose-output", false);
            plugin.saveConfig();
        }
        plugin.getVerboseLogger().loadConfig();
        plugin.getVerboseLogger().info("CommandBridge has been enabled!");
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "commandbridge:main");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "commandbridge:main", new MessageListener(plugin));
        plugin.getScripts().loadScripts();
    }


    public void onDisable() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin);
        plugin.getVerboseLogger().info("CommandBridge has been disabled!");
    }


    private void copyExampleScripts() {
        File scriptsFolder = new File(plugin.getDataFolder(), "bukkit-scripts");
        if (!scriptsFolder.exists()) {
            boolean created = scriptsFolder.mkdirs();
            if (!created) {
                plugin.getLogger().warning("Could not create the Scripts folder.");
                return;
            }
        }

        File exampleScript = new File(scriptsFolder, "example-bukkit.yml");
        if (!exampleScript.exists()) {
            try (InputStream in = plugin.getResource("example-bukkit.yml");
                 OutputStream out = new FileOutputStream(exampleScript)) {
                if (in == null) {
                    plugin.getLogger().warning("Resource example-bukkit.yml not found.");
                    return;
                }
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to copy example-bukkit.yml: " + e.getMessage());
            }
        } else {
            plugin.getLogger().info("Example script is already present in the bukkit-scripts folder.");
        }
    }

}
