package org.commandbridge.runtime;

import org.commandbridge.CommandBridge;
import org.commandbridge.utilities.VerboseLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

public class Scripts {

    private final File scriptsFolder = new File("plugins/CommandBridge/bukkit-scripts");
    private final CommandBridge plugin;
    private final VerboseLogger verboseLogger;

    public Scripts(CommandBridge plugin) {
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
                verboseLogger.info("Loading script: " + file.getName());
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(input);
                if (Boolean.TRUE.equals(data.get("enabled"))) {
                    this.plugin.getCommandRegister().registerCommands(data);
                    verboseLogger.info("Command registered successfully: " + file.getName());
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
            plugin.getCommandUnregister().unregisterCommand(command);
            verboseLogger.info("Command " + command + " unregistered successfully.");
        }
        plugin.clearRegisteredCommands();
    }
}
