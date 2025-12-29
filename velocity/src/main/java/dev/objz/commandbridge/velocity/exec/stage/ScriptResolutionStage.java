package dev.objz.commandbridge.velocity.exec.stage;

import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.exec.ExecutionContext;
import dev.objz.commandbridge.velocity.exec.ExecutionResult;
import dev.objz.commandbridge.velocity.exec.Pipeline;

import java.util.function.Consumer;

public class ScriptResolutionStage implements Pipeline {

	private final ScriptManager scriptManager;

	public ScriptResolutionStage(ScriptManager scriptManager) {
		this.scriptManager = scriptManager;
	}

	@Override
	public void process(ExecutionContext context, Consumer<ExecutionResult> next) {
		String name = context.invoked().name();

		for (Script script : scriptManager.enabled()) {
			if (matches(script, name)) {
				next.accept(ExecutionResult.ok(context.withScript(script)));
				return;
			}
		}

		next.accept(ExecutionResult.stop("No script found for command: " + name));
	}

	private boolean matches(Script script, String name) {
		if (script.name().equalsIgnoreCase(name))
			return true;
		if (script.aliases() == null)
			return false;
		for (String alias : script.aliases()) {
			if (alias.equalsIgnoreCase(name))
				return true;
		}
		return false;
	}
}
