package dev.objz.commandbridge.velocity.cmd.bridge.framework;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import java.util.Objects;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class VelocityArgumentBridge {
    private static final Set<ArgType> BUILTIN_TYPES = EnumSet.of(
            ArgType.STRING,
            ArgType.INTEGER,
            ArgType.BOOLEAN,
            ArgType.DOUBLE,
            ArgType.TEXT,
            ArgType.GREEDY_STRING,
            ArgType.SERVER);

    private final ProxyServer proxy;
    private final CustomArgumentRegistry argumentRegistry;

    public VelocityArgumentBridge(ProxyServer proxy, CustomArgumentRegistry argumentRegistry) {
        this.proxy = Objects.requireNonNull(proxy);
        this.argumentRegistry = Objects.requireNonNull(argumentRegistry);
    }

    public Argument<?> map(ArgMapping argMapping) {
        if (argMapping == null || argMapping.name() == null || argMapping.name().isBlank()) {
            throw new IllegalArgumentException("Argument name cannot be null or blank");
        }
        if (argMapping.type() == null) {
            throw new IllegalArgumentException("ArgType cannot be null");
        }

        String argName = argMapping.name();
        ArgType type = argMapping.type();

        Argument<?> argument = argumentRegistry.createArgument(argMapping)
                .orElseGet(() -> switch (type) {
                    case STRING -> new StringArgument(argName);
                    case INTEGER -> new IntegerArgument(argName);
                    case BOOLEAN -> new BooleanArgument(argName);
                    case DOUBLE -> new DoubleArgument(argName);
                    case TEXT -> new TextArgument(argName);
                    case GREEDY_STRING -> new GreedyStringArgument(argName);

                    case SERVER -> new StringArgument(argName).includeSuggestions(
                                ArgumentSuggestions.strings(
                                        proxy.getAllServers().stream()
                                                .map(server -> server.getServerInfo().getName())
                                                .toArray(String[]::new)));

                    default -> throw new UnsupportedOperationException(
                            "ArgType." + type + " is not supported on Velocity. " +
                                    "Supported types: " + supportedTypeNames());
                });

        if (argMapping.suggestions() != null && !argMapping.suggestions().isEmpty()) {
            argument.includeSuggestions(ArgumentSuggestions.strings(
                    argMapping.suggestions().toArray(String[]::new)));
        }

        return argument;
    }

    private String supportedTypeNames() {
        Set<ArgType> supported = EnumSet.copyOf(BUILTIN_TYPES);
        supported.addAll(argumentRegistry.supportedTypes());
        return supported.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
