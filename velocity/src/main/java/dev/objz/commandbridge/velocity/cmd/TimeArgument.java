package dev.objz.commandbridge.velocity.cmd;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.Message;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CommandAPIArgumentType;
import dev.jorel.commandapi.executors.CommandArguments;

//TODO: modify outgoing package to use minecraft:time instead of brigadier:string
//TODO: also implement other types for velocity that are originally missing in brigadier
public final class TimeArgument extends Argument<Integer> {

	public TimeArgument(String nodeName) {
		super(nodeName, StringArgumentType.word());

		replaceSuggestions(ArgumentSuggestions.strings(info -> {
			String input = info.currentArg().toLowerCase();

			if (input.isEmpty()) {
				return new String[0];
			}

			String numeric = input.replaceAll("[^0-9]", "");

			if (numeric.isEmpty()) {
				return new String[0];
			}

			if (!input.isEmpty() && Character.isDigit(input.charAt(input.length() - 1))) {
				return new String[] {
						numeric,
						numeric + "d",
						numeric + "s",
						numeric + "t"
				};
			}

			if (input.matches("\\d+[dst]")) {
				return new String[] { input };
			}

			return new String[0];
		}));
	}

	@Override
	public Class<Integer> getPrimitiveType() {
		return Integer.class;
	}

	@Override
	public CommandAPIArgumentType getArgumentType() {
		return CommandAPIArgumentType.PRIMITIVE_STRING;
	}

	@Override
	public <Source> Integer parseArgument(CommandContext<Source> cmdCtx, String key,
			CommandArguments previousArgs) throws CommandSyntaxException {
		String rawInput = cmdCtx.getArgument(key, String.class);

		if (rawInput == null || rawInput.isBlank()) {
			throw new SimpleCommandExceptionType(
					(Message) () -> "Time value cannot be empty").create();
		}

		final String input = rawInput.toLowerCase().trim();

		if (input.matches("\\d+")) {
			try {
				int value = Integer.parseInt(input);
				if (value < 0) {
					throw new SimpleCommandExceptionType(
							(Message) () -> "Time value must be non-negative").create();
				}
				return value;
			} catch (NumberFormatException e) {
				final String errorInput = input;
				throw new SimpleCommandExceptionType(
						(Message) () -> "Invalid number: " + errorInput).create();
			}
		}

		if (!input.matches("\\d+[dst]")) {
			throw new SimpleCommandExceptionType(
					(Message) () -> "Invalid time format.")
					.create();
		}

		char unit = input.charAt(input.length() - 1);
		String numericPart = input.substring(0, input.length() - 1);

		int value;
		try {
			value = Integer.parseInt(numericPart);
		} catch (NumberFormatException e) {
			final String errorPart = numericPart;
			throw new SimpleCommandExceptionType(
					(Message) () -> "Invalid number: " + errorPart).create();
		}

		if (value < 0) {
			throw new SimpleCommandExceptionType(
					(Message) () -> "Time value must be non negative").create();
		}

		return switch (unit) {
			case 'd' -> value * 24000;
			case 's' -> value * 20;
			case 't' -> value;
			default -> throw new SimpleCommandExceptionType(
					(Message) () -> "Unknown time unit: " + unit).create();
		};
	}
}
