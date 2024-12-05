package dev.consti.commandbridge.velocity.utils;

import dev.consti.commandbridge.velocity.command.CommandRegistrar;
import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.ScriptManager;

public class ScriptUtils extends ScriptManager {
    private final Logger logger;
    private final CommandRegistrar registrar;

    public ScriptUtils(Logger logger, String pluginName) {
        super(logger, pluginName);
        this.logger = logger;
        this.registrar = Runtime.getInstance().getRegistrar();
    }

    @Override
    public void onFileProcessed(String fileName, ScriptConfig scriptConfig) {
        if (scriptConfig.isEnabled()) {
            logger.info("Loaded script: {}", scriptConfig.getName());
            try {
                registrar.registerCommand(getScriptConfig(fileName));
                logger.debug("Registered command for script: {}", scriptConfig.getName());
            } catch (Exception e) {
                logger.error("Failed to register command for script: {}. Error: {}", scriptConfig.getName(), e.getMessage(), e);
            }
        } else {
            logger.info("Skipped disabled script: {}", scriptConfig.getName());
        }
    }
}
