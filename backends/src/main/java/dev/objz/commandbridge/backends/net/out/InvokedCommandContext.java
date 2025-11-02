package dev.objz.commandbridge.backends.net.out;

import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.net.payloads.cmd.SenderContext;

import java.util.List;
import java.util.Objects;

/**
 * Context for invoked command events.
 */
public final class InvokedCommandContext {
	public final String commandName;
	public final List<InvokedCommand.TypedArgument> arguments;
	public final SenderContext sender;

	public InvokedCommandContext(String commandName, 
								 List<InvokedCommand.TypedArgument> arguments, 
								 SenderContext sender) {
		this.commandName = Objects.requireNonNull(commandName);
		this.arguments = Objects.requireNonNullElseGet(arguments, List::of);
		this.sender = Objects.requireNonNull(sender);
	}
}
