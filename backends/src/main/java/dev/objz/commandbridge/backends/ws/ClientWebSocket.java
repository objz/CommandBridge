package dev.objz.commandbridge.backends.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.objz.commandbridge.main.config.model.BackendsConfig;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import okhttp3.*;

import java.io.Closeable;
import java.time.Duration;

public final class ClientWebSocket extends WebSocketListener implements Closeable {
	private final BackendsConfig cfg;
	private final OkHttpClient http;
	private final ObjectMapper mapper = new ObjectMapper();
	private final IncomingDispatcher dispatcher;

	private volatile WebSocket socket;
	private volatile ClientState state = ClientState.DISCONNECTED;

	public ClientWebSocket(BackendsConfig cfg) {
		this.cfg = cfg;
		this.http = new OkHttpClient.Builder()
				.callTimeout(Duration.ZERO)
				.readTimeout(Duration.ZERO)
				.build();
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
			Log.warn("Server closed the connection (EOF).");
		} else if (t instanceof java.net.ConnectException) {
			Log.warn("Cannot connect (connection refused).");
		} else {
			Log.error(t, "WS failure");
		}
	}
}
