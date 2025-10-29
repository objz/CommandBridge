package dev.objz.commandbridge.backends.net.out;

import dev.objz.commandbridge.backends.net.ClientStatus;
import dev.objz.commandbridge.backends.net.WsClient;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.OutboundRouter;
import dev.objz.commandbridge.net.payloads.util.AuthRequestPayload;
import dev.objz.commandbridge.net.payloads.util.AuthResponsePayload;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.security.AuthService;
import io.undertow.websockets.core.WebSocketChannel;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class AuthRequest implements OutboundRouter.OutboundHandler<AuthRequest.Args>,
		OutboundRouter.Typed<AuthRequest.Args> {

	private final String clientId;
	private final AuthService auth;
	private final WsClient ws;

	public AuthRequest(String clientId, AuthService auth, WsClient ws) {
		this.clientId = Objects.requireNonNull(clientId);
		this.auth = Objects.requireNonNull(auth);
		this.ws = Objects.requireNonNull(ws);
	}

	@Override
	public Class<Args> argType() {
		return Args.class;
	}

	public static final class Args {
		public final WebSocketChannel ch;
		public final Duration timeout;
		public final Consumer<ClientStatus> statusSink;

		public Args(WebSocketChannel ch, Duration timeout, Consumer<ClientStatus> statusSink) {
			this.ch = Objects.requireNonNull(ch);
			this.timeout = Objects.requireNonNull(timeout);
			this.statusSink = Objects.requireNonNull(statusSink);
		}
	}

	@Override
	public CompletableFuture<Envelope> accept(AuthRequest.Args a) {
		final String clientNonce = UUID.randomUUID().toString();
		final String mac = auth.sign(clientId, clientNonce);

		var payload = Envelope.MAPPER.valueToTree(new AuthRequestPayload(clientNonce, mac));
		Envelope env = Envelope.make(MessageType.AUTH_REQUEST, clientId, "proxy-auth", payload);

		return ws.send(env)
				.match(reply -> reply.id().equals(env.id())
						&& (reply.type() == MessageType.AUTH_OK
								|| reply.type() == MessageType.AUTH_FAIL))
				.timeout(a.timeout)
				.await()
				.thenApply(reply -> {
					if (reply.type() == MessageType.AUTH_FAIL) {
						a.statusSink.accept(ClientStatus.AUTH_FAILED);
						Log.error("Authentication rejected by server");
						return reply;
					}

					AuthResponsePayload sp;
					try {
						sp = Envelope.MAPPER.treeToValue(reply.payload(),
								AuthResponsePayload.class);
					} catch (Exception e) {
						a.statusSink.accept(ClientStatus.AUTH_FAILED);
						Log.error(e, "Authentication response malformed");
						return reply;
					}

					final String serverNonce = sp.serverNonce();
					final String serverMac = sp.hmac();
					final boolean ok = auth.verifyServerProof(clientId, clientNonce, serverNonce,
							serverMac);

					if (!ok) {
						a.statusSink.accept(ClientStatus.AUTH_FAILED);
						Log.error("Authentication failed: invalid server proof");
					} else {
						a.statusSink.accept(ClientStatus.AUTH_OK);
						Log.success("Authenticated successfully");
					}
					return reply;
				})
				.exceptionally(ex -> {
					a.statusSink.accept(ClientStatus.AUTH_FAILED);
					var cause = (ex.getCause() != null) ? ex.getCause() : ex;
					if (cause instanceof java.util.concurrent.TimeoutException) {
						Log.error("Authentication timeout");
					} else {
						Log.error(cause, "Authentication error");
					}
					return null;
				});
	}

}
