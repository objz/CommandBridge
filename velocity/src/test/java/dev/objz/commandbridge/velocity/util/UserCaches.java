package dev.objz.commandbridge.velocity.util;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.net.OutNode;
import dev.objz.commandbridge.velocity.net.session.SessionHub;

import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Function;

/** Gives tests outside this package a {@link UserCache} that never calls the Mojang API. */
public final class UserCaches {

    public static UserCache offline(ProxyServer proxy, SessionHub sessions, OutNode outNode, Path cachePath,
            Function<String, UUID> mojangLookup) {
        return new UserCache(proxy, sessions, outNode, cachePath, mojangLookup);
    }

    private UserCaches() {
    }
}
