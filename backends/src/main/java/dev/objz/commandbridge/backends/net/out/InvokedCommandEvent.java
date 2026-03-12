package dev.objz.commandbridge.backends.net.out;

import dev.jorel.commandapi.executors.CommandArguments;
import dev.objz.commandbridge.backends.net.out.ctx.InvokedCommandContext;
import dev.objz.commandbridge.cmd.ref.EntityRef;
import dev.objz.commandbridge.cmd.ref.Location2D;
import dev.objz.commandbridge.cmd.ref.Location3D;
import dev.objz.commandbridge.net.OutboundHandler;
import dev.objz.commandbridge.net.SendOperation;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.net.payloads.cmd.SenderContext;
import dev.objz.commandbridge.net.proto.Envelope;
import dev.objz.commandbridge.net.proto.MessageType;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class InvokedCommandEvent extends OutboundHandler<InvokedCommandContext> {

    @Override
    public SendOperation accept(InvokedCommandContext ctx) {
        SenderContext senderCtx = mapSender(ctx.sender);

        List<InvokedCommand.TypedArgument> typedArgs = new ArrayList<>();
        CommandStub stub = ctx.stub;
        CommandArguments args = ctx.args;

        if (stub.args() != null && !stub.args().isEmpty()) {
            for (ArgMapping mapping : stub.args()) {
                ArgType type = mapping.type();
                Object raw = args.getOptional(mapping.name()).orElse(null);
                Object value = mapArgument(type, raw);
                typedArgs.add(new InvokedCommand.TypedArgument(type, value));
            }
        }

        var payload = Envelope.MAPPER.valueToTree(
                new InvokedCommand(ctx.commandName, typedArgs, senderCtx));

        Envelope env = Envelope.make(
                MessageType.INVOKED_COMMAND,
                clientId,
                serverId,
                payload);

        SendOperation op = send(env);
        op.dispatch();
        return op;
    }

    private SenderContext mapSender(Object sender) {
        if (sender == null) {
            return new SenderContext.Other("null");
        }

        SenderContext.Player player = asPlayerSender(sender);
        if (player != null) {
            return player;
        }

        if (isConsoleSender(sender)) {
            return new SenderContext.Console();
        }

        Location3D block = extractBlockLocation(sender);
        if (block != null) {
            return new SenderContext.Block(block);
        }

        return new SenderContext.Other(sender.getClass().getSimpleName());
    }

    private SenderContext.Player asPlayerSender(Object sender) {
        UUID uuid = invokeUuid(sender, "getUniqueId");
        if (uuid == null) {
            return null;
        }

        String name = invokeString(sender, "getUsername");
        if (name == null || name.isBlank()) {
            name = invokeString(sender, "getName");
        }
        if (name == null || name.isBlank()) {
            name = sender.getClass().getSimpleName();
        }

        return new SenderContext.Player(name, uuid.toString());
    }

    private boolean isConsoleSender(Object sender) {
        String className = sender.getClass().getName();
        return className.endsWith("ConsoleCommandSender")
                || className.endsWith("ConsoleCommandSource");
    }

    private Location3D extractBlockLocation(Object sender) {
        Object block = invoke(sender, "getBlock");
        if (block == null) {
            return null;
        }
        Object location = invoke(block, "getLocation");
        return toLocation3D(location);
    }

    private Object mapArgument(ArgType type, Object raw) {
        return switch (type) {
            case STRING, TEXT, GREEDY_STRING, ENTITY_TYPE, OFFLINE_PLAYER, RANGE,
                    WORLD, ANGLE, ROTATION, ITEM_STACK, ENCHANTMENT, POTION_EFFECT,
                    SOUND, BIOME, SERVER -> raw != null ? raw.toString() : null;

            case INTEGER, TIME -> toInt(raw);

            case DOUBLE -> toDouble(raw);

            case BOOLEAN -> toBoolean(raw);

            case LOCATION -> toLocation3D(raw);

            case LOCATION_2D -> toLocation2D(raw);

            case PLAYERS, ENTITIES -> toEntityRefs(raw);
        };
    }

    private int toInt(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw == null) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private double toDouble(Object raw) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(raw.toString());
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private boolean toBoolean(Object raw) {
        if (raw instanceof Boolean value) {
            return value;
        }
        return raw != null && Boolean.parseBoolean(raw.toString());
    }

    private Location3D toLocation3D(Object raw) {
        if (raw == null) {
            return null;
        }

        Double x = invokeDouble(raw, "getX");
        Double y = invokeDouble(raw, "getY");
        Double z = invokeDouble(raw, "getZ");
        if (x == null || y == null || z == null) {
            return null;
        }

        return new Location3D(resolveWorldName(raw), x, y, z);
    }

    private Location2D toLocation2D(Object raw) {
        if (raw == null) {
            return null;
        }

        Double x = invokeDouble(raw, "getX");
        Double z = invokeDouble(raw, "getZ");
        if (x == null || z == null) {
            return null;
        }

        return new Location2D(resolveWorldName(raw), x, z);
    }

    private String resolveWorldName(Object location) {
        Object world = invoke(location, "getWorld");
        String worldName = invokeString(world, "getName");
        if (worldName == null || worldName.isBlank()) {
            worldName = invokeString(location, "getWorldName");
        }
        return (worldName == null || worldName.isBlank()) ? "unknown" : worldName;
    }

    private List<EntityRef> toEntityRefs(Object raw) {
        if (!(raw instanceof Collection<?> collection)) {
            return List.of();
        }

        return collection.stream()
                .map(this::toEntityRef)
                .filter(Objects::nonNull)
                .toList();
    }

    private EntityRef toEntityRef(Object entity) {
        if (entity == null) {
            return null;
        }

        if (entity instanceof String name && !name.isBlank()) {
            return new EntityRef("PLAYER", null, name);
        }

        String uuid = null;
        UUID parsedUuid = invokeUuid(entity, "getUniqueId");
        if (parsedUuid != null) {
            uuid = parsedUuid.toString();
        }

        String name = invokeString(entity, "getName");

        Object typeObj = invoke(entity, "getType");
        String type = invokeString(typeObj, "name");
        if (type == null || type.isBlank()) {
            type = typeObj != null ? typeObj.toString() : "UNKNOWN";
        }

        if ((name == null || name.isBlank()) && uuid == null) {
            return null;
        }

        return new EntityRef(type, uuid, name);
    }

    private Object invoke(Object target, String method) {
        if (target == null) {
            return null;
        }

        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private String invokeString(Object target, String method) {
        Object value = invoke(target, method);
        return value != null ? value.toString() : null;
    }

    private UUID invokeUuid(Object target, String method) {
        Object value = invoke(target, method);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String str) {
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private Double invokeDouble(Object target, String method) {
        Object value = invoke(target, method);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
