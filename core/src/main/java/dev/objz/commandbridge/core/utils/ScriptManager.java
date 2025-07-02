package dev.objz.commandbridge.core.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import dev.objz.commandbridge.core.Logger;

public abstract class ScriptManager {
    private final Map<String, ScriptConfig> scripts = new HashMap<>();
    private final Yaml yaml;
    private final Logger logger;
    private final String scriptsDirectory;

    public ScriptManager(Logger logger, String pluginName) {
        this.logger = logger;
        this.scriptsDirectory = "plugins" + File.separator + pluginName + File.separator + "scripts";

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);
    }

    public void loadAllScripts() {
        File dir = new File(scriptsDirectory);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Failed to create scripts directory: " + scriptsDirectory);
        }

        try {
            Files.list(dir.toPath())
                    .filter(path -> path.toString().endsWith(".yml"))
                    .forEach(this::loadScriptFile);

            logger.debug("All script files have been loaded from directory: {}", scriptsDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scripts: " + e.getMessage(), e);
        }
    }

    public void reload() {
        scripts.clear();

        loadAllScripts();

        logger.info("All scripts have been successfully reloaded");
    }

    private void loadScriptFile(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            Map<String, Object> fileData = yaml.load(inputStream);
            if (fileData == null)
                fileData = new HashMap<>();

            ScriptConfig scriptConfig = new ScriptConfig(fileData);
            scripts.put(path.getFileName().toString(), scriptConfig);

            logger.debug("Script file loaded successfully: {}", path.getFileName().toString());

            onFileProcessed(path.getFileName().toString(), scriptConfig);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load script file '" + path.getFileName() + "': " + e.getMessage(), e);
        }
    }

    public ScriptConfig getScriptConfig(String fileName) {
        logger.debug("Retrieved config");
        return scripts.get(fileName);
    }

    public void copyDefaultScript(String resourceName, String targetFileName) {
        File scriptDir = new File(scriptsDirectory);
        if (!scriptDir.exists() && !scriptDir.mkdirs()) {
            throw new RuntimeException("Failed to create script directory: " + scriptsDirectory);
        }

        File scriptFile = new File(scriptDir, targetFileName);

        if (scriptFile.exists()) {
            logger.debug("Script file '{}' already exists, skipping copy", scriptFile.getAbsolutePath());
            return;
        }

        try (InputStream in = getClass().getResourceAsStream("/" + resourceName);
                OutputStream out = Files.newOutputStream(scriptFile.toPath())) {
            if (in == null) {
                throw new RuntimeException("Resource '" + resourceName + "' not found in the plugin JAR");
            }

            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            logger.info("Default script '{}' copied to: {}", resourceName, scriptFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy default script file " + resourceName + ": " + e.getMessage(), e);
        }
    }

    public static class ScriptConfig {
        private final String name;
        private final List<String> aliases = new ArrayList<>();
        private final boolean enabled;
        private final boolean ignorePermissionCheck;
        private final boolean hidePermissionWarning;
        private final List<Command> commands;

        @SuppressWarnings("unchecked")
        public ScriptConfig(Map<String, Object> data) {
            this.name = (String) data.getOrDefault("name", "Unnamed Command");
            this.aliases.addAll((List<String>) data.getOrDefault("aliases", new ArrayList<>()));
            this.enabled = (boolean) data.getOrDefault("enabled", false);
            this.ignorePermissionCheck = (boolean) data.getOrDefault("ignore-permission-check", false);
            this.hidePermissionWarning = (boolean) data.getOrDefault("hide-permission-warning", false);

            this.commands = new ArrayList<>();
            Object commandsObject = data.get("commands");
            if (commandsObject instanceof List<?> commandsList) {
                for (Object commandData : commandsList) {
                    if (commandData instanceof Map) {
                        commands.add(new Command((Map<String, Object>) commandData));
                    }
                }
            }
        }

        public String getName() {
            return name;
        }

        public List<String> getAliases() {
            return aliases;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean shouldIgnorePermissionCheck() {
            return ignorePermissionCheck;
        }

        public boolean shouldHidePermissionWarning() {
            return hidePermissionWarning;
        }

        public List<Command> getCommands() {
            return commands;
        }
    }

    public static class Command {
        private final String command;
        private final int delay;
        private final List<String> targetClientIds;
        private final String targetExecutor;
        private final boolean waitUntilPlayerIsOnline;
        private final boolean checkIfExecutorIsPlayer;
        private final boolean checkIfExecutorIsOnServer;

        @SuppressWarnings("unchecked")
        public Command(Map<String, Object> data) {
            this.command = (String) data.get("command");
            this.delay = (int) data.getOrDefault("delay", 0);
            this.targetClientIds = (List<String>) data.getOrDefault("target-client-ids", new ArrayList<>());
            this.targetExecutor = (String) data.getOrDefault("target-executor", "console");
            this.waitUntilPlayerIsOnline = (boolean) data.getOrDefault("wait-until-player-is-online", false);
            this.checkIfExecutorIsPlayer = (boolean) data.getOrDefault("check-if-executor-is-player", true);
            this.checkIfExecutorIsOnServer = (boolean) data.getOrDefault("check-if-executor-is-on-server", true);
        }

        public String getCommand() {
            return command;
        }

        public int getDelay() {
            return delay;
        }

        public List<String> getTargetClientIds() {
            return targetClientIds;
        }

        public String getTargetExecutor() {
            return targetExecutor;
        }

        public boolean shouldWaitUntilPlayerIsOnline() {
            return waitUntilPlayerIsOnline;
        }

        public boolean isCheckIfExecutorIsPlayer() {
            return checkIfExecutorIsPlayer;
        }

        public boolean isCheckIfExecutorIsOnServer() {
            return checkIfExecutorIsOnServer;
        }
    }

    public abstract void onFileProcessed(String fileName, ScriptConfig scriptConfig);
}
