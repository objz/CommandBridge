package dev.objz.commandbridge.scripting.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.objz.commandbridge.scripting.anno.Default;
import dev.objz.commandbridge.scripting.anno.Max;
import dev.objz.commandbridge.scripting.anno.Min;
import dev.objz.commandbridge.scripting.anno.ModelRoot;
import dev.objz.commandbridge.scripting.anno.Pattern;
import dev.objz.commandbridge.scripting.anno.Required;
import dev.objz.commandbridge.scripting.bind.PlaceholderExtractor;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.IdMapping;

@ModelRoot("script")
public record Script(
		@Min(1) @Max(2) @Required int version,

		@Required @Pattern(regex = "^[a-z][a-z0-9-]{2,32}$") String name,

		@Default("true") boolean enabled,

		String description,

		List<String> aliases,

		@Required Permissions permissions,

		@Required List<IdMapping> register,

		@Required Defaults defaults,

		@Required List<ArgMapping> args,

		@Required List<CmdMapping> commands

) {

	public List<ArgMapping> usedArguments() {
		if (commands == null || commands.isEmpty()) {
			return List.of();
		}

		if (args == null || args.isEmpty()) {
			return List.of();
		}

		List<String> commandStrings = new ArrayList<>();
		for (CmdMapping cmd : commands) {
			if (cmd != null && cmd.command() != null) {
				commandStrings.add(cmd.command());
			}
		}

		List<String> usedNames = PlaceholderExtractor.extractAll(commandStrings);

		Map<String, ArgMapping> argsByName = args.stream()
				.filter(arg -> arg != null && arg.name() != null)
				.collect(Collectors.toMap(ArgMapping::name, arg -> arg, (a, b) -> a));

		List<ArgMapping> result = new ArrayList<>();
		for (String name : usedNames) {
			ArgMapping arg = argsByName.get(name);
			if (arg != null) {
				result.add(arg);
			}
		}

		return List.copyOf(result);
	}
}
