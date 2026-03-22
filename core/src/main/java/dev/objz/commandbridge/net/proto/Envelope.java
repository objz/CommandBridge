package dev.objz.commandbridge.net.proto;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public record Envelope(int v, UUID id, MessageType type, String from, String to, long ts, JsonNode payload) {

    //fix for Optional and <?>
    public static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule()).findAndRegisterModules();

    public static Envelope make(MessageType type, String from, String to, JsonNode payload) {
        return new Envelope(1, UUID.randomUUID(), type, from, to, System.currentTimeMillis(),
                payload != null ? payload : MAPPER.nullNode());
    }

    // Reuses req.id() intentionally for request-response correlation in ResponseAwaiter
    public static Envelope reply(Envelope req, MessageType type, String from, JsonNode payload) {
        return new Envelope(req.v(), req.id(), type, from, req.from(), System.currentTimeMillis(),
                payload != null ? payload : MAPPER.nullNode());
    }


}
