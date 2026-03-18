package dev.objz.commandbridge.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.objz.commandbridge.logging.Log;

public final class ModrinthAPI {

    private static final String MODRINTH_URL = "https://api.modrinth.com/v2/";
    private static final String USER_AGENT = "CommandBridge/" + BuildMeta.VERSION + " (https://github.com/objz/commandbridge; support@objz.dev)";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final long DEFAULT_RATELIMIT_SECONDS = 60;
    private static volatile long rateLimitedUntilEpochMs = 0L;

    public static String getLatestVersion(String projectId) {
        try {
            if (isRateLimited()) {
                Log.debug("Skipping Modrinth request due to rate limit.");
                return null;
            }

            String response = request("project/" + projectId + "/version?include_changelog=false");
            JsonArray versions = JsonParser.parseString(response).getAsJsonArray();

            if (versions.size() > 0) {
                JsonObject latestVersion = versions.get(0).getAsJsonObject();
                return latestVersion.get("version_number").getAsString();
            }
        } catch (RateLimitedException e) {
            Log.warn("Modrinth rate limited (HTTP 429). Retrying after {}.", e.retryAfterMessage());
        } catch (Exception e) {
            Log.error("Error fetching latest version: {}", e.getMessage());
        }
        return null;
    }

    private static String request(String endpoint) throws Exception {
        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(MODRINTH_URL + endpoint))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            long retryAfterSeconds = resolveRetryAfterSeconds(response).orElse(DEFAULT_RATELIMIT_SECONDS);
            setRateLimitedUntil(retryAfterSeconds);
            throw new RateLimitedException(retryAfterSeconds);
        }

        if (response.statusCode() != 200) {
            throw new Exception("API returned status " + response.statusCode() + ": " + response.body());
        }

        return response.body();
    }

    private static boolean isRateLimited() {
        return System.currentTimeMillis() < rateLimitedUntilEpochMs;
    }

    private static void setRateLimitedUntil(long retryAfterSeconds) {
        long now = System.currentTimeMillis();
        rateLimitedUntilEpochMs = now + (retryAfterSeconds * 1000L);
    }

    private static Optional<Long> resolveRetryAfterSeconds(HttpResponse<?> response) {
        Optional<String> retryAfter = response.headers().firstValue("Retry-After");
        if (retryAfter.isPresent()) {
            Long parsed = tryParseSeconds(retryAfter.get());
            if (parsed != null) return Optional.of(parsed);
        }

        Optional<String> reset = response.headers().firstValue("X-Ratelimit-Reset");
        if (reset.isPresent()) {
            Long parsed = tryParseSeconds(reset.get());
            if (parsed != null) return Optional.of(parsed);
        }

        return Optional.empty();
    }

    private static Long tryParseSeconds(String value) {
        try {
            long seconds = Long.parseLong(value.trim());
            return seconds > 0 ? seconds : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static class RateLimitedException extends Exception {
        private final long retryAfterSeconds;

        private RateLimitedException(long retryAfterSeconds) {
            this.retryAfterSeconds = retryAfterSeconds;
        }

        private String retryAfterMessage() {
            Instant instant = Instant.ofEpochMilli(rateLimitedUntilEpochMs);
            String timestamp = DateTimeFormatter.ofPattern("HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(instant);
            return retryAfterSeconds + "s (until " + timestamp + ")";
        }
    }
}
