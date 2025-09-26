package dev.objz.commandbridge.main.scripting.v3.model;

import dev.objz.commandbridge.main.scripting.v3.enums.ExecutorMode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class StepTargetSpec {
	@Setting("run-as")
	private ExecutorMode runAs;
	@Setting("id")
	private String id;
	@Setting("kind")
	private KindSpec kind;
	@Setting("server")
	private ServerSpec server;

	// flattened conveniences
	@Setting("target-required")
	private Boolean targetRequired;
	@Setting("schedule-online")
	private Boolean scheduleOnline;

	public ExecutorMode runAs() {
		return runAs;
	}

	public String id() {
		return id;
	}

	public KindSpec kind() {
		return kind;
	}

	public ServerSpec server() {
		return server;
	}

	public Boolean targetRequired() {
		return targetRequired;
	}

	public Boolean scheduleOnline() {
		return scheduleOnline;
	}
}
