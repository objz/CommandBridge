package dev.objz.commandbridge.main.scripting.v3.model.args;

import dev.objz.commandbridge.main.scripting.v3.enums.ArgType;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class SimpleArgDef implements ArgDef {
	@Setting("name")
	private String name;
	@Setting("required")
	private Boolean required;
	@Setting("type")
	private ArgType type;

	public SimpleArgDef() {
	}

	public SimpleArgDef(String name, Boolean required, ArgType type) {
		this.name = name;
		this.required = required;
		this.type = type;
	}

	public String name() {
		return name;
	}

	public Boolean required() {
		return required;
	}

	public ArgType type() {
		return type;
	}
}
