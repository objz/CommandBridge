package dev.objz.commandbridge.velocity.cmd;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.jorel.commandapi.arguments.Argument;
import dev.objz.commandbridge.cmd.ArgumentMapperInterface;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.CustomArgumentRegistry;
import dev.objz.commandbridge.velocity.cmd.bridge.framework.VelocityArgumentBridge;

public final class ArgumentMapper implements ArgumentMapperInterface<Argument<?>> {
    private final VelocityArgumentBridge bridge;

    public ArgumentMapper(ProxyServer proxy, CustomArgumentRegistry argumentRegistry) {
        this.bridge = new VelocityArgumentBridge(proxy, argumentRegistry);
    }

    @Override
    public Argument<?> map(ArgMapping argMapping) {
        return bridge.map(argMapping);
    }
}
