package dev.objz.commandbridge.core.json;

import org.json.JSONArray;
import org.json.JSONObject;

public class MessageParser {

    private final JSONObject jsonObject;

    public MessageParser(String jsonString) {
        jsonObject = new JSONObject(jsonString);
    }

    public String getType() {
        return jsonObject.optString("type", null);
    }

    public String getStatus() {
        return jsonObject.optString("status", null);
    }

    public String getTimestamp() {
        return jsonObject.optString("timestamp", null);
    }

    public JSONObject getBody() {
        return jsonObject.optJSONObject("body");
    }

    public Object getBodyValue(String key) {
        JSONObject body = getBody();
        return body != null ? body.opt(key) : null;
    }

    public String getBodyValueAsString(String key) {
        return (String) getBodyValue(key);
    }

    public int getBodyValueAsInt(String key) {
        Object value = getBodyValue(key);
        return value instanceof Integer ? (int) value : 0;
    }

    public boolean getBodyValueAsBoolean(String key) {
        Object value = getBodyValue(key);
        return value instanceof Boolean && (boolean) value;
    }

    public JSONArray getBodyValueAsArray(String key) {
        Object value = getBodyValue(key);
        return value instanceof JSONArray ? (JSONArray) value : null;
    }

    public JSONObject getBodyValueAsObject(String key) {
        Object value = getBodyValue(key);
        return value instanceof JSONObject ? (JSONObject) value : null;
    }

    public boolean containsBodyKey(String key) {
        JSONObject body = getBody();
        return body != null && body.has(key);
    }
}
