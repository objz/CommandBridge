package dev.objz.commandbridge.velocity.exec.stage;

import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.velocity.ScriptManager;
import dev.objz.commandbridge.velocity.exec.ExecutionContext;
import dev.objz.commandbridge.velocity.exec.ExecutionResult;

import java.util.Objects;

public final class ScriptMapper implements Pipeline {

	private final ScriptManager scriptManager;

	public ScriptMapper(ScriptManager scriptManager) {
		this.scriptManager = Objects.requireNonNull(scriptManager, "ScriptManager cannot be null");
	}

	@Override
	public int order() {
		return 100;
	}

	@Override
	public ExecutionResult process(ExecutionContext context) {
		String commandName = context.invoked().name();

		for (Script script : scriptManager.enabled()) {
			if (matches(script, commandName)) {
				return ExecutionResult.next(context.withScript(script));
			}
		}

		return ExecutionResult.stop("No enabled script found for command: " + commandName);
	}

	private boolean matches(Script script, String commandName) {
		if (script.name().equalsIgnoreCase(commandName)) {
			return true;
		}

		if (script.aliases() != null) {
			for (String alias : script.aliases()) {
				if (alias.equalsIgnoreCase(commandName)) {
					return true;
				}
			}
		}

		return false;
	}
}
