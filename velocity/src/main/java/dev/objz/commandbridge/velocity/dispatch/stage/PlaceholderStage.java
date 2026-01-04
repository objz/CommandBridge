package dev.objz.commandbridge.velocity.dispatch.stage;

import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionContext;
import dev.objz.commandbridge.velocity.dispatch.model.ExecutionResult;
import dev.objz.commandbridge.velocity.dispatch.model.Pipeline;

import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderStage implements Pipeline {

	private static final Pattern PATTERN = Pattern.compile("\\$\\{([^}]+)}");

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

		Matcher checkMatcher = PATTERN.matcher(rawCommand);
		if (!checkMatcher.find()) {
			next.accept(ExecutionResult.ok(context));
			return;
		}

		Map<String, Object> args = context.arguments();
		if (args == null) {
			args = Map.of();
		}

		Matcher matcher = PATTERN.matcher(rawCommand);
		StringBuilder sb = new StringBuilder();

		while (matcher.find()) {
			String key = matcher.group(1);
			if (key == null || key.isBlank()) {
				matcher.appendReplacement(sb, Matcher.quoteReplacement("${}"));
				continue;
			}

			Object value = args.get(key);
			String replacement;

			if (value == null) {
				replacement = "";
			} else {
				replacement = convertToString(value);
			}

			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(sb);

		String resolvedCommand = sb.toString();

		CmdMapping resolvedCmd = new CmdMapping(
				resolvedCommand,
				currentCmd.runAs(),
				currentCmd.execute(),
				currentCmd.server(),
				currentCmd.delay(),
				currentCmd.cooldown());

		next.accept(ExecutionResult.ok(context.nextCommand(resolvedCmd, context.commandIndex())));
	}

	private String convertToString(Object value) {
		if (value == null) {
			return "";
		}

		if (value instanceof java.util.Collection<?> collection) {
			if (collection.isEmpty()) {
				return "";
			}
			StringBuilder result = new StringBuilder();
			boolean first = true;
			for (Object item : collection) {
				if (!first) {
					result.append(" ");
				}
				first = false;
				result.append(itemToString(item));
			}
			return result.toString();
		}

		return String.valueOf(value);
	}

	private String itemToString(Object item) {
		if (item == null) {
			return "";
		}

		if (item instanceof dev.objz.commandbridge.cmd.ref.EntityRef ref) {
			return ref.name() != null ? ref.name() : ref.uuid();
		}

		if (item instanceof dev.objz.commandbridge.cmd.ref.Location3D loc) {
			return loc.x() + " " + loc.y() + " " + loc.z();
		}

		if (item instanceof dev.objz.commandbridge.cmd.ref.Location2D loc) {
			return loc.x() + " " + loc.y();
		}

		return String.valueOf(item);
	}
}
