package dev.objz.commandbridge.velocity.editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.logging.Log;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.cli.subcommands.ReloadCommand;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** Live web editor session over a {@link RelayChannel}. */
public final class EditorSession {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_CHARS = "abcdefghjkmnpqrstuvwxyz23456789";

    /** Changes the editor wants saved, held until someone runs /cb editor confirm with the code. */
    private record Pending(String code, Map<String, String> files, List<String> deletions) {
    }

    private final ScriptManager scripts;
    private final ReloadCommand reload;
    private final CommandSource owner;
    private final Supplier<Map<String, Map<String, String>>> servers;
    private final EditorLayoutStore layout;
    private final EditorLayoutStore draft;
    private volatile Pending pending;
    private volatile RelayChannel channel;

    private EditorSession(ScriptManager scripts, ReloadCommand reload, CommandSource owner,
            Supplier<Map<String, Map<String, String>>> servers, EditorLayoutStore layout) {
        this.scripts = scripts;
        this.layout = layout;
        this.draft = layout.sibling("editor-draft.json");
        this.reload = reload;
        this.owner = owner;
        this.servers = servers;
    }

    public static CompletableFuture<EditorSession> open(ScriptManager scripts, ReloadCommand reload,
            CommandSource owner, Supplier<Map<String, Map<String, String>>> servers, EditorLayoutStore layout, Runnable onClosed) {
        EditorSession session = new EditorSession(scripts, reload, owner, servers, layout);
        return RelayChannel.open("Editor", "editor/", session::handle, onClosed)
                .thenApply(channel -> {
                    session.channel = channel;
                    return session;
                });
    }

    public String url() {
        return channel.url();
    }

    public boolean isOpen() {
        return channel.isOpen();
    }

    /** For /cb editor list: who opened it, since when, when the tab was last there, and a waiting code. */
    public String describe() {
        long now = System.currentTimeMillis();
        long lastSeen = channel.lastSeenMillis();
        String who = owner instanceof com.velocitypowered.api.proxy.Player p ? p.getUsername() : "console";
        String seen = lastSeen == 0 ? "not opened yet" : "editor seen " + (now - lastSeen) / 1000 + "s ago";
        Pending p = pending;
        return channel.id().substring(0, 8) + " by " + who + ", open " + (now - channel.openedAtMillis()) / 60_000 + " min, " + seen
                + (p != null ? ", waiting for /cb editor confirm " + p.code() : "");
    }

    /**
     * Saves the changes the editor staged under {@code code}, then reloads once.
     * Returns what went wrong; empty when everything was saved.
     */
    public synchronized List<String> confirm(String code) throws Exception {
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
        channel.close();
    }

    private synchronized void handle(JsonNode msg) throws Exception {
        switch (msg.path("type").asText("")) {
            case "hello" -> sendState();
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
            default -> Log.warn("Editor session {} got an unknown message type", channel.id());
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

    private void send(Object message) throws Exception {
        channel.send(message);
    }
}
