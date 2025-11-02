package dev.objz.commandbridge.backends.net.out;

import dev.objz.commandbridge.backends.net.out.ctx.InvokedCommandContext;
import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;

public final class InvokedCommandEvent extends OutboundHandler<InvokedCommandContext> {

	@Override
	public SendOperation accept(InvokedCommandContext ctx) {
		var payload = Envelope.MAPPER.valueToTree(
				new InvokedCommand(ctx.commandName, ctx.arguments, ctx.sender));
		Envelope env = Envelope.make(MessageType.INVOKED_COMMAND, clientId, serverId, payload);
		SendOperation op = send(env);
		op.dispatch();
		return op;
	}
}
