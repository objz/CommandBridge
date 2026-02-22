package dev.objz.commandbridge.backends.net.in;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.InboundHandler;
import dev.objz.commandbridge.net.payloads.util.PingPayload;
import dev.objz.commandbridge.net.payloads.util.PongPayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;

public final class PingHandler extends InboundHandler {

    @Override
    public void accept(Endpoint endpoint, Envelope env) {
        try {
            PingPayload ping = Envelope.MAPPER.treeToValue(env.payload(), PingPayload.class);
            Log.debug("Received ping request from '{}' with timestamp {}", env.from(), ping.timestamp());

            PongPayload pong = new PongPayload(ping.timestamp());
            reply(endpoint, env, MessageType.PONG, pong)
                    .dispatch()
                    .exceptionally(ex -> {
                        Log.warn("Failed to send pong response: {}", ex.toString());
                        return null;
                    });
        } catch (Exception e) {
            Log.error(e, "Failed to handle ping request from '{}'", env.from());
        }
    }
}
