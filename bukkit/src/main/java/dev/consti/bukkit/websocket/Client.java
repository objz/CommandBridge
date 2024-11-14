package dev.consti.bukkit.websocket;

import dev.consti.logging.Logger;
import dev.consti.websocket.SimpleWebSocketClient;
import org.json.JSONObject;

public class Client extends SimpleWebSocketClient {

    public Client(Logger logger, String secret) {
        super(logger, secret);
    }

    @Override
    protected void onMessage(JSONObject jsonObject) {

    }
}
