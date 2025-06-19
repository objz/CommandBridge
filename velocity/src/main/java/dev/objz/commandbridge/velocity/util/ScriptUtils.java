package dev.objz.commandbridge.velocity.util;

import dev.objz.commandbridge.velocity.command.CommandRegistrar;
import dev.objz.commandbridge.velocity.core.Runtime;
import dev.objz.commandbridge.core.Logger;
import dev.objz.commandbridge.core.utils.ScriptManager;

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
            logger.info("Loaded script: {}", fileName);
            try {
                registrar.registerCommand(getScriptConfig(fileName));
                logger.debug("Registered command: {}", scriptConfig.getName());
            } catch (Exception e) {
                logger.error("Failed to register script '{}' : {}",
                        scriptConfig.getName(),
                        logger.getDebug() ? e : e.getMessage());
            }
        } else {
            logger.info("Skipped disabled script: {}", scriptConfig.getName());
        }
    }
}
