package dev.objz.commandbridge.velocity.core;

import dev.objz.commandbridge.velocity.Main;
import dev.objz.commandbridge.velocity.command.CommandDispatcher;
import dev.objz.commandbridge.velocity.command.CommandForwarder;
import dev.objz.commandbridge.velocity.command.CommandRegistrar;
import dev.objz.commandbridge.velocity.util.Encoder;
import dev.objz.commandbridge.velocity.util.GeneralUtils;
import dev.objz.commandbridge.velocity.util.ScriptUtils;
import dev.objz.commandbridge.velocity.websocket.HttpServer;
import dev.objz.commandbridge.velocity.websocket.Server;
import dev.objz.commandbridge.core.Logger;
import dev.objz.commandbridge.core.utils.ConfigManager;

public class Runtime {
    private static Runtime instance;
    private Logger logger;
    private ConfigManager config;
    private ScriptUtils scriptUtils;
    private Server server;
    private Startup startup;
    private CommandForwarder helper;
    private CommandRegistrar registrar;
    private GeneralUtils generalUtils;
    private CommandDispatcher commandDispatcher;
    private HttpServer httpServer;
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

    public synchronized CommandForwarder getHelper() {
        if (helper == null) {
            helper = new CommandForwarder(getLogger(), Main.getInstance());
            getLogger().debug("CommandForwarder initialized.");
        }
        return helper;
    }

    public synchronized CommandRegistrar getRegistrar() {
        if (registrar == null) {
            registrar = new CommandRegistrar(getLogger());
            getLogger().debug("InternalRegistrar initialized.");
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

    public synchronized CommandDispatcher getCommandExecutor() {
        if (commandDispatcher == null) {
            commandDispatcher = new CommandDispatcher();
            getLogger().debug("CommandDispatcher initialized.");
        }
        return commandDispatcher;
    }

    public synchronized HttpServer getHttpServer() {
        if (httpServer == null) {
            httpServer = new HttpServer(logger);
            getLogger().debug("HttpServer initialized.");
        }
        return httpServer;
    }

    public synchronized Encoder getEncoder() {
        if (encoder == null) {
            encoder = new Encoder();
            getLogger().debug("Encoder initialized.");
        }
        return encoder;
    }
}
