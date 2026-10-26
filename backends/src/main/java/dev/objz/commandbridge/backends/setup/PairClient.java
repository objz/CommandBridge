package dev.objz.commandbridge.backends.setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.config.model.BackendsConfig;
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

    public static final String UNPAIRED_HINT =
            "CommandBridge isn't paired with a proxy yet. Run /cb setup on the proxy, then /cbc pair <code> here";

    /** A config nobody changed can't reach a proxy: its secret is the placeholder. */
    public static boolean isUnpaired(BackendsConfig cfg) {
        return BackendsConfig.defaults().equals(cfg);
    }

    public interface Connector {
        CompletableFuture<WebSocket> connect(URI uri, WebSocket.Listener listener);
    }

    public record Paired(String proxyId, BackendsConfig config) {
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

    public CompletableFuture<Paired> pair(String typedCode, String platform, String version) {
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
        final CompletableFuture<Paired> result = new CompletableFuture<>();
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
                Path file = dataDir.resolve("config.yml");
                if (Files.exists(file)) {
                    Log.warn("Replacing config.yml, the old one is kept as config.yml.bak");
                    Files.copy(file, dataDir.resolve("config.yml.bak"), StandardCopyOption.REPLACE_EXISTING);
                }
                if (!new ConfigManager<>(dataDir, BackendsConfig.class).save(config))
                    throw new IllegalStateException("Could not write config.yml");
                send(ws, Map.of("type", "done"));
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "paired");
                result.complete(new Paired(msg.path("proxy").asText(""), config));
            } catch (Exception e) {
                send(ws, Map.of("type", "error", "message", String.valueOf(e.getMessage())));
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "pairing failed");
                result.completeExceptionally(new IllegalStateException("Pairing failed: " + e.getMessage(), e));
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
