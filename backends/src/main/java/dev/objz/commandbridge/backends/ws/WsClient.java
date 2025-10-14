package dev.objz.commandbridge.backends.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.model.TlsMode;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.security.AuthService;
import dev.objz.commandbridge.security.TlsResolver;
import okhttp3.*;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

public final class WsClient extends WebSocketListener implements AutoCloseable {
	private final BackendsConfig cfg;
	private final Path dataDir;

	private final BackendsConfig.Security sec;
	private final TlsMode mode;
	private final String host;
	private final int port;

	private final boolean requireAuth;

	private volatile WebSocket socket;
	private volatile ClientState state = ClientState.DISCONNECTED;

	private final AuthService auth;
	private volatile String clientNonce;

	private volatile Handshake lastHandshake;

	private final OkHttpClient http;
	private final URI serverUri;
	private final ObjectMapper mapper = new ObjectMapper();
	private final MessageRouter router;

	public WsClient(BackendsConfig cfg, Path dataDir) {
		this.cfg = cfg;
		this.dataDir = dataDir;

		this.requireAuth = Boolean.TRUE.equals(cfg.security().requireAuth());

		this.sec = cfg.security();
		this.mode = sec.tlsMode();
		this.host = cfg.host();
		this.port = cfg.port();

		String scheme = TlsResolver.schemeFor(mode); // "ws" | "wss"
		this.serverUri = URI.create(scheme + "://" + host + ":" + port + "/ws");

		this.auth = new AuthService(sec.secret());

		OkHttpClient.Builder b = new OkHttpClient.Builder()
				.callTimeout(Duration.ZERO)
				.readTimeout(Duration.ZERO);

		if (mode != TlsMode.PLAIN) {
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

			String explicitPin = (sec.tlsPin() != null && !sec.tlsPin().isBlank()) ? sec.tlsPin().trim()
					: null;
			String tofuPin = loadTofuPinIfAny();

			if (explicitPin != null) {
				b.certificatePinner(new CertificatePinner.Builder().add(host, explicitPin).build());
				Log.info("TLS pin enabled for {}", host);
			} else if (tofuPin != null) {
				b.certificatePinner(new CertificatePinner.Builder().add(host, tofuPin).build());
				Log.info("TLS TOFU pin loaded for {}", host);
			} else if (mode == TlsMode.TOFU) {
				Log.info("TOFU will pin automatically after first successful auth");
			}
		}

		this.http = b.build();
		this.router = new MessageRouter(this, mapper, cfg);
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

	public boolean requireAuth() {
		return requireAuth;
	}

	public synchronized void start() {
		if (state != ClientState.DISCONNECTED)
			return;
		state = ClientState.CONNECTING;
		Request req = new Request.Builder()
				.url(serverUri.toString())
				.header("User-Agent", "CommandBridge-Backend (" + cfg.clientId() + ")")
				.build();
		this.socket = http.newWebSocket(req, this);
		Log.info("Connecting to {} ...", serverUri);
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

	// ---- public API

	public void send(String json) {
		WebSocket s = socket;
		if (s != null)
			s.send(json);
	}

	public void send(dev.objz.commandbridge.proto.Envelope env) {
		try {
			send(mapper.writeValueAsString(env));
		} catch (Exception e) {
			Log.error(e, "Failed to serialize envelope");
		}
	}

	public void markAuthenticated() {
		state = ClientState.AUTHENTICATED;
	}

	public void markNotAuthenticated() {
		state = ClientState.AUTHENTICATING;
	}

	public void persistTlsPinIfNeeded() {
		if (mode == TlsMode.PLAIN)
			return;
		if (sec.tlsPin() != null && !sec.tlsPin().isBlank())
			return;

		try {
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
			Files.writeString(pinFile, pin + System.lineSeparator(),
					StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
			Log.success(true, "Pinned TLS (TOFU) for {} -> '{}'", host, pin);
		} catch (Exception e) {
			Log.warn("Could not persist TLS TOFU pin: {}", e.toString());
		}
	}

	@Override
	public void onOpen(WebSocket webSocket, Response response) {
		this.socket = webSocket;
		this.lastHandshake = response.handshake();
		state = requireAuth ? ClientState.AUTHENTICATING : ClientState.AUTHENTICATED;

		if (requireAuth) {
			this.clientNonce = java.util.UUID.randomUUID().toString().replace("-", "");
			var payload = new com.fasterxml.jackson.databind.node.ObjectNode(mapper.getNodeFactory())
					.put("clientId", clientId())
					.put("nonce", clientNonce)
					.put("hmac", auth.sign(clientId(), clientNonce));
			send(dev.objz.commandbridge.proto.Envelope.make(
					dev.objz.commandbridge.proto.MessageType.AUTH, null, null, payload));
		}
		Log.success("Connected");
	}

	@Override
	public void onMessage(WebSocket webSocket, String text) {
		router.dispatch(text);
	}

	@Override
	public void onClosed(WebSocket webSocket, int code, String reason) {
		state = ClientState.DISCONNECTED;
		Log.warn("Disconnected: {} ({})", reason, code);
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
