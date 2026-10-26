package dev.objz.commandbridge.backends.setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
import dev.objz.commandbridge.config.profile.BackendsConfigProfile;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.security.PairCrypto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * The server's side of /cbc pair: joins the relay channel named by the code, takes the config
 * the proxy sealed with that code, and saves it as config.yml.
 */
public final class PairClient {

    public static final String RELAY_URL = System.getProperty("commandbridge.relay.url", "wss://cb.objz.dev/");
    static final String NO_SETUP = "No setup is waiting for this code. Check it, or run /cb setup again on the proxy";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_FRAME = 64 * 1024;

    public static final String UNPAIRED_HINT =
            "CommandBridge isn't paired with a proxy yet. Run /cb setup on the proxy, then /cbc pair <code> here";

    /** A config nobody changed can't reach a proxy: its secret is the placeholder. */
    public static boolean isUnpaired(BackendsConfig cfg) {
        return BackendsConfig.defaults().equals(cfg);
    }

    public interface Connector {
        CompletableFuture<WebSocket> connect(URI uri, WebSocket.Listener listener);
    }

    /**
     * A config the proxy sent and nothing written yet. Whoever runs /cbc pair sees where it would connect and
     * confirms or cancels, so a code from someone else's setup page can't quietly move the server to their proxy.
     */
    public final class Pending {
        private final String proxyId;
        private final BackendsConfig config;
        private final WebSocket ws;
        private final java.util.concurrent.atomic.AtomicBoolean settled = new java.util.concurrent.atomic.AtomicBoolean();

        Pending(String proxyId, BackendsConfig config, WebSocket ws) {
            this.proxyId = proxyId;
            this.config = config;
            this.ws = ws;
        }

        public String proxyId() {
            return proxyId;
        }

        public BackendsConfig config() {
            return config;
        }

        /** Where the server would connect to. */
        public String where() {
            if (config.endpointType() == dev.objz.commandbridge.config.model.EndpointType.REDIS)
                return "Redis at " + config.endpoints().redis().host() + ":" + config.endpoints().redis().port();
            return config.endpoints().websocket().host() + ":" + config.endpoints().websocket().port();
        }

        /** Saves the config (the old one kept as config.yml.bak) and tells the proxy. */
        public CompletableFuture<BackendsConfig> confirm() {
            if (!settled.compareAndSet(false, true))
                return CompletableFuture.failedFuture(new IllegalStateException("This pairing is over"));
            try {
                Path file = dataDir.resolve("config.yml");
                if (Files.exists(file)) {
                    Log.warn("Replacing config.yml, the old one is kept as config.yml.bak");
                    Files.copy(file, dataDir.resolve("config.yml.bak"), StandardCopyOption.REPLACE_EXISTING);
                }
                if (!new ConfigManager<>(dataDir, BackendsConfig.class).save(config))
                    throw new IllegalStateException("Could not write config.yml");
                ws.sendText(MAPPER.writeValueAsString(Map.of("type", "done")), true);
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "paired");
                return CompletableFuture.completedFuture(config);
            } catch (Exception e) {
                cancelWith("Could not save the config on the server");
                return CompletableFuture.failedFuture(new IllegalStateException("Pairing failed: " + e.getMessage(), e));
            }
        }

        public void cancel() {
            if (settled.compareAndSet(false, true))
                cancelWith("cancelled on the server");
        }

        private void cancelWith(String reason) {
            try {
                ws.sendText(MAPPER.writeValueAsString(Map.of("type", "error", "message", reason)), true);
            } catch (Exception ignored) {
            }
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "pairing cancelled");
        }
    }

    private final Path dataDir;
    private final String relayBase;
    private final Connector connector;

    public PairClient(Path dataDir, String relayBase, Connector connector) {
        this.dataDir = dataDir;
        this.relayBase = relayBase;
        this.connector = connector;
    }

    public static PairClient overRelay(Path dataDir) {
        HttpClient http = HttpClient.newHttpClient();
        return new PairClient(dataDir, RELAY_URL, (uri, listener) -> http.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(uri, listener));
    }

    public CompletableFuture<Pending> pair(String typedCode, String platform, String version) {
        String code;
        try {
            code = PairCrypto.normalize(typedCode);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(e);
        }
        URI uri = URI.create(relayBase + "api/pair/" + PairCrypto.channelId(code) + "/ws?role=server");
        Session session = new Session(code, platform, version);
        connector.connect(uri, session).whenComplete((ws, error) -> {
            if (error != null)
                session.result.completeExceptionally(explain(error));
        });
        return session.result;
    }

    private static Throwable explain(Throwable error) {
        Throwable t = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        if (t instanceof WebSocketHandshakeException h && h.getResponse().statusCode() == 404)
            return new IllegalStateException(NO_SETUP);
        return new IllegalStateException("Could not reach the relay: " + t.getMessage(), t);
    }

    private final class Session implements WebSocket.Listener {
        final CompletableFuture<Pending> result = new CompletableFuture<>();
        private final String code;
        private final String platform;
        private final String version;
        private final StringBuilder partial = new StringBuilder();

        Session(String code, String platform, String version) {
            this.code = code;
            this.platform = platform;
            this.version = version;
        }

        @Override
        public void onOpen(WebSocket ws) {
            send(ws, Map.of("type", "hello", "platform", platform, "version", version));
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            partial.append(data);
            if (partial.length() > MAX_FRAME) {
                result.completeExceptionally(new IllegalStateException("The proxy sent more than a config"));
                ws.abort();
                return null;
            }
            if (last) {
                String frame = partial.toString();
                partial.setLength(0);
                handle(ws, frame);
            }
            ws.request(1);
            return null;
        }

        private void handle(WebSocket ws, String frame) {
            if (result.isDone())
                return;
            try {
                JsonNode msg = MAPPER.readTree(frame);
                if (!"config".equals(msg.path("type").asText("")))
                    return;
                BackendsConfig config = MAPPER.readValue(PairCrypto.open(code, msg.path("sealed").asText("")),
                        BackendsConfig.class);
                if (config == null || config.endpoints() == null || config.endpoints().websocket() == null
                        || config.endpoints().redis() == null || config.security() == null || config.timeouts() == null)
                    throw new IllegalArgumentException("Incomplete config");
                var normalized = new BackendsConfigProfile().normalize(config);
                if (!normalized.ok())
                    throw new IllegalArgumentException("Invalid config");
                result.complete(new Pending(msg.path("proxy").asText(""), normalized.config(), ws));
            } catch (Exception e) {
                // Parser errors can quote decrypted values; never send them back over the unencrypted relay.
                Log.warn("Pairing rejected an unreadable or invalid config ({})", e.getClass().getSimpleName());
                String message = "The proxy sent an unreadable or invalid config";
                send(ws, Map.of("type", "error", "message", message));
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "pairing failed");
                result.completeExceptionally(new IllegalStateException(message));
            }
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            result.completeExceptionally(new IllegalStateException("The proxy ended the pairing before sending a config"));
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            result.completeExceptionally(explain(error));
        }

        private void send(WebSocket ws, Map<String, String> message) {
            try {
                ws.sendText(MAPPER.writeValueAsString(message), true);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        }
    }
}
