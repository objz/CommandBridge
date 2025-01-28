package dev.consti.commandbridge.velocity.helper;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.consti.commandbridge.velocity.Main;
import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.foundationlib.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.TimeUnit;

public class FailureChecker implements Runnable {
    private final Logger logger;
    private final ProxyServer proxy;
    private final Main plugin;
    private final StatusManager statusManager;
    private final CommandSource source;
    private final int maxRetries;
    private final int[] retries;

    public FailureChecker(Logger logger, ProxyServer proxy, Main plugin, StatusManager statusManager, CommandSource source) {
        this.logger = logger;
        this.proxy = proxy;
        this.plugin = plugin;
        this.statusManager = statusManager;
        this.source = source;
        this.maxRetries = Integer.parseInt(Runtime.getInstance().getConfig().getKey("config.yml", "timeout"));
        this.retries = new int[]{0};
    }

    @Override
    public void run() {
        retries[0]++;
        logger.debug("Failure check attempt {}/{}", retries[0], maxRetries);

        try {
            String failedClients = statusManager.checkForFailures();

            if (failedClients == null) {
                source.sendMessage(
                        Component.text("Everything has reloaded successfully!")
                                .color(NamedTextColor.GREEN)
                );
                logger.info("Scripts reloaded successfully");
                statusManager.clearStatusMap();
            } else if (retries[0] >= maxRetries) {
                // Timeout after 8 seconds
                source.sendMessage(
                        Component.text("ReloadCommand failed: " + failedClients)
                                .color(NamedTextColor.RED)
                );
                logger.error("Reload command failed '{}'", failedClients);
                statusManager.clearStatusMap();
            } else {
                proxy.getScheduler().buildTask(plugin, this).delay(1, TimeUnit.SECONDS).schedule();
            }
        } catch (Exception e) {
            logger.error("An error occurred during the reload process: {}", logger.getDebug() ? e : e.getMessage());
            source.sendMessage(
                    Component.text("Reload command failed due to an internal error. Check logs for details")
                            .color(NamedTextColor.RED)
            );
        }
    }
}
