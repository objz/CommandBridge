package dev.objz.commandbridge.net.payloads.cmd;

import java.util.List;

public record RegisterCommands(
		List<CommandStub> commands) {
}
