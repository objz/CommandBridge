package dev.objz.commandbridge.velocity.cmd.bridge.types;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.Message;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CommandAPIArgumentType;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.objz.commandbridge.cmd.ref.EntityRef;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class PlayersArgument extends Argument<List<EntityRef>> {

    private final ProxyServer proxy;

    public PlayersArgument(String nodeName, ProxyServer proxy) {
        super(nodeName, StringArgumentType::word);
        this.proxy = proxy;
    }

    @Override
    public Class<List<EntityRef>> getPrimitiveType() {
        return (Class<List<EntityRef>>) (Class<?>) List.class;
    }

    @Override
    public CommandAPIArgumentType getArgumentType() {
        return CommandAPIArgumentType.PRIMITIVE_STRING;
    }

    @Override
    public <Source> List<EntityRef> parseArgument(CommandContext<Source> cmdCtx, String key,
            CommandArguments previousArgs) throws CommandSyntaxException {
        String rawInput = cmdCtx.getArgument(key, String.class);

        if (rawInput == null || rawInput.isBlank()) {
            throw error("Player selector cannot be empty");
        }

        String input = rawInput.trim();

        if (input.startsWith("@")) {
            return resolveSelector(input, cmdCtx);
        }

        return resolveByName(input);
    }

    private <Source> List<EntityRef> resolveSelector(String input, CommandContext<Source> cmdCtx)
            throws CommandSyntaxException {
        String base = input.contains("[") ? input.substring(0, input.indexOf('[')) : input;

        return switch (base) {
            case "@a", "@e" -> resolveAll();
            case "@r" -> resolveRandom();
            case "@s", "@p" -> resolveSender(cmdCtx);
            default -> throw error("Unknown selector: " + base);
        };
    }

    private List<EntityRef> resolveAll() throws CommandSyntaxException {
        var players = proxy.getAllPlayers();
        if (players.isEmpty()) {
            throw error("No players found");
        }
        return players.stream()
                .map(this::toEntityRef)
                .toList();
    }

    private List<EntityRef> resolveRandom() throws CommandSyntaxException {
        var players = List.copyOf(proxy.getAllPlayers());
        if (players.isEmpty()) {
            throw error("No players found");
        }
        Player picked = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        return List.of(toEntityRef(picked));
    }

    private <Source> List<EntityRef> resolveSender(CommandContext<Source> cmdCtx)
            throws CommandSyntaxException {
        Source source = cmdCtx.getSource();
        if (source instanceof Player player) {
            return List.of(toEntityRef(player));
        }
        throw error("This selector requires a player");
    }

    private List<EntityRef> resolveByName(String name) throws CommandSyntaxException {
        Player player = proxy.getPlayer(name).orElse(null);
        if (player == null) {
            final String n = name;
            throw error("No player found: " + n);
        }
        return List.of(toEntityRef(player));
    }

    private EntityRef toEntityRef(Player player) {
        return new EntityRef("PLAYER", player.getUniqueId().toString(), player.getUsername());
    }

    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType((Message) () -> message).create();
    }
}
