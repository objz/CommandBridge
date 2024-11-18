package dev.consti.velocity.utils;

import dev.consti.logging.Logger;
import dev.consti.utils.ScriptManager;
import dev.consti.velocity.command.Builder;

public class onScript extends ScriptManager {
    private final Logger logger;
    private final Builder command;


    public onScript(Logger logger, String pluginName) {
        super(logger, pluginName);
        this.logger = logger;
        this.command = new Builder();
    }

    @Override
    public void onFileProcessed(String fileName, ScriptConfig scriptConfig) {

        if (scriptConfig.isEnabled()) {
            logger.info("Loaded Script: {}", scriptConfig.getName());
            command.registerCommand(getScriptConfig(fileName));
        } else {
            logger.debug("Skipped file: {}", scriptConfig.getName());
        }

    }


}
