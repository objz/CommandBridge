package dev.objz.commandbridge.paper.core;

import dev.objz.commandbridge.paper.Main;
import dev.objz.commandbridge.paper.command.CommandRegistrar;
import dev.objz.commandbridge.paper.utils.Encoder;
import dev.objz.commandbridge.paper.utils.GeneralUtils;
import dev.objz.commandbridge.paper.utils.ScriptUtils;
import dev.objz.commandbridge.paper.websocket.Client;
import dev.objz.commandbridge.core.Logger;
import dev.objz.commandbridge.core.utils.ConfigManager;
import dev.objz.commandbridge.paper.command.CommandExecutor;
import dev.objz.commandbridge.paper.command.CommandForwarder;

public class Runtime {
    private static Runtime instance;
    private Logger logger;
    private ConfigManager config;
    private ScriptUtils scriptUtils;
    private Client client;
    private Startup startup;
    private CommandForwarder forwarder;
    private CommandRegistrar registrar;
    private GeneralUtils generalUtils;
    private CommandExecutor commandExecutor;
    private Encoder encoder;

    private Runtime() {
    }

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

    public synchronized Client getClient() {
        if (client == null) {
            client = new Client(getLogger(), getConfig().getKey("config.yml", "secret"));
            getLogger().debug("Server initialized.");
        }
        return client;
    }

    public synchronized Startup getStartup() {
        if (startup == null) {
            startup = new Startup(getLogger());
            getLogger().debug("Startup initialized.");
        }
        return startup;
    }

    public synchronized CommandForwarder getForwarder() {
        if (forwarder == null) {
            forwarder = new CommandForwarder(getLogger(), Main.getInstance());
            getLogger().debug("CommandHelper initialized.");
        }
        return forwarder;
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
    public synchronized Encoder getEncoder() {
        if (encoder == null) {
            encoder = new Encoder();
            getLogger().debug("Encoder initialized.");
        }
        return encoder;
    }
}
