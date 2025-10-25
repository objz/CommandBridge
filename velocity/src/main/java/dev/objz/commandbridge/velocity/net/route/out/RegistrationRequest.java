package dev.objz.commandbridge.velocity.net.route.out;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.objz.commandbridge.logging.FeedbackLog;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.proto.Envelope;
import dev.objz.commandbridge.proto.MessageType;
import dev.objz.commandbridge.proto.cmd.CommandStub;
import dev.objz.commandbridge.proto.cmd.payloads.RegisterCommands;
import dev.objz.commandbridge.proto.feedback.Feedback;
import dev.objz.commandbridge.velocity.net.WsServer;
import dev.objz.commandbridge.velocity.net.route.OutboundRouter;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.scripting.model.Script;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

public final class RegistrationRequest implements OutboundRouter.OutboundHandler<RegistrationRequest.Args>,
		OutboundRouter.Typed<RegistrationRequest.Args> {

	private final String serverId;
	private final ObjectMapper mapper;
	private final WsServer ws;

	public static final class Args {
		public final ClientSession session;
		public final Set<Script> scripts;
		public final Duration timeout;

		public Args(ClientSession session, Set<Script> scripts, Duration timeout) {
			this.session = Objects.requireNonNull(session);
			this.scripts = Objects.requireNonNull(scripts);
			this.timeout = Objects.requireNonNull(timeout);
		}
	}

	@Override
	public Class<Args> argType() {
		return Args.class;
	}

	public RegistrationRequest(String serverId, ObjectMapper mapper, WsServer ws) {
		this.serverId = Objects.requireNonNull(serverId);
		this.mapper = Objects.requireNonNull(mapper);
		this.ws = Objects.requireNonNull(ws);
	}

	@Override
	public CompletableFuture<Envelope> accept(Args a) {
		String clientId = a.session.id();

		List<CommandStub> stubs = a.scripts.stream().map(s -> {
			try {
				return new CommandStub(
						s.name(),
						s.aliases(),
						s.description(),
						s.args());
			} catch (Exception e) {
				Log.error(e, "Stub export failed for '{}'", s.name());
				return null;
			}
		}).filter(Objects::nonNull).collect(toList());

		if (stubs.isEmpty()) {
			Log.warn("No valid command stubs for '{}'", clientId);
			var cf = new CompletableFuture<Envelope>();
			cf.completeExceptionally(new IllegalStateException("no stubs"));
			return cf;
		}

		var payload = new RegisterCommands(stubs);
		ObjectNode payloadNode = mapper.valueToTree(payload);
		final Envelope env = Envelope.make(MessageType.REGISTER_COMMANDS, serverId, clientId, payloadNode);

		return ws.send(a.session.ch(), env)
				.expect(MessageType.FEEDBACK)
				.timeout(a.timeout)
				.await()
				.exceptionally(ex -> {
					var cause = (ex.getCause() != null) ? ex.getCause() : ex;
					if (cause instanceof java.util.concurrent.TimeoutException) {
						Log.error("Timeout from '{}' after {}", clientId, a.timeout);
					} else {
						Log.error(cause, "Failed to receive feedback from '{}'", clientId);
					}
					return null;
				})
				.thenApply(feedbackEnv -> {
					if (feedbackEnv != null) {
						try {
							Feedback feedback = mapper.treeToValue(feedbackEnv.payload(),
									Feedback.class);

							FeedbackLog.summary("Feedback", feedback, clientId);
							FeedbackLog.details(feedback, clientId);

						} catch (Exception e) {
							Log.error(e, "Failed to process registration feedback from '{}'",
									clientId);
						}
					}
					return env;
				});
	}
}
