package dev.objz.commandbridge.main.scripting.v3.model;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class StepSpec {
	@Setting("command")
	private String command;
	@Setting("target")
	private StepTargetSpec target;
	@Setting("delay")
	private String delay;
	@Setting("timeout")
	private String timeout;

	public String command() {
		return command;
	}

	public StepTargetSpec target() {
		return target;
	}

	public String delay() {
		return delay;
	}

	public String timeout() {
		return timeout;
	}
}
