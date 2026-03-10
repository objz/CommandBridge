package dev.objz.commandbridge.velocity.dump;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Locale;
import java.util.Map;

public final class DumpSanitizer {

    private DumpSanitizer() {
    }

    public static JsonNode sanitize(JsonNode node) {
        if (node == null || node.isNull()) {
            return JsonNodeFactory.instance.nullNode();
        }

        if (node.isObject()) {
            ObjectNode out = JsonNodeFactory.instance.objectNode();
            for (var it = node.fields(); it.hasNext();) {
                Map.Entry<String, JsonNode> entry = it.next();
                String key = entry.getKey();
                if (isSensitiveKey(key)) {
                    out.put(key, "<redacted>");
                } else {
                    out.set(key, sanitize(entry.getValue()));
                }
            }
            return out;
        }

        if (node.isArray()) {
            ArrayNode out = JsonNodeFactory.instance.arrayNode();
            for (JsonNode child : node) {
                out.add(sanitize(child));
            }
            return out;
        }

        return node.deepCopy();
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }

        String lower = key.toLowerCase(Locale.ROOT);
        return lower.equals("secret")
                || lower.contains("password")
                || lower.contains("token")
                || lower.equals("tls-pin")
                || lower.equals("tlspin")
                || lower.equals("authorization")
                || lower.equals("api-key")
                || lower.equals("apikey");
    }
}
