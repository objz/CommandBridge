package dev.objz.commandbridge.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.objz.commandbridge.logging.Log;

public class MojangAPI {

    private static final String API_URL = "https://api.mojang.com/";
    private static final String USER_AGENT = "CommandBridge/" + BuildMeta.VERSION
            + " (https://github.com/objz/commandbridge; support@objz.dev)";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final long DEFAULT_RATELIMIT_SECONDS = 60;
    private static volatile long rateLimitedUntilEpochMs = 0L;

    public static UUID getUuid(String name) {
        try {
            if (isRateLimited()) {
                Log.debug("Skipping Mojang request due to rate limit.");
                return null;
            }

            String response = request("users/profiles/minecraft/" + name);
            JsonObject obj = JsonParser.parseString(response).getAsJsonObject();

            if (obj.has("id")) {
                String trimmed = obj.get("id").getAsString();
                return fromTrimmedUuid(trimmed);
            }
        } catch (RateLimitedException e) {
            Log.warn("Mojang API rate limited (HTTP 429). Retrying after {}.", e.retryAfterMessage());
        } catch (NotFoundException ignored) {
            Log.debug("Mojang API: player '{}' not found", name);
        } catch (Exception e) {
            Log.error("Error fetching UUID for '{}': {}", name, e.getMessage());
        }
        return null;
    }

    private static UUID fromTrimmedUuid(String id) {
        return UUID.fromString(
                id.substring(0, 8) + "-" + id.substring(8, 12) + "-"
                        + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20));
    }

    private static String request(String endpoint) throws Exception {
        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + endpoint))
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

        if (response.statusCode() == 204 || response.statusCode() == 404) {
            throw new NotFoundException();
        }

        if (response.statusCode() != 200) {
            throw new Exception("Mojang API returned status " + response.statusCode() + ": " + response.body());
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

    private static class NotFoundException extends Exception {
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
