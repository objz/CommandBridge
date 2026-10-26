package dev.objz.commandbridge.net.payloads.cmd;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.objz.commandbridge.cmd.ref.EntityRef;
import dev.objz.commandbridge.cmd.ref.Location2D;
import dev.objz.commandbridge.cmd.ref.Location3D;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.scripting.model.enums.ArgType;

public record InvokedCommand(
        String name,
        List<TypedArgument> args,
        SenderContext sender) {

    public record TypedArgument(
            ArgType type,
            Object value) {
        public TypedArgument {
            value = normalizeValue(type, value);
        }
    }

    /** Only reconstruct the known argument records, never Java types supplied by a peer. */
    public static Object normalizeValue(ArgType type, Object value) {
        if (value == null || type == null) {
            return value;
        }
        return switch (type) {
            case LOCATION -> value instanceof Map<?, ?> ? Envelope.MAPPER.convertValue(value, Location3D.class) : value;
            case LOCATION_2D -> value instanceof Map<?, ?> ? Envelope.MAPPER.convertValue(value, Location2D.class) : value;
            case PLAYERS, ENTITIES -> {
                if (value instanceof Collection<?> values) {
                    yield values.stream().map(InvokedCommand::normalizeEntity).toList();
                }
                yield normalizeEntity(value);
            }
            default -> value;
        };
    }

    private static Object normalizeEntity(Object value) {
        return value instanceof Map<?, ?> ? Envelope.MAPPER.convertValue(value, EntityRef.class) : value;
    }
}
