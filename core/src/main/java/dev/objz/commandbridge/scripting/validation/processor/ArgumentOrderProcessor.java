package dev.objz.commandbridge.scripting.validation.processor;

import dev.objz.commandbridge.scripting.bind.BindContext;
import dev.objz.commandbridge.scripting.bind.PlaceholderExtractor;
import dev.objz.commandbridge.scripting.bind.RecordBinder;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.scripting.validation.PostProcessor;

import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates that required arguments do not follow optional arguments
 * **based on their usage order in command strings**, not declaration order.
 */
public final class ArgumentOrderProcessor implements PostProcessor {

    @Override
    public void process(RecordBinder.MutableRecordBuffer buf, BindContext ctx) {
        if (!Script.class.equals(buf.recordClass())) {
            return;
        }

        int idxArgs = -1, idxCommands = -1;
        RecordComponent[] comps = buf.components();

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

            boolean seenOptional = false;
            String firstOptionalName = null;

            for (String argName : usedArgNames) {
                ArgMapping arg = argsByName.get(argName);
                if (arg == null) {
                    continue;
                }

                if (arg.required()) {
                    if (seenOptional) {
                        ctx.problems().error(
                                "commands[" + cmdIdx + "]",
                                String.format(
                                        "Required argument '%s' is used after optional argument '%s' in command string",
                                        argName,
                                        firstOptionalName));
                    }
                } else {
                    if (!seenOptional) {
                        seenOptional = true;
                        firstOptionalName = argName;
                    }
                }
            }
        }
    }
}
