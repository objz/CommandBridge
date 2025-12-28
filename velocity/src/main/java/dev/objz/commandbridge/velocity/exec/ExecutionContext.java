package dev.objz.commandbridge.velocity.exec;

import dev.objz.commandbridge.net.payloads.cmd.InvokedCommand;
import dev.objz.commandbridge.scripting.model.Script;
import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.util.Objects;

public record ExecutionContext(
		InvokedCommand invoked,
		ClientSession source,
		Script script) {
	public ExecutionContext {
		Objects.requireNonNull(invoked);
		Objects.requireNonNull(source);
	}

	public ExecutionContext(InvokedCommand invoked, ClientSession source) {
		this(invoked, source, null);
	}

	public ExecutionContext withScript(Script script) {
		return new ExecutionContext(this.invoked, this.source, script);
	}
}
