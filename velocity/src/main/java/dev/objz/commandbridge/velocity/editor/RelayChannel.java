package dev.objz.commandbridge.velocity.editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.security.RelayCrypto;

import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A channel between the proxy and a page on cb.objz.dev. The proxy connects out to the relay,
 * the page joins the same channel, and every message is encrypted with a key from the secret that only lives
 * in the link's #fragment ({@link RelayCrypto}), so the relay can neither read nor forge messages.
 */
public final class RelayChannel implements WebSocket.Listener {

    public static final String RELAY_URL = System.getProperty("commandbridge.relay.url", "wss://cb.objz.dev/");
    private static final String WEB_URL = System.getProperty("commandbridge.web.url", "https://cb.objz.dev/");
    public static final String AUTH_TOKEN = "cb-dump-upload-v1";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_FRAME = 4 * 1024 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    // the page pings every minute; a tab gone this long, or a link nobody opened, ends the session
    private static final long GONE_AFTER_MS = TimeUnit.MINUTES.toMillis(5);
    private static final long UNUSED_AFTER_MS = TimeUnit.MINUTES.toMillis(15);
    private static final ScheduledExecutorService WATCH = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CommandBridge-relay-watch");
        t.setDaemon(true);
        return t;
    });

    /** Handles one verified message from the page. */
    public interface Handler {
        void accept(JsonNode message) throws Exception;
    }

    private final String name;
    private final String pagePath;
    private final Handler onMessage;
    private final Runnable onClosed;
    private final long openedAt = System.currentTimeMillis();
    private volatile long lastSeen;
    private volatile ScheduledFuture<?> watch;
    private final String id;
    private final byte[] secret;
    private final SecretKeySpec key;

    private final StringBuilder partial = new StringBuilder();
    private volatile WebSocket ws;
    private volatile boolean closed;
    private long sendSeq;
    private final Map<String, Long> recvSeqByConn = new HashMap<>();
    private volatile String conn = "";

    private RelayChannel(String name, String pagePath, Handler onMessage, Runnable onClosed) {
        this.name = name;
        this.pagePath = pagePath;
        this.onMessage = onMessage;
        this.onClosed = onClosed;
        byte[] idBytes = new byte[16];
        RANDOM.nextBytes(idBytes);
        this.id = HexFormat.of().formatHex(idBytes);
        this.secret = new byte[32];
        RANDOM.nextBytes(secret);
        try {
            this.key = RelayCrypto.key(secret);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@code name} is how logs call the session ("Editor", "Setup"); {@code pagePath} is the page under the web url. */
    public static CompletableFuture<RelayChannel> open(String name, String pagePath, Handler onMessage, Runnable onClosed) {
        RelayChannel channel = new RelayChannel(name, pagePath, onMessage, onClosed);
        return HTTP.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + AUTH_TOKEN)
                .buildAsync(URI.create(RELAY_URL + "api/editor/" + channel.id + "/ws?role=proxy"), channel)
                .thenApply(socket -> {
                    channel.ws = socket;
                    channel.watch = WATCH.scheduleAtFixedRate(channel::expire, 30, 30, TimeUnit.SECONDS);
                    if (channel.closed)
                        channel.watch.cancel(false);
                    return channel;
                });
    }

    public String url() {
        return WEB_URL + pagePath + "#" + id + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }

    public String id() {
        return id;
    }

    public boolean isOpen() {
        return !closed;
    }

    public long lastSeenMillis() {
        return lastSeen;
    }

    public long openedAtMillis() {
        return openedAt;
    }

    private void expire() {
        long now = System.currentTimeMillis();
        if (lastSeen == 0 && now - openedAt > UNUSED_AFTER_MS) {
            Log.info("{} session {} closed: the link was not opened within 15 minutes", name, id);
            close();
        } else if (lastSeen != 0 && now - lastSeen > GONE_AFTER_MS) {
            Log.info("{} session {} closed: the page has been gone for 5 minutes", name, id);
            close();
        }
    }

    public void close() {
        WebSocket socket = ws;
        if (socket != null && !closed) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "closed by proxy");
        }
        markClosed();
    }

    @Override
    public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
        partial.append(data);
        if (partial.length() > MAX_FRAME) {
            // nothing the page sends comes close; a frame this big is dropped without parsing
            Log.warn("{} session {} dropped a message over {} characters", name, id, MAX_FRAME);
            partial.setLength(0);
            socket.request(1);
            return null;
        }
        if (last) {
            String frame = partial.toString();
            partial.setLength(0);
            try {
                handle(frame);
            } catch (Exception e) {
                Log.warn("{} message failed: {}", name, e.getMessage());
            }
        }
        socket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
        Log.info("{} session {} closed by relay: {} {}", name, id, statusCode, reason);
        markClosed();
        return null;
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
        Log.warn("{} session {} failed: {}", name, id, error.getMessage());
        markClosed();
    }

    private void markClosed() {
        synchronized (this) {
            if (closed)
                return;
            closed = true;
            ScheduledFuture<?> w = watch;
            if (w != null)
                w.cancel(false);
        }
        onClosed.run();
    }

    private void handle(String frame) throws Exception {
        JsonNode envelope = MAPPER.readTree(frame);
        String from = envelope.path("conn").asText("");
        JsonNode seqNode = envelope.path("seq");
        long seq = seqNode.isIntegralNumber() ? seqNode.asLong() : -1;
        String body = null;
        if (!from.isEmpty() && seq > recvSeqByConn.getOrDefault(from, 0L)) {
            try {
                body = RelayCrypto.open(key, "editor", from, seq, envelope.path("iv").asText(""), envelope.path("box").asText(""));
            } catch (GeneralSecurityException e) {
                body = null;
            }
        }
        if (body == null) {
            Log.warn("{} session {} dropped a message it could not open or had already seen", name, id);
            return;
        }
        recvSeqByConn.put(from, seq);
        conn = from;
        lastSeen = System.currentTimeMillis();

        JsonNode msg = MAPPER.readTree(body);
        if (!"ping".equals(msg.path("type").asText(""))) {
            onMessage.accept(msg);
        }
    }

    public synchronized void send(Object message) throws Exception {
        if (closed || ws == null) {
            return;
        }
        String body = MAPPER.writeValueAsString(message);
        long seq = ++sendSeq;
        RelayCrypto.Sealed sealed = RelayCrypto.seal(key, "proxy", conn, seq, body);
        Map<String, Object> envelope = Map.of("conn", conn, "seq", seq, "iv", sealed.iv(), "box", sealed.box());
        ws.sendText(MAPPER.writeValueAsString(envelope), true).join();
    }
}
