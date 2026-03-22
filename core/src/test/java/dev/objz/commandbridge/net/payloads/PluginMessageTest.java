package dev.objz.commandbridge.net.payloads;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.objz.commandbridge.net.proto.Envelope;

class PluginMessageTest {

    @Test
    void backwardCompatConstructorProducesNullConditions() {
        JsonNode data = Envelope.MAPPER.createObjectNode();
        PluginMessage msg = new PluginMessage("test-channel", data, true);

        assertNull(msg.requirePlayer());
        assertNull(msg.whenOnline());
        assertNull(msg.error());
    }

    @Test
    void jacksonRoundTripWithNullConditions() throws Exception {
        JsonNode data = Envelope.MAPPER.createObjectNode();
        PluginMessage original = new PluginMessage("test-channel", data, false);

        String json = Envelope.MAPPER.writeValueAsString(original);
        PluginMessage deserialized = Envelope.MAPPER.readValue(json, PluginMessage.class);

        assertNull(deserialized.requirePlayer());
        assertNull(deserialized.whenOnline());
        assertNull(deserialized.error());
        assertEquals("test-channel", deserialized.channelType());
        assertEquals(false, deserialized.expectsResponse());
    }

    @Test
    void jacksonRoundTripWithConditions() throws Exception {
        UUID playerUuid = UUID.randomUUID();
        JsonNode data = Envelope.MAPPER.createObjectNode();
        PluginMessage original = new PluginMessage("test-channel", data, true, playerUuid, null, null);

        String json = Envelope.MAPPER.writeValueAsString(original);
        PluginMessage deserialized = Envelope.MAPPER.readValue(json, PluginMessage.class);

        assertEquals(playerUuid, deserialized.requirePlayer());
        assertNull(deserialized.whenOnline());
        assertNull(deserialized.error());
        assertEquals("test-channel", deserialized.channelType());
        assertEquals(true, deserialized.expectsResponse());
    }
}
