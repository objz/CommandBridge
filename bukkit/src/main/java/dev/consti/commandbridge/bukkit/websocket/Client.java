package dev.consti.commandbridge.bukkit.websocket;

import dev.consti.commandbridge.bukkit.Main;
import dev.consti.commandbridge.bukkit.core.Runtime;
import dev.consti.foundationlib.json.MessageBuilder;
import dev.consti.foundationlib.json.MessageParser;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.websocket.SimpleWebSocketClient;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Client extends SimpleWebSocketClient {
  private final Logger logger;

  public Client(Logger logger, String secret) {
    super(logger, secret);
    this.logger = logger;
  }

  @Override
  protected void onMessage(String message) {
    MessageParser parser = new MessageParser(message);
    logger.debug("Received message: {}", message);
    try {
      String type = parser.getType();
      logger.info("Processing message of type: {}", type);
      switch (type) {
        case "command":
          handleCommandRequest(message);
          break;
        case "system":
          handleSystemRequest(message);
          break;
        default:
          logger.warn("Unknown message type received: {}", type);
          sendError("Unknown message type");
      }
    } catch (Exception e) {
      logger.error("Error while processing message: {}: {}", e.getMessage(), e);
      sendError("Internal client error");
    }
  }

  @Override
  protected void afterAuth() {
    logger.debug("Sending server information's...");
    MessageBuilder builder = new MessageBuilder("system");
    builder.addToBody("channel", "name");
    builder.addToBody("name", Runtime.getInstance().getConfig().getKey("config.yml", "client-id"));
    logger.debug("Payload: {}", builder.build().toString());
    sendMessage(builder.build());
  }

  private void handleCommandRequest(String message) {
    logger.debug("Handling command response: {}", message);
    Runtime.getInstance().getCommandExecutor().dispatchCommand(message);
  }

  private void handleSystemRequest(String message) {
    logger.debug("Handling system request.");
    MessageParser parser = new MessageParser(message);
    String channel = parser.getBodyValueAsString("channel");
    String status = parser.getStatus();

    if (channel.equals("error")) {
      logger.warn("Error Message from server: {}", status);
    } else if (channel.equals("info")) {
      logger.info("Info from server: {}", status);
    } else if (channel.equals("command")) {
      systemCommand(parser);
    }
  }

  private void systemCommand(MessageParser parser) {
    if (parser.getBodyValueAsString("command").equals("reload")) {
      logger.debug("Running on thread: {}", Thread.currentThread().getName());
      unloadCommands(
          () -> {
            Bukkit.getScheduler()
                .runTaskLater(Main.getInstance(), this::reloadConfigsAndScripts, 10L);
          });
    }
  }

  private void unloadCommands(Runnable callback) {
    Bukkit.getScheduler()
        .runTask(
            Main.getInstance(),
            () -> {
              logger.debug("Running on thread (unload): {}", Thread.currentThread().getName());
              Runtime.getInstance().getRegistrar().unregisterAllCommands();
              logger.debug("All commands have been unloaded");
              callback.run();
            });
  }


private void reloadConfigsAndScripts() {
    Bukkit.getScheduler()
        .runTask(
            Main.getInstance(),
            () -> {
                logger.debug("Running on thread (reload): {}", Thread.currentThread().getName());
                try {
                    Runtime.getInstance().getConfig().reload();
                    logger.debug("All configs have been reloaded");
                    Runtime.getInstance().getScriptUtils().reload();
                    logger.debug("All scripts have been reloaded");
                    logger.info("Everything Reloaded!");

                    // Send success message
                    MessageBuilder builder = new MessageBuilder("system");
                    builder.addToBody("channel", "command");
                    builder.addToBody("command", "reload");
                    builder.addToBody("client-id", Runtime.getInstance().getConfig().getKey("config.yml", "client-id"));
                    builder.withStatus("success");
                    Runtime.getInstance().getClient().sendMessage(builder.build());
                } catch (Exception e) {
                    logger.error("Error occurred while reloading: {}", e.getMessage(), e);

                    // Send failure message
                    MessageBuilder builder = new MessageBuilder("system");
                    builder.addToBody("channel", "command");
                    builder.addToBody("command", "reload");
                    builder.addToBody("client-id", Runtime.getInstance().getConfig().getKey("config.yml", "client-id"));
                    builder.withStatus("failure");
                    builder.addToBody("error", e.getMessage());
                    Runtime.getInstance().getClient().sendMessage(builder.build());
                }
            });
}


  private void sendError(String errorMessage) {
    MessageBuilder builder = new MessageBuilder("system");
    builder.addToBody("channel", "error");
    builder.withStatus(errorMessage);
    sendMessage(builder.build());
  }

  public void sendJSON(
      String command, String server, String[] arguments, Player executor, String target) {
    MessageBuilder builder = new MessageBuilder("command");
    builder.addToBody("command", command);
    builder.addToBody("server", server);
    builder.addToBody("arguments", arguments);
    builder.addToBody("target", target);

    if (target.equals("player")) {
      builder.addToBody("name", executor.getName());
      builder.addToBody("uuid", executor.getUniqueId());
    }
    logger.info("Sending JSON command to server: {}", server);
    logger.debug("Payload: {}", builder.build().toString());
    sendMessage(builder.build());
  }
}
