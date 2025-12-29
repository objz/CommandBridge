package dev.objz.commandbridge.velocity.net.out;

import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.cmd.ExecuteCommand;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.velocity.net.out.ctx.ExecuteCommandContext;

public final class ExecuteCommandRequest extends OutboundHandler<ExecuteCommandContext> {

	@Override
	public SendOperation accept(ExecuteCommandContext ctx) {
		ExecuteCommand payload = new ExecuteCommand(
				ctx.command(),
				ctx.runAs(),
				ctx.uuid());

		Envelope env = Envelope.make(
				MessageType.EXECUTE_COMMAND,
				serverId,
				ctx.session().id(),
				Envelope.MAPPER.valueToTree(payload));

		SendOperation op = send(ctx.session().ch(), env);
		op.dispatch(); // Fire and forget ;O
		return op;
	}
}
