package org.commandbridge;

import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;

public final class CommandBridge extends JavaPlugin {
    private final VerboseLogger verboseLogger;
    private final MessageSender messageSender;


    public CommandBridge() {
        this.verboseLogger = new VerboseLogger(this, getLogger());
        this.messageSender = new MessageSender(this);
    }


    @Override
    public void onEnable() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
            getConfig().set("server-id", "REPLACE THIS WITH YOUR SERVER NAME");
            getConfig().set("verbose-output", false);
            saveConfig();
        }
        verboseLogger.loadConfig();
        verboseLogger.info("CommandBridge has been enabled!");
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "commandbridge:main");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "commandbridge:main", new MessageListener(this));
    }

    @Override
    public void onDisable() {
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
        verboseLogger.info("CommandBridge has been disabled!");
    }

    public CommandRegister getCommandRegister() {
        return new CommandRegister(this);
    }

    public VerboseLogger getVerboseLogger() {
        return verboseLogger;
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }



}
