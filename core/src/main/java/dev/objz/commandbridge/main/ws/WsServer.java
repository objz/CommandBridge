package dev.objz.commandbridge.main.ws;

import dev.objz.commandbridge.main.core.CommandRouter;
import dev.objz.commandbridge.main.logging.Log;
import io.undertow.Undertow;
import io.undertow.Handlers;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

public final class WsServer {
	private final CommandRouter router;
	private final SessionHub sessions;
	private final String host;
	private final int port;
	private Undertow server;

	public WsServer(String host, int port, CommandRouter router, SessionHub sessions) {
		this.host = host;
		this.port = port;
		this.router = router;
		this.sessions = sessions;
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
				protected void onClose(WebSocketChannel c,
						StreamSourceFrameChannel ignored) {
					sessions.remove(c);
				}
			});
			ch.resumeReceives();
		};
		server = Undertow.builder()
				.addHttpListener(port, host)
				.setHandler(Handlers.path().addPrefixPath("/ws", Handlers.websocket(cb)))
				.build();
		server.start();
		sessions.start();
		Log.success("WebSocket listening on {}:{}", host, port);
	}

	public void stop() {
		if (server != null)
			server.stop();
		sessions.stop();
		Log.info("WebSocket server stopped");
	}
}
