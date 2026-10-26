package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.editor.EditorLayoutStore;
import dev.objz.commandbridge.velocity.editor.EditorSession;
import dev.objz.commandbridge.velocity.editor.RelayChannel;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests replacement and closure of overlapping editor sessions. */
final class EditorCommandTest {
    @TempDir
    Path dir;

    @BeforeAll
    static void setup() {
        TestFixtures.ensureLog();
    }

    @Test
    void closingThePreviousSessionDoesNotForgetItsReplacement() throws Exception {
        EditorCommand command = new EditorCommand(null, null, new SessionHub(), "proxy", dir);
        EditorSession first = session(command);
        EditorSession replacement = session(command);
        lifecycle(command, "sessionOpened", first);
        lifecycle(command, "sessionOpened", replacement);

        assertFalse(first.isOpen());
        assertTrue(replacement.isOpen());
        assertSame(replacement, active(command));
        lifecycle(command, "sessionClosed", first);
        assertSame(replacement, active(command));
        replacement.close();
        assertNull(active(command));
    }

    @Test
    void concurrentOpenCompletionsLeaveOnlyTheActiveSessionOpen() throws Exception {
        EditorCommand command = new EditorCommand(null, null, new SessionHub(), "proxy", dir);
        EditorSession first = session(command);
        EditorSession second = session(command);

        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> lifecycle(command, "sessionOpened", first)),
                CompletableFuture.runAsync(() -> lifecycle(command, "sessionOpened", second))).join();

        assertTrue(first.isOpen() != second.isOpen());
        assertSame(first.isOpen() ? first : second, active(command));
        command.closeSession();
        assertNull(active(command));
    }

    @Test
    void bothClosePathsInvalidatePendingOpens() throws Exception {
        for (boolean shutdown : new boolean[] { false, true }) {
            EditorCommand command = new EditorCommand(null, null, new SessionHub(), "proxy", dir);
            CompletableFuture<EditorSession> pending = new CompletableFuture<>();
            CompletableFuture<String> link = command.openFor(null, () -> pending);

            if (shutdown)
                command.closeSession();
            else
                command.close(null);
            EditorSession late = session(command);
            pending.complete(late);

            assertFalse(late.isOpen());
            assertNull(active(command));
            assertTrue(link.isCompletedExceptionally());
        }
    }

    @Test
    void theLatestOpeningWinsInEitherCompletionOrder() throws Exception {
        for (boolean oldCompletesFirst : new boolean[] { false, true }) {
            EditorCommand command = new EditorCommand(null, null, new SessionHub(), "proxy", dir);
            CompletableFuture<EditorSession> oldOpen = new CompletableFuture<>();
            CompletableFuture<EditorSession> newOpen = new CompletableFuture<>();
            CompletableFuture<String> oldLink = command.openFor(null, () -> oldOpen);
            CompletableFuture<String> newLink = command.openFor(null, () -> newOpen);
            EditorSession obsolete = session(command);
            EditorSession latest = session(command);

            if (oldCompletesFirst) {
                oldOpen.complete(obsolete);
                newOpen.complete(latest);
            } else {
                newOpen.complete(latest);
                oldOpen.complete(obsolete);
            }

            assertFalse(obsolete.isOpen());
            assertTrue(latest.isOpen());
            assertSame(latest, active(command));
            assertTrue(oldLink.isCompletedExceptionally());
            assertFalse(newLink.isCompletedExceptionally());
            command.closeSession();
        }
    }

    @Test
    void aFailedReplacementDoesNotReviveAnEarlierOpening() throws Exception {
        EditorCommand command = new EditorCommand(null, null, new SessionHub(), "proxy", dir);
        CompletableFuture<EditorSession> oldOpen = new CompletableFuture<>();
        command.openFor(null, () -> oldOpen);
        command.openFor(null, () -> CompletableFuture.failedFuture(new IllegalStateException("offline")));
        EditorSession obsolete = session(command);
        oldOpen.complete(obsolete);

        assertFalse(obsolete.isOpen());
        assertNull(active(command));
    }

    private EditorSession session(EditorCommand command) throws Exception {
        var constructor = EditorSession.class.getDeclaredConstructor(ScriptManager.class, ReloadCommand.class,
                CommandSource.class, Supplier.class, EditorLayoutStore.class);
        constructor.setAccessible(true);
        EditorSession session = constructor.newInstance(null, null, null, (Supplier<?>) Map::of, new EditorLayoutStore(dir));
        var relayConstructor = RelayChannel.class.getDeclaredConstructor(String.class, String.class,
                RelayChannel.Handler.class, Runnable.class);
        relayConstructor.setAccessible(true);
        RelayChannel relay = relayConstructor.newInstance("Test", "editor/", (RelayChannel.Handler) message -> {
        }, (Runnable) () -> lifecycle(command, "sessionClosed", session));
        var channel = EditorSession.class.getDeclaredField("channel");
        channel.setAccessible(true);
        channel.set(session, relay);
        return session;
    }

    private static Object active(EditorCommand command) throws Exception {
        var active = EditorCommand.class.getDeclaredField("active");
        active.setAccessible(true);
        return ((AtomicReference<?>) active.get(command)).get();
    }

    private static void lifecycle(EditorCommand command, String method, EditorSession session) {
        try {
            var callback = EditorCommand.class.getDeclaredMethod(method, EditorSession.class);
            callback.setAccessible(true);
            callback.invoke(command, session);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
