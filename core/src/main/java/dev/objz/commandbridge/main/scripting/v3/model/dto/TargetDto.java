package dev.objz.commandbridge.main.scripting.v3.model.dto;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class TargetDto {
	@Setting("run-as")
	public String runAs;
	@Setting("id")
	public String id;
	@Setting("kind")
	public TargetKindDto kind;
	@Setting("server")
	public TargetServerDto server;
}
