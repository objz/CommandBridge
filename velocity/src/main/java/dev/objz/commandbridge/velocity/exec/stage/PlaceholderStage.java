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

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

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

		// Check if there are any placeholders to resolve
		Matcher checkMatcher = PLACEHOLDER_PATTERN.matcher(rawCommand);
		if (!checkMatcher.find()) {
			// No placeholders in command, execute as-is
			next.accept(ExecutionResult.ok(context));
			return;
		}

		// Reset matcher for actual replacement
		Map<String, Object> args = context.arguments();
		if (args == null) {
			args = Map.of();
		}

		Matcher matcher = PLACEHOLDER_PATTERN.matcher(rawCommand);
		StringBuilder sb = new StringBuilder();

		while (matcher.find()) {
			String key = matcher.group(1);
			if (key == null || key.isBlank()) {
				// Empty placeholder ${}, skip replacement
				matcher.appendReplacement(sb, Matcher.quoteReplacement("${}"));
				continue;
			}

			Object value = args.get(key);
			String replacement;

			if (value == null) {
				// Argument not found or null - use empty string
				replacement = "";
			} else {
				// Convert value to string representation
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

		// Handle collections (like List<EntityRef> from PLAYERS/ENTITIES types)
		if (value instanceof java.util.Collection<?> collection) {
			if (collection.isEmpty()) {
				return "";
			}
			// Join collection elements with space
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

		// Handle EntityRef specifically
		if (item instanceof dev.objz.commandbridge.cmd.ref.EntityRef ref) {
			// Return the entity name for command usage
			return ref.name() != null ? ref.name() : ref.uuid();
		}

		// Handle Location3D
		if (item instanceof dev.objz.commandbridge.cmd.ref.Location3D loc) {
			return loc.x() + " " + loc.y() + " " + loc.z();
		}

		// Handle Location2D
		if (item instanceof dev.objz.commandbridge.cmd.ref.Location2D loc) {
			return loc.x() + " " + loc.y();
		}

		return String.valueOf(item);
	}
}
