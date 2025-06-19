package dev.objz.commandbridge.core.json;

import java.time.Instant;

import org.json.JSONObject;

public class MessageBuilder {

    private final JSONObject jsonObject;
    private final JSONObject bodyObject;

    public MessageBuilder(String type) {
        jsonObject = new JSONObject();
        bodyObject = new JSONObject();

        jsonObject.put("type", type);
        jsonObject.put("body", bodyObject);
        jsonObject.put("timestamp", Instant.now().toString());
    }

    public MessageBuilder addToBody(String key, Object value) {
        bodyObject.put(key, value);
        return this;
    }

    public MessageBuilder withStatus(String status) {
        jsonObject.put("status", status);
        return this;
    }

    public JSONObject build() {
        return jsonObject;
    }

}
