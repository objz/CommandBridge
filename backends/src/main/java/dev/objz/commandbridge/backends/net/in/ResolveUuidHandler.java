package dev.objz.commandbridge.backends.net.in;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.payloads.util.ResolveUuidPayload;
import dev.objz.commandbridge.net.payloads.util.ResolveUuidResponsePayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public final class ResolveUuidHandler extends InboundHandler {

    private final Function<String, UUID> resolver;

    public ResolveUuidHandler(Function<String, UUID> resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    @Override
    public void accept(Endpoint endpoint, Envelope env) {
        if (env.payload() == null) {
            Log.warn("Received RESOLVE_UUID with null payload from '{}'", env.from());
            sendResponse(endpoint, env, null, null);
            return;
        }

        ResolveUuidPayload payload;
        try {
            payload = Envelope.MAPPER.treeToValue(env.payload(), ResolveUuidPayload.class);
        } catch (Exception e) {
            Log.error(e, "Failed to parse RESOLVE_UUID from '{}'", env.from());
            sendResponse(endpoint, env, null, null);
            return;
        }

        if (payload == null || payload.name() == null || payload.name().isBlank()) {
            Log.warn("Received empty RESOLVE_UUID from '{}'", env.from());
            sendResponse(endpoint, env, "", null);
            return;
        }

        UUID resolved = resolver.apply(payload.name());
        String uuidStr = resolved != null ? resolved.toString() : null;

        Log.debug("Resolved '{}' -> {}", payload.name(), uuidStr != null ? uuidStr : "not found");
        sendResponse(endpoint, env, payload.name(), uuidStr);
    }

    private void sendResponse(Endpoint endpoint, Envelope env, String name, String uuid) {
        var response = new ResolveUuidResponsePayload(name, uuid);
        reply(endpoint, env, MessageType.RESOLVE_UUID_RESPONSE, response)
                .dispatch()
                .exceptionally(ex -> {
                    Log.warn("Failed to send RESOLVE_UUID_RESPONSE: {}", ex.toString());
                    return null;
                });
    }
}
