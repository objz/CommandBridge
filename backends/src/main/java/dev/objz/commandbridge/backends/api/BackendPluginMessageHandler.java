package dev.objz.commandbridge.backends.api;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.payloads.PluginMessage;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;

import java.util.Objects;

public final class BackendPluginMessageHandler extends InboundHandler {

    private final BackendCommandBridgeImpl api;
    private final boolean response;

    public BackendPluginMessageHandler(BackendCommandBridgeImpl api, boolean response) {
        this.api = Objects.requireNonNull(api);
        this.response = response;
    }

    @Override
    public void accept(Endpoint endpoint, Envelope env) {
        if (response) {
            api.handlePluginMessageResponse(env);
            return;
        }

        PluginMessage replyPayload = api.handlePluginMessageRequest(env);
        if (replyPayload == null || !replyPayload.expectsResponse()) {
            return;
        }

        reply(endpoint, env, MessageType.PLUGIN_MESSAGE_RESPONSE, replyPayload)
                .dispatch()
                .exceptionally(ex -> {
                    Log.warn("Failed to send plugin message response: {}", ex.getMessage());
                    return null;
                });
    }
}
