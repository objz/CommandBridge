package org.commandbridge;

import com.velocitypowered.api.proxy.ProxyServer;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class VelocityRuntime {

    private final ProxyServer server;
    private final VerboseLogger verboseLogger;
    private final CommandBridge plugin;
    private final File scriptsFolder = new File("plugins/CommandBridgeVelocity/scripts");

    public VelocityRuntime(ProxyServer server, CommandBridge plugin) {
        this.server = server;
        this.plugin = plugin;
        this.verboseLogger = plugin.getVerboseLogger();
    }

    public void loadScripts() {
        verboseLogger.info("Loading scripts...");
        unloadScripts();
        File[] files = scriptsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            verboseLogger.warn("No scripts found.");
            return;
        }

        for (File file : files) {
            try (InputStream input = new FileInputStream(file)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(input);
                if (Boolean.TRUE.equals(data.get("enabled")) && Boolean.FALSE.equals(data.get("reverse-registration"))) {
                    this.plugin.getCommandRegistrar().registerCommand(data);
                    verboseLogger.info("Command registered successfully: " + file.getName());
                } else if (Boolean.TRUE.equals(data.get("enabled")) && Boolean.TRUE.equals(data.get("reverse-registration"))) {
                    verboseLogger.info("Skipping command with reverse registration in: " + file.getName());
                    this.plugin.getCommandRegistrar().registerBukkitCommand(data);
                }else if (Boolean.FALSE.equals(data.get("enabled")) && Boolean.TRUE.equals(data.get("reverse-registration"))) {
                    verboseLogger.info("Skipping disabled command with reverse registration in: " + file.getName());
                    this.plugin.getCommandRegistrar().unregisterBukkitCommand(data);

                } else {
                    verboseLogger.info("Skipping disabled command in: " + file.getName());
                }
            } catch (Exception e) {
                verboseLogger.error("Failed to load or parse script file: " + file.getName(), e);
            }
        }
    }


    public void unloadScripts() {
        ArrayList<String> registeredCommands = new ArrayList<>(plugin.getRegisteredCommands());
        for (String command : registeredCommands) {
            server.getCommandManager().unregister(command);
            verboseLogger.info("Command " + command + " unregistered successfully.");
        }
        plugin.clearRegisteredCommands();
    }
}
