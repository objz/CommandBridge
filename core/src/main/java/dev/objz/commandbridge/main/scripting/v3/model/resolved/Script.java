package dev.objz.commandbridge.main.scripting.v3.model.resolved;

import java.util.List;

public record Script(
		int version,
		String name,
		String description,
		boolean enabled,
		List<String> aliases,
		Permissions permissions,
		Defaults defaults,
		List<ArgDef> args,
		List<CommandStep> commands) {
	public record ArgDef(String name, boolean required, ArgType type) {
	}

	public enum ArgType {
		STRING, NUMBER, UUID, ENCHANTMENT, CHOICE
	} // adjust to your set
}
