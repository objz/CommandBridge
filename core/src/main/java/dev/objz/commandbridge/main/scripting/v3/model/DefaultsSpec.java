package dev.objz.commandbridge.main.scripting.v3.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class DefaultsSpec {
	@Setting("target")
	private TargetDefaultsSpec target;
	@Setting("delay")
	private String delay;
	@Setting("cooldown")
	private String cooldown;
	@Setting("timeout")
	private String timeout;

	public TargetDefaultsSpec target() {
		return target;
	}

	public String delay() {
		return delay;
	}

	public String cooldown() {
		return cooldown;
	}

	public String timeout() {
		return timeout;
	}
}
