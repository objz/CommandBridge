package dev.objz.commandbridge.main.scripting.v3.model.args;

import dev.objz.commandbridge.main.scripting.v3.enums.ArgType;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class RangeArgDef implements ArgDef {
	@Setting("name")
	private String name;
	@Setting("required")
	private Boolean required;
	@Setting("type")
	private ArgType type = ArgType.RANGE;
	@Setting("min")
	private Long min;
	@Setting("max")
	private Long max;

	public RangeArgDef() {
	}

	public RangeArgDef(String name, Boolean required, Long min, Long max) {
		this.name = name;
		this.required = required;
		this.min = min;
		this.max = max;
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

	public Long min() {
		return min;
	}

	public Long max() {
		return max;
	}
}
