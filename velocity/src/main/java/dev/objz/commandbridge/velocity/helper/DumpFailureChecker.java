package dev.objz.commandbridge.velocity.helper;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.velocity.Main;
import dev.objz.commandbridge.velocity.core.Runtime;
import dev.objz.commandbridge.velocity.util.ProxyUtils;
import dev.objz.commandbridge.core.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DumpFailureChecker implements Runnable {
    private static final String DUMP_API_URL = "http://localhost:8787/api/dump";
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
        Map<String, String> scripts = Runtime.getInstance().getEncoder().getClientsScripts();

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

            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(DUMP_API_URL))
                        .header("Content-Type", "text/plain")
                        .POST(HttpRequest.BodyPublishers.ofString(compressed))
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                int status = resp.statusCode();
                if (status == 429) {
                    logger.warn("Rate limit exceeded when posting dump");
                    source.sendMessage(Component.text("Rate limit exceeded, please try again later")
                            .color(NamedTextColor.RED));
                    return;
                }

                if (status != 200) {
                    throw new IOException("HTTP " + status);
                }

                String body = resp.body().trim();
                int start = body.indexOf("\"id\":\"") + 6;
                int end = body.indexOf("\"", start);
                if (start < 6 || end < start) {
                    throw new IOException("Unexpected API response: " + body);
                }
                String id = body.substring(start, end);

                String link = "https://cb.objz.dev/dump?id=" + id;
                source.sendMessage(Component.text("Your dump is here: ")
                        .append(Component.text("https://cb.objz.dev/dump?id=" + id)
                                .color(NamedTextColor.LIGHT_PURPLE)
                                .decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.openUrl("https://cb.objz.dev/dump?id=" + id))));
                logger.info("dump link: {}", link);

            } catch (IOException | InterruptedException e) {
                logger.error("Failed to POST dump to remote API: {}", e.getMessage());
                source.sendMessage(Component.text("Failed to upload dump: " + e.getMessage())
                        .color(NamedTextColor.RED));
            } finally {
                Runtime.getInstance().getEncoder().clearClientsScripts();
            }

        } else if (attempt >= maxRetries) {
            String missingList = String.join(", ", missing);
            source.sendMessage(Component.text("DumpCommand failed: missing responses from " + missingList)
                    .color(NamedTextColor.RED));
            logger.error("Dump command failed: missing responses from {}", missingList);
            Runtime.getInstance().getEncoder().clearClientsScripts();
        } else {
            // retry after 1 second
            proxy.getScheduler()
                    .buildTask(plugin, this)
                    .delay(1, TimeUnit.SECONDS)
                    .schedule();
        }
    }
}
