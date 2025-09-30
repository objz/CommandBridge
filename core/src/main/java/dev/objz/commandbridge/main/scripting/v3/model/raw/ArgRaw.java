package dev.objz.commandbridge.main.scripting.v3.model.raw;

import java.util.List;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class ArgRaw {
	@Setting("name")
	public String name;
	@Setting("required")
	public Boolean required;
	@Setting("type")
	public String type;

	public static final class ListWrapper {
		public final List<ArgRaw> list;

		public ListWrapper(List<ArgRaw> list) {
			this.list = list;
		}
	}
}
