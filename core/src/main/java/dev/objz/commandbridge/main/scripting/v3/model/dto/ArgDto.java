package dev.objz.commandbridge.main.scripting.v3.model.dto;

import java.util.List;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public final class ArgDto {
	@Setting("name")
	public String name;
	@Setting("required")
	public Boolean required;
	@Setting("type")
	public String type;

	public static final class ListWrapper {
		public final List<ArgDto> list;

		public ListWrapper(List<ArgDto> list) {
			this.list = list;
		}
	}
}
