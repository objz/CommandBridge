package dev.objz.commandbridge.main.ws;

import dev.objz.commandbridge.main.core.CommandRouter;
import dev.objz.commandbridge.main.logging.Log;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import javax.net.ssl.SSLContext;

public final class WsServer {
	private final CommandRouter router;
	private final SessionHub sessions;
	private final String host;
	private final int port;
	private final boolean tlsEnabled;
	private final SSLContext sslContext;

	private Undertow server;

	public WsServer(String host, int port, CommandRouter router, SessionHub sessions) {
		this(host, port, router, sessions, false, null);
	}

	public WsServer(String host, int port, CommandRouter router, SessionHub sessions, boolean tlsEnabled,
			SSLContext sslContext) {
		this.host = host;
		this.port = port;
		this.router = router;
		this.sessions = sessions;
		this.tlsEnabled = tlsEnabled;
		this.sslContext = sslContext;
	}

	public void start() {
		WebSocketConnectionCallback cb = (WebSocketHttpExchange ex, WebSocketChannel ch) -> {
			sessions.register(ch);
			ch.getReceiveSetter().set(new AbstractReceiveListener() {
				@Override
				protected void onFullTextMessage(WebSocketChannel c, BufferedTextMessage m) {
					router.onText(c, m.getData());
				}

				@Override
				protected void onClose(WebSocketChannel c, StreamSourceFrameChannel ignored) {
					sessions.remove(c);
				}
			});
			ch.resumeReceives();
		};

		Undertow.Builder builder = Undertow.builder()
				.setHandler(Handlers.path().addPrefixPath("/ws", Handlers.websocket(cb)));

		if (tlsEnabled) {
			if (sslContext == null) {
				throw new IllegalStateException("TLS is enabled but SSLContext is null");
			}
			builder.addHttpsListener(port, host, sslContext);
			Log.success("WebSocket TLS listening on {}:{}", host, port);
		} else {
			builder.addHttpListener(port, host);
			Log.warn("TLS is disabled! This is insecure and not recommended for production use");
			Log.success("WebSocket HTTP listening on {}:{}", host, port);
		}

		server = builder.build();
		server.start();
		sessions.start();
	}

	public void stop() {
		if (server != null)
			server.stop();
		sessions.stop();
		Log.info("WebSocket server stopped");
	}
}
