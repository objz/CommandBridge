package org.commandbridge.runtime;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import org.commandbridge.CommandBridge;
import org.commandbridge.message.channel.MessageListener;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;

public class Startup {
    private final CommandBridge plugin;


    public Startup(CommandBridge plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        CommandAPI.onEnable();
        copyExampleScripts();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            plugin.getConfig().set("#DO NOT CHANGE THIS VALUE", null);
            plugin.getConfig().set("config-version", 2);
            plugin.getConfig().set("server-id", "REPLACE THIS WITH YOUR SERVER NAME");
            plugin.getConfig().set("verbose-output", false);
            plugin.saveConfig();
        }
        plugin.getVerboseLogger().loadConfig();
        if (Objects.equals(plugin.getConfig().getString("server-id"), "REPLACE THIS WITH YOUR SERVER NAME")) {
            plugin.getVerboseLogger().warn("Please replace the server-id in the config.yml file with your server name.");
        }
        if (plugin.getConfig().getInt("config-version") < plugin.getConfig_version()) {
            plugin.getVerboseLogger().warn("Your config version is older than the plugin version, please update the plugin to use this script. Wiki: https://72-s.github.io/CommandBridge");
        } else if (plugin.getConfig().getInt("config-version") > plugin.getConfig_version()) {
            plugin.getVerboseLogger().warn("Your config version is newer than the plugin version, please update the plugin to use this script. Wiki: https://72-s.github.io/CommandBridge");
        }
        plugin.getVerboseLogger().info("CommandBridge has been enabled!");
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "commandbridge:main");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "commandbridge:main", new MessageListener(plugin));
        plugin.getScripts().loadScripts();
    }


    public void onDisable() {
        CommandAPI.onDisable();
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin);
        plugin.getVerboseLogger().info("CommandBridge has been disabled!");
    }

    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(plugin).verboseOutput(false).usePluginNamespace().silentLogs(true));
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
                 OutputStream out = Files.newOutputStream(exampleScript.toPath())) {
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
