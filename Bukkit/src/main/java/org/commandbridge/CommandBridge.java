package org.commandbridge;

import org.bukkit.plugin.java.JavaPlugin;
import org.commandbridge.core.handler.CommandRegister;
import org.commandbridge.core.handler.CommandUnregister;
import org.commandbridge.core.channel.MessageSender;
import org.commandbridge.core.runtime.ScriptHandler;
import org.commandbridge.core.Startup;
import org.commandbridge.core.utilities.VerboseLogger;


import java.util.ArrayList;
import java.util.List;

public final class CommandBridge extends JavaPlugin {
    private final VerboseLogger verboseLogger;
    private final MessageSender messageSender;
    private final Startup startup;
    private final List<String> registeredCommands = new ArrayList<>();


    public CommandBridge() {
        this.verboseLogger = new VerboseLogger(this, getLogger());
        this.messageSender = new MessageSender(this);
        this.startup = new Startup(this);
    }

    @Override
    public void onLoad() {
        startup.onLoad();
    }

    @Override
    public void onEnable() {
        startup.onEnable();

    }

    @Override
    public void onDisable() {
        startup.onDisable();
    }


    /*
    private void initializeVersionHandler() {
        String version = getServer().getVersion();
        try {
            Class<?> clazz = null;
            if (version.startsWith("1.20")) {
                clazz = Class.forName("org.commandbridge.version.v1_20.VersionHandlerImpl");
                verboseLogger.info("Using 1.20 version handler");
            } else if (version.startsWith("1.21")) {
                clazz = Class.forName("org.commandbridge.version.v1_21.VersionHandlerImpl");
                verboseLogger.info("Using 1.21 version handler");
            } else {
                verboseLogger.warn("Unsupported server version: " + version);
            }

            if (clazz != null) {
                // Pass an instance of VerboseLogger to the constructor
                versionHandler = (VersionSpecificHandler) clazz.getConstructor(VerboseLogger.class).newInstance(verboseLogger);
            }
        } catch (Exception e) {
            verboseLogger.error("Failed to initialize version handler: ", e);
            throw new RuntimeException(e);
        }
    } */



    public CommandRegister getCommandRegister() {
        return new CommandRegister(this);
    }

    public CommandUnregister getCommandUnregister() {
        return new CommandUnregister(this);
    }

    public VerboseLogger getVerboseLogger() {
        return verboseLogger;
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    public List<String> getRegisteredCommands() {
        return registeredCommands;
    }

    public void clearRegisteredCommands() {
        registeredCommands.clear();
    }

    public void addRegisteredCommand(String command) {
        if (!registeredCommands.contains(command)) {
            registeredCommands.add(command);
        }
    }

    public ScriptHandler getScripts() {
        return new ScriptHandler(this);
    }

    public String getBukkitVersion() {
        return this.getServer().getBukkitVersion();
    }

    public Integer getConfig_version() {
        return 2;
    }

    public Integer getScript_version() {
        return 3;
    }

}
