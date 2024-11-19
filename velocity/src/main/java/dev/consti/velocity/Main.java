package dev.consti.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.consti.logging.Logger;
import dev.consti.utils.VersionChecker;
import dev.consti.velocity.core.Runtime;
import dev.consti.velocity.utils.GeneralUtils;
import dev.consti.velocity.utils.ProxyUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

@Plugin(id = "commandbridge", name = "CommandBridge", version = "2.0.0", authors = "72-S")
public class Main {
    private static Main instance;
    private Runtime runtime;
    private final ProxyServer proxy;

    @Inject
    public Main(ProxyServer proxy) {
        this.proxy = proxy;
        instance = this;
        ProxyUtils.setProxyServer(proxy);
    }

    public static Main getInstance() {
        return instance;
    }

    public static String getVersion() {
        return Main.class.getAnnotation(Plugin.class).version();
    }

    private Logger getLogger() {
        return Runtime.getInstance().getLogger();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        getLogger().info("Initializing CommandBridge...");
        runtime = Runtime.getInstance();
        try {
            runtime.getStartup().start();
            getLogger().info("CommandBridge initialized successfully.");
        } catch (Exception e) {
            getLogger().error("Failed to initialize CommandBridge: {}", e.getMessage(), e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        getLogger().info("Stopping CommandBridge...");
        try {
            runtime.getStartup().stop();
            getLogger().info("CommandBridge stopped successfully.");
        } catch (Exception e) {
            getLogger().error("Failed to stop CommandBridge: {}", e.getMessage(), e);
        }
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            getLogger().warn("PostLoginEvent triggered with a null player object.");
            return;
        }

        if (player.hasPermission("commandbridge.admin")) {
            getLogger().debug("Player {} has admin permissions. Checking for updates...", player.getUsername());

            proxy.getScheduler().buildTask(this, () -> {
                String currentVersion = Main.getVersion();
                String latestVersion = VersionChecker.getLatestVersion();

                if (latestVersion == null) {
                    player.sendMessage(Component.text("Unable to check for updates.").color(NamedTextColor.RED));
                    getLogger().warn("Update check failed: Unable to retrieve the latest version.");
                    return;
                }

                if (VersionChecker.isNewerVersion(latestVersion, currentVersion)) {
                    player.sendMessage(Component.text("A new version of CommandBridge is available: " + latestVersion).color(NamedTextColor.RED));
                    player.sendMessage(Component.text("Please download the latest release: ")
                            .append(Component.text("here")
                                    .color(NamedTextColor.BLUE)
                                    .decorate(TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.openUrl(VersionChecker.getDownloadUrl()))));
                    getLogger().info("Notified player {} about the new version: {}", player.getUsername(), latestVersion);
                } else {
                    getLogger().info("Player {} is running the latest version: {}", player.getUsername(), currentVersion);
                }
            }).schedule();
        } else {
            getLogger().debug("Player {} does not have admin permissions. No update check performed.", player.getUsername());
        }
    }
}
