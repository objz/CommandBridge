package dev.consti.velocity.websocket;

import dev.consti.logging.Logger;
import dev.consti.websocket.SimpleWebSocketServer;
import org.java_websocket.WebSocket;
import org.json.JSONObject;

public class Server extends SimpleWebSocketServer {

    public Server(Logger logger, String secret) {
        super(logger, secret);
    }

    @Override
    protected void onMessage(WebSocket webSocket, JSONObject jsonObject) {

    }

}
