package dev.consti.bukkit.utils;

import dev.consti.logging.Logger;
import dev.consti.utils.ScriptManager;

public class onScript extends ScriptManager {
    private final Logger logger;

    public onScript(Logger logger, String pluginName) {
        super(logger, pluginName);
        this.logger = logger;
    }

    @Override
    public void onFileProcessed(String fileName, ScriptConfig scriptConfig) {

        if (scriptConfig.isEnabled()) {
            logger.info("Loaded Script: {}", scriptConfig.getName());
        }

    }


}
