package dev.objz.commandbridge.velocity.cmd.bridge.framework;

import dev.jorel.commandapi.arguments.Argument;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomArgumentRegistry {
    private final Map<ArgType, CustomArgumentType<?>> customTypes = new EnumMap<>(ArgType.class);
    private final Map<String, CommandOverride> commandOverrides = new ConcurrentHashMap<>();

    public void register(CustomArgumentType<?> customType) {
        if (customType == null || customType.type() == null) {
            throw new IllegalArgumentException("CustomArgumentType and its ArgType cannot be null");
        }
        customTypes.put(customType.type(), customType);
    }

    public Optional<Argument<?>> createArgument(ArgMapping mapping) {
        if (mapping == null || mapping.type() == null) {
            return Optional.empty();
        }
        String name = mapping.name();
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        CustomArgumentType<?> customType = customTypes.get(mapping.type());
        if (customType == null) {
            return Optional.empty();
        }
        return Optional.of(customType.create(name));
    }

    public Optional<PacketArgumentSpec> packetSpec(ArgMapping mapping) {
        if (mapping == null || mapping.type() == null) {
            return Optional.empty();
        }
        CustomArgumentType<?> customType = customTypes.get(mapping.type());
        if (customType == null) {
            return Optional.empty();
        }
        return customType.packetSpec();
    }

    public void registerCommand(CommandStub stub) {
        if (stub == null) {
            return;
        }
        Set<String> commandNames = collectCommandNames(stub.name(), stub.aliases());
        if (commandNames.isEmpty()) {
            return;
        }

        Map<String, PacketArgumentSpec> specs = new HashMap<>();
        List<ArgMapping> args = stub.args();
        if (args != null) {
            for (ArgMapping arg : args) {
                if (arg == null || arg.name() == null || arg.name().isBlank()) {
                    continue;
                }
                packetSpec(arg).ifPresent(spec -> specs.put(arg.name(), spec));
            }
        }

        if (specs.isEmpty()) {
            for (String name : commandNames) {
                commandOverrides.remove(name);
            }
            return;
        }

        CommandOverride override = new CommandOverride(specs);
        for (String name : commandNames) {
            commandOverrides.put(name, override);
        }
    }

    public Optional<CommandOverride> getOverride(String commandName) {
        if (commandName == null || commandName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(commandOverrides.get(commandName));
    }

    public void clearCommands() {
        commandOverrides.clear();
    }

    public Set<ArgType> supportedTypes() {
        return Set.copyOf(customTypes.keySet());
    }

    private static Set<String> collectCommandNames(String primary, List<String> aliases) {
        Set<String> names = new HashSet<>();
        if (primary != null && !primary.isBlank()) {
            names.add(primary);
        }
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias != null && !alias.isBlank()) {
                    names.add(alias);
                }
            }
        }
        return names;
    }
}
