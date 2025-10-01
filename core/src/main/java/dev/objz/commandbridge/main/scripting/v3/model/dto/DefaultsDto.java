package dev.objz.commandbridge.main.scripting.v3.model.dto;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.time.Duration;

@ConfigSerializable
public final class DefaultsDto {
	@Setting("target")
	public TargetDto target;
	@Setting("delay")
	public Duration delay;
	@Setting("cooldown")
	public Duration cooldown;
}
