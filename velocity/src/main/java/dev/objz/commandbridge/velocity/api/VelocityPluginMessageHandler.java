package dev.objz.commandbridge.velocity.api;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.proto.Envelope;

import java.util.Objects;

public final class VelocityPluginMessageHandler extends InboundHandler {

    private final VelocityCommandBridgeImpl api;
    private final boolean response;

    public VelocityPluginMessageHandler(VelocityCommandBridgeImpl api, boolean response) {
        this.api = Objects.requireNonNull(api);
        this.response = response;
    }

    @Override
    public void accept(Endpoint endpoint, Envelope env) {
        if (response) {
            api.handlePluginMessageResponse(env);
            return;
        }
        api.handlePluginMessageRequest(endpoint, env);
    }
}
