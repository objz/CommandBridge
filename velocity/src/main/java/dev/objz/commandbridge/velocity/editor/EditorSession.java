package dev.objz.commandbridge.velocity.editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.cli.subcommands.ReloadCommand;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Live web editor session. The proxy connects out to the relay on cb.objz.dev,
 * the browser joins the same channel, and every message is signed with a secret
 * that only lives in the editor link's #fragment, so the relay can't forge edits.
 */
public final class EditorSession implements WebSocket.Listener {

    private static final String RELAY_URL = System.getProperty("commandbridge.editor.relay",
            "wss://cb.objz.dev/api/editor/");
    private static final String EDITOR_URL = System.getProperty("commandbridge.editor.url",
            "https://cb.objz.dev/editor/#");
    private static final String AUTH_TOKEN = "cb-dump-upload-v1";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    // the editor pings every minute; a tab gone this long, or a link nobody opened, ends the session
    private static final long GONE_AFTER_MS = TimeUnit.MINUTES.toMillis(5);
    private static final long UNUSED_AFTER_MS = TimeUnit.MINUTES.toMillis(15);
    private static final String CODE_CHARS = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final ScheduledExecutorService WATCH = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CommandBridge-editor-watch");
        t.setDaemon(true);
        return t;
    });

    /** Changes the editor wants saved, held until someone runs /cb editor confirm with the code. */
    private record Pending(String code, Map<String, String> files, List<String> deletions) {
    }

    private final ScriptManager scripts;
    private final ReloadCommand reload;
    private final CommandSource owner;
    private final Supplier<Map<String, Map<String, String>>> servers;
    private final EditorLayoutStore layout;
    private final EditorLayoutStore draft;
    private final Runnable onClosed;
    private final long openedAt = System.currentTimeMillis();
    private volatile long lastSeen;
    private volatile Pending pending;
    private volatile ScheduledFuture<?> watch;
    private final String id;
    private final byte[] secret;

    private final StringBuilder partial = new StringBuilder();
    private volatile WebSocket ws;
    private volatile boolean closed;
    private long sendSeq;
    private final Map<String, Long> recvSeqByConn = new HashMap<>();
    private volatile String conn = "";

    private EditorSession(ScriptManager scripts, ReloadCommand reload, CommandSource owner,
            Supplier<Map<String, Map<String, String>>> servers, EditorLayoutStore layout, Runnable onClosed) {
        this.scripts = scripts;
        this.layout = layout;
        this.draft = layout.sibling("editor-draft.json");
        this.reload = reload;
        this.owner = owner;
        this.servers = servers;
        this.onClosed = onClosed;
        byte[] idBytes = new byte[16];
        RANDOM.nextBytes(idBytes);
        this.id = HexFormat.of().formatHex(idBytes);
        this.secret = new byte[32];
        RANDOM.nextBytes(secret);
    }

    public static CompletableFuture<EditorSession> open(ScriptManager scripts, ReloadCommand reload,
            CommandSource owner, Supplier<Map<String, Map<String, String>>> servers, EditorLayoutStore layout, Runnable onClosed) {
        EditorSession session = new EditorSession(scripts, reload, owner, servers, layout, onClosed);
        return HTTP.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + AUTH_TOKEN)
                .buildAsync(URI.create(RELAY_URL + session.id + "/ws?role=proxy"), session)
                .thenApply(socket -> {
                    session.ws = socket;
                    session.watch = WATCH.scheduleAtFixedRate(session::expire, 30, 30, TimeUnit.SECONDS);
                    return session;
                });
    }

    public String url() {
        return EDITOR_URL + id + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }

    public boolean isOpen() {
        return !closed;
    }

    private void expire() {
        long now = System.currentTimeMillis();
        if (lastSeen == 0 && now - openedAt > UNUSED_AFTER_MS) {
            Log.info("Editor session {} closed: the link was not opened within 15 minutes", id);
            close();
        } else if (lastSeen != 0 && now - lastSeen > GONE_AFTER_MS) {
            Log.info("Editor session {} closed: the editor tab has been gone for 5 minutes", id);
            close();
        }
    }

    /** For /cb editor list: who opened it, since when, when the tab was last there, and a waiting code. */
    public String describe() {
        long now = System.currentTimeMillis();
        String who = owner instanceof com.velocitypowered.api.proxy.Player p ? p.getUsername() : "console";
        String seen = lastSeen == 0 ? "not opened yet" : "editor seen " + (now - lastSeen) / 1000 + "s ago";
        Pending p = pending;
        return id.substring(0, 8) + " by " + who + ", open " + (now - openedAt) / 60_000 + " min, " + seen
                + (p != null ? ", waiting for /cb editor confirm " + p.code() : "");
    }

    /**
     * Saves the changes the editor staged under {@code code}, then reloads once.
     * Returns what went wrong; empty when everything was saved.
     */
    public List<String> confirm(String code) throws Exception {
        Pending p = pending;
        if (p == null || code == null || !MessageDigest.isEqual(p.code().getBytes(StandardCharsets.US_ASCII),
                code.trim().toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII))) {
            return List.of("No changes are waiting for that code");
        }
        pending = null;
        List<String> errors = new ArrayList<>();
        Map<String, Object> results = new LinkedHashMap<>();
        for (var e : p.files().entrySet()) {
            List<String> problems = scripts.save(e.getKey(), e.getValue());
            results.put(e.getKey(), problems);
            problems.forEach(msg -> errors.add(e.getKey() + ": " + msg));
        }
        for (String file : p.deletions()) {
            boolean deleted = scripts.delete(file);
            results.put(file, deleted ? List.of() : List.of("Script file not found"));
            if (!deleted) {
                errors.add(file + ": not found");
            }
        }
        reload.execute(owner);
        long broken = scripts.errors();
        String warning = broken == 0 ? null
                : "Saved, but the reload was aborted: other scripts have " + broken + " error(s)";
        if (errors.isEmpty()) {
            draft.write(MAPPER.createObjectNode());
        }
        Map<String, Object> applied = new LinkedHashMap<>();
        applied.put("type", "applied");
        applied.put("results", results);
        applied.put("warning", warning);
        send(applied);
        sendState();
        return errors;
    }

    public void close() {
        ScheduledFuture<?> w = watch;
        if (w != null) {
            w.cancel(false);
        }
        WebSocket socket = ws;
        if (socket != null && !closed) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "closed by proxy");
        }
        markClosed();
    }

    @Override
    public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
        partial.append(data);
        if (last) {
            String frame = partial.toString();
            partial.setLength(0);
            try {
                handle(frame);
            } catch (Exception e) {
                Log.warn("Editor message failed: {}", e.getMessage());
            }
        }
        socket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
        Log.info("Editor session {} closed by relay: {} {}", id, statusCode, reason);
        markClosed();
        return null;
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
        Log.warn("Editor session {} failed: {}", id, error.getMessage());
        markClosed();
    }

    private void markClosed() {
        if (!closed) {
            closed = true;
            onClosed.run();
        }
    }

    private void handle(String frame) throws Exception {
        JsonNode envelope = MAPPER.readTree(frame);
        String from = envelope.path("conn").asText("");
        long seq = envelope.path("seq").asLong(-1);
        String body = envelope.path("body").asText(null);
        String mac = envelope.path("mac").asText("");
        if (body == null || from.isEmpty() || seq <= recvSeqByConn.getOrDefault(from, 0L)
                || !verify("editor", from, seq, body, mac)) {
            Log.warn("Editor session {} dropped an unsigned or replayed message", id);
            return;
        }
        recvSeqByConn.put(from, seq);
        conn = from;
        lastSeen = System.currentTimeMillis();

        JsonNode msg = MAPPER.readTree(body);
        switch (msg.path("type").asText("")) {
            case "hello" -> sendState();
            case "ping" -> {
                // only keeps the session alive
            }
            case "stage" -> stage(msg);
            case "unstage" -> {
                pending = null;
                sendState();
            }
            case "draft" -> {
                String error = draft.write(msg.path("draft"));
                if (error != null) {
                    send(Map.of("type", "layout-error", "error", error));
                }
            }
            case "layout" -> {
                String error = layout.write(msg.path("layout"));
                if (error != null) {
                    send(Map.of("type", "layout-error", "error", error));
                }
            }
            default -> Log.warn("Editor session {} got an unknown message type", id);
        }
    }

    /**
     * The editor's Save: every file is checked now, but nothing is written until someone with access
     * to the proxy runs /cb editor confirm with the code, so a leaked editor link alone can't change scripts.
     */
    private void stage(JsonNode msg) throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        msg.path("files").fields().forEachRemaining(e -> files.put(e.getKey(), e.getValue().asText("")));
        List<String> deletions = new ArrayList<>();
        msg.path("deletions").forEach(d -> deletions.add(d.asText("")));
        Map<String, Object> errors = new LinkedHashMap<>();
        for (var e : files.entrySet()) {
            List<String> problems = scripts.validate(e.getKey(), e.getValue());
            if (!problems.isEmpty()) {
                errors.put(e.getKey(), problems);
            }
        }
        Map<String, Object> reply = new LinkedHashMap<>();
        reply.put("type", "staged");
        reply.put("errors", errors);
        if (errors.isEmpty() && (!files.isEmpty() || !deletions.isEmpty())) {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            pending = new Pending(code.toString(), files, deletions);
            reply.put("code", pending.code());
            Log.info("Web editor changes to {} file(s) are waiting: run /cb editor confirm {} to save them",
                    files.size() + deletions.size(), pending.code());
        }
        send(reply);
    }

    private void sendState() throws Exception {
        Pending p = pending;
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("type", "state");
        state.put("scripts", scripts.readSources());
        state.put("servers", servers.get());
        state.put("layout", layout.read());
        state.put("draft", draft.read());
        state.put("pending", p != null ? p.code() : null);
        send(state);
    }

    private synchronized void send(Object message) throws Exception {
        if (closed || ws == null) {
            return;
        }
        String body = MAPPER.writeValueAsString(message);
        long seq = ++sendSeq;
        Map<String, Object> envelope = Map.of("conn", conn, "seq", seq, "body", body,
                "mac", sign("proxy", conn, seq, body));
        ws.sendText(MAPPER.writeValueAsString(envelope), true).join();
    }

    private String sign(String role, String connId, long seq, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        byte[] out = mac.doFinal((role + "\n" + connId + "\n" + seq + "\n" + body).getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
    }

    private boolean verify(String role, String connId, long seq, String body, String macB64) throws Exception {
        return MessageDigest.isEqual(
                sign(role, connId, seq, body).getBytes(StandardCharsets.US_ASCII),
                macB64.getBytes(StandardCharsets.US_ASCII));
    }
}
