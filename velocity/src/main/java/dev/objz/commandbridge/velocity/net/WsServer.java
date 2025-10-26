package dev.objz.commandbridge.velocity.net;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.InboundRouter;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import javax.net.ssl.SSLContext;

import java.net.BindException;

public final class WsServer {

	private final String host;
	private final int port;
	private final boolean tlsEnabled;
	private final SSLContext sslContext;
	private final SessionHub sessions;
	private final InboundRouter inRouter;

	private final ResponseAwaiter responses = new ResponseAwaiter();
	private Undertow server;

	public WsServer(String host, int port, SessionHub sessions, InboundRouter router) {
		this(host, port, sessions, router, false, null);
	}

	public WsServer(String host, int port, SessionHub sessions, InboundRouter router,
			boolean tlsEnabled, SSLContext sslContext) {
		this.host = host;
		this.port = port;
		this.sessions = sessions;
		this.inRouter = router;
		this.tlsEnabled = tlsEnabled;
		this.sslContext = sslContext;

		this.inRouter.setInboundTap(this::signalInbound);
	}

	public void start() {
		WebSocketConnectionCallback cb = (WebSocketHttpExchange ex, WebSocketChannel ch) -> {
			ch.getReceiveSetter().set(new AbstractReceiveListener() {
				@Override
				protected void onFullTextMessage(WebSocketChannel c, BufferedTextMessage msg) {
					inRouter.onText(c, msg.getData());
				}
			});

			ch.resumeReceives();
		};

		var builder = Undertow.builder()
				.setHandler(Handlers.path().addPrefixPath("/ws", Handlers.websocket(cb)));

		try {
			if (tlsEnabled) {
				if (sslContext == null)
					throw new IllegalStateException("TLS is enabled but SSLContext is null");
				builder.addHttpsListener(port, host, sslContext);
				Log.info("Starting WebSocket TLS on {}:{}", host, port);
			} else {
				builder.addHttpListener(port, host);
				Log.warn("TLS is disabled. This is insecure for production.");
				Log.info("Starting WebSocket HTTP on {}:{}", host, port);
			}
			server = builder.build();
			server.start();
			Log.success(true, "WebSocket {} listening on {}:{}",
					tlsEnabled ? "TLS" : "HTTP", host, port);
		} catch (Exception e) {
			try {
				if (server != null)
					server.stop();
			} catch (Exception ignore) {
			}
			var cause = (e.getCause() != null) ? e.getCause() : e;
			if (cause instanceof BindException be) {
				Log.error("WebSocket {} failed to bind on {}:{} ({})",
						tlsEnabled ? "TLS" : "HTTP", host, port, be.getMessage());
			} else {
				Log.error(e, "WebSocket {} failed to start on {}:{}", tlsEnabled ? "TLS" : "HTTP", host,
						port);
			}
			throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
		}
	}

	public void stop() {
		try {
			if (server != null)
				server.stop();
		} catch (Exception ignore) {
		}
		sessions.clear();
		Log.info("WebSocket server stopped");
	}

	public SendOperation send(WebSocketChannel ch, Envelope request) {
		return new SendOperation(ch, request, responses);
	}

	// always safe close
	public void close(WebSocketChannel ch) {
		if (ch == null)
			return;

		if (!ch.isOpen() || ch.isCloseFrameSent() || ch.isCloseFrameReceived()) {
			try {
				org.xnio.IoUtils.safeClose(ch);
			} finally {
				sessions.remove(ch);
			}
			return;
		}

		try {
			ch.suspendReceives();
		} catch (Throwable ignore) {
		}

		ch.addCloseTask(c -> {
			sessions.remove(c);
			org.xnio.IoUtils.safeClose(c);
		});

		WebSockets.sendClose(
				CloseMessage.GOING_AWAY,
				"server closing",
				ch,
				new WebSocketCallback<Void>() {
					@Override
					public void complete(WebSocketChannel channel, Void context) {
						try {
							org.xnio.IoUtils.safeClose(channel);
						} finally {
							sessions.remove(channel);
						}
					}

					@Override
					public void onError(WebSocketChannel channel, Void context, Throwable cause) {
						try {
							org.xnio.IoUtils.safeClose(channel);
						} finally {
							sessions.remove(channel);
						}
					}
				});
	}

	private boolean signalInbound(Envelope env) {
		try {
			return responses.signal(env);
		} catch (Exception ex) {
			Log.debug("Awaiter signal failed: {}", ex.toString());
			return false;
		}
	}

}
