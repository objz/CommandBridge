package dev.objz.commandbridge.backends.net.out;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import dev.objz.commandbridge.backends.net.WsClient;
import dev.objz.commandbridge.net.OutboundRouter;
import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.net.payloads.cmd.SenderContext;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;

public final class InvokedCommandEvent implements OutboundRouter.OutboundHandler<InvokedCommandEvent.Args>,
		OutboundRouter.Typed<InvokedCommandEvent.Args> {

	private final String clientId;
	private final WsClient ws;

	public InvokedCommandEvent(String clientId, WsClient ws) {
		this.clientId = Objects.requireNonNull(clientId);
		this.ws = Objects.requireNonNull(ws);
	}

	@Override
	public Class<Args> argType() {
		return Args.class;
	}

	public static final class Args {
		public final String commandName;
		public final List<InvokedCommand.TypedArgument> arguments;
		public final SenderContext sender;

		public Args(String commandName, java.util.List<InvokedCommand.TypedArgument> arguments, SenderContext sender) {
			this.commandName = java.util.Objects.requireNonNull(commandName);
			this.arguments = java.util.Objects.requireNonNullElseGet(arguments, java.util.List::of);
			this.sender = java.util.Objects.requireNonNull(sender);
		}
	}

	@Override
	public CompletableFuture<Envelope> accept(InvokedCommandEvent.Args a) {
		var payload = Envelope.MAPPER.valueToTree(new InvokedCommand(a.commandName, a.arguments, a.sender));
		Envelope env = Envelope.make(MessageType.INVOKED_COMMAND, clientId, ws.serverId(), payload);
		return ws.send(env).dispatch().thenApply(resp -> env);
	}

}
