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
    }

    @Override
    protected void onMessage(WebSocket webSocket, JSONObject jsonObject) {

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

        logger.info("Sending JSON: {}", jsonObject.toString());
        sendMessage(jsonObject);
    }


}
