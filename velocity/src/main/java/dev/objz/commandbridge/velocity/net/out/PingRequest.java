package dev.objz.commandbridge.velocity.net.out;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.util.PingPayload;
import dev.objz.commandbridge.net.payloads.util.PongPayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.velocity.net.out.ctx.PingRequestContext;

public final class PingRequest extends OutboundHandler<PingRequestContext> {

    @Override
    public SendOperation accept(PingRequestContext ctx) {
        String clientId = ctx.session.id();
        long startTime = System.currentTimeMillis();

        var payload = new PingPayload(startTime);
        ObjectNode payloadNode = Envelope.MAPPER.valueToTree(payload);
        final Envelope env = Envelope.make(MessageType.PING, serverId, clientId, payloadNode);

        SendOperation op = send(ctx.session.ch(), env)
                .expect(MessageType.PONG)
                .timeout(ctx.timeout);

        op.await().thenApply(pongEnv -> {
            if (pongEnv != null) {
                try {
                    long endTime = System.currentTimeMillis();
                    PongPayload pong = Envelope.MAPPER.treeToValue(
                            pongEnv.payload(),
                            PongPayload.class);

                    long latency = endTime - pong.timestamp();
                    Log.debug("Received pong from '{}' with {}ms latency", clientId, latency);
                    ctx.resultCallback.accept(true, latency);
                } catch (Exception e) {
                    Log.error(e, "Failed to process pong from '{}'", clientId);
                    ctx.resultCallback.accept(false, -1L);
                }
            }
            return env;
        }).exceptionally(ex -> {
            var cause = (ex.getCause() != null) ? ex.getCause() : ex;
            if (cause instanceof java.util.concurrent.TimeoutException) {
                Log.debug("Ping timeout for '{}'", clientId);
            } else {
                Log.error(cause, "Ping failed for '{}'", clientId);
            }
            ctx.resultCallback.accept(false, -1L);
            return null;
        });

        return op;
    }
}
