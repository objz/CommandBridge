package dev.objz.commandbridge.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.objz.commandbridge.logging.Log;

public class ModrinthAPI {

	private static final String MODRINTH_URL = "https://api.modrinth.com/v2/";
	private static final String USER_AGENT = "CommandBridge/" + BuildMeta.VERSION + " (https://github.com/objz/commandbridge)";
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	public static String getLatestVersion(String projectId) {
		try {
			String response = request("project/" + projectId + "/versions");
			JsonArray versions = JsonParser.parseString(response).getAsJsonArray();

			if (versions.size() > 0) {
				JsonObject latestVersion = versions.get(0).getAsJsonObject();
				return latestVersion.get("name").getAsString();
			}
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

		if (response.statusCode() != 200) {
			throw new Exception("API returned status " + response.statusCode() + ": " + response.body());
		}

		return response.body();
	}
}
