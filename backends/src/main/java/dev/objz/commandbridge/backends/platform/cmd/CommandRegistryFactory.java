package dev.objz.commandbridge.backends.platform.cmd;

import dev.objz.commandbridge.cmd.ArgumentMapper;
import dev.objz.commandbridge.cmd.CommandAPIRegistry;
import dev.objz.commandbridge.cmd.CommandRegistry;
import dev.objz.commandbridge.scripting.model.enums.Location;

import java.util.function.BiConsumer;

public final class CommandRegistryFactory {
	
	private CommandRegistryFactory() {}
	
	/**
	 * Creates the appropriate CommandRegistry based on the location.
	 * 
	 * @param argumentMapper The argument mapper for the platform
	 * @param location The location where commands will be registered
	 * @param executionLogger Optional logger for command execution
	 * @return CommandRegistry instance for the specified location
	 */
	public static CommandRegistry create(
			ArgumentMapper argumentMapper, 
			Location location, 
			BiConsumer<String, Object> executionLogger) {
		
		if (location == Location.BACKEND) {
			return new BackendCommandAPIRegistry(argumentMapper, executionLogger);
		} else {
			// VELOCITY or any other location uses the base implementation
			return new CommandAPIRegistry(argumentMapper, location, executionLogger);
		}
	}
	
	/**
	 * Creates the appropriate CommandRegistry based on the location.
	 * Uses default execution logger.
	 * 
	 * @param argumentMapper The argument mapper for the platform
	 * @param location The location where commands will be registered
	 * @return CommandRegistry instance for the specified location
	 */
	public static CommandRegistry create(ArgumentMapper argumentMapper, Location location) {
		return create(argumentMapper, location, null);
	}
}
