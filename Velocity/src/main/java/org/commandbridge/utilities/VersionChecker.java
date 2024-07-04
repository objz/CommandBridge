package org.commandbridge.utilities;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

public class VersionChecker {
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/wIuI4ru2/version";
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+");

    public static String getLatestVersion() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MODRINTH_API_URL))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        JSONArray versions = new JSONArray(responseBody);

        if (versions.length() > 0) {
            JSONObject latestVersion = versions.getJSONObject(0);
            return latestVersion.getString("version_number");
        } else {
            return null;
        }
    }

    public static boolean isNewerVersion(String latestVersion, String currentVersion) {
        String[] latestParts = latestVersion.split("\\.");
        String[] currentParts = currentVersion.split("\\.");

        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            Integer latestPart = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
            Integer currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;

            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
        }

        return false;
    }

    private static Integer parseVersionPart(String versionPart) {
        var matcher = VERSION_PATTERN.matcher(versionPart);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }
        return 0;
    }

    public static String getDownloadUrl() {
        return "https://modrinth.com/plugin/wIuI4ru2/versions";
    }
}


