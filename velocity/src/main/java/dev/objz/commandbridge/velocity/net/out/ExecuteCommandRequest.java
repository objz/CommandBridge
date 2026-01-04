package dev.objz.commandbridge.velocity.net.out;

import dev.objz.commandbridge.logging.Log;
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
				ctx.uuid(),
				ctx.grantedPermissions());

		Log.debug("Sending EXECUTE_COMMAND to '{}':  command='{}', runAs={}, uuid={}, permissions={}",
				ctx.session().id(),
				ctx.command(),
				ctx.runAs(),
				ctx.uuid(),
				ctx.grantedPermissions() != null ? ctx.grantedPermissions().size() : 0);

		Envelope env = Envelope.make(
				MessageType.EXECUTE_COMMAND,
				serverId,
				ctx.session().id(),
				Envelope.MAPPER.valueToTree(payload));

		SendOperation op = send(ctx.session().ch(), env);
		op.dispatch()
				.exceptionally(ex -> {
					Log.error(ex, "Failed to send EXECUTE_COMMAND to '{}'", ctx.session().id());
					return null;
				});
		return op;
	}
}
