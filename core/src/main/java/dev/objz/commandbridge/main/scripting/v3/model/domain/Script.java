package dev.objz.commandbridge.main.scripting.v3.model.domain;

import java.util.List;

public record Script(
		int version,
		String name,
		String description,
		boolean enabled,
		List<String> aliases,
		Permissions permissions,
		Defaults defaults,
		List<ArgDef> args) {
	public record ArgDef(String name, boolean required, ArgType type) {
	}

	public enum ArgType {
		STRING, NUMBER, UUID, ENCHANTMENT, CHOICE
	}
}
