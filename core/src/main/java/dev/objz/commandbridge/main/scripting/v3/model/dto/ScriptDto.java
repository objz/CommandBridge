package dev.objz.commandbridge.main.scripting.v3.model.dto;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public final class ScriptDto {
	@Setting("version")
	public Integer version;
	@Setting("name")
	public String name;
	@Setting("description")
	public String description;
	@Setting("enabled")
	public Boolean enabled;
	@Setting("aliases")
	public List<String> aliases;

	@Setting("permissions")
	public PermissionsDto permissions;
	@Setting("defaults")
	public DefaultsDto defaults;
	@Setting("args")
	public List<ArgDto> args;
	@Setting("commands")
	public List<CommandStepDto> commands;
}
