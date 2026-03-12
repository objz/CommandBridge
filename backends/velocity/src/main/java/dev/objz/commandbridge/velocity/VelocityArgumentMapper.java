package dev.objz.commandbridge.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.objz.commandbridge.cmd.ArgumentMapperInterface;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Collectors;

final class VelocityArgumentMapper implements ArgumentMapperInterface<Argument<?>> {

    private final ProxyServer proxy;

    VelocityArgumentMapper(ProxyServer proxy) {
        this.proxy = Objects.requireNonNull(proxy, "proxy");
    }

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

            case SERVER -> new StringArgument(argName).includeSuggestions(
                    ArgumentSuggestions.strings(proxy.getAllServers().stream()
                            .map(server -> server.getServerInfo().getName())
                            .toArray(String[]::new)));

            case OFFLINE_PLAYER -> new StringArgument(argName).includeSuggestions(
                    ArgumentSuggestions.strings(proxy.getAllPlayers().stream()
                            .map(player -> player.getUsername())
                            .toArray(String[]::new)));

            default -> throw new UnsupportedOperationException(
                    "ArgType." + type + " is not supported on Velocity client mode. "
                            + "Supported types: " + supportedTypeNames());
        };

        if (argMapping.suggestions() != null && !argMapping.suggestions().isEmpty()) {
            argument.includeSuggestions(ArgumentSuggestions.strings(
                    argMapping.suggestions().toArray(String[]::new)));
        }

        return argument;
    }

    private String supportedTypeNames() {
        EnumSet<ArgType> supported = EnumSet.of(
                ArgType.STRING,
                ArgType.INTEGER,
                ArgType.BOOLEAN,
                ArgType.DOUBLE,
                ArgType.TEXT,
                ArgType.GREEDY_STRING,
                ArgType.SERVER,
                ArgType.OFFLINE_PLAYER);
        return supported.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
