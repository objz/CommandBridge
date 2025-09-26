package dev.objz.commandbridge.main.scripting.v3.model;

import dev.objz.commandbridge.main.scripting.v3.enums.ScriptSide;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class KindSpec {
	@Setting("register")
	private ScriptSide register;
	@Setting("execute")
	private ScriptSide execute;

	public ScriptSide register() {
		return register;
	}

	public ScriptSide execute() {
		return execute;
	}
}
