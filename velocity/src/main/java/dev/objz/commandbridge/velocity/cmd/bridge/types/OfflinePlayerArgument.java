package dev.objz.commandbridge.velocity.cmd.bridge.types;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CommandAPIArgumentType;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.objz.commandbridge.velocity.util.UserCache;

import java.util.LinkedHashSet;
import java.util.Set;

public final class OfflinePlayerArgument extends Argument<String> {

    private final ProxyServer proxy;
    private final UserCache userCache;

    public OfflinePlayerArgument(String nodeName, ProxyServer proxy, UserCache userCache) {
        super(nodeName, StringArgumentType::word);
        this.proxy = proxy;
        this.userCache = userCache;

        includeSuggestions(ArgumentSuggestions.strings(info -> {
            Set<String> names = new LinkedHashSet<>();
            for (Player player : proxy.getAllPlayers()) {
                names.add(player.getUsername());
            }
            names.addAll(userCache.knownNames());
            return names.toArray(String[]::new);
        }));
    }

    @Override
    public Class<String> getPrimitiveType() {
        return String.class;
    }

    @Override
    public CommandAPIArgumentType getArgumentType() {
        return CommandAPIArgumentType.PRIMITIVE_STRING;
    }

    @Override
    public <Source> String parseArgument(CommandContext<Source> cmdCtx, String key,
            CommandArguments previousArgs) {
        return cmdCtx.getArgument(key, String.class);
    }
}
