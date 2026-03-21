package dev.objz.commandbridge.net.payloads;

import com.fasterxml.jackson.databind.JsonNode;

public record PluginMessage(String channelType, JsonNode data, boolean expectsResponse) {
}
