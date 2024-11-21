package dev.consti.bukkit.websocket;

import org.bukkit.entity.Player;
import org.json.JSONObject;

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
    protected void onMessage(JSONObject jsonObject) {
        logger.debug("Received message: {}", jsonObject.toString());

        try {
            String type = jsonObject.optString("type", "unknown");
            logger.info("Processing message of type: {}", type);

            switch (type) {
                case "command":
                handleCommandRequest(jsonObject);
                break;
            case "system":
                handleSystemRequest(jsonObject);
            default:
                logger.warn("Unknown message type received: {}", type);
                sendError("Unknown message type");
            }
        } catch (Exception e) {
            logger.error("Error while processing message: {}. Error: {}", jsonObject.toString(), e.getMessage(), e);
            sendError("Internal server error");
        }
    }

    private void handleCommandRequest(JSONObject jsonObject) { 
        logger.info("Handling command response: {}", jsonObject.toString());
    }

        private void handleSystemRequest(JSONObject jsonObject) {
        logger.info("Handling system request.");
        String type = jsonObject.getString("type");
        String message = jsonObject.getString("message");


        if (type.equals("name")) {
            if (message != null) {
                logger.info("Added connected client: {}", message);
            } else {
                logger.warn("No name provided in system request.");
            }
        } else {
            logger.warn("No type provided");
        }
    }

    private void sendError(String errorMessage) {
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
            executorDetails.put("name", executor.getName());
            executorDetails.put("uuid", executor.getUniqueId().toString());
            jsonObject.put("executor", executorDetails);
        }

        jsonObject.put("timestamp", java.time.Instant.now().toString());
        jsonObject.put("status", "success");

        logger.info("Sending JSON command to server: {} | Payload: {}", server, jsonObject.toString());
        sendMessage(jsonObject);
    }
}
