package dev.objz.commandbridge.main.scripting.v3.model.dto;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class TargetKindDto {
	@Setting("register")
	public String register; // "VELOCITY" | "BACKEND"
	@Setting("execute")
	public String execute; 
}
