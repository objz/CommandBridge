package dev.objz.commandbridge.backends.net;

import dev.objz.commandbridge.backends.net.out.AuthRequest;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.InboundRouter;
import dev.objz.commandbridge.net.OutboundRouter;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.security.AuthService;
import dev.objz.commandbridge.security.SecretLoader;
import dev.objz.commandbridge.security.TlsResolver;
import dev.objz.commandbridge.security.TrustManager;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.client.WebSocketClient;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import org.xnio.IoFuture;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.ssl.JsseXnioSsl;
import org.xnio.ssl.XnioSsl;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class WsClient implements AutoCloseable {
	private final BackendsConfig cfg;
	private final Path dataDir;

	private XnioWorker worker;
	private ByteBufferPool pool;
	private XnioSsl ssl;

	private final InboundRouter inRouter = new InboundRouter();
	private final OutboundRouter outRouter = new OutboundRouter();
	private final ResponseAwaiter awaiter = new ResponseAwaiter();
	private volatile ClientStatus status = ClientStatus.DISCONNECTED;
	private volatile WebSocketChannel ch;

	public InboundRouter inboundRouter() {
		return inRouter;
	}

	public OutboundRouter outboundRouter() {
		return outRouter;
	}

	public WsClient(BackendsConfig cfg, Path dataDir) {
		this.cfg = Objects.requireNonNull(cfg);
		this.dataDir = Objects.requireNonNull(dataDir);
	}

	public synchronized void start() throws Exception {
		if (ch != null && ch.isOpen())
			return;

		String clientId = cfg.clientId();
		String sharedSecret = resolveSecret();
		AuthService auth = new AuthService(sharedSecret);

		TlsMode mode = (cfg.security() != null && cfg.security().tlsMode() != null)
				? cfg.security().tlsMode()
				: TlsMode.TOFU;

		String scheme = TlsResolver.schemeFor(mode);
		String url = scheme + "://" + cfg.host() + ":" + cfg.port() + "/ws";

		SSLContext sslContext = buildPinnedSslContext(mode);

		Xnio xnio = Xnio.getInstance("nio", getClass().getClassLoader());
		this.worker = xnio.createWorker(OptionMap.EMPTY);
		this.ssl = (TlsResolver.isTlsEnabled(mode) && sslContext != null)
				? new JsseXnioSsl(worker.getXnio(), OptionMap.EMPTY, sslContext)
				: null;

		Log.info("Connecting to {} as '{}'", url, clientId);

		this.pool = new DefaultByteBufferPool(
				/* direct */ false,
				/* bufferSize */ 16 * 1024,
				/* maximumPoolSize */ -1,
				/* threadLocalCacheSize */ 4,
				/* leakDetectionPercent */ 10);

		var builder = WebSocketClient.connectionBuilder(worker, pool, URI.create(url));
		if (ssl != null) {
			builder.setSsl(ssl);
		}
		IoFuture<WebSocketChannel> io = builder.connect();
		CompletableFuture<WebSocketChannel> f = new CompletableFuture<>();
		io.addNotifier((future, attachment) -> {
			switch (future.getStatus()) {
				case DONE -> {
					try {
						f.complete(future.get());
					} catch (Exception e) {
						f.completeExceptionally(e);
					}
				}
				case FAILED -> f.completeExceptionally(future.getException());
				case CANCELLED -> f.cancel(true);
				default -> {
					/* ignore */ }
			}
		}, null);

		this.ch = f.get();
		status = ClientStatus.CONNECTED;

		inRouter.setInboundTap(env -> {
			boolean matched = false;
			try {
				matched = awaiter.signal(env);
			} catch (Exception ignore) {
			}
			// boolean authed = (status == ClientStatus.AUTHENTICATED);
			// block if not authenticated and not an auth message
			// but right now remove it
			// TODO
			// if (!PreAuth.backendInboundAllowed(authed, env.type()))
			// return true;
			return matched;
		});

		ch.getReceiveSetter().set(new AbstractReceiveListener() {
			@Override
			protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
				try {
					inRouter.onText(channel, message.getData());
				} catch (Throwable t) {
					Log.error(t, "Inbound message handling failed");
				}
			}

			@Override
			protected void onFullCloseMessage(WebSocketChannel channel, BufferedBinaryMessage message) {
				try {
					Log.warn("WebSocket closed by server");
					IoUtils.safeClose(channel);
				} catch (Throwable ignore) {
				}
			}

			@Override
			protected void onClose(WebSocketChannel channel, StreamSourceFrameChannel frameChannel) {
				try {
					Log.warn("WebSocket closed");
					IoUtils.safeClose(channel);
				} catch (Throwable ignore) {
				}
			}
		});

		ch.resumeReceives();

		outRouter.register(MessageType.AUTH_REQUEST, new AuthRequest(clientId, auth, this));

		if (Boolean.TRUE.equals(cfg.security().requireAuth())) {
			//TODO: dont hardcode timeout
			var timeout = Duration.ofSeconds(120);
			outRouter.send(
					MessageType.AUTH_REQUEST,
					new AuthRequest.Args(
							ch,
							timeout,
							s -> this.status = s));
		} else {
			Log.warn("Auth disabled by config; continuing unauthenticated");
		}
	}

	public SendOperation send(Envelope request) {
		if (ch == null || !ch.isOpen())
			throw new IllegalStateException("WebSocket not connected");
		return new SendOperation(ch, request, awaiter);
	}

	private String resolveSecret() {
		String s = cfg.security() != null ? cfg.security().secret() : null;
		if (s != null && !s.isBlank())
			return s;
		return new SecretLoader(dataDir).loadOrCreate();
	}

	// close without sending status to server if not connected/authenticated
	public synchronized void close() throws Exception {
		try {
			if (ch != null) {
				try {
					ch.suspendReceives();
				} catch (Throwable ignore) {
				}

				if (status != ClientStatus.DISCONNECTED && status != ClientStatus.AUTH_FAILED) {
					if (ch.isOpen() && !ch.isCloseFrameSent() && !ch.isCloseFrameReceived()) {
						WebSockets.sendClose(
								CloseMessage.NORMAL_CLOSURE,
								"client closing",
								ch,
								new WebSocketCallback<>() {
									@Override
									public void complete(WebSocketChannel channel,
											Void context) {
										IoUtils.safeClose(channel);
									}

									@Override
									public void onError(WebSocketChannel channel,
											Void context, Throwable cause) {
										// close silently
										IoUtils.safeClose(channel);
									}
								});
					} else {
						IoUtils.safeClose(ch);
					}
				} else {
					// same here
					IoUtils.safeClose(ch);
				}

				ch = null;
			}

			if (inRouter != null)
				inRouter.setInboundTap(null);
		} finally {
			status = ClientStatus.DISCONNECTED;

			try {
				if (worker != null)
					worker.shutdown();
			} catch (Throwable ignore) {
			}
			try {
				if (worker != null)
					worker.awaitTermination(5, TimeUnit.SECONDS);
			} catch (Throwable ignore) {
			}

			ssl = null;
			worker = null;
			pool = null;
		}
	}

	public ClientStatus status() {
		return status;
	}

	private SSLContext buildPinnedSslContext(TlsMode mode) throws Exception {
		if (!TlsResolver.isTlsEnabled(mode))
			return null;

		String configuredPin = (cfg.security() != null) ? cfg.security().tlsPin() : null;
		TrustManager tm;
		try {
			tm = new TrustManager(mode, configuredPin);
		} catch (Exception e) {
			Log.error(e, "Failed to create TrustManager for TLS mode {}", mode);
			throw e;
		}
		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(/* keyManagers */ null, new javax.net.ssl.TrustManager[] { tm }, /* random */ null);

		if (mode == TlsMode.TOFU) {
			if (configuredPin != null && !configuredPin.isBlank()) {
				Log.debug("TLS mode=TOFU with configured pin (will verify)");
			} else {
				Log.debug("TLS mode=TOFU without configured pin (will auto pin in memory)");
			}
		}

		return ctx;
	}
}
