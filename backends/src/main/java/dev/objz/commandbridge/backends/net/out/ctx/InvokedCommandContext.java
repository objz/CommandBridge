package dev.objz.commandbridge.backends.net.out.ctx;

import dev.jorel.commandapi.executors.CommandArguments;
import dev.objz.commandbridge.net.payloads.cmd.CommandStub;

import java.util.Objects;

import org.bukkit.command.CommandSender;

public final class InvokedCommandContext {

	public final String commandName;
	public final CommandSender sender;
	public final CommandArguments args;
	public final CommandStub stub;

	public InvokedCommandContext(
			String commandName,
			CommandSender sender,
			CommandArguments args,
			CommandStub stub) {
		this.commandName = Objects.requireNonNull(commandName, "commandName");
		this.sender = Objects.requireNonNull(sender, "sender");
		this.args = Objects.requireNonNull(args, "args");
		this.stub = Objects.requireNonNull(stub, "stub");
	}
}
