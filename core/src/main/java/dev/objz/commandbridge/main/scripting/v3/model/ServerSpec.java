package dev.objz.commandbridge.main.scripting.v3.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class ServerSpec {
	@Setting("target-required")
	private Boolean targetRequired;
	@Setting("schedule-online")
	private Boolean scheduleOnline;
	@Setting("timeout")
	private String timeout; 
	@Setting("frequency")
	private String frequency;

	public Boolean targetRequired() {
		return targetRequired;
	}

	public Boolean scheduleOnline() {
		return scheduleOnline;
	}

	public String timeoutEffective() {
		return timeout;
	}

	public String frequencyEffective() {
		return frequency;
	}
}
