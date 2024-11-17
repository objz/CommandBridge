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
import dev.consti.velocity.utils.Helper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;


@Plugin(id = "commandbridge", name = "CommandBridge", version = "2.0.0", authors = "72-S")


public class Main{
    private final Startup startup = Startup.getInstance();
    private final ProxyServer server;
    
    @Inject
    public Main(ProxyServer server) {
        this.server = server;
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        startup.start();
        Helper helper = new Helper(server, this);
        helper.registerCommands();
    }    
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        startup.stop();
    }

    public static String getVersion() {
        return Main.class.getAnnotation(Plugin.class).version();
    }


    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Logger logger = Startup.getInstance().getLogger();
        Player player = event.getPlayer();

        if (player == null) {
            logger.warn("Player object is null");
            return;
        }

        if (player.hasPermission("commandbridge.admin")) {
            logger.debug("Checking for updates...");

            server.getScheduler().buildTask(this, () -> {
                String currentVersion = Main.getVersion();
                String latestVersion = VersionChecker.getLatestVersion();

                if (latestVersion == null) {
                    player.sendMessage(Component.text("Unable to check for updates.").color(NamedTextColor.RED));
                    logger.warn("Unable to check for updates: latestVersion is null.");
                    return;
                }

                if (VersionChecker.isNewerVersion(latestVersion, currentVersion)) {
                    player.sendMessage(Component.text("A new version of CommandBridge is available: " + latestVersion).color(NamedTextColor.RED));
                    player.sendMessage(Component.text("Please download the latest release: ")
                            .append(Component.text("here")
                                    .color(NamedTextColor.BLUE)
                                    .decorate(TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.openUrl(VersionChecker.getDownloadUrl()))));
                    logger.debug("Notified player " + player.getUsername() + " about the new version: " + latestVersion);
                } else {
                    logger.debug("Player " + player.getUsername() + " is running the latest version: " + currentVersion);
                }
            }).schedule();
        }

    }

}
