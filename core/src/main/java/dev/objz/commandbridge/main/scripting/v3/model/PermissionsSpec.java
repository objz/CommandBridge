package dev.objz.commandbridge.main.scripting.v3.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class PermissionsSpec {
	@Setting("enabled")
	private Boolean enabled;
	@Setting("silent")
	private Boolean silent;

	public Boolean enabled() {
		return enabled;
	}

	public Boolean silent() {
		return silent;
	}
}
