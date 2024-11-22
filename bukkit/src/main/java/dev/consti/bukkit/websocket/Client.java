package dev.consti.bukkit.websocket;

import dev.consti.bukkit.core.Runtime;
import dev.consti.json.MessageBuilder;
import dev.consti.json.MessageParser;
import org.bukkit.entity.Player;

import dev.consti.logging.Logger;
import dev.consti.websocket.SimpleWebSocketClient;

public class Client extends SimpleWebSocketClient {
    private final Logger logger;

    public Client(Logger logger, String secret) {
        super(logger, secret);
        this.logger = logger;
        logger.debug("WebSocket client initialized.");
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
            logger.error("Error while processing message: {}. Error: {}", message, e.getMessage(), e);
            sendError("Internal server error");
        }
    }

    @Override
    protected void afterAuth(){
        logger.debug("Sending server information's...");
        MessageBuilder builder = new MessageBuilder("system");
        builder.addToBody("channel", "name");
        builder.addToBody("name", Runtime.getInstance().getConfig().getKey("config.yml", "name"));
        logger.debug("Payload: {}", builder.build().toString());
        sendMessage(builder.build());
    }

    private void handleCommandRequest(String message) {
        logger.info("Handling command response: {}", message);

    }

    private void handleSystemRequest(String message) {
        logger.info("Handling system request.");
        MessageParser parser = new MessageParser(message);
        String channel = parser.getBodyValueAsString("channel");
        String status = parser.getStatus();

        if (channel.equals("error")) {
            logger.warn("Error Message from server: {}", status);
        } else if (channel.equals("info")) {
            logger.info("Info from server: {}", status);
        }

    }

    private void sendError(String errorMessage) {
        MessageBuilder builder = new MessageBuilder("system");
        builder.addToBody("channel", "error");
        builder.withStatus(errorMessage);
        sendMessage(builder.build());
    }

    public void sendJSON(String command, String server, String[] arguments, Player executor) {
        MessageBuilder builder = new MessageBuilder("command");
        builder.addToBody("command", command);
        builder.addToBody("server", server);
        builder.addToBody("arguments", arguments);

        if (executor != null) {
            builder.addToBody("name", executor.getName());
            builder.addToBody("uuid", executor.getUniqueId());
        }

        logger.info("Sending JSON command to server: {} | Payload: {}", server, builder.build().toString());
        sendMessage(builder.build());
    }
}
