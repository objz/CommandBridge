package dev.objz.commandbridge.velocity.cli.subcommands;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.config.ConfigManager;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.editor.RelayChannel;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import dev.objz.commandbridge.velocity.setup.PairOffer;
import dev.objz.commandbridge.velocity.setup.SetupSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SetupCommandTest {
    @TempDir
    Path dir;

    @BeforeAll
    static void setup() {
        TestFixtures.ensureLog();
    }

    @Test
    void theLatestOpeningWinsInEitherCompletionOrderAndClosesObsoleteOffers() throws Exception {
        for (boolean oldCompletesFirst : new boolean[] { false, true }) {
            SetupCommand command = command();
            CompletableFuture<SetupSession> oldOpen = new CompletableFuture<>();
            CompletableFuture<SetupSession> newOpen = new CompletableFuture<>();
            command.start(null, "opened", "note", () -> oldOpen);
            command.start(null, "opened", "note", () -> newOpen);
            SetupSession obsolete = session();
            SetupSession latest = session();
            PairOffer obsoleteOffer = offer(obsolete);
            PairOffer latestOffer = offer(latest);

            if (oldCompletesFirst) {
                oldOpen.complete(obsolete);
                newOpen.complete(latest);
            } else {
                newOpen.complete(latest);
                oldOpen.complete(obsolete);
            }

            assertFalse(obsolete.isOpen());
            assertFalse(obsoleteOffer.isOpen());
            assertTrue(latest.isOpen());
            assertTrue(latestOffer.isOpen());
            assertSame(latest, active(command));
            latest.closeOffers();
            latest.close();
        }
    }

    @Test
    void concurrentCompletionsLeaveOnlyTheLatestSessionAndOffersOpen() throws Exception {
        SetupCommand command = command();
        CompletableFuture<SetupSession> oldOpen = new CompletableFuture<>();
        CompletableFuture<SetupSession> newOpen = new CompletableFuture<>();
        command.start(null, "opened", "note", () -> oldOpen);
        command.start(null, "opened", "note", () -> newOpen);
        SetupSession obsolete = session();
        SetupSession latest = session();
        PairOffer obsoleteOffer = offer(obsolete);
        PairOffer latestOffer = offer(latest);

        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> oldOpen.complete(obsolete)),
                CompletableFuture.runAsync(() -> newOpen.complete(latest))).join();

        assertFalse(obsolete.isOpen());
        assertFalse(obsoleteOffer.isOpen());
        assertSame(latest, active(command));
        assertTrue(latestOffer.isOpen());
        latest.closeOffers();
        latest.close();
    }

    @Test
    void replacingAnOpenSessionClosesItsOffersEvenIfTheNewOpenFails() throws Exception {
        SetupCommand command = command();
        SetupSession previous = session();
        PairOffer previousOffer = offer(previous);
        command.start(null, "opened", "note", () -> CompletableFuture.completedFuture(previous));

        command.start(null, "opened", "note",
                () -> CompletableFuture.failedFuture(new IllegalStateException("offline")));

        assertFalse(previous.isOpen());
        assertFalse(previousOffer.isOpen());
        assertNull(active(command));
    }

    private SetupCommand command() {
        return new SetupCommand(null, null, new SessionHub(), dir, () -> {
        }, sender -> CompletableFuture.completedFuture(""));
    }

    private SetupSession session() throws Exception {
        var constructor = SetupSession.class.getDeclaredConstructor(ProxyServer.class,
                ConfigManager.class, SessionHub.class, Path.class, Path.class,
                Supplier.class);
        constructor.setAccessible(true);
        SetupSession session = constructor.newInstance(null, null, new SessionHub(), dir, dir.resolve("velocity.toml"),
                (Supplier<CompletableFuture<String>>) () -> CompletableFuture.completedFuture(""));
        var relayConstructor = RelayChannel.class.getDeclaredConstructor(String.class, String.class,
                RelayChannel.Handler.class, Runnable.class);
        relayConstructor.setAccessible(true);
        RelayChannel relay = relayConstructor.newInstance("Setup test", "setup/", (RelayChannel.Handler) message -> {
        }, (Runnable) () -> {
        });
        var channel = SetupSession.class.getDeclaredField("channel");
        channel.setAccessible(true);
        channel.set(session, relay);
        return session;
    }

    @SuppressWarnings("unchecked")
    private static PairOffer offer(SetupSession session) throws Exception {
        PairOffer offer = new PairOffer("lobby", "proxy", "test", new byte[0], hello -> {
        });
        var offers = SetupSession.class.getDeclaredField("offers");
        offers.setAccessible(true);
        ((Map<String, PairOffer>) offers.get(session)).put("lobby", offer);
        return offer;
    }

    private static Object active(SetupCommand command) throws Exception {
        var active = SetupCommand.class.getDeclaredField("active");
        active.setAccessible(true);
        return active.get(command);
    }
}
