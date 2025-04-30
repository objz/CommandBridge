package dev.consti.commandbridge.velocity.websocket;

import java.util.*;

import com.velocitypowered.api.proxy.Player;

import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.foundationlib.json.MessageBuilder;
import dev.consti.foundationlib.json.MessageParser;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.websocket.SimpleWebSocketServer;
import io.netty.channel.Channel;

public class Server extends SimpleWebSocketServer {
    private final Logger logger;
    private final Map<String, Channel> clientConnections = new HashMap<>();

    public Server(Logger logger, String secret) {
        super(logger, secret);
        super.addHttpHandler(Runtime.getInstance().getHttpServer());
        this.logger = logger;
    }

    @Override
    protected void onMessage(Channel webSocket, String message) {
        MessageParser parser = new MessageParser(message);
        logger.debug("Received message: {}", message);
        try {
            String type = parser.getType();
            switch (type) {
                case "command" -> handleCommandRequest(webSocket, message);
                case "system" -> handleSystemRequest(webSocket, message);
                default -> {
                    logger.warn("Invalid type: {}", type);
                    sendError(webSocket, "Invalid type: " + type);
                }
            }
        } catch (Exception e) {
            logger.error("Error while processing message: {}",
                    logger.getDebug() ? e : e.getMessage());
            sendError(webSocket, "Internal server error: " + e.getMessage());
        }
    }

    @Override
    protected void onConnectionClose(Channel conn, int code, String reason) {
        String clientAddress = conn.remoteAddress().toString();

        if (getConnections().contains(conn)) {
            String disconnectedClientName = clientConnections.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(conn))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            logger.info("Client '{}' disconnected", clientAddress);
            if (disconnectedClientName != null) {
                clientConnections.remove(disconnectedClientName);
                logger.debug("Removed disconnected client: {}", disconnectedClientName);
            } else {
                logger.warn("Disconnected WebSocket client '{}' not found in client connections map.", clientAddress);
            }
        } 
    }

    private void handleCommandRequest(Channel webSocket, String message) {
        logger.debug("Handling command request");
        Runtime.getInstance().getCommandExecutor().dispatchCommand(message);
    }

    private void handleSystemRequest(Channel webSocket, String message) {
        logger.debug("Handling system request");
        MessageParser parser = new MessageParser(message);
        String channel = parser.getBodyValueAsString("channel");
        String name = parser.getBodyValueAsString("name");
        String client = parser.getBodyValueAsString("client");
        String status = parser.getStatus();

        switch (channel) {
            case "name" -> {
                if (name != null) {
                    clientConnections.put(name, webSocket);
                    logger.info("Added connected client: {}", name);
                } else {
                    logger.warn("Client did not provide 'name' in system request");
                }
            }
            case "error" -> logger.warn("Message from client '{}' : {}", client, status);
            case "info" -> logger.info("Message from client '{}' : {}", client, status);
            case "task" -> systemTask(parser, status, client);
            default -> logger.warn("Invalid channel: {}", channel);
        }
    }

    private void systemTask(MessageParser parser, String status, String client) {
        String task = parser.getBodyValueAsString("task");
        switch (task) {
            case "reload" -> Runtime.getInstance().getGeneralUtils().addClientToStatus(client, parser.getStatus());
            default -> logger.warn("Invalid task: {}", task);
        }
    }

    public void sendError(Channel webSocket, String errorMessage) {
        MessageBuilder builder = new MessageBuilder("system");
        builder.addToBody("channel", "error")
                .addToBody("server", Runtime.getInstance().getConfig().getKey("config.yml", "server-id"))
                .withStatus(errorMessage);
        sendMessage(builder.build(), webSocket);
    }

    public void sendInfo(Channel webSocket, String infoMessage) {
        MessageBuilder builder = new MessageBuilder("system");
        builder.addToBody("channel", "info")
                .addToBody("server", Runtime.getInstance().getConfig().getKey("config.yml", "server-id"))
                .withStatus(infoMessage);
        sendMessage(builder.build(), webSocket);
    }

    public void sendTask(Channel webSocket, String task, String status) {
        MessageBuilder builder = new MessageBuilder("system");
        builder.addToBody("channel", "task").addToBody("task", task)
                .addToBody("server", Runtime.getInstance().getConfig().getKey("config.yml", "server-id"))
                .withStatus(status);
        sendMessage(builder.build(), webSocket);
    }

    public void sendCommand(String command, String client, String target, Player executor) {
        Channel conn = clientConnections.get(client);
        if (conn == null) {
            logger.warn("Client '{}' is not connected, cannot send message.", client);
            return;
        }

        MessageBuilder builder = new MessageBuilder("command");
        builder.addToBody("command", command).addToBody("client", client).addToBody("target", target);

        if (target.equals("player")) {
            builder.addToBody("name", executor.getUsername()).addToBody("uuid", executor.getUniqueId());
        }
        logger.info("Sending command '{}' to client: {}", command, client);
        logger.debug("Sending payload: {}", builder.build().toString());
        sendMessage(builder.build(), conn);
    }

    public boolean isServerConnected(String clientName) {
        boolean exists = clientConnections.containsKey(clientName);
        logger.debug("Checking if client '{}' is connected: {}", clientName, exists);
        return exists;
    }

    public Set<String> getConnectedClients() {
        return clientConnections.keySet();
    }

    public Channel getWebSocket(String client) {
        return clientConnections.get(client);
    }

}
