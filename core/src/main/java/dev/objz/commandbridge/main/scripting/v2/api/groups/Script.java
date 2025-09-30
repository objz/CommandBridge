package dev.objz.commandbridge.main.scripting.v3.api.groups;

import java.util.ArrayList;
import java.util.List;

import dev.objz.commandbridge.main.scripting.v3.api.Meta;
import dev.objz.commandbridge.main.scripting.v3.api.ProblemSink;
import dev.objz.commandbridge.main.scripting.v3.api.constants.ScriptConstants;
import dev.objz.commandbridge.main.scripting.v3.api.groups.commands.CommandStep;
import dev.objz.commandbridge.main.scripting.v3.api.groups.commands.ResolvedStep;
import dev.objz.commandbridge.main.scripting.v3.api.groups.commands.StepResolver;

public record Script(
		Meta<Integer> version,
		Meta<String> name,
		Meta<String> description,
		Meta<Boolean> enabled,
		Meta<List<String>> aliases,

		Permissions permissions,
		Defaults defaults,

		List<Arg> args,
		List<CommandStep> commands) {
	public static Script withDefaults(String name) {
		return new Script(
				Meta.isRequired(),
				Meta.isRequired(),
				Meta.isOptional(ScriptConstants.DESCRIPTION),
				Meta.isOptional(ScriptConstants.ENABLED),
				Meta.isOptional(ScriptConstants.ALIASES),
				Permissions.withDefaults(),
				Defaults.withDefaults(),
				List.of(),
				List.of());
	}

	public void validate(ProblemSink problems) { 
		for (int i = 0; i < args.size(); i++) {
			args.get(i).validate(problems);
		}
		for (int i = 0; i < commands.size(); i++) {
			commands.get(i).validate(problems, i);
		}
	}

	public List<ResolvedStep> resolveSteps(ProblemSink problems) { 
		ArrayList<ResolvedStep> out = new ArrayList<>(commands.size());
		for (int i = 0; i < commands.size(); i++) {
			out.add(StepResolver.resolve(commands.get(i), this, problems, i));
		}
		return List.copyOf(out);
	}
}
