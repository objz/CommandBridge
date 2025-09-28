package dev.objz.commandbridge.main.scripting.v3.api.groups;

import java.util.List;

import dev.objz.commandbridge.main.scripting.v3.api.Meta;
import dev.objz.commandbridge.main.scripting.v3.api.constants.ScriptConstants;


public record Script(
		Meta<Integer> version,
		Meta<String> name,
		Meta<String> description,
		Meta<Boolean> enabled,
		Meta<List<String>> aliases,

		Permissions permissions,
		Defaults defaults,

		List<Arg> args
// List<CommandStep> commands
) {
	public static Script withDefaults(String name) {
		return new Script(
				Meta.isRequired(),
				Meta.isRequired(),
				Meta.isOptional(ScriptConstants.DESCRIPTION),
				Meta.isOptional(ScriptConstants.ENABLED),
				Meta.isOptional(ScriptConstants.ALIASES),
				Permissions.withDefaults(),
				Defaults.withDefaults(),
				List.of());
	}

}
