package dev.objz.commandbridge.main.scripting.v3.model.raw;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class TargetKindRaw {
	@Setting("register")
	public String register; // e.g., "VELOCITY"
	@Setting("execute")
	public String execute; // e.g., "BACKEND"
}
