package dev.objz.commandbridge.velocity.net.out;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.velocity.net.out.ctx.DumpRequestContext;

public final class DumpRequest extends OutboundHandler<DumpRequestContext> {

    @Override
    public SendOperation accept(DumpRequestContext ctx) {
        String clientId = ctx.session.id();
        ObjectNode payload = Envelope.MAPPER.createObjectNode();
        payload.put("requested", "dump");

        Envelope env = Envelope.make(MessageType.DUMP_REQUEST, serverId, clientId, payload);

        return send(ctx.session.endpoint(), env)
                .expect(MessageType.DUMP_RESPONSE)
                .timeout(ctx.timeout);
    }
}
