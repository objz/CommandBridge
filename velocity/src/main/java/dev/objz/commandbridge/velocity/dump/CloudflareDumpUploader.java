package dev.objz.commandbridge.velocity.dump;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class CloudflareDumpUploader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static final String UPLOAD_URL = "https://cb.objz.dev/api/dump/upload";
    private static final String PUBLIC_URL = "https://cb.objz.dev/dump/?id=";
    private static final String AUTH_TOKEN = "cb-dump-upload-v1";
    private static final int TIMEOUT_SECONDS = 12;
    private static final int RETENTION_DAYS = 14;

    public String publicUrlFor(String id) {
        return PUBLIC_URL + id;
    }

    public DumpUploadResult upload(String jsonPayload) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(UPLOAD_URL))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .header("X-Dump-Retention-Days", String.valueOf(RETENTION_DAYS))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

        if (AUTH_TOKEN != null && !AUTH_TOKEN.isBlank()) {
            request.header("Authorization", "Bearer " + AUTH_TOKEN);
        }

        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("Upload failed with HTTP " + status + ": " + compact(response.body()));
        }

        JsonNode body = MAPPER.readTree(response.body());
        String id = text(body, "id");
        String url = text(body, "url");
        String expiresAt = text(body, "expiresAt");

        if ((url == null || url.isBlank()) && id != null && !id.isBlank()) {
            url = publicUrlFor(id);
        }

        return new DumpUploadResult(id, url, expiresAt);
    }

    private static String text(JsonNode root, String key) {
        if (root == null || !root.has(key)) {
            return null;
        }
        JsonNode node = root.get(key);
        return node == null || node.isNull() ? null : node.asText();
    }

    private static String compact(String body) {
        if (body == null) {
            return "";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 240) {
            return compact;
        }
        return compact.substring(0, 240) + "...";
    }
}
