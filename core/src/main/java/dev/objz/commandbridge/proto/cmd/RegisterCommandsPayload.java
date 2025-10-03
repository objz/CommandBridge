package dev.objz.commandbridge.proto.cmd;

import java.util.List;

public record RegisterCommandsPayload(
		boolean reload,
		List<CommandStub> commands) {
}
