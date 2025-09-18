package dev.objz.commandbridge.backends.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.objz.commandbridge.backends.PlatformInterface;
import dev.objz.commandbridge.main.config.model.BackendsConfig;
import dev.objz.commandbridge.main.config.model.BackendsConfig.TlsMode;
import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.proto.Envelope;
import dev.objz.commandbridge.main.proto.MessageType;
import dev.objz.commandbridge.main.security.AuthService;
import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public final class WsClient extends WebSocketListener implements Closeable {
	private final BackendsConfig cfg;
	private final OkHttpClient http;
	private final ObjectMapper mapper = new ObjectMapper();
	private final MessageRouter router;
	private final PlatformInterface platform;
	private final Path dataDir;

	private volatile WebSocket socket;
	private volatile ClientState state = ClientState.DISCONNECTED;

	private final AuthService auth;
	private volatile String clientNonce;

	private volatile Handshake lastHandshake;

	public WsClient(BackendsConfig cfg, PlatformInterface platform, Path dataDir) {
		this.cfg = Objects.requireNonNull(cfg, "cfg");
		this.platform = Objects.requireNonNull(platform, "platform");
		this.dataDir = Objects.requireNonNull(dataDir, "dataDir");
		this.auth = new AuthService(cfg.secret());

		OkHttpClient.Builder b = new OkHttpClient.Builder()
				.callTimeout(Duration.ZERO)
				.readTimeout(Duration.ZERO);

		TlsMode mode = cfg.effectiveTlsMode();
		if (mode != TlsMode.PLAINTEXT) {
			try {
				if (mode == TlsMode.TOFU) {
					X509TrustManager tm = trustAllManager();
					SSLContext sc = SSLContext.getInstance("TLS");
					sc.init(null, new javax.net.ssl.TrustManager[] { tm },
							new java.security.SecureRandom());
					b.sslSocketFactory(sc.getSocketFactory(), tm)
							.hostnameVerifier((hostname, session) -> true);
				} 
			} catch (Exception e) {
				Log.error(e, "Failed to init TLS socket factory");
			}

			String explicitPin = (cfg.tlsPin() != null && !cfg.tlsPin().isBlank()) ? cfg.tlsPin().trim()
					: null;
			String tofuPin = loadTofuPinIfAny();
			String host = cfg.uri().getHost();

			if (explicitPin != null) {
				b.certificatePinner(new CertificatePinner.Builder().add(host, explicitPin).build());
				Log.info("TLS pin enabled for {}", host);
			} else if (tofuPin != null) {
				b.certificatePinner(new CertificatePinner.Builder().add(host, tofuPin).build());
				Log.info("TLS TOFU pin loaded for {}", host);
			} else if (mode == TlsMode.TOFU) {
				Log.warn("TOFU will pin automatically after first successful auth");
			}
		}

		this.http = b.build();
		this.router = new MessageRouter(this, mapper, platform);
	}

	public String clientId() {
		return cfg.clientId();
	}

	public ClientState state() {
		return state;
	}

	public AuthService auth() {
		return auth;
	}

	public String getClientNonce() {
		return clientNonce;
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
		if (s != null)
			try {
				s.close(1000, "shutdown");
			} catch (Throwable ignored) {
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
		this.clientNonce = java.util.UUID.randomUUID().toString().replace("-", "");
		ObjectNode payload = mapper.createObjectNode();
		payload.put("clientId", cfg.clientId());
		payload.put("nonce", clientNonce);
		payload.put("hmac", auth.sign(cfg.clientId(), clientNonce));
		send(Envelope.make(MessageType.AUTH, cfg.clientId(), null, payload));
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
		lastHandshake = response != null ? response.handshake() : null;
		Log.success("Connected (HTTP {})", response != null ? response.code() : 101);
		sendAuth();
	}

	@Override
	public void onMessage(WebSocket webSocket, String text) {
		router.dispatch(text);
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
		if (t instanceof java.io.EOFException)
			Log.warn("Server closed the connection (EOF)");
		else if (t instanceof java.net.ConnectException)
			Log.warn("Cannot connect (connection refused)");
		else
			Log.error(t, "WS failure");
	}

	public void persistTlsPinIfNeeded() {
		if (!cfg.isTlsEnabled())
			return;
		if (cfg.tlsPin() != null && !cfg.tlsPin().isBlank())
			return;

		try {
			String host = cfg.uri().getHost();
			Path pinFile = dataDir.resolve("tls.pin");
			if (Files.exists(pinFile))
				return;

			if (lastHandshake == null)
				return;
			List<Certificate> chain = lastHandshake.peerCertificates();
			if (chain == null || chain.isEmpty())
				return;

			String pin = spkiPin((X509Certificate) chain.get(0));
			Files.createDirectories(dataDir);
			Files.writeString(pinFile, pin + System.lineSeparator(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE_NEW);
			Log.success(true, "Pinned TLS (TOFU) for {} -> '{}'", host, pin);
		} catch (Exception e) {
			Log.warn("Could not persist TLS TOFU pin: {}", e.toString());
		}
	}

	private String loadTofuPinIfAny() {
		try {
			Path pinFile = dataDir.resolve("tls.pin");
			if (Files.exists(pinFile)) {
				String pin = Files.readString(pinFile, StandardCharsets.UTF_8).trim();
				return pin.isEmpty() ? null : pin;
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private static String spkiPin(X509Certificate cert) throws Exception {
		byte[] spki = cert.getPublicKey().getEncoded();
		byte[] sha = MessageDigest.getInstance("SHA-256").digest(spki);
		return "sha256/" + Base64.getEncoder().encodeToString(sha);
	}

	private static X509TrustManager trustAllManager() {
		return new X509TrustManager() {
			public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
			}

			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[0];
			}
		};
	}
}
