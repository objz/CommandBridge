package dev.objz.commandbridge.main.scripting.v3.model.args;

import dev.objz.commandbridge.main.scripting.v3.enums.ArgType;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;

@ConfigSerializable
public final class ChoiceArgDef implements ArgDef {
	@Setting("name")
	private String name;
	@Setting("required")
	private Boolean required;
	@Setting("type")
	private ArgType type = ArgType.CHOICE;
	@Setting("choices")
	private List<String> choices;

	public ChoiceArgDef() {
	}

	public ChoiceArgDef(String name, Boolean required, List<String> choices) {
		this.name = name;
		this.required = required;
		this.choices = choices;
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

	public List<String> choices() {
		return choices;
	}
}
