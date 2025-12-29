package dev.objz.commandbridge.velocity.net.out.ctx;

import dev.objz.commandbridge.scripting.model.enums.RunAs;
import dev.objz.commandbridge.velocity.net.session.ClientSession;

import java.util.Objects;
import java.util.UUID;

public record ExecuteCommandContext(
		ClientSession session,
		String command,
		RunAs runAs,
		UUID uuid) {
	public ExecuteCommandContext {
		Objects.requireNonNull(session);
		Objects.requireNonNull(command);
		Objects.requireNonNull(runAs);
		// uuid can be null for console
	}
}
