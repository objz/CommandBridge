package dev.objz.commandbridge.velocity.cmd.bridge.types;

import dev.jorel.commandapi.arguments.Argument;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.CustomArgumentType;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.PacketArgumentSpec;
import java.util.List;
import java.util.Optional;

public final class TimeArgumentType implements CustomArgumentType<Integer> {
    private static final PacketArgumentSpec PACKET_SPEC = PacketArgumentSpec.ofParser("minecraft:time",
            List.of((byte) 0));

    @Override
    public ArgType type() {
        return ArgType.TIME;
    }

    @Override
    public Argument<Integer> create(String nodeName) {
        return new TimeArgument(nodeName);
    }

    @Override
    public Optional<PacketArgumentSpec> packetSpec() {
        return Optional.of(PACKET_SPEC);
    }
}
