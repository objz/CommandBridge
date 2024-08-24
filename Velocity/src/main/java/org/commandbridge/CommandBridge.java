package org.commandbridge;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.commandbridge.message.UUIDManager;
import org.commandbridge.message.channel.MessageSender;
import org.commandbridge.message.channel.MessageListener;
import org.commandbridge.handler.CommandRegistrar;
import org.commandbridge.runtime.Startup;
import org.commandbridge.runtime.VelocityRuntime;
import org.commandbridge.utilities.Metrics;
import org.commandbridge.utilities.VerboseLogger;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Plugin(id = "commandbridge", name = "CommandBridgeVelocity", version = "1.7.1-SNAPSHOT", description = "A plugin to bridge commands between servers", authors = {"72S_"}, url = "https://modrinth.com/plugin/commandbridge")
public class CommandBridge {

    private final ProxyServer server;
    private final VerboseLogger verboseLogger;
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("commandbridge", "main");
    private final List<String> registeredCommands = new ArrayList<>();
    private final VelocityRuntime velocityRuntime;
    private final Startup startup;
    private final Metrics.Factory metricsFactory;
    private String serverId;
    private final UUIDManager uuidManager;


    @Inject
    public CommandBridge(ProxyServer server, Logger logger, Metrics.Factory metricsFactory, UUIDManager uuidManager) {
        this.server = server;
        this.verboseLogger = new VerboseLogger(this, logger);
        this.velocityRuntime = new VelocityRuntime(server, this);
        this.startup = new Startup(server, this);
        this.metricsFactory = metricsFactory;
        this.uuidManager = uuidManager;
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        int pluginId = 22008;
        metricsFactory.make(this, pluginId);
        this.server.getChannelRegistrar().register(CHANNEL);
        this.server.getEventManager().register(this, new MessageListener(this, server));
        EventManager eventManager = server.getEventManager();
        eventManager.register(this, startup);
        startup.loadConfig();
        startup.registerCommands();
        verboseLogger.forceInfo("CommandBridgeVelocity has been enabled!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        server.getChannelRegistrar().unregister(CHANNEL);
        uuidManager.shutdown();
        verboseLogger.forceInfo("CommandBridgeVelocity has been disabled!");
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

    public MessageSender getMessageSender() { return new MessageSender(server, this); }

    public boolean isVerboseOutputEnabled() {
        return startup.isVerboseOutput();
    }

    public Startup getStartup() {
        return startup;
    }

    public String getVersion() {
        return this.getClass().getAnnotation(Plugin.class).version();
    }

    public MinecraftChannelIdentifier getChannelIdentifier() {
        return CHANNEL;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Integer getScript_version() {
        return 2; }

    public Integer getConfig_version() {
        return 2; }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

}
