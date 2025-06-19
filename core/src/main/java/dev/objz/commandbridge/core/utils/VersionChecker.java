package dev.objz.commandbridge.core.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

public class VersionChecker {

    private static String projectId;
    private static String MODRINTH_API_URL;
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+");

    public static void setProjectId(String id) {
        projectId = id;
        MODRINTH_API_URL = "https://api.modrinth.com/v2/project/" + projectId + "/version";
    }

    public static String getLatestVersion() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MODRINTH_API_URL))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            JSONArray versions = new JSONArray(responseBody);

            if (!versions.isEmpty()) {
                JSONObject latestVersion = versions.getJSONObject(0);
                return latestVersion.getString("version_number");
            }
        } catch (IOException | InterruptedException ignored) {
        }
        return null;
    }

    public static boolean isNewerVersion(String latestVersion, String currentVersion) {
        String[] latestParts = latestVersion.split("\\.");
        String[] currentParts = currentVersion.split("\\.");

        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latestPart = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
            int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;

            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
        }
        return false;
    }

    private static int parseVersionPart(String versionPart) {
        var matcher = VERSION_PATTERN.matcher(versionPart);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }
        return 0;
    }

    public static String getDownloadUrl() {
        return "https://modrinth.com/plugin/" + projectId + "/versions";
    }

    public static boolean checkBukkitVersion(String bukkitVersion, String currentVersion) {
        String[] bukkitParts = bukkitVersion.split("\\.");
        String[] currentParts = currentVersion.split("\\.");

        for (int i = 0; i < Math.min(bukkitParts.length, currentParts.length); i++) {
            if (!bukkitParts[i].equals(currentParts[i])) {
                return false;
            }
        }
        return bukkitParts.length == currentParts.length;
    }
}
