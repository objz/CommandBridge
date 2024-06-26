package org.commandbridge;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.commandbridge.message.channel.Bridge;
import org.commandbridge.message.channel.MessageListener;
import org.commandbridge.command.utils.CommandRegistrar;
import org.commandbridge.runtime.Startup;
import org.commandbridge.runtime.VelocityRuntime;
import org.commandbridge.utilities.Metrics;
import org.commandbridge.utilities.VerboseLogger;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Plugin(id = "commandbridge", name = "CommandBridgeVelocity", version = "1.3-SNAPSHOT", description = "A plugin to bridge commands between servers", authors = {"72S_"}, url = "https://modrinth.com/plugin/commandbridge")
public class CommandBridge {

    private final ProxyServer server;
    private final VerboseLogger verboseLogger;
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("commandbridge", "main");
    private final List<String> registeredCommands = new ArrayList<>();
    private final VelocityRuntime velocityRuntime;
    private final Startup startup;
    private final Metrics.Factory metricsFactory;

    @Inject
    public CommandBridge(ProxyServer server, Logger logger, Metrics.Factory metricsFactory) {
        this.server = server;
        this.verboseLogger = new VerboseLogger(this, logger);
        this.velocityRuntime = new VelocityRuntime(server, this);
        this.startup = new Startup(server, this);
        this.metricsFactory = metricsFactory;
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        int pluginId = 22008;
        Metrics metrics = metricsFactory.make(this, pluginId);

        this.server.getChannelRegistrar().register(CHANNEL);
        server.getEventManager().register(this, new MessageListener(server, this));
        startup.loadConfig();
        startup.registerCommands();
        verboseLogger.ForceInfo("CommandBridgeVelocity has been enabled!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        server.getChannelRegistrar().unregister(CHANNEL);
        verboseLogger.ForceInfo("CommandBridgeVelocity has been disabled!");
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

    public VelocityRuntime getRuntime() {
        return velocityRuntime;
    }

    public CommandRegistrar getCommandRegistrar() {
        return new CommandRegistrar(server, this);
    }

    public Bridge getBridge() { return new Bridge(server, this); }

    public boolean isVerboseOutputEnabled() {
        return startup.isVerboseOutput();
    }

    public Startup getStartup() {
        return startup;
    }

    public MinecraftChannelIdentifier getChannelIdentifier() {
        return CHANNEL;
    }
}
