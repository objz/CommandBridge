package dev.objz.commandbridge.main.scripting.v3.model.raw;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.time.Duration;

@ConfigSerializable
public final class DefaultsRaw {
	@Setting("target")
	public TargetRaw target;
	@Setting("delay")
	public Duration delay;
	@Setting("cooldown")
	public Duration cooldown;
}
