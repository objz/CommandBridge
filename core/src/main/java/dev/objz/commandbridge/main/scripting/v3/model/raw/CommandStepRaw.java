package dev.objz.commandbridge.main.scripting.v3.model.raw;

import java.util.List;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class CommandStepRaw {
	@Setting("command")
	public String command;
	@Setting("target")
	public TargetRaw target; // shorthand override
	@Setting("delay")
	public java.time.Duration delay;
	@Setting("timeout")
	public java.time.Duration timeout;

	public static final class ListWrapper {
		public final List<CommandStepRaw> list;

		public ListWrapper(List<CommandStepRaw> list) {
			this.list = list;
		}
	}
}
