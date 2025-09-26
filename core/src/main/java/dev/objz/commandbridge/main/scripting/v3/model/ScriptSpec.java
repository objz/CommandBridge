package dev.objz.commandbridge.main.scripting.v3.model;

import dev.objz.commandbridge.main.scripting.v3.model.args.ArgDef;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public final class ScriptSpec {
	@Setting("version")
	private Integer version;
	@Setting("name")
	private String name;
	@Setting("description")
	private String description;
	@Setting("enabled")
	private Boolean enabled;
	@Setting("aliases")
	private List<String> aliases;

	@Setting("permissions")
	private PermissionsSpec permissions;
	@Setting("defaults")
	private DefaultsSpec defaults;

	@Setting("args")
	private List<ArgDef> args;
	@Setting("commands")
	private List<StepSpec> commands;

	public Integer version() {
		return version;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public Boolean enabled() {
		return enabled;
	}

	public List<String> aliases() {
		return aliases;
	}

	public PermissionsSpec permissions() {
		return permissions;
	}

	public DefaultsSpec defaults() {
		return defaults;
	}

	public List<ArgDef> args() {
		return args;
	}

	public List<StepSpec> commands() {
		return commands;
	}
}
