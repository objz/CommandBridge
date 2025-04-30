package dev.consti.commandbridge.paper.websocket;

import dev.consti.commandbridge.paper.Main;
import dev.consti.commandbridge.paper.core.Runtime;
import dev.consti.commandbridge.paper.utils.SchedulerAdapter;
import dev.consti.foundationlib.json.MessageBuilder;
import dev.consti.foundationlib.json.MessageParser;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.websocket.SimpleWebSocketClient;
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
        logger.debug("Received payload: {}", message);
        try {
            String type = parser.getType();
            switch (type) {
                case "command" -> handleCommandRequest(message);
                case "system" -> handleSystemRequest(message);
                default -> {
                    logger.warn("Invalid type: {}", type);
                    sendError("Invalid type: " + type);
                }
            }
        } catch (Exception e) {
            logger.error("Error while processing message: {}",
                    logger.getDebug() ? e : e.getMessage());
            sendError("Internal client error: " + e.getMessage());
        }
    }

    @Override
    protected void afterAuth() {
        logger.debug("Sending server information's...");
        MessageBuilder builder = new MessageBuilder("system");
        builder.addToBody("channel", "name");
        builder.addToBody("name", Runtime.getInstance().getConfig().getKey("config.yml", "client-id"));
        logger.debug("Sending payload: {}", builder.build().toString());
        sendMessage(builder.build());
    }

    private void handleCommandRequest(String message) {
        logger.debug("Handling command response");
        Runtime.getInstance().getCommandExecutor().dispatchCommand(message);
    }

    private void handleSystemRequest(String message) {
        logger.debug("Handling system request");
        MessageParser parser = new MessageParser(message);
        String channel = parser.getBodyValueAsString("channel");
        String status = parser.getStatus();

        switch (channel) {
            case "error" -> logger.warn("Message from server '{}' : {}", parser.getBodyValueAsString("server"), status);
            case "info" -> logger.info("Message from server '{}' : {}", parser.getBodyValueAsString("server"), status);
            case "task" -> systemTask(parser, status);
            default -> logger.warn("Invalid channel: {}", channel);
        }
    }

    private void systemTask(MessageParser parser, String status) {
        String task = parser.getBodyValueAsString("task");
        switch (task) {
            case "reload" ->
                Runtime.getInstance().getScriptUtils().unloadCommands(() -> new SchedulerAdapter(Main.getInstance())
                        .runLater(Runtime.getInstance().getGeneralUtils()::reloadAll, 10L));
            case "reconnect" -> Ping.reconnect(logger);
            default -> logger.warn("Invalid task: {}", task);
        }
    }

    public void sendError(String errorMessage) {
        MessageBuilder builder = new MessageBuilder("system");
        builder.addToBody("channel", "error").withStatus(errorMessage).addToBody("client",
                Runtime.getInstance().getConfig().getKey("config.yml", "client-id"));
        sendMessage(builder.build());
    }

    public void sendInfo(String infoMessage) {
        MessageBuilder builder = new MessageBuilder("system");
        builder.addToBody("channel", "info").withStatus(infoMessage).addToBody("client",
                Runtime.getInstance().getConfig().getKey("config.yml", "client-id"));
        sendMessage(builder.build());
    }

    public void sendTask(String task, String status) {
        MessageBuilder builder = new MessageBuilder("system");
        builder.addToBody("channel", "task").addToBody("task", task)
                .addToBody("client", Runtime.getInstance().getConfig().getKey("config.yml", "client-id"))
                .withStatus(status);
        sendMessage(builder.build());
    }

    public void sendCommand(String command, String client, String target, Player executor) {
        MessageBuilder builder = new MessageBuilder("command");
        builder.addToBody("command", command).addToBody("client", client).addToBody("target", target);

        if (target.equals("player")) {
            builder.addToBody("name", executor.getName()).addToBody("uuid", executor.getUniqueId());
        }
        logger.info("Sending command '{}' to server", command);
        logger.debug("Sending payload: {}", builder.build().toString());
        sendMessage(builder.build());
    }
}
