package dev.objz.commandbridge.main.proto.cmd;

import java.util.List;

public record RegisterCommandsPayload(
		boolean reload,
		List<CommandStub> commands) {
}
