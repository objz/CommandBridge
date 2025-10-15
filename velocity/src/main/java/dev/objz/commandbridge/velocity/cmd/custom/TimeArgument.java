package dev.objz.commandbridge.velocity.cmd.custom;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jorel.commandapi.arguments.CommandAPIArgumentType;
import dev.jorel.commandapi.arguments.SafeOverrideableArgument;
import dev.jorel.commandapi.executors.CommandArguments;

/**
 * An argument that represents a duration of time in ticks for Velocity.
 * Uses IntegerArgumentType as the underlying Brigadier type.
 */
public class TimeArgument extends SafeOverrideableArgument<Integer, Integer> {

	/**
	 * A Time argument. Represents the number of in-game ticks
	 * 
	 * @param nodeName the name of the node for this argument
	 */
	public TimeArgument(String nodeName) {
		super(nodeName, IntegerArgumentType.integer(0), String::valueOf);
	}

	/**
	 * A Time argument with a minimum value
	 * 
	 * @param nodeName the name of the node for this argument
	 * @param min      The minimum value in ticks (inclusive)
	 */
	public TimeArgument(String nodeName, int min) {
		super(nodeName, IntegerArgumentType.integer(min), String::valueOf);
	}

	/**
	 * A Time argument with a minimum and maximum value
	 * 
	 * @param nodeName the name of the node for this argument
	 * @param min      The minimum value in ticks (inclusive)
	 * @param max      The maximum value in ticks (inclusive)
	 */
	public TimeArgument(String nodeName, int min, int max) {
		super(nodeName, IntegerArgumentType.integer(min, max), String::valueOf);
	}

	@Override
	public Class<Integer> getPrimitiveType() {
		return int.class;
	}

	@Override
	public CommandAPIArgumentType getArgumentType() {
		return CommandAPIArgumentType.TIME;
	}

	@Override
	public <CommandSourceStack> Integer parseArgument(CommandContext<CommandSourceStack> cmdCtx,
			String key,
			CommandArguments previousArgs)
			throws CommandSyntaxException {
		return cmdCtx.getArgument(key, Integer.class);
	}
}
