package dev.objz.commandbridge.velocity.util;

import dev.objz.commandbridge.net.Endpoint;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.security.AuthStatus;
import dev.objz.commandbridge.velocity.FakeProxy;
import dev.objz.commandbridge.velocity.TestFixtures;
import dev.objz.commandbridge.velocity.net.out.ctx.ResolveUuidRequestContext;
import dev.objz.commandbridge.velocity.net.session.SessionHub;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UserCacheTest {

    private static final UUID NOTCH = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    private static final UUID OTHER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @TempDir
    Path dir;

    private final FakeProxy proxy = new FakeProxy();
    private final SessionHub sessions = new SessionHub();
    private final OutNode outNode = new OutNode().setEndpointSendFactory((ep, env) -> null);
    private final List<String> mojangCalls = new ArrayList<>();

    @BeforeAll
    static void setUpLog() {
        TestFixtures.ensureLog();
    }

    private Path cacheFile() {
        return dir.resolve("data").resolve("usercache.json");
    }

    private UserCache cache(UUID mojangAnswer) {
        return new UserCache(proxy.server, sessions, outNode, cacheFile(), name -> {
            mojangCalls.add(name);
            return mojangAnswer;
        });
    }

    private void remoteProxyAnswers(Consumer<ResolveUuidRequestContext> answer) {
        var session = sessions.add("proxy-2", new Endpoint() {
            @Override
            public CompletableFuture<Void> send(Envelope env) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public boolean isOpen() {
                return true;
            }
        });
        session.location(Location.VELOCITY);
        session.status(AuthStatus.AUTH_OK);
        outNode.register(MessageType.RESOLVE_UUID, new OutboundHandler<ResolveUuidRequestContext>() {
            @Override
            public SendOperation accept(ResolveUuidRequestContext ctx) {
                answer.accept(ctx);
                return null;
            }
        });
    }

    private static UUID resolve(UserCache cache, String name) throws Exception {
        return cache.resolve(name).get(2, TimeUnit.SECONDS);
    }

    @Test
    void savedEntriesAreLoadedByNewInstance() {
        var first = cache(null);
        first.cachePlayer("Notch", NOTCH);
        first.save();

        var second = cache(null);

        assertEquals(List.of("Notch"), List.copyOf(second.knownNames()));
        assertEquals("Notch", second.nameOf(NOTCH).orElseThrow());
    }

    @Test
    void namesAreMatchedCaseInsensitively() throws Exception {
        var cache = cache(OTHER);
        cache.cachePlayer("Notch", NOTCH);

        assertEquals(NOTCH, resolve(cache, "nOTCH"));
        assertTrue(mojangCalls.isEmpty());
    }

    @Test
    void corruptFileIsIgnored() throws IOException {
        Files.createDirectories(cacheFile().getParent());
        Files.writeString(cacheFile(), "{ not json");

        var cache = cache(null);

        assertTrue(cache.knownNames().isEmpty());
    }

    @Test
    void onlinePlayerWinsOverCacheAndMojang() throws Exception {
        proxy.addPlayer(OTHER, "Notch");
        var cache = cache(NOTCH);
        cache.cachePlayer("Notch", NOTCH);

        assertEquals(OTHER, resolve(cache, "notch"));
        assertTrue(mojangCalls.isEmpty());
        assertEquals("Notch", cache.displayName(OTHER));
    }

    @Test
    void remoteProxyWinsOverCache() throws Exception {
        remoteProxyAnswers(ctx -> ctx.resultFuture().complete(OTHER));
        var cache = cache(null);
        cache.cachePlayer("Notch", NOTCH);

        assertEquals(OTHER, resolve(cache, "Notch"));
        assertEquals("Notch", cache.nameOf(OTHER).orElseThrow());
    }

    @Test
    void cacheWinsOverMojang() throws Exception {
        var cache = cache(OTHER);
        cache.cachePlayer("Notch", NOTCH);

        assertEquals(NOTCH, resolve(cache, "Notch"));
        assertTrue(mojangCalls.isEmpty());
    }

    @Test
    void mojangIsLastResortAndResultIsPersisted() throws Exception {
        var cache = cache(NOTCH);

        assertEquals(NOTCH, resolve(cache, "Notch"));
        assertEquals(List.of("Notch"), mojangCalls);
        assertEquals("Notch", cache(null).nameOf(NOTCH).orElseThrow());
    }

    @Test
    void unknownEverywhereResolvesToNull() throws Exception {
        assertNull(resolve(cache(null), "nobody"));
    }

    @Test
    void blankNameIsNotLookedUp() throws Exception {
        assertNull(resolve(cache(NOTCH), " "));
        assertTrue(mojangCalls.isEmpty());
    }

    @Test
    void remoteTimeoutFallsBackToCache() throws Exception {
        // ResolveUuidRequest completes the result with null when the remote proxy times out
        remoteProxyAnswers(ctx -> CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS)
                .execute(() -> ctx.resultFuture().complete(null)));
        var cache = cache(OTHER);
        cache.cachePlayer("Notch", NOTCH);

        assertEquals(NOTCH, resolve(cache, "Notch"));
        assertTrue(mojangCalls.isEmpty());
    }

    @Test
    void failingRemoteSendFallsBackToMojang() throws Exception {
        remoteProxyAnswers(ctx -> {
            throw new IllegalStateException("socket closed");
        });

        assertEquals(NOTCH, resolve(cache(NOTCH), "Notch"));
    }

    @Test
    void displayNameFallsBackToShortUuid() {
        assertEquals("069a79f4", cache(null).displayName(NOTCH));
        assertEquals("unknown", cache(null).displayName(null));
    }
}
