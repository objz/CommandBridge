package dev.objz.commandbridge.net.channel;

import dev.objz.commandbridge.api.channel.ChannelPayload;
import dev.objz.commandbridge.api.channel.command.CommandPayload;
import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.api.message.MessageListener;
import dev.objz.commandbridge.api.message.Subscription;
import dev.objz.commandbridge.api.platform.Platform;
import dev.objz.commandbridge.net.payloads.PluginMessage;
import dev.objz.commandbridge.net.proto.Envelope;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PluginMessageChannelTest {

    public record Greeting(String text) implements ChannelPayload {
    }

    private record Sent(Platform.ServerTarget target, PluginMessage message, Duration timeout) {
    }

    private static final Platform.ServerTarget SURVIVAL = Platform.backend("survival-1");
    private static final Platform.ServerTarget LOBBY = Platform.backend("lobby");

    private final List<Sent> sent = new ArrayList<>();
    private CompletableFuture<PluginMessage> nextResponse = new CompletableFuture<>();

    private <P extends ChannelPayload> PluginMessageChannel<P> channel(Class<P> type) {
        return new PluginMessageChannel<>(type,
                (target, msg) -> {
                    sent.add(new Sent(target, msg, null));
                    return CompletableFuture.completedFuture(null);
                },
                (target, msg, timeout) -> {
                    sent.add(new Sent(target, msg, timeout));
                    return nextResponse;
                },
                listener -> () -> {
                });
    }

    @Test
    void sendWrapsPayloadForSingleTarget() {
        channel(Greeting.class).to(List.of(SURVIVAL)).send(new Greeting("hi")).join();

        assertEquals(1, sent.size());
        Sent s = sent.getFirst();
        assertEquals(SURVIVAL, s.target());
        assertEquals(Greeting.class.getName(), s.message().channelType());
        assertEquals("hi", s.message().data().get("text").asText());
        assertFalse(s.message().expectsResponse());
        assertNull(s.message().requirePlayer());
        assertNull(s.message().whenOnline());
    }

    @Test
    void explicitRequirePlayerIsForwarded() {
        UUID player = UUID.randomUUID();
        channel(Greeting.class).to(List.of(SURVIVAL)).requirePlayer(player).send(new Greeting("hi"));

        assertEquals(player, sent.getFirst().message().requirePlayer());
        assertNull(sent.getFirst().message().whenOnline());
    }

    @Test
    void requirePlayerWithoutArgumentUsesCommandPayloadPlayer() {
        UUID player = UUID.randomUUID();
        channel(CommandPayload.class).to(List.of(SURVIVAL)).requirePlayer()
                .send(new CommandPayload("spawn", RunAs.PLAYER, player));

        assertEquals(player, sent.getFirst().message().requirePlayer());
    }

    @Test
    void whenOnlineWithoutArgumentUsesCommandPayloadPlayer() {
        UUID player = UUID.randomUUID();
        channel(CommandPayload.class).to(List.of(SURVIVAL)).whenOnline()
                .send(new CommandPayload("spawn", RunAs.PLAYER, player));

        assertEquals(player, sent.getFirst().message().whenOnline());
        assertNull(sent.getFirst().message().requirePlayer());
    }

    @Test
    void payloadDerivedConditionFailsWithoutPlayer() {
        var sender = channel(CommandPayload.class).to(List.of(SURVIVAL)).requirePlayer();

        assertThrows(IllegalStateException.class,
                () -> sender.send(new CommandPayload("say hi", RunAs.CONSOLE)));
        assertTrue(sent.isEmpty());
    }

    @Test
    void payloadDerivedConditionFailsForNonCommandPayload() {
        var sender = channel(Greeting.class).to(List.of(SURVIVAL)).whenOnline();

        assertThrows(IllegalStateException.class, () -> sender.send(new Greeting("hi")));
    }

    @Test
    void requirePlayerAndWhenOnlineCannotBeCombined() {
        UUID player = UUID.randomUUID();
        var single = channel(Greeting.class).to(List.of(SURVIVAL)).requirePlayer(player);
        assertThrows(IllegalStateException.class, () -> single.whenOnline(player));

        var multi = channel(Greeting.class).to(List.of(SURVIVAL, LOBBY)).whenOnline(player);
        assertThrows(IllegalStateException.class, multi::requirePlayer);
    }

    @Test
    void requestUsesDefaultTimeoutAndConvertsResponse() throws Exception {
        nextResponse = CompletableFuture.completedFuture(new PluginMessage(Greeting.class.getName(),
                Envelope.MAPPER.valueToTree(new Greeting("pong")), false));

        Greeting reply = channel(Greeting.class).to(List.of(SURVIVAL)).request(new Greeting("ping"))
                .get(1, TimeUnit.SECONDS);

        assertEquals(new Greeting("pong"), reply);
        Sent s = sent.getFirst();
        assertTrue(s.message().expectsResponse());
        assertEquals(Duration.ofSeconds(15), s.timeout());
    }

    @Test
    void requestForwardsCustomTimeoutAndRequirePlayer() {
        UUID player = UUID.randomUUID();
        channel(Greeting.class).to(List.of(SURVIVAL)).requirePlayer(player)
                .request(new Greeting("ping"), Duration.ofMillis(250));

        assertEquals(Duration.ofMillis(250), sent.getFirst().timeout());
        assertEquals(player, sent.getFirst().message().requirePlayer());
    }

    @Test
    void requestTimeoutPropagatesToCaller() {
        var future = channel(Greeting.class).to(List.of(SURVIVAL)).request(new Greeting("ping"), Duration.ofMillis(10));
        nextResponse.completeExceptionally(new TimeoutException());

        var ex = assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
        assertInstanceOf(TimeoutException.class, ex.getCause());
    }

    @Test
    void unconvertibleResponseFailsFuture() {
        nextResponse = CompletableFuture.completedFuture(new PluginMessage(Greeting.class.getName(),
                Envelope.MAPPER.createArrayNode().add(1), false));

        var future = channel(Greeting.class).to(List.of(SURVIVAL)).request(new Greeting("ping"));

        assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
    }

    @Test
    void requestWithWhenOnlineIsUnsupported() {
        var sender = channel(Greeting.class).to(List.of(SURVIVAL)).whenOnline(UUID.randomUUID());

        assertThrows(UnsupportedOperationException.class, () -> sender.request(new Greeting("ping")));
        assertTrue(sent.isEmpty());
    }

    @Test
    void multiTargetSendReachesEveryTargetOnce() {
        channel(Greeting.class).to(List.of(SURVIVAL, LOBBY, SURVIVAL)).send(new Greeting("hi")).join();

        assertEquals(List.of(SURVIVAL, LOBBY), sent.stream().map(Sent::target).toList());
        assertSame(sent.get(0).message(), sent.get(1).message());
    }

    @Test
    void multiTargetRequestIsUnsupported() {
        var sender = channel(Greeting.class).to(List.of(SURVIVAL, LOBBY));

        assertThrows(UnsupportedOperationException.class, () -> sender.request(new Greeting("x")));
    }

    @Test
    void broadcastTargetsAllBackends() {
        channel(Greeting.class).toAll().send(new Greeting("hi"));

        assertEquals(Platform.backend("*"), sent.getFirst().target());
    }

    @Test
    void broadcastRejectsDeliveryConditions() {
        var sender = channel(Greeting.class).toAll();

        assertThrows(UnsupportedOperationException.class, sender::requirePlayer);
        assertThrows(UnsupportedOperationException.class, () -> sender.whenOnline(UUID.randomUUID()));
    }

    @Test
    void emptyTargetListIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> channel(Greeting.class).to(List.of()));
    }

    @Test
    void listenDelegatesToRegistrar() {
        List<MessageListener<Greeting>> registered = new ArrayList<>();
        Subscription subscription = () -> {
        };
        var channel = new PluginMessageChannel<>(Greeting.class,
                (t, m) -> CompletableFuture.completedFuture(null),
                (t, m, d) -> new CompletableFuture<>(),
                listener -> {
                    registered.add(listener);
                    return subscription;
                });
        MessageListener<Greeting> listener = (ctx, payload) -> {
        };

        assertSame(subscription, channel.listen(listener));
        assertEquals(List.of(listener), registered);
    }
}
