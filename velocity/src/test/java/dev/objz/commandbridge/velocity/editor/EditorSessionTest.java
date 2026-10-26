package dev.objz.commandbridge.velocity.editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.cli.subcommands.ReloadCommand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorSessionTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @TempDir
    Path dir;
    private EditorSession session;
    private RelayChannel relay;

    @BeforeAll
    static void setUpLog() {
        TestFixtures.ensureLog();
    }

    @BeforeEach
    void setUp() throws Exception {
        ScriptManager scripts = new ScriptManager(dir, null);
        try (var in = getClass().getResourceAsStream("/example.yml")) {
            String example = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("type: PLAYERS", "type: TEXT");
            assertTrue(scripts.save("example.yml", example).isEmpty());
        }
        var constructor = EditorSession.class.getDeclaredConstructor(ScriptManager.class, ReloadCommand.class,
                CommandSource.class, Supplier.class, EditorLayoutStore.class);
        constructor.setAccessible(true);
        session = constructor.newInstance(scripts, null, null, (Supplier<?>) Map::of, new EditorLayoutStore(dir));
        var relayConstructor = RelayChannel.class.getDeclaredConstructor(String.class, String.class,
                RelayChannel.Handler.class, Runnable.class);
        relayConstructor.setAccessible(true);
        relay = relayConstructor.newInstance("Test", "editor/", (RelayChannel.Handler) msg -> {
        }, (Runnable) () -> {
        });
        WebSocket socket = (WebSocket) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{WebSocket.class},
                (proxy, method, args) -> CompletableFuture.completedFuture(proxy));
        var ws = RelayChannel.class.getDeclaredField("ws");
        ws.setAccessible(true);
        ws.set(relay, socket);
        var channel = EditorSession.class.getDeclaredField("channel");
        channel.setAccessible(true);
        channel.set(session, relay);
    }

    private void stage(String json) throws Exception {
        var handle = EditorSession.class.getDeclaredMethod("handle", JsonNode.class);
        handle.setAccessible(true);
        handle.invoke(session, MAPPER.readTree(json));
    }

    private Object pending() throws Exception {
        var pending = EditorSession.class.getDeclaredField("pending");
        pending.setAccessible(true);
        return pending.get(session);
    }

    @Test
    void invalidOrEmptyRestagingRevokesThePreviousApproval() throws Exception {
        for (String next : new String[]{
                "{\"type\":\"stage\",\"files\":{},\"deletions\":[]}",
                "{\"type\":\"stage\",\"files\":{\"bad.yml\":\"name: [\"},\"deletions\":[]}"}) {
            stage("{\"type\":\"stage\",\"files\":{},\"deletions\":[\"example.yml\"]}");
            assertNotNull(pending());
            stage(next);
            assertNull(pending());
        }
    }

    @Test
    void rejectsInvalidDeletionsAndMalformedFileValues() throws Exception {
        for (String json : new String[]{
                "{\"type\":\"stage\",\"files\":{},\"deletions\":[\"../config.yml\"]}",
                "{\"type\":\"stage\",\"files\":{},\"deletions\":[\"missing.yml\"]}",
                "{\"type\":\"stage\",\"files\":[],\"deletions\":[\"example.yml\"]}",
                "{\"type\":\"stage\",\"files\":{},\"deletions\":[42]}",
                "{\"type\":\"stage\",\"files\":{\"example.yml\":null},\"deletions\":[\"example.yml\"]}"}) {
            stage(json);
            assertNull(pending(), json);
        }
    }

    @Test
    void aRelayDisconnectCancelsItsWatchdog() throws Exception {
        var scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        try {
            var task = scheduler.schedule(() -> {
            }, 1, java.util.concurrent.TimeUnit.DAYS);
            var watch = RelayChannel.class.getDeclaredField("watch");
            watch.setAccessible(true);
            watch.set(relay, task);

            relay.onClose(null, 1000, "test");

            assertTrue(task.isCancelled());
        } finally {
            scheduler.shutdownNow();
        }
    }
}
