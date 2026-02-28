package dev.objz.commandbridge.velocity.cmd.bridge.types;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.jorel.commandapi.arguments.Argument;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.CustomArgumentType;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.PacketArgumentSpec;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PlayersArgumentType implements CustomArgumentType<List> {

    // minecraft:entity flags: 0x01 = single only, 0x02 = players only
    // ManyPlayers = 0x02 (multiple allowed, players only)
    private static final PacketArgumentSpec PACKET_SPEC =
            PacketArgumentSpec.ofParser("minecraft:entity", List.of((Object) (byte) 2));

    private final ProxyServer proxy;

    public PlayersArgumentType(ProxyServer proxy) {
        this.proxy = Objects.requireNonNull(proxy);
    }

    @Override
    public ArgType type() {
        return ArgType.PLAYERS;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Argument<List> create(String nodeName) {
        return (Argument<List>) (Argument<?>) new PlayersArgument(nodeName, proxy);
    }

    @Override
    public Optional<PacketArgumentSpec> packetSpec() {
        return Optional.of(PACKET_SPEC);
    }
}
