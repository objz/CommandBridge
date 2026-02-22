package dev.objz.commandbridge.velocity.cmd.bridge.framework;

import dev.jorel.commandapi.arguments.Argument;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import java.util.Optional;

public interface CustomArgumentType<T> {
    ArgType type();

    Argument<T> create(String nodeName);

    default Optional<PacketArgumentSpec> packetSpec() {
        return Optional.empty();
    }
}
