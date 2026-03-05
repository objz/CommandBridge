package dev.objz.commandbridge.velocity.cmd.bridge.types;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.jorel.commandapi.arguments.Argument;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.CustomArgumentType;
import dev.objz.commandbridge.velocity.util.UserCache;

import java.util.Objects;

public final class OfflinePlayerArgumentType implements CustomArgumentType<String> {

    private final ProxyServer proxy;
    private final UserCache userCache;

    public OfflinePlayerArgumentType(ProxyServer proxy, UserCache userCache) {
        this.proxy = Objects.requireNonNull(proxy);
        this.userCache = Objects.requireNonNull(userCache);
    }

    @Override
    public ArgType type() {
        return ArgType.OFFLINE_PLAYER;
    }

    @Override
    public Argument<String> create(String nodeName) {
        return new OfflinePlayerArgument(nodeName, proxy, userCache);
    }
}
