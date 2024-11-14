package dev.consti.velocity.utils;

import dev.consti.logging.Logger;
import dev.consti.utils.ScriptManager;

public class Script extends ScriptManager {
    private final Logger logger;

    public Script(Logger logger, String pluginName) {
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
