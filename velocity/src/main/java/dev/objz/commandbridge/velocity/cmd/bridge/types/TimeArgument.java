package dev.objz.commandbridge.velocity.cmd.bridge.types;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.Message;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CommandAPIArgumentType;
import dev.jorel.commandapi.executors.CommandArguments;

public final class TimeArgument extends Argument<Integer> {

    public TimeArgument(String nodeName) {
        super(nodeName, StringArgumentType::word);

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
        return CommandAPIArgumentType.TIME;
    }

    @Override
    public <Source> Integer parseArgument(CommandContext<Source> cmdCtx, String key,
            CommandArguments previousArgs) throws CommandSyntaxException {
        try {
            return TimeParser.ticks(cmdCtx.getArgument(key, String.class));
        } catch (IllegalArgumentException e) {
            throw new SimpleCommandExceptionType((Message) e::getMessage).create();
        }
    }
}
