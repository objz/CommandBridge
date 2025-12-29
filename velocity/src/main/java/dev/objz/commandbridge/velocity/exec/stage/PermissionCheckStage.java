package dev.objz.commandbridge.velocity.exec.stage;

import dev.objz.commandbridge.scripting.model.Permissions;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.velocity.exec.ExecutionContext;
import dev.objz.commandbridge.velocity.exec.ExecutionResult;
import dev.objz.commandbridge.velocity.exec.Pipeline;
import dev.objz.commandbridge.velocity.util.MM;

import java.util.function.Consumer;

public final class PermissionCheckStage implements Pipeline {

	@Override
	public void process(ExecutionContext context, Consumer<ExecutionResult> next) {
		Script script = context.script();

		if (script == null) {
			next.accept(ExecutionResult.error("Script not resolved before permission check"));
			return;
		}

		Permissions perms = script.permissions();

		if (!perms.enabled()) {
			next.accept(ExecutionResult.ok(context));
			return;
		}

		String permissionNode = "commandbridge.command." + script.name();

		if (context.source().hasPermission(permissionNode)) {
			next.accept(ExecutionResult.ok(context));
			return;
		}

		if (!perms.silent()) {
			context.source().sendMessage(
					MM.parse("<red>You do not have permission to execute this command."));
		}

		next.accept(ExecutionResult.stop("Permission denied: " + permissionNode));
	}
}
