package dev.objz.commandbridge.velocity.cmd.bridge.framework;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.velocity.cmd.bridge.types.VelocityArgumentTypes;
import java.util.Objects;

public final class ArgumentBridge {
    private final CustomArgumentRegistry registry;

    public ArgumentBridge(ProxyServer proxy, PlatformFeatures platformFeatures) {
        this.registry = new CustomArgumentRegistry();
        VelocityArgumentTypes.register(registry,
                platformFeatures != null ? platformFeatures : PlatformFeatures.none(),
                proxy);
    }

    public ArgumentBridge(ProxyServer proxy, CustomArgumentRegistry registry, PlatformFeatures platformFeatures) {
        this.registry = Objects.requireNonNull(registry);
        VelocityArgumentTypes.register(registry,
                platformFeatures != null ? platformFeatures : PlatformFeatures.none(),
                proxy);
    }

    public CustomArgumentRegistry registry() {
        return registry;
    }
}
