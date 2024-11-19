package dev.consti.velocity.websocket;

import com.velocitypowered.api.proxy.Player;
import dev.consti.logging.Logger;
import dev.consti.websocket.SimpleWebSocketServer;
import org.java_websocket.WebSocket;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Server extends SimpleWebSocketServer {
    private final Logger logger;
    private final List<String> connectedClients;

    public Server(Logger logger, String secret) {
        super(logger, secret);
        this.logger = logger;
        this.connectedClients = new ArrayList<>();
        logger.debug("WebSocket server initialized with secret.");
    }

    @Override
    protected void onMessage(WebSocket webSocket, JSONObject jsonObject) {
        logger.debug("Received message: {}", jsonObject.toString());

        try {
            String type = jsonObject.optString("type", "unknown");
            logger.info("Processing message of type: {}", type);

            switch (type) {
                case "command":
                    handleCommandRequest(webSocket, jsonObject);
                    break;
                case "system":
                    handleSystemRequest(webSocket, jsonObject);
                    break;
                default:
                    logger.warn("Unknown message type received: {}", type);
                    sendError(webSocket, "Unknown message type");
            }
        } catch (Exception e) {
            logger.error("Error while processing message: {}. Error: {}", jsonObject.toString(), e.getMessage(), e);
            sendError(webSocket, "Internal server error");
        }
    }

    private void handleCommandRequest(WebSocket webSocket, JSONObject jsonObject) {
        logger.info("Handling command response: {}", jsonObject.toString());
        // Add your handling logic here
    }

    private void handleSystemRequest(WebSocket webSocket, JSONObject jsonObject) {
        logger.info("Handling system request.");
        String type = jsonObject.getString("type");
        String message = jsonObject.getString("message");


        if (type.equals("name")) {
            if (message != null) {
                connectedClients.add(message);
                logger.info("Added connected client: {}", message);
            } else {
                logger.warn("No name provided in system request.");
            }
        } else {
            logger.warn("No type provided");
        }
    }

    public boolean isServerConnected(String serverName) {
        boolean exists = connectedClients.contains(serverName);
        logger.debug("Checking if server '{}' is connected: {}", serverName, exists);
        return exists;
    }

    private void sendError(WebSocket webSocket, String errorMessage) {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("type", "error");
        errorResponse.put("message", errorMessage);
        sendMessage(errorResponse);
    }

    public void sendJSON(String command, String server, String[] arguments, Player executor) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "command");
        jsonObject.put("command", command);
        jsonObject.put("server", server);
        jsonObject.put("arguments", arguments);

        if (executor != null) {
            JSONObject executorDetails = new JSONObject();
            executorDetails.put("name", executor.getUsername());
            executorDetails.put("uuid", executor.getUniqueId().toString());
            jsonObject.put("executor", executorDetails);
        }

        jsonObject.put("timestamp", java.time.Instant.now().toString());
        jsonObject.put("status", "success");

        logger.info("Sending JSON command to server: {} | Payload: {}", server, jsonObject.toString());
        sendMessage(jsonObject);
    }


}
