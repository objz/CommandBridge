package dev.objz.commandbridge.velocity.exec.stage;

import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.velocity.exec.ExecutionContext;
import dev.objz.commandbridge.velocity.exec.ExecutionResult;
import dev.objz.commandbridge.velocity.exec.Pipeline;

import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderStage implements Pipeline {

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]*)}");

	@Override
	public void process(ExecutionContext context, Consumer<ExecutionResult> next) {
		CmdMapping currentCmd = context.currentCommand();
		if (currentCmd == null) {
			next.accept(ExecutionResult.ok(context));
			return;
		}

		String rawCommand = currentCmd.command();
		if (rawCommand == null || rawCommand.isBlank()) {
			next.accept(ExecutionResult.ok(context));
			return;
		}

		Map<String, Object> args = context.arguments();
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(rawCommand);
		StringBuilder sb = new StringBuilder();

		while (matcher.find()) {
			String key = matcher.group(1);
			Object value = args.get(key);
			String replacement = value != null ? String.valueOf(value) : "";
			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(sb);

		CmdMapping resolvedCmd = new CmdMapping(
				sb.toString(),
				currentCmd.runAs(),
				currentCmd.execute(),
				currentCmd.server(),
				currentCmd.delay(),
				currentCmd.cooldown());

		next.accept(ExecutionResult.ok(context.nextCommand(resolvedCmd, context.commandIndex())));
	}
}
