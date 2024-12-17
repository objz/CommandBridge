package dev.consti.commandbridge.velocity.websocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.java_websocket.WebSocket;

import com.velocitypowered.api.proxy.Player;

import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.foundationlib.json.MessageBuilder;
import dev.consti.foundationlib.json.MessageParser;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.websocket.SimpleWebSocketServer;

public class Server extends SimpleWebSocketServer {
    private final Logger logger;
    private final List<String> connectedClients;
    private final Map<String, WebSocket> clientConnections = new HashMap<>();

    public Server(Logger logger, String secret) {
        super(logger, secret);
        this.logger = logger;
        this.connectedClients = new ArrayList<>();
    }

    @Override
    protected void onMessage(WebSocket webSocket, String message) {
        MessageParser parser = new MessageParser(message);
        logger.debug("Received message: {}", message);
        try {
            String type = parser.getType();
            logger.info("Processing message of type: {}", type);
            switch (type) {
                case "command":
                    handleCommandRequest(webSocket, message);
                    break;
                case "system":
                    handleSystemRequest(webSocket, message);
                    break;
                default:
                    logger.warn("Unknown message type received: {}", type);
                    sendError(webSocket, "Unknown message type");
            }
        } catch (Exception e) {
            logger.error("Error while processing message: {}: {}", e.getMessage(), e);
            sendError(webSocket, "Internal server error");
        }
    }

    @Override
    protected void onConnectionClose(WebSocket conn, int code, String reason) {
        logger.info("Client {} disconnected with reason: {}", conn.getRemoteSocketAddress(), reason);

        String disconnectedClientName = null;
        for (Map.Entry<String, WebSocket> entry : clientConnections.entrySet()) {
            if (entry.getValue().equals(conn)) {
                disconnectedClientName = entry.getKey();
                break;
            }
        }

        if (disconnectedClientName != null) {
            connectedClients.remove(disconnectedClientName);
            clientConnections.remove(disconnectedClientName);
            logger.debug("Removed disconnected client: {}", disconnectedClientName);
        } else {
            logger.warn("Disconnected client not found in client connections map.");
        }
    }

    private void handleCommandRequest(WebSocket webSocket, String message) {
        logger.debug("Handling command request.");
        Runtime.getInstance().getCommandExecutor().dispatchCommand(message);
    }

    private void handleSystemRequest(WebSocket webSocket, String message) {
        logger.debug("Handling system request.");
        MessageParser parser = new MessageParser(message);
        String channel = parser.getBodyValueAsString("channel");
        String name = parser.getBodyValueAsString("name");
        String status = parser.getStatus();


        if (channel.equals("name")) {
            if (name != null) {
                connectedClients.add(name);
                clientConnections.put(name, webSocket);
                logger.info("Added connected client: {}", name);
            } else {
                logger.warn("No name provided in system request.");
            }
        } else if (channel.equals("error")) {
            logger.warn("Error Message from client: {}: {}", webSocket.toString(), status);
        } else if (channel.equals("info")) {
            logger.info("Info from client: {}: {}", webSocket.toString(), status);
        } else if (channel.equals("command")) {
            systemCommand(parser);
        }
    }


    private void systemCommand(MessageParser parser) {
        if (parser.getBodyValueAsString("command").equals("reload")) {
            Runtime.getInstance().getGeneralUtils().addClientToStatus(parser.getBodyValueAsString("client-id"), parser.getStatus());
        }
    }

    public boolean isServerConnected(String serverName) {
        boolean exists = connectedClients.contains(serverName);
        logger.debug("Checking if server '{}' is connected: {}", serverName, exists);
        return exists;
    }

    public List<String> getConnectedClients() {
        return connectedClients;
    }

    private void sendError(WebSocket webSocket, String errorMessage) {
        MessageBuilder builder = new MessageBuilder("system");
        builder.addToBody("channel", "error");
        builder.withStatus(errorMessage);
        sendMessage(builder.build(), webSocket);
    }

    public void sendJSON(String command, String client, String[] arguments, Player executor, String target) {
        WebSocket conn = clientConnections.get(client);
        if (conn == null) {
            logger.warn("Server '{}' is not connected, cannot send message.", client);
            return;
        }

        MessageBuilder builder = new MessageBuilder("command");
        builder.addToBody("command", command);
        builder.addToBody("client", client);
        builder.addToBody("arguments", arguments);
        builder.addToBody("target", target);

        if (target.equals("player")) {
            builder.addToBody("name", executor.getUsername());
            builder.addToBody("uuid", executor.getUniqueId());
        }
        logger.info("Senging JSON command to client: {}", client);
        logger.debug("Payload: {}", builder.build().toString());
        sendMessage(builder.build(), conn);
    }


}
