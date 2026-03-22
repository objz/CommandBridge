package dev.objz.commandbridge.net.channel;

import static dev.objz.commandbridge.api.platform.Platform.backend;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.objz.commandbridge.api.channel.MessageChannel.Sender;
import dev.objz.commandbridge.api.channel.command.CommandPayload;
import dev.objz.commandbridge.api.channel.command.RunAs;
import dev.objz.commandbridge.net.payloads.PluginMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

class PluginMessageChannelTest {

    @Test
    void requirePlayerChaining() {
        List<PluginMessage> sent = new ArrayList<>();
        PluginMessageChannel<CommandPayload> channel = createChannel(sent);
        Sender<CommandPayload> sender = channel.to(List.of(backend("s1")));

        Sender<CommandPayload> chained = sender.requirePlayer(UUID.randomUUID());

        assertSame(sender, chained);
    }

    @Test
    void mutualExclusivity() {
        List<PluginMessage> sent = new ArrayList<>();
        PluginMessageChannel<CommandPayload> channel = createChannel(sent);
        Sender<CommandPayload> sender = channel.to(List.of(backend("s1")));

        sender.requirePlayer(UUID.randomUUID());

        assertThrows(IllegalStateException.class, () -> sender.whenOnline(UUID.randomUUID()));
    }

    @Test
    void broadcastSenderRejectsConditions() {
        List<PluginMessage> sent = new ArrayList<>();
        PluginMessageChannel<CommandPayload> channel = createChannel(sent);

        assertThrows(UnsupportedOperationException.class, () -> channel.toAll().requirePlayer(UUID.randomUUID()));
    }

    @Test
    void singleTargetRequirePlayerPropagated() {
        List<PluginMessage> sent = new ArrayList<>();
        PluginMessageChannel<CommandPayload> channel = createChannel(sent);
        UUID playerUuid = UUID.randomUUID();

        channel.to(List.of(backend("s1"))).requirePlayer(playerUuid).send(new CommandPayload("cmd", RunAs.CONSOLE)).join();

        assertEquals(1, sent.size());
        assertEquals(playerUuid, sent.get(0).requirePlayer());
    }

    @Test
    void noArgRequirePlayerFromPayload() {
        List<PluginMessage> sent = new ArrayList<>();
        PluginMessageChannel<CommandPayload> channel = createChannel(sent);
        UUID playerUuid = UUID.randomUUID();

        channel.to(List.of(backend("s1"))).requirePlayer().send(new CommandPayload("cmd", RunAs.PLAYER, playerUuid)).join();

        assertEquals(1, sent.size());
        assertEquals(playerUuid, sent.get(0).requirePlayer());
    }

    @Test
    void noArgWithNullPlayerThrows() {
        List<PluginMessage> sent = new ArrayList<>();
        PluginMessageChannel<CommandPayload> channel = createChannel(sent);
        Sender<CommandPayload> sender = channel.to(List.of(backend("s1"))).requirePlayer();

        assertThrows(IllegalStateException.class, () -> sender.send(new CommandPayload("cmd", RunAs.CONSOLE)));
    }

    @Test
    void whenOnlineRequestThrows() {
        List<PluginMessage> sent = new ArrayList<>();
        PluginMessageChannel<CommandPayload> channel = createChannel(sent);
        Sender<CommandPayload> sender = channel.to(List.of(backend("s1"))).whenOnline(UUID.randomUUID());

        assertThrows(UnsupportedOperationException.class, () -> sender.request(new CommandPayload("cmd", RunAs.CONSOLE)));
    }

    @Test
    void multiTargetAcceptsConditions() {
        List<PluginMessage> sent = new ArrayList<>();
        PluginMessageChannel<CommandPayload> channel = createChannel(sent);
        UUID playerUuid = UUID.randomUUID();

        channel.to(List.of(backend("s1"), backend("s2")))
                .requirePlayer(playerUuid)
                .send(new CommandPayload("cmd", RunAs.CONSOLE))
                .join();

        assertEquals(2, sent.size());
        assertEquals(playerUuid, sent.get(0).requirePlayer());
        assertEquals(playerUuid, sent.get(1).requirePlayer());
    }

    private PluginMessageChannel<CommandPayload> createChannel(List<PluginMessage> sent) {
        return new PluginMessageChannel<>(
                CommandPayload.class,
                (target, msg) -> {
                    sent.add(msg);
                    return CompletableFuture.completedFuture(null);
                },
                (target, msg, timeout) -> CompletableFuture.completedFuture(msg),
                listener -> () -> {
                });
    }
}
