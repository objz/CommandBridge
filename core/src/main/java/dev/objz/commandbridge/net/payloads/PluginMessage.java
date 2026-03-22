package dev.objz.commandbridge.net.payloads;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public record PluginMessage(String channelType, JsonNode data, boolean expectsResponse, UUID requirePlayer, UUID whenOnline, String error) {

    public PluginMessage(String channelType, JsonNode data, boolean expectsResponse) {
        this(channelType, data, expectsResponse, null, null, null);
    }
}
