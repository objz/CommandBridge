package dev.objz.commandbridge.velocity.registry;

import dev.objz.commandbridge.proto.cmd.CommandStub;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StubExporter {
	private StubExporter() {
	}

	public static List<CommandStub> export(List<Script> scripts) {
		List<CommandStub> out = new ArrayList<>();
		if (scripts == null || scripts.isEmpty())
			return out;

		for (Script s : scripts) {
			List<ArgMapping> usedArgs = s.usedArguments();
			List<ArgMapping> deduplicatedArgs = deduplicateArguments(usedArgs);

			out.add(new CommandStub(
					s.name(),
					s.aliases(),
					s.description(),
					deduplicatedArgs));
		}
		return out;
	}

	private static List<ArgMapping> deduplicateArguments(List<ArgMapping> args) {
		if (args == null || args.isEmpty()) {
			return List.of();
		}

		Map<String, Integer> nameCounts = new HashMap<>();
		for (ArgMapping arg : args) {
			if (arg != null && arg.name() != null) {
				nameCounts.put(arg.name(), nameCounts.getOrDefault(arg.name(), 0) + 1);
			}
		}

		Map<String, Integer> currentIndices = new HashMap<>();
		List<ArgMapping> result = new ArrayList<>();

		for (ArgMapping arg : args) {
			if (arg == null || arg.name() == null) {
				continue;
			}

			String originalName = arg.name();
			int totalCount = nameCounts.get(originalName);

			if (totalCount == 1) {
				result.add(arg);
			} else {
				int currentIndex = currentIndices.getOrDefault(originalName, 0);
				String indexedName = originalName + "[" + currentIndex + "]";

				ArgMapping indexedArg = new ArgMapping(
						indexedName,
						arg.required(),
						arg.type(),
						arg.suggestions());

				result.add(indexedArg);
				currentIndices.put(originalName, currentIndex + 1);
			}
		}

		return List.copyOf(result);
	}
}
