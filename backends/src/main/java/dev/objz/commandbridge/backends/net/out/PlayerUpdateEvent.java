package dev.objz.commandbridge.backends.net.out;

import dev.objz.commandbridge.backends.net.out.ctx.PlayerUpdateContext;
import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.util.PlayerUpdatePayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;

public final class PlayerUpdateEvent extends OutboundHandler<PlayerUpdateContext> {

    private final MessageType messageType;

    public PlayerUpdateEvent(MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public SendOperation accept(PlayerUpdateContext ctx) {
        var payload = Envelope.MAPPER.valueToTree(new PlayerUpdatePayload(ctx.player()));

        Envelope env = Envelope.make(
                messageType,
                clientId,
                serverId,
                payload);

        SendOperation op = send(env);
        op.dispatch();
        return op;
    }
}
