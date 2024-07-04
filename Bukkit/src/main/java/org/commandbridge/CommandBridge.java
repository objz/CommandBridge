package org.commandbridge;

import org.bukkit.plugin.java.JavaPlugin;
import org.commandbridge.command.manager.CommandRegister;
import org.commandbridge.command.manager.CommandUnregister;
import org.commandbridge.message.channel.MessageSender;
import org.commandbridge.runtime.Scripts;
import org.commandbridge.runtime.Startup;
import org.commandbridge.utilities.VerboseLogger;


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
    public void onEnable() {
        startup.onEnable();
    }

    @Override
    public void onDisable() {
        startup.onDisable();
    }

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

    public Scripts getScripts() {
        return new Scripts(this);
    }


}
