package dev.consti.commandbridge.velocity.utils;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.consti.commandbridge.velocity.Main;
import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.foundationlib.json.MessageBuilder;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.VersionChecker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class GeneralUtils {
  private final Logger logger;
  private final ProxyServer proxy;
  private final Main plugin;
  private Map<String, String> statusMap = new HashMap<>();
  private Set<String> connectedClients = Runtime.getInstance().getServer().getConnectedClients();

  public GeneralUtils(Logger logger) {
    this.logger = logger;
    this.proxy = ProxyUtils.getProxyServer();
    this.plugin = Main.getInstance();
  }

  public void addClientToStatus(String clientId, String status) {
    statusMap.put(clientId, status);
    logger.debug("Updated statusMap: {}", statusMap);
  }

  private String checkForFailures() {
    logger.debug("Checking for failures...");

    // Check for missing clients
    List<String> missingClients =
        connectedClients.stream().filter(client -> !statusMap.containsKey(client)).toList();

    if (!missingClients.isEmpty()) {
      String missingClientString = String.join(", ", missingClients);
      logger.warn("Waiting for responses from clients: {}", missingClientString);
      return "Missing responses from: " + missingClientString;
    }

    // Check for clients with a "failure" status
    String failedClients =
        statusMap.entrySet().stream()
            .filter(entry -> !"success".equals(entry.getValue())) // Check for non-success statuses
            .map(Map.Entry::getKey)
            .reduce((a, b) -> a + ", " + b)
            .orElse(null);

    if (failedClients != null) {
      logger.error("Failure detected on clients: {}", failedClients);
      return "Failure detected on: " + failedClients;
    }

    // All clients are operational
    logger.debug("No failures detected. All clients are operational.");
    return null;
  }

  private void startFailureCheck(CommandSource source) {
    statusMap.clear(); // Clear previous statuses to start fresh

    final int maxRetries = 8;
    final int[] retries = {0}; // Use an array to allow modification in the lambda

    Runnable checkTask =
        new Runnable() {
          @Override
          public void run() {
            retries[0]++;
            logger.debug("Failure check attempt {}/{}", retries[0], maxRetries);

            try {
              // Check for missing or failed clients
              String failedClients = checkForFailures();

              if (failedClients == null) {
                // All clients responded successfully
                source.sendMessage(
                    Component.text("Everything has reloaded successfully!")
                        .color(NamedTextColor.GREEN));
                logger.info("Scripts reloaded successfully.");
                statusMap.clear(); // Clear statuses after success
              } else if (retries[0] >= maxRetries) {
                // Timeout after 8 seconds
                source.sendMessage(
                    Component.text("Reload failed: " + failedClients).color(NamedTextColor.RED));
                logger.error("Reload failed: {}", failedClients);
                statusMap.clear(); // Clear statuses after failure
              } else {
                // Reschedule the check with a 1-second delay
                proxy.getScheduler().buildTask(plugin, this).delay(1, TimeUnit.SECONDS).schedule();
                return; // Exit early to avoid stopping the scheduler
              }
            } catch (Exception e) {
              // Handle any unexpected exceptions
              logger.error("An error occurred during the reload process: {}", e.getMessage(), e);
              source.sendMessage(
                  Component.text("Reload failed due to an internal error. Check logs for details.")
                      .color(NamedTextColor.RED));
            }
          }
        };

    try {
      // Start the first check after a short delay
      proxy.getScheduler().buildTask(plugin, checkTask).delay(1, TimeUnit.SECONDS).schedule();
    } catch (Exception e) {
      // Handle exceptions during task scheduling
      logger.error("Failed to schedule the reload check task: {}", e.getMessage(), e);
      source.sendMessage(
          Component.text("Reload failed: Unable to start the failure check.")
              .color(NamedTextColor.RED));
    }
  }

  public void registerCommands() {
    logger.info("Registering commands for CommandBridge...");
    try {
      LiteralCommandNode<CommandSource> commandBridgeNode =
          LiteralArgumentBuilder.<CommandSource>literal("commandbridge")
              .executes(
                  context -> {
                    if (context.getSource().hasPermission("commandbridge.admin")) {
                      logger.debug("Help command executed by: {}", context.getSource());
                      return sendHelpMessage(context);
                    }
                    context
                        .getSource()
                        .sendMessage(
                            Component.text(
                                "You do not have permission to use this command.",
                                NamedTextColor.RED));
                    return 0;
                  })
              .then(
                  LiteralArgumentBuilder.<CommandSource>literal("reload")
                      .executes(
                          context -> {
                            CommandSource source = context.getSource();
                            if (source.hasPermission("commandbridge.admin")) {
                              try {
                                // Attempt to unregister commands
                                Runtime.getInstance().getRegistrar().unregisterAllCommands();
                                logger.debug("All commands have been unregistered.");

                                // Attempt to reload configurations
                                Runtime.getInstance().getConfig().reload();
                                logger.debug("Configuration files have been reloaded.");

                                // Attempt to reload scripts
                                Runtime.getInstance().getScriptUtils().reload();
                                logger.debug("Scripts have been reloaded.");

                                // Notify clients about the reload
                                MessageBuilder builder = new MessageBuilder("system");
                                builder.addToBody("channel", "command");
                                builder.addToBody("command", "reload");
                                Runtime.getInstance()
                                    .getServer()
                                    .broadcastServerMessage(builder.build());

                                // Notify the command source about the process
                                source.sendMessage(
                                    Component.text("Waiting for clients to respond...")
                                        .color(NamedTextColor.YELLOW));
                                logger.debug("Waiting for clients to respond...");
                                startFailureCheck(source);

                              } catch (Exception e) {
                                // Log the error and notify the command source
                                logger.error(
                                    "An error occurred during the reload process: {}",
                                    e.getMessage(),
                                    e);
                                source.sendMessage(
                                    Component.text(
                                            "Reload failed due to an internal error. Check logs for"
                                                + " details.")
                                        .color(NamedTextColor.RED));
                              }
                              return 1;
                            }
                            source.sendMessage(
                                Component.text("You do not have permission to reload scripts.")
                                    .color(NamedTextColor.RED));
                            return 0;
                          })
                      .build())
              .then(
                  LiteralArgumentBuilder.<CommandSource>literal("version")
                      .executes(
                          context -> {
                            if (context.getSource().hasPermission("commandbridge.admin")) {
                              CommandSource source = context.getSource();
                              String currentVersion = Main.getVersion();

                              source.sendMessage(
                                  Component.text("Checking for updates...")
                                      .color(NamedTextColor.YELLOW));
                              logger.debug("Version command executed by: {}", source);

                              new Thread(
                                      () -> {
                                        String latestVersion = VersionChecker.getLatestVersion();

                                        if (latestVersion == null) {
                                          source.sendMessage(
                                              Component.text("Unable to check for updates.")
                                                  .color(NamedTextColor.RED));
                                          logger.warn(
                                              "Failed to retrieve latest version for update"
                                                  + " check.");
                                          return;
                                        }

                                        logger.debug(
                                            "Current version: {}, Latest version: {}",
                                            currentVersion,
                                            latestVersion);

                                        if (VersionChecker.isNewerVersion(
                                            latestVersion, currentVersion)) {
                                          source.sendMessage(
                                              Component.text(
                                                      "A new version is available: "
                                                          + latestVersion)
                                                  .color(NamedTextColor.RED));
                                          source.sendMessage(
                                              Component.text("Please download the latest release: ")
                                                  .append(
                                                      Component.text("here")
                                                          .color(NamedTextColor.BLUE)
                                                          .decorate(TextDecoration.UNDERLINED)
                                                          .clickEvent(
                                                              ClickEvent.openUrl(
                                                                  VersionChecker
                                                                      .getDownloadUrl()))));
                                          logger.warn(
                                              "A newer version is available: {}", latestVersion);
                                        } else {
                                          source.sendMessage(
                                              Component.text(
                                                      "You are running the latest version: "
                                                          + currentVersion)
                                                  .color(NamedTextColor.GREEN));
                                        }
                                      })
                                  .start();

                              return 1;
                            }
                            context
                                .getSource()
                                .sendMessage(
                                    Component.text(
                                        "You do not have permission to check the version.",
                                        NamedTextColor.RED));
                            return 0;
                          })
                      .build())
              .then(
                  LiteralArgumentBuilder.<CommandSource>literal("help")
                      .executes(this::sendHelpMessage)
                      .build())
              .then(
                  LiteralArgumentBuilder.<CommandSource>literal("list")
                      .executes(this::listServers)
                      .build())
              .then(
                  LiteralArgumentBuilder.<CommandSource>literal("stop")
                      .executes(this::stopProxy)
                      .build())
              .build();

      CommandMeta commandMeta =
          proxy
              .getCommandManager()
              .metaBuilder("commandbridge")
              .aliases("cb")
              .plugin(plugin)
              .build();

      BrigadierCommand brigadierCommand = new BrigadierCommand(commandBridgeNode);
      proxy.getCommandManager().register(commandMeta, brigadierCommand);
      logger.info("CommandBridge commands registered successfully.");
    } catch (Exception e) {
      logger.error("Failed to register CommandBridge commands: {}", e.getMessage(), e);
    }
  }

  private int listServers(CommandContext<CommandSource> context) {
    CommandSource source = context.getSource();
    if (connectedClients.isEmpty()) {
      source.sendMessage(
          Component.text("No clients are currently connected.").color(NamedTextColor.RED));
    } else {
      // Convert the list of clients to a single string, separated by commas
      String clientsString = String.join(", ", connectedClients);

      // Send the message with the connected clients
      source.sendMessage(
          Component.text("===== Connected Clients =====").color(NamedTextColor.GOLD));
      source.sendMessage(Component.text(clientsString).color(NamedTextColor.GREEN));
      source.sendMessage(Component.text("============================").color(NamedTextColor.GOLD));
    }

    return 1;
  }

  private int stopProxy(CommandContext<CommandSource> context) {
    CommandSource source = context.getSource();

    Runtime.getInstance().getStartup().stop();

    source.sendMessage(Component.text("Websocket Server stopped.").color(NamedTextColor.YELLOW));
    return 1;
  }

  private int sendHelpMessage(CommandContext<CommandSource> context) {
    CommandSource source = context.getSource();
    logger.debug("Sending help message to: {}", source);

    source.sendMessage(Component.text("===== CommandBridge Help =====").color(NamedTextColor.GOLD));
    source.sendMessage(Component.text(""));

    source.sendMessage(Component.text("Commands:").color(NamedTextColor.YELLOW));
    source.sendMessage(
        Component.text("  - ")
            .append(Component.text("/commandbridge reload").color(NamedTextColor.GREEN))
            .append(Component.text(" - Reloads scripts").color(NamedTextColor.WHITE)));
    source.sendMessage(
        Component.text("  - ")
            .append(Component.text("/commandbridge version").color(NamedTextColor.GREEN))
            .append(Component.text(" - Displays the plugin version").color(NamedTextColor.WHITE)));
    source.sendMessage(
        Component.text("  - ")
            .append(Component.text("/commandbridge help").color(NamedTextColor.GREEN))
            .append(Component.text(" - Displays this help message").color(NamedTextColor.WHITE)));
    source.sendMessage(Component.text(""));

    source.sendMessage(
        Component.text("Detailed Documentation: ")
            .append(
                Component.text("https://72-s.github.io/CommandBridge/")
                    .color(NamedTextColor.LIGHT_PURPLE)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.openUrl("https://72-s.github.io/CommandBridge/"))));

    source.sendMessage(Component.text("============================").color(NamedTextColor.GOLD));
    return 1;
  }
}
