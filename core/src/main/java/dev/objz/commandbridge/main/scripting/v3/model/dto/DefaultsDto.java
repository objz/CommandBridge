package dev.objz.commandbridge.main.scripting.v3.model.dto;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.time.Duration;

@ConfigSerializable

public final class DefaultsDto {
	@Setting("run-as")
	public String runAs;

	@Setting("id")
	public String id;

	@Setting("kind")
	public TargetKindDto kind;

	@Setting("server")
	public TargetServerDto server;

	@Setting("delay")
	public Duration delay;

	@Setting("cooldown")
	public Duration cooldown;
}
