package dev.objz.commandbridge.velocity.cmd.bridge.framework;

import dev.objz.commandbridge.scripting.platform.PlatformFeatures;
import dev.objz.commandbridge.velocity.cmd.bridge.types.VelocityArgumentTypes;
import java.util.Objects;

public final class ArgumentBridge {
	private final CustomArgumentRegistry registry;

	public ArgumentBridge(PlatformFeatures platformFeatures) {
		this.registry = new CustomArgumentRegistry();
		VelocityArgumentTypes.register(registry,
				platformFeatures != null ? platformFeatures : PlatformFeatures.none());
	}

	public ArgumentBridge(CustomArgumentRegistry registry, PlatformFeatures platformFeatures) {
		this.registry = Objects.requireNonNull(registry);
		VelocityArgumentTypes.register(registry,
				platformFeatures != null ? platformFeatures : PlatformFeatures.none());
	}

	public CustomArgumentRegistry registry() {
		return registry;
	}
}
