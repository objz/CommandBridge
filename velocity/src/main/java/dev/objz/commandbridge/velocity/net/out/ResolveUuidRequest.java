package dev.objz.commandbridge.velocity.net.out;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.util.ResolveUuidPayload;
import dev.objz.commandbridge.net.payloads.util.ResolveUuidResponsePayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.velocity.net.out.ctx.ResolveUuidRequestContext;

import java.util.UUID;

public final class ResolveUuidRequest extends OutboundHandler<ResolveUuidRequestContext> {

    @Override
    public SendOperation accept(ResolveUuidRequestContext ctx) {
        String clientId = ctx.session().id();

        var payload = new ResolveUuidPayload(ctx.name());
        ObjectNode payloadNode = Envelope.MAPPER.valueToTree(payload);
        final Envelope env = Envelope.make(MessageType.RESOLVE_UUID, serverId, clientId, payloadNode);

        SendOperation op = send(ctx.session().endpoint(), env)
                .expect(MessageType.RESOLVE_UUID_RESPONSE)
                .timeout(ctx.timeout());

        op.await().thenApply(responseEnv -> {
            if (responseEnv != null) {
                try {
                    ResolveUuidResponsePayload response = Envelope.MAPPER.treeToValue(
                            responseEnv.payload(),
                            ResolveUuidResponsePayload.class);

                    if (response != null && response.uuid() != null) {
                        UUID resolved = UUID.fromString(response.uuid());
                        Log.debug("Resolved '{}' -> {} via '{}'", ctx.name(), resolved, clientId);
                        ctx.resultFuture().complete(resolved);
                    } else {
                        ctx.resultFuture().complete(null);
                    }
                } catch (Exception e) {
                    Log.error(e, "Failed to parse RESOLVE_UUID_RESPONSE from '{}'", clientId);
                    ctx.resultFuture().complete(null);
                }
            } else {
                ctx.resultFuture().complete(null);
            }
            return env;
        }).exceptionally(ex -> {
            var cause = (ex.getCause() != null) ? ex.getCause() : ex;
            if (cause instanceof java.util.concurrent.TimeoutException) {
                Log.debug("RESOLVE_UUID timeout for '{}' from '{}'", ctx.name(), clientId);
            } else {
                Log.error(cause, "RESOLVE_UUID failed for '{}' from '{}'", ctx.name(), clientId);
            }
            ctx.resultFuture().complete(null);
            return null;
        });

        return op;
    }
}
