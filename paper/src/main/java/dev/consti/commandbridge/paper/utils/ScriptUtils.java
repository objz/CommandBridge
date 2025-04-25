package dev.consti.commandbridge.paper.utils;

import dev.consti.commandbridge.paper.Main;
import dev.consti.commandbridge.paper.command.CommandRegistrar;
import dev.consti.commandbridge.paper.core.Runtime;
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
            logger.info("Loaded script: {}", fileName);
            try {
                registrar.registerCommand(getScriptConfig(fileName));
                logger.debug("Registered command: {}", scriptConfig.getName());
            } catch (Exception e) {
                logger.error("Failed to register script '{}' : {}",
                        scriptConfig.getName(),
                        logger.getDebug() ? e : e.getMessage()
                );
            }
        } else {
            logger.info("Skipped disabled script: {}", scriptConfig.getName());
        }
    }

    public void unloadCommands(Runnable callback) {
        new SchedulerAdapter(Main.getInstance())
                .run(
                        () -> {
                            logger.debug("Running on thread (unload): {}", Thread.currentThread().getName());
                            Runtime.getInstance().getRegistrar().unregisterAllCommands();
                            logger.debug("All commands have been unloaded");
                            callback.run();
                });
    }




}
