package org.commandbridge;

import com.velocitypowered.api.proxy.ProxyServer;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class Runtime {

    private final ProxyServer server;
    private final VerboseLogger verboseLogger;
    private final CommandBridge plugin;
    private final File scriptsFolder = new File("plugins/CommandBridgeVelocity/scripts");

    public Runtime(ProxyServer server, CommandBridge plugin) {
        this.server = server;
        this.plugin = plugin;
        this.verboseLogger = plugin.getVerboseLogger();
    }

    public void loadScripts() {
        unloadScripts();
        File[] files = scriptsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try (InputStream input = new FileInputStream(file)) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> data = yaml.load(input);
                    if (Boolean.TRUE.equals(data.get("enabled"))) {
                        this.plugin.getCommandRegistrar().registerCommand(data);
                    }
                } catch (Exception e) {
                    verboseLogger.error("Failed to load or parse script file: " + file.getName(), e);
                }
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
