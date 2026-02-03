package dev.objz.commandbridge.velocity.net;

import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.net.ResponseAwaiter;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.InNode;
import dev.objz.commandbridge.velocity.net.session.ClientSession;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import javax.net.ssl.SSLContext;

import org.xnio.IoUtils;

import java.io.IOException;
import java.net.BindException;

public final class WsServer {

	private final String host;
	private final int port;
	private final boolean tlsEnabled;
	private final SSLContext sslContext;
	private final SessionHub sessions;
	private final InNode inNode;

	private final ResponseAwaiter responses = new ResponseAwaiter();
	private Undertow server;

	public WsServer(String host, int port, SessionHub sessions, InNode inNode) {
		this(host, port, sessions, inNode, false, null);
	}

	public WsServer(String host, int port, SessionHub sessions, InNode inNode,
			boolean tlsEnabled, SSLContext sslContext) {
		this.host = host;
		this.port = port;
		this.sessions = sessions;
		this.inNode = inNode;
		this.tlsEnabled = tlsEnabled;
		this.sslContext = sslContext;

		this.inNode.setInboundTap(this::signalInbound);
		this.inNode.setSendOperationFactory(this::createSendOperation);
	}

	private SendOperation createSendOperation(WebSocketChannel ch, Envelope env) {
		return new SendOperation(ch, env, responses);
	}

	public void start() {
		WebSocketConnectionCallback cb = (WebSocketHttpExchange ex, WebSocketChannel ch) -> {

			ch.getReceiveSetter().set(new AbstractReceiveListener() {
				@Override
				protected void onFullTextMessage(WebSocketChannel c, BufferedTextMessage msg) {
					inNode.onText(c, msg.getData());
				}

				@Override
				protected void onFullCloseMessage(WebSocketChannel channel,
						BufferedBinaryMessage message) {
					try {
						var data = message.getData();
						data.close();
					} catch (Throwable t) {
						Log.debug("Failed to release close frame buffer: {}", t.getMessage());
					}
				}

				@Override
				protected void onClose(WebSocketChannel channel,
						StreamSourceFrameChannel frameChannel) {
					try {
						super.onClose(channel, frameChannel);
					} catch (IOException e) {
						Log.debug("Failed to buffer close frame: {}", e.getMessage());
					}

					try {
						Log.warn("WebSocket closed: {}", channel.getSourceAddress());
						IoUtils.safeClose(channel);
					} catch (Throwable ignore) {
					}
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
			Log.success(true, "WebSocket {} listening on '{}:{}'",
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
				for (ClientSession s : sessions) {
					close(s.ch());
				}
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
