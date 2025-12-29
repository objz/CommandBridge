package dev.objz.commandbridge.velocity.exec;

import com.velocitypowered.api.command.CommandSource;
import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.scripting.model.records.mapping.CmdMapping;
import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.util.Map;

public record ExecutionContext(
		InvokedCommand invoked,
		ClientSession session,
		CommandSource source,
		Script script,
		Map<String, Object> arguments,
		CmdMapping currentCommand,
		int commandIndex) {

	public ExecutionContext withScript(Script script) {
		return new ExecutionContext(invoked, session, source, script, arguments, currentCommand, commandIndex);
	}

	public ExecutionContext withSource(CommandSource source) {
		return new ExecutionContext(invoked, session, source, script, arguments, currentCommand, commandIndex);
	}

	public ExecutionContext withArguments(Map<String, Object> args) {
		return new ExecutionContext(invoked, session, source, script, args, currentCommand, commandIndex);
	}

	public ExecutionContext nextCommand(CmdMapping cmd, int index) {
		return new ExecutionContext(invoked, session, source, script, arguments, cmd, index);
	}
}
