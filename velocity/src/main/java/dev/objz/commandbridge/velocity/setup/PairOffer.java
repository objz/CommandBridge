package dev.objz.commandbridge.velocity.setup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.security.PairCrypto;
import dev.objz.commandbridge.velocity.editor.RelayChannel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Waits on the relay channel named by a pairing code until a server runs /cbc pair with it,
 * then hands that server its config, sealed with the code. One server per code.
 */
public final class PairOffer implements WebSocket.Listener {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final long VALID_MINUTES = 30;
    private static final int MAX_FRAME = 64 * 1024;
    private static final ScheduledExecutorService EXPIRY = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CommandBridge-pair-expiry");
        t.setDaemon(true);
        return t;
    });

    private final String serverId;
    private final String proxyId;
    private final String code;
    private final byte[] configJson;
    private final Consumer<JsonNode> onPaired;
    private final StringBuilder partial = new StringBuilder();
    private volatile WebSocket ws;
    private volatile ScheduledFuture<?> expiry;
    private boolean answered;
    private JsonNode hello;
    private volatile boolean closed;

    /** {@code onPaired} gets the server's hello ({@code platform}, {@code version}) once it saved the config. */
    public PairOffer(String serverId, String proxyId, String code, byte[] configJson, Consumer<JsonNode> onPaired) {
        this.serverId = serverId;
        this.proxyId = proxyId;
        this.code = code;
        this.configJson = configJson;
        this.onPaired = onPaired;
    }

    public CompletableFuture<PairOffer> start() {
        URI uri = URI.create(RelayChannel.RELAY_URL + "api/pair/" + PairCrypto.channelId(code) + "/ws?role=proxy");
        return HTTP.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + RelayChannel.AUTH_TOKEN)
                .buildAsync(uri, this)
                .thenApply(socket -> {
                    ws = socket;
                    expiry = EXPIRY.schedule(this::close, VALID_MINUTES, TimeUnit.MINUTES);
                    if (closed)
                        close();
                    return this;
                });
    }

    public String code() {
        return code;
    }

    public boolean isOpen() {
        return !closed;
    }

    public void close() {
        closed = true;
        ScheduledFuture<?> e = expiry;
        if (e != null)
            e.cancel(false);
        WebSocket socket = ws;
        if (socket != null && !socket.isOutputClosed())
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "pairing over");
    }

    @Override
    public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
        if (closed)
            return null;
        ws = socket;
        partial.append(data);
        if (partial.length() > MAX_FRAME) {
            // a hello is a few bytes; anything this big is not a server pairing
            Log.warn("Pairing {} got an oversized message and stopped", serverId);
            partial.setLength(0);
            close();
            return null;
        }
        if (last) {
            String frame = partial.toString();
            partial.setLength(0);
            try {
                handle(socket, MAPPER.readTree(frame));
            } catch (Exception e) {
                Log.warn("Pairing {} failed: {}", serverId, e.getMessage());
            }
        }
        socket.request(1);
        return null;
    }

    private synchronized void handle(WebSocket socket, JsonNode msg) throws Exception {
        if (closed)
            return;
        switch (msg.path("type").asText("")) {
            case "hello" -> {
                if (answered)
                    return;
                answered = true;
                hello = msg;
                String sealed = PairCrypto.seal(code, configJson);
                socket.sendText(MAPPER.writeValueAsString(Map.of("type", "config", "proxy", proxyId, "sealed", sealed)),
                        true).join();
            }
            case "done" -> {
                if (hello == null)
                    return;
                Log.info("Server {} paired", serverId);
                close();
                onPaired.accept(hello);
            }
            case "error" -> Log.warn("Server {} could not pair: {}", serverId, msg.path("message").asText(""));
            default -> Log.warn("Pairing {} got an unknown message type", serverId);
        }
    }

    @Override
    public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
        closed = true;
        ScheduledFuture<?> e = expiry;
        if (e != null)
            e.cancel(false);
        return null;
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
        Log.warn("Pairing {} lost the relay: {}", serverId, error.getMessage());
        closed = true;
        ScheduledFuture<?> e = expiry;
        if (e != null)
            e.cancel(false);
    }
}
