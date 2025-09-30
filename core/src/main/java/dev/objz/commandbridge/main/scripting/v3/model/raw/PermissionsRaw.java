package dev.objz.commandbridge.main.scripting.v3.model.raw;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class PermissionsRaw {
	@Setting("enabled")
	public Boolean enabled;
	@Setting("silent")
	public Boolean silent;
}
