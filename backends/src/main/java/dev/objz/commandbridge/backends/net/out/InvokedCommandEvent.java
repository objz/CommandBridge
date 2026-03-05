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
import java.util.List;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public final class InvokedCommandEvent extends OutboundHandler<InvokedCommandContext> {

    @Override
    public SendOperation accept(InvokedCommandContext ctx) {
        CommandSender sender = ctx.sender;
        SenderContext senderCtx;
        if (sender instanceof Player p) {
            senderCtx = new SenderContext.Player(p.getName(), p.getUniqueId().toString());
        } else if (sender instanceof ConsoleCommandSender) {
            senderCtx = new SenderContext.Console();
        } else if (sender instanceof BlockCommandSender b) {
            var l = b.getBlock().getLocation();
            senderCtx = new SenderContext.Block(
                    new Location3D(
                            l.getWorld().getName(),
                            l.getX(),
                            l.getY(),
                            l.getZ()));
        } else {
            senderCtx = new SenderContext.Other(sender.getClass().getSimpleName());
        }

        List<InvokedCommand.TypedArgument> typedArgs = new ArrayList<>();
        CommandStub stub = ctx.stub;
        CommandArguments args = ctx.args;

        if (stub.args() != null && !stub.args().isEmpty()) {
            for (ArgMapping m : stub.args()) {
                String name = m.name();
                ArgType type = m.type();
                Object raw = args.getOptional(name).orElse(null);

                Object value = switch (type) {
                    case STRING, TEXT, GREEDY_STRING -> (raw != null ? raw.toString() : null);

                    case INTEGER, TIME -> (raw != null ? ((Number) raw).intValue() : 0);

                    case DOUBLE -> (raw != null ? ((Number) raw).doubleValue() : 0.0);

                    case BOOLEAN -> (raw instanceof Boolean b ? b
                            : raw != null && Boolean.TRUE.equals(raw));

                    case LOCATION -> {
                        org.bukkit.Location l = (org.bukkit.Location) raw;
                        yield (l == null) ? null
                                : new Location3D(
                                        l.getWorld().getName(),
                                        l.getX(),
                                        l.getY(),
                                        l.getZ());
                    }

                    case LOCATION_2D -> {
                        org.bukkit.Location l = (org.bukkit.Location) raw;
                        yield (l == null) ? null
                                : new Location2D(
                                        l.getWorld().getName(),
                                        l.getX(),
                                        l.getZ());
                    }

                    case PLAYERS, ENTITIES -> {
                        var list = ((raw instanceof java.util.Collection<?>)
                                ? (java.util.Collection<?>) raw
                                : List.of())
                                .stream()
                                .filter(o -> o instanceof org.bukkit.entity.Entity)
                                .map(o -> {
                                    var e = (org.bukkit.entity.Entity) o;
                                    return new EntityRef(
                                            e.getType().name(),
                                            e.getUniqueId().toString(),
                                            e.getName());
                                })
                                .toList();
                        yield list; // List<EntityRef>
                    }

                    case ENTITY_TYPE -> (raw != null ? raw.toString() : null);

                    case OFFLINE_PLAYER -> (raw != null ? raw.toString() : null);

                    case RANGE -> (raw != null ? raw.toString() : null);

                    case WORLD, ANGLE, ROTATION, ITEM_STACK, ENCHANTMENT, POTION_EFFECT,
                            SOUND, BIOME, SERVER ->
                        (raw != null ? raw.toString() : null);
                };

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
}
