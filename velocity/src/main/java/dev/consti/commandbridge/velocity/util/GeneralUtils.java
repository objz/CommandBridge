package dev.consti.commandbridge.velocity.util;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.consti.commandbridge.velocity.Main;
import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.commandbridge.velocity.helper.InternalRegistrar;
import dev.consti.commandbridge.velocity.helper.FailureChecker;
import dev.consti.commandbridge.velocity.helper.StatusManager;
import dev.consti.foundationlib.logging.Logger;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GeneralUtils {
    private final Logger logger;
    private final ProxyServer proxy;
    private final Main plugin;
    private final StatusManager statusManager;
    private final Set<String> connectedClients;
    private CommandMeta meta;

    public GeneralUtils(Logger logger) {
        this.logger = logger;
        this.proxy = ProxyUtils.getProxyServer();
        this.plugin = Main.getInstance();
        this.connectedClients = Runtime.getInstance().getServer().getConnectedClients();
        this.statusManager = new StatusManager(logger);
    }

    public void addClientToStatus(String clientId, String status) {
        statusManager.addClientToStatus(clientId, status);
    }

    public void startFailureCheck(CommandSource source) {
        statusManager.clearStatusMap();
        FailureChecker checkTask = new FailureChecker(logger, proxy, plugin, statusManager, source);
        try {
            proxy.getScheduler().buildTask(plugin, checkTask).delay(1, TimeUnit.SECONDS).schedule();
        } catch (Exception e) {
            logger.error("Failed to schedule the reload check task: {}", logger.getDebug() ? e : e.getMessage());
            source.sendMessage(
                    net.kyori.adventure.text.Component.text("Reload command failed: Unable to start the failure check")
                            .color(net.kyori.adventure.text.format.NamedTextColor.RED));
        }
    }

    public void registerCommands() {
        InternalRegistrar registrar = new InternalRegistrar(logger, proxy, plugin, connectedClients);
        registrar.registerCommands();
    }

    public CommandMeta getMeta() {
        return meta;
    }

    public void setMeta(CommandMeta commandMeta) {
        meta = commandMeta;
    }

    public void unregisterCommands() {
        proxy.getCommandManager().unregister(meta);
    }
}
