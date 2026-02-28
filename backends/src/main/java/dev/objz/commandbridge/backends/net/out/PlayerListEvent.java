package dev.objz.commandbridge.backends.net.out;

import dev.objz.commandbridge.backends.net.out.ctx.PlayerListContext;
import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.util.PlayerListPayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;

public final class PlayerListEvent extends OutboundHandler<PlayerListContext> {

    @Override
    public SendOperation accept(PlayerListContext ctx) {
        var payload = Envelope.MAPPER.valueToTree(new PlayerListPayload(ctx.players()));

        Envelope env = Envelope.make(
                MessageType.PLAYER_LIST,
                clientId,
                serverId,
                payload);

        SendOperation op = send(env);
        op.dispatch();
        return op;
    }
}
