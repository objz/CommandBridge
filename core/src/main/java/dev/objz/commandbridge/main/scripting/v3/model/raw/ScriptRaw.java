package dev.objz.commandbridge.main.scripting.v3.model.raw;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public final class ScriptRaw {
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
	public PermissionsRaw permissions;
	@Setting("defaults")
	public DefaultsRaw defaults;
	@Setting("args")
	public List<ArgRaw> args;
	@Setting("commands")
	public List<CommandStepRaw> commands;
}
