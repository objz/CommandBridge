package dev.objz.commandbridge.main.scripting.v3.model.dto;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class PermissionsDto {
	@Setting("enabled")
	public Boolean enabled;
	@Setting("silent")
	public Boolean silent;
}
