package dev.consti.velocity.websocket;

import com.velocitypowered.api.proxy.Player;
import dev.consti.logging.Logger;
import dev.consti.websocket.SimpleWebSocketServer;
import org.java_websocket.WebSocket;
import org.json.JSONObject;

public class Server extends SimpleWebSocketServer {
    private final Logger logger;

    public Server(Logger logger, String secret) {
        super(logger, secret);
        this.logger = logger;
        logger.debug("WebSocket server initialized with secret.");
    }

    @Override
    protected void onMessage(WebSocket webSocket, JSONObject jsonObject) {
        logger.debug("Received message: {}", jsonObject.toString());

        try {
            String type = jsonObject.optString("type", "unknown");
            logger.info("Processing message of type: {}", type);

            switch (type) {
                case "commandResponse":
                    handleCommandResponse(webSocket, jsonObject);
                    break;
                case "status":
                    handleStatusRequest(webSocket);
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

    private void handleCommandResponse(WebSocket webSocket, JSONObject jsonObject) {
        logger.info("Handling command response: {}", jsonObject.toString());
        // Add your handling logic here
    }

    private void handleStatusRequest(WebSocket webSocket) {
        logger.info("Handling status request.");
        JSONObject response = new JSONObject();
        response.put("status", "running");
        response.put("timestamp", java.time.Instant.now().toString());
        sendMessage(response);
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
