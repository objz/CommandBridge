package dev.objz.commandbridge.backends.platform.cmd;

import dev.jorel.commandapi.arguments.*;
import dev.objz.commandbridge.cmd.ArgumentMapperInterface;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.stream.Collectors;

public final class ArgumentMapper implements ArgumentMapperInterface<Argument<?>> {

    @Override
    public Argument<?> map(ArgMapping argMapping) {
        if (argMapping == null || argMapping.name() == null || argMapping.name().isBlank()) {
            throw new IllegalArgumentException("Argument name cannot be null or blank");
        }
        if (argMapping.type() == null) {
            throw new IllegalArgumentException("ArgType cannot be null");
        }

        String argName = argMapping.name();
        ArgType type = argMapping.type();

        Argument<?> argument = switch (type) {
            case STRING -> new StringArgument(argName);
            case INTEGER -> new IntegerArgument(argName);
            case BOOLEAN -> new BooleanArgument(argName);
            case DOUBLE -> new DoubleArgument(argName);
            case TEXT -> new TextArgument(argName);
            case GREEDY_STRING -> new GreedyStringArgument(argName);

            case RANGE -> new DoubleRangeArgument(argName);

            case PLAYERS -> new EntitySelectorArgument.ManyPlayers(argName);
            case OFFLINE_PLAYER -> {
                var arg = new StringArgument(argName);
                arg.replaceSuggestions(ArgumentSuggestions.strings(info -> onlinePlayerNames()));
                yield arg;
            }
            case ENTITIES -> new EntitySelectorArgument.ManyEntities(argName);
            case ENTITY_TYPE -> new EntityTypeArgument(argName);

            case WORLD -> new WorldArgument(argName);
            case LOCATION -> new LocationArgument(argName);
            case LOCATION_2D -> new Location2DArgument(argName);
            case ANGLE -> new AngleArgument(argName);
            case ROTATION -> new RotationArgument(argName);

            case ITEM_STACK -> new ItemStackArgument(argName);
            case ENCHANTMENT -> new EnchantmentArgument(argName);
            case POTION_EFFECT -> new PotionEffectArgument(argName);

            case SOUND -> new SoundArgument(argName);
            case BIOME -> new BiomeArgument(argName);

            case TIME -> new TimeArgument(argName);

            default -> throw new UnsupportedOperationException(
                    "ArgType." + type + " is not supported on backends. " +
                            "Supported types: " + supportedTypeNames());
        };

        if (argMapping.suggestions() != null && !argMapping.suggestions().isEmpty()) {
            argument.includeSuggestions(ArgumentSuggestions.strings(
                    argMapping.suggestions().toArray(String[]::new)));
        }

        return argument;
    }

    private String[] onlinePlayerNames() {
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object onlinePlayers = bukkit.getMethod("getOnlinePlayers").invoke(null);
            if (!(onlinePlayers instanceof Collection<?> players)) {
                return new String[0];
            }

            var names = new ArrayList<String>(players.size());
            for (Object player : players) {
                if (player == null) {
                    continue;
                }
                Object rawName = player.getClass().getMethod("getName").invoke(player);
                if (rawName != null) {
                    String name = rawName.toString();
                    if (!name.isBlank()) {
                        names.add(name);
                    }
                }
            }
            return names.toArray(String[]::new);
        } catch (Throwable ignored) {
            return new String[0];
        }
    }

    private String supportedTypeNames() {
        EnumSet<ArgType> supported = EnumSet.of(
                ArgType.STRING,
                ArgType.INTEGER,
                ArgType.BOOLEAN,
                ArgType.DOUBLE,
                ArgType.TEXT,
                ArgType.GREEDY_STRING,
                ArgType.RANGE,
                ArgType.PLAYERS,
                ArgType.OFFLINE_PLAYER,
                ArgType.ENTITIES,
                ArgType.ENTITY_TYPE,
                ArgType.WORLD,
                ArgType.LOCATION,
                ArgType.LOCATION_2D,
                ArgType.ANGLE,
                ArgType.ROTATION,
                ArgType.ITEM_STACK,
                ArgType.ENCHANTMENT,
                ArgType.POTION_EFFECT,
                ArgType.SOUND,
                ArgType.BIOME,
                ArgType.TIME);
        return supported.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
