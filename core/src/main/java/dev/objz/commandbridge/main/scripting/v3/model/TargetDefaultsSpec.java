package dev.objz.commandbridge.main.scripting.v3.model;

import dev.objz.commandbridge.main.scripting.v3.enums.ExecutorMode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class TargetDefaultsSpec {
	@Setting("run-as")
	private ExecutorMode runAs;
	@Setting("id")
	private String id;
	@Setting("kind")
	private KindSpec kind;
	@Setting("server")
	private ServerSpec server;

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
}
