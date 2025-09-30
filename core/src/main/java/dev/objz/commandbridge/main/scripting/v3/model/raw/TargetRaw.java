package dev.objz.commandbridge.main.scripting.v3.model.raw;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class TargetRaw {
	@Setting("run-as")
	public String runAs;
	@Setting("id")
	public String id;
	@Setting("kind")
	public TargetKindRaw kind;
	@Setting("server")
	public TargetServerRaw server;
}
