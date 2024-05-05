package org.commandbridge;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Plugin(id = "commandbridge", name = "CommandBridgeVelocity", version = "1.0-SNAPSHOT", description = "A plugin to bridge commands between servers", authors = {"72S_"}, url = "https://github.com/72-S")
public class CommandBridge {

    private final ProxyServer server;
    private final VerboseLogger verboseLogger;
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("commandbridge", "main");
    private final List<String> registeredCommands = new ArrayList<>();
    private final Runtime runtime;
    private final Startup startup;

    @Inject
    public CommandBridge(ProxyServer server, Logger logger) {
        this.server = server;
        this.verboseLogger = new VerboseLogger(this, logger);
        this.runtime = new Runtime(server, this);
        this.startup = new Startup(server, this);
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.server.getChannelRegistrar().register(CHANNEL);
        startup.loadConfig();
        startup.registerCommands();
        verboseLogger.info("CommandBridgeVelocity has been enabled!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        server.getChannelRegistrar().unregister(CHANNEL);
        verboseLogger.info("CommandBridgeVelocity has been disabled!");
    }

    public VerboseLogger getVerboseLogger() {
        return verboseLogger;
    }

    public List<String> getRegisteredCommands() {
        return registeredCommands;
    }

    public void addRegisteredCommand(String command) {
        if (!registeredCommands.contains(command)) {
            registeredCommands.add(command);
        }
    }

    public void clearRegisteredCommands() {
        registeredCommands.clear();
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public CommandRegistrar getCommandRegistrar() {
        return new CommandRegistrar(server, this);
    }

    public boolean isVerboseOutputEnabled() {
        return startup.isVerboseOutput();
    }

    public Startup getStartup() {
        return startup;
    }
}
