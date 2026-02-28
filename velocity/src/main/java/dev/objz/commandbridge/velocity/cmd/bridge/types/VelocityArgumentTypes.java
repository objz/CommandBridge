package dev.objz.commandbridge.velocity.cmd.bridge.types;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.scripting.model.enums.Location;
import dev.objz.commandbridge.scripting.platform.PlatformFeatureKeys;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.CustomArgumentRegistry;
import java.util.Objects;

public final class VelocityArgumentTypes {
    private VelocityArgumentTypes() {
    }

    public static void register(CustomArgumentRegistry registry, PlatformFeatures features, ProxyServer proxy) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(proxy, "proxy");
        if (features == null) {
            return;
        }
        if (features.isEnabled(Location.VELOCITY, PlatformFeatureKeys.PACKET_EVENTS)) {
            registry.register(new TimeArgumentType());
            registry.register(new PlayersArgumentType(proxy));
        }
    }
}
