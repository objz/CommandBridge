package dev.objz.commandbridge.velocity.helper;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.velocity.Main;
import dev.objz.commandbridge.velocity.core.Runtime;
import dev.objz.commandbridge.velocity.util.ProxyUtils;
import dev.objz.commandbridge.core.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DumpFailureChecker implements Runnable {
    private final Logger logger;
    private final ProxyServer proxy;
    private final Main plugin;
    private final CommandSource source;
    private final int maxRetries;
    private int attempt = 0;

    public DumpFailureChecker(CommandSource source, Logger logger) {
        this.source = source;
        this.logger = logger;
        this.proxy = ProxyUtils.getProxyServer();
        this.plugin = Main.getInstance();
        this.maxRetries = Integer.parseInt(
            Runtime.getInstance().getConfig().getKey("config.yml", "timeout"));
    }

    @Override
    public void run() {
        attempt++;
        logger.debug("Dump check attempt {}/{}", attempt, maxRetries);

        Set<String> connected = Runtime.getInstance().getServer().getConnectedClients();
        Map<String, String> scripts =
            Runtime.getInstance().getEncoder().getClientsScripts();

        Set<String> missing = connected.stream()
            .filter(client -> !scripts.containsKey(client))
            .collect(Collectors.toSet());

        if (missing.isEmpty()) {
            String compact = Runtime.getInstance().getEncoder().encode();
            String compressed;
            try {
                compressed = Runtime.getInstance().getEncoder().compress(compact);
            } catch (Exception e) {
                logger.error("Failed to compress aggregated dump: {}", logger.getDebug() ? e : e.getMessage());
                source.sendMessage(Component.text("Error while compressing dump data")
                    .color(NamedTextColor.RED));
                return;
            }

            source.sendMessage(Component.text(compact).color(NamedTextColor.LIGHT_PURPLE));
            source.sendMessage(Component.text("===== Dumped Data =======")
                .color(NamedTextColor.GOLD));
            source.sendMessage(Component.text(compressed).color(NamedTextColor.GREEN));
            source.sendMessage(Component.text("============================")
                .color(NamedTextColor.GOLD));

            Runtime.getInstance().getEncoder().clearClientsScripts();

        } else if (attempt >= maxRetries) {
            String missingList = String.join(", ", missing);
            source.sendMessage(Component.text("DumpCommand failed: missing responses from " + missingList)
                .color(NamedTextColor.RED));
            logger.error("Dump command failed: missing responses from {}", missingList);
            Runtime.getInstance().getEncoder().clearClientsScripts();
        } else {
            proxy.getScheduler()
                 .buildTask(plugin, this)
                 .delay(1, TimeUnit.SECONDS)
                 .schedule();
        }
    }
}
