package dev.objz.commandbridge.main.scripting.v3.model.dto;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.time.Duration;
import java.util.List;

@ConfigSerializable
public final class CommandStepDto {
	@Setting("command")
	public String command;

	// optional
	
	@Setting("kind")
	public TargetKindDto kind;

	@Setting("server")
	public TargetServerDto server;

	@Setting("delay")
	public Duration delay;

	@Setting("timeout")
	public Duration timeout;

	public static final class ListWrapper {
		public final List<CommandStepDto> list;

		public ListWrapper(List<CommandStepDto> list) {
			this.list = list;
		}
	}
}
