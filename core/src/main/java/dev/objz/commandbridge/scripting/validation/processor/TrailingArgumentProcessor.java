package dev.objz.commandbridge.scripting.validation.processor;

import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.PlaceholderExtractor;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.enums.ArgType;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.validation.PostProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TrailingArgumentProcessor implements PostProcessor {
	private static final Set<ArgType> TRAILING_TYPES = Set.of(ArgType.GREEDY_STRING);

	@Override
	public void process(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
		if (!Script.class.equals(buf.recordClass())) {
			return;
		}

		int idxArgs = -1;
		int idxCommands = -1;
		var comps = buf.components();

		for (int i = 0; i < comps.length; i++) {
			String name = comps[i].getName();
			if ("args".equals(name)) {
				idxArgs = i;
			} else if ("commands".equals(name)) {
				idxCommands = i;
			}
		}

		if (idxArgs < 0 || idxCommands < 0) {
			return;
		}

		Object argsObj = buf.get(idxArgs);
		Object commandsObj = buf.get(idxCommands);
		if (!(argsObj instanceof List<?> argsList) || argsList.isEmpty()) {
			return;
		}
		if (!(commandsObj instanceof List<?> commandsList) || commandsList.isEmpty()) {
			return;
		}

		Map<String, ArgMapping> argsByName = new HashMap<>();
		for (Object obj : argsList) {
			if (obj instanceof ArgMapping arg && arg.name() != null) {
				argsByName.put(arg.name(), arg);
			}
		}

		for (int cmdIdx = 0; cmdIdx < commandsList.size(); cmdIdx++) {
			Object cmdObj = commandsList.get(cmdIdx);
			if (!(cmdObj instanceof CmdMapping cmd)) {
				continue;
			}
			String commandStr = cmd.command();
			if (commandStr == null || commandStr.isBlank()) {
				continue;
			}

			List<String> usedArgNames = PlaceholderExtractor.extract(commandStr);
			if (usedArgNames.isEmpty()) {
				continue;
			}

			List<ArgMapping> usedArgs = new ArrayList<>();
			for (String argName : usedArgNames) {
				ArgMapping arg = argsByName.get(argName);
				if (arg != null) {
					usedArgs.add(arg);
				}
			}

			if (usedArgs.isEmpty()) {
				continue;
			}

			int lastIndex = usedArgs.size() - 1;
			for (int i = 0; i < usedArgs.size(); i++) {
				ArgMapping arg = usedArgs.get(i);
				if (arg == null || arg.type() == null) {
					continue;
				}
				if (TRAILING_TYPES.contains(arg.type()) && i != lastIndex) {
					String argName = arg.name() != null ? arg.name() : "<unnamed>";
					ctx.problems().error(
							"commands[" + cmdIdx + "]",
							String.format(
									"Argument '%s' (type '%s') must be the last argument in command string",
									argName,
									arg.type().name()));
				}
			}
		}
	}
}
