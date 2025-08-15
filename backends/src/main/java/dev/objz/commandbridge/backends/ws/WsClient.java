package dev.objz.commandbridge.backends.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.objz.commandbridge.main.config.model.BackendsConfig;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import okhttp3.*;

import java.io.Closeable;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class WsClient extends WebSocketListener implements Closeable {
	private final BackendsConfig cfg;
	private final OkHttpClient http;
	private final ObjectMapper mapper = new ObjectMapper();
	private final IncomingDispatcher dispatcher;

	private volatile WebSocket socket;
	private volatile ClientState state = ClientState.DISCONNECTED;

	public WsClient(BackendsConfig cfg) {
		this.cfg = cfg;
		OkHttpClient.Builder b = new OkHttpClient.Builder()
				.callTimeout(Duration.ZERO)
				.readTimeout(Duration.ZERO);

		if (cfg.tls()) {
			try {
				X509TrustManager tm = trustAllManager();
				SSLContext sc = SSLContext.getInstance("TLS");
				sc.init(null, new TrustManager[] { tm }, new SecureRandom());
				b.sslSocketFactory(sc.getSocketFactory(), tm)
						.hostnameVerifier((hostname, session) -> true);
			} catch (Exception e) {
				Log.error(e, "Failed to init insecure TLS context");
			}
		}

		this.http = b.build();
		this.dispatcher = new IncomingDispatcher(this, mapper);
	}

	public String clientId() {
		return cfg.clientId();
	}

	public ClientState state() {
		return state;
	}

	public synchronized void start() {
		if (state != ClientState.DISCONNECTED)
			return;
		state = ClientState.CONNECTING;

		Request req = new Request.Builder()
				.url(cfg.uri().toString())
				.header("User-Agent", "CommandBridge-Backend (" + cfg.clientId() + ")")
				.build();

		this.socket = http.newWebSocket(req, this);
		Log.info("Connecting to {} ...", cfg.uri());
	}

	@Override
	public synchronized void close() {
		WebSocket s = socket;
		socket = null;
		state = ClientState.DISCONNECTED;
		if (s != null) {
			try {
				s.close(1000, "shutdown");
			} catch (Throwable ignored) {
			}
		}
		http.dispatcher().executorService().shutdown();
		http.connectionPool().evictAll();
	}

	public void send(Envelope env) {
		WebSocket s = socket;
		if (s == null)
			return;
		try {
			s.send(mapper.writeValueAsString(env));
		} catch (Exception e) {
			Log.error(e, "WS send failed");
		}
	}

	private void sendAuth() {
		ObjectNode payload = mapper.createObjectNode();
		payload.put("clientId", cfg.clientId());
		payload.put("secret", cfg.secret());
		Envelope env = Envelope.make(MessageType.AUTH, cfg.clientId(), null, payload);
		send(env);
	}

	public void markAuthenticated() {
		state = ClientState.AUTHENTICATED;
	}

	public void markNotAuthenticated() {
		state = ClientState.DISCONNECTED;
		close();
	}

	@Override
	public void onOpen(WebSocket webSocket, Response response) {
		state = ClientState.AUTHENTICATING;
		Log.success("Connected (HTTP {})", response != null ? response.code() : 101);
		sendAuth();
	}

	@Override
	public void onMessage(WebSocket webSocket, String text) {
		dispatcher.dispatch(text);
	}

	@Override
	public void onClosing(WebSocket webSocket, int code, String reason) {
		webSocket.close(code, reason);
	}

	@Override
	public void onClosed(WebSocket webSocket, int code, String reason) {
		state = ClientState.DISCONNECTED;
		Log.info("Disconnected ({} {})", code, reason);
	}

	@Override
	public void onFailure(WebSocket webSocket, Throwable t, Response r) {
		state = ClientState.DISCONNECTED;
		if (t instanceof java.io.EOFException) {
			Log.warn("Server closed the connection (EOF)");
		} else if (t instanceof java.net.ConnectException) {
			Log.warn("Cannot connect (connection refused)");
		} else {
			Log.error(t, "WS failure");
		}
	}

	private static X509TrustManager trustAllManager() {
		return new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] chain, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] chain, String authType) {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		};
	}
}
