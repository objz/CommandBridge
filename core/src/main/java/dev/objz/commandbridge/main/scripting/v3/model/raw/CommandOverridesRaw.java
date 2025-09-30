package dev.objz.commandbridge.main.scripting.v3.model.raw;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.time.Duration;

@ConfigSerializable
public final class CommandOverridesRaw {
	@Setting("target")
	public TargetRaw target; // per-step target override
	@Setting("delay")
	public Duration delay;
	@Setting("timeout")
	public Duration timeout;
}
