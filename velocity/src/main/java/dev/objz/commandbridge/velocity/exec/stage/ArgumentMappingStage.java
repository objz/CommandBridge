package dev.objz.commandbridge.velocity.exec.stage;

import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.ArgMapping;
import dev.objz.commandbridge.velocity.exec.ExecutionContext;
import dev.objz.commandbridge.velocity.exec.ExecutionResult;
import dev.objz.commandbridge.velocity.exec.Pipeline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ArgumentMappingStage implements Pipeline {

	@Override
	public void process(ExecutionContext context, Consumer<ExecutionResult> next) {
		Script script = context.script();
		List<InvokedCommand.TypedArgument> invokedArgs = context.invoked().args();

		List<ArgMapping> definedArgs = script.registeredArguments();

		if (definedArgs == null || definedArgs.isEmpty()) {
			next.accept(ExecutionResult.ok(context.withArguments(Map.of())));
			return;
		}

		Map<String, Object> mapped = new HashMap<>();

		for (int i = 0; i < definedArgs.size(); i++) {
			ArgMapping def = definedArgs.get(i);
			Object value = null;

			if (invokedArgs != null && i < invokedArgs.size()) {
				value = invokedArgs.get(i).value();
			}

			if (value == null && def.required()) {
				next.accept(ExecutionResult.error("Missing required argument: " + def.name()));
				return;
			}

			mapped.put(def.name(), value);
		}

		next.accept(ExecutionResult.ok(context.withArguments(mapped)));
	}
}
