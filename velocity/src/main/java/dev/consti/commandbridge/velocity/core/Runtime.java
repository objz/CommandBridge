package dev.consti.commandbridge.velocity.core;

import dev.consti.commandbridge.velocity.Main;
import dev.consti.commandbridge.velocity.command.CommandExecutor;
import dev.consti.commandbridge.velocity.command.CommandHelper;
import dev.consti.commandbridge.velocity.command.CommandRegistrar;
import dev.consti.commandbridge.velocity.utils.GeneralUtils;
import dev.consti.commandbridge.velocity.utils.ScriptUtils;
import dev.consti.commandbridge.velocity.websocket.Server;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.ConfigManager;

public class Runtime {
    private static Runtime instance;
    private Logger logger;
    private ConfigManager config;
    private ScriptUtils scriptUtils;
    private Server server;
    private Startup startup;
    private CommandHelper helper;
    private CommandRegistrar registrar;
    private GeneralUtils generalUtils;
    private CommandExecutor commandExecutor;

    private Runtime() {}

    public static synchronized Runtime getInstance() {
        if (instance == null) {
            instance = new Runtime();
            instance.getLogger().debug("Runtime singleton instance initialized.");
        }
        return instance;
    }

    public synchronized Logger getLogger() {
        if (logger == null) {
            logger = new Logger("CommandBridge");
            logger.debug("Logger initialized.");
        }
        return logger;
    }

    public synchronized ConfigManager getConfig() {
        if (config == null) {
            config = new ConfigManager(getLogger(), "CommandBridge");
            getLogger().debug("ConfigManager initialized.");
        }
        return config;
    }

    public synchronized ScriptUtils getScriptUtils() {
        if (scriptUtils == null) {
            scriptUtils = new ScriptUtils(getLogger(), "CommandBridge");
            getLogger().debug("ScriptUtils initialized.");
        }
        return scriptUtils;
    }

    public synchronized Server getServer() {
        if (server == null) {
            server = new Server(getLogger(), getConfig().getSecret());
            getLogger().debug("Server initialized.");
        }
        return server;
    }

    public synchronized Startup getStartup() {
        if (startup == null) {
            startup = new Startup(getLogger());
            getLogger().debug("Startup initialized.");
        }
        return startup;
    }

    public synchronized CommandHelper getHelper() {
        if (helper == null) {
            helper = new CommandHelper(getLogger(), Main.getInstance());
            getLogger().debug("CommandHelper initialized.");
        }
        return helper;
    }

    public synchronized CommandRegistrar getRegistrar() {
        if (registrar == null) {
            registrar = new CommandRegistrar(getLogger());
            getLogger().debug("CommandRegistrar initialized.");
        }
        return registrar;
    }

    public synchronized GeneralUtils getGeneralUtils() {
        if (generalUtils == null) {
            generalUtils = new GeneralUtils(getLogger());
            getLogger().debug("GeneralUtils initialized.");
        }
        return generalUtils;
    }

    public synchronized CommandExecutor getCommandExecutor() {
        if (commandExecutor == null) {
            commandExecutor = new CommandExecutor();
            getLogger().debug("CommandExecutor initialized.");
        }
        return commandExecutor;
    }
}
